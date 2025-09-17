package com.example.organizer;

import java.sql.*;
import java.io.File;

/**
 * Database 유틸리티 클래스
 * - SQLite 파일(todo.db) 연결
 * - 앱 시작 시 테이블/컬럼 자동 생성 (migrate 메서드)
 * - 컬럼이 없을 경우 안전하게 ALTER TABLE로 보강
 */
public class Database {

    private static final String DB_URL = "jdbc:sqlite:" + userDbPath();
    // SQLite 파일 경로 (프로젝트 실행 폴더에 todo.db 생성됨)
    //private static final String DB_URL = "jdbc:sqlite:todo.db";

    /**
     * DB 연결을 반환하는 메서드
     * - Connection을 얻어서 SQL 실행에 사용
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 앱 시작 시 호출
     * 1) tasks 테이블이 없으면 생성
     * 2) 반복 업무 관련 컬럼이 없으면 추가
     */
    public static void migrate() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            // 1) 기본 테이블 생성 (없으면 새로 만듦)
            st.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,   -- 고유 ID
                    title TEXT NOT NULL,                    -- 할 일 제목
                    priority INTEGER NOT NULL,              -- 우선순위 (1=High, 2=Medium, 3=Low)
                    due_at TEXT,                            -- 마감일(단발성 일정용, "YYYY-MM-DD" 또는 datetime)
                    is_recurring INTEGER DEFAULT 0,         -- 반복 여부 (0=단발, 1=반복)
                    next_fire_at TEXT,                      -- 다음 알림 시간 (ISO 문자열)
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP, -- 생성 시각
                    completed INTEGER DEFAULT 0             --  완료 여부(0/1)
                )
            """);

            // 2) 반복업무용 컬럼 보강 (없으면 추가)
            addColumnIfMissing(conn, "tasks", "completed",     "INTEGER", "0");
            addColumnIfMissing(conn, "tasks", "recur_days",     "INTEGER", "0");   // 요일 비트마스크
            addColumnIfMissing(conn, "tasks", "recur_start",    "TEXT",    null);  // 반복 시작일
            addColumnIfMissing(conn, "tasks", "recur_until",    "TEXT",    null);  // 반복 종료일
            addColumnIfMissing(conn, "tasks", "recur_interval", "INTEGER", "1");   // 반복 간격(주 단위)

        } catch (SQLException e) {
            throw new RuntimeException("DB migrate 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 테이블에 특정 컬럼이 없으면 ALTER TABLE로 추가
     *
     * @param conn      DB 연결
     * @param table     테이블 이름
     * @param column    추가할 컬럼 이름
     * @param type      데이터 타입 (예: TEXT, INTEGER)
     * @param defaultVal 기본값 (null이면 DEFAULT 없이 추가)
     */
    private static void addColumnIfMissing(Connection conn, String table, String column, String type, String defaultVal)
            throws SQLException {
        if (columnExists(conn, table, column)) return; // 이미 있으면 skip

        // ALTER TABLE 쿼리 작성
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(table)
                .append(" ADD COLUMN ").append(column).append(" ").append(type);
        if (defaultVal != null) {
            sb.append(" DEFAULT ").append(defaultVal);
        }

        try (Statement st = conn.createStatement()) {
            st.execute(sb.toString());
            System.out.println("[DB] 컬럼 추가: " + table + "." + column + " (" + type +
                    (defaultVal != null ? " DEFAULT " + defaultVal : "") + ")");
        }
    }

    /**
     * PRAGMA table_info를 이용해서
     * 특정 테이블에 컬럼이 이미 존재하는지 확인
     */
    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        String pragma = "PRAGMA table_info(" + table + ")";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(pragma)) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (column.equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }
    // Database.java
    private static String userDbPath() {
        // Windows: %APPDATA%\TodoProgram\todo.db
        String appData = System.getenv("APPDATA");
        String base = (appData != null && !appData.isBlank())
                ? appData + File.separator + "TodoProgram"
                : System.getProperty("user.home") + File.separator + ".todoprogram";
        new File(base).mkdirs();
        return base + File.separator + "todo.db";
    }



}
