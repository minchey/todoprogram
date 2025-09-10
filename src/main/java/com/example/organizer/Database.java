package com.example.organizer;

import java.sql.Connection;        //DB 소켓같은것. 연결 표현하는 클래스
import java.sql.DriverManager;     //DB와 실제 연결을 만들어주는 클래스. 문열어주는 관리자 같은 것
import java.sql.SQLException;      //DB 작업 중 예외처리
import java.sql.Statement;

public class Database {
    private static String DB_URL = "jdbc:sqlite:todo.db";    // Java Database Connectivity - 자바와 db가 통신 : 어떤db쓸건지 : 파일경로

    private static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(DB_URL);
    }

    private static void migrate(){
        String sql = """
                CREATE TABLE IF NOT EXISTS tasks(
                    id              INTEGER PRIMARY KEY AUTOINCREMENT, 
                    title           TEXT NOT NULL,
                    priority        INTEGER DEFAULT 2,
                    due_at          TEXT,
                    is_recurring    INTEGER DEFAULT 0,
                    next_fire_at    TEXT,
                    create_at       TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()){
            stmt.execute(sql);
            System.out.println("[DB] migrate OK");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
}
