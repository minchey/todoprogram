package com.example.organizer;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

public class TaskDao {

    /**
     * DB에 새로운 할 일을 추가하는 메서드 (CREATE)
     *
     * @param title    할 일 제목
     * @param priority 우선순위 (1=High, 2=Medium, 3=Low)
     * @param dueAt    마감일 (없으면 null)
     */
    public void addTask(String title, int priority, String dueAt) {
        // SQL 문: tasks 테이블에 새 행 추가
        String sql = "INSERT INTO tasks(title, priority, due_at, is_recurring, next_fire_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();               // DB 연결
             PreparedStatement ps = conn.prepareStatement(sql)) {      // SQL 준비

            // 1번째 ? → 제목
            ps.setString(1, title);
            // 2번째 ? → 우선순위
            ps.setInt(2, priority);

            if (dueAt != null) {
                // 3번째 ? → 마감일 (문자열로 저장)
                ps.setString(3, dueAt);
                // 4번째 ? → 반복여부 (단발성: 0)
                ps.setInt(4, 0);
                // 5번째 ? → 알림시간 (단발성은 마감일과 동일)
                ps.setString(5, dueAt);
            } else {
                // 마감일이 없을 때 null 처리
                ps.setNull(3, Types.VARCHAR);    // due_at
                ps.setInt(4, 0);                 // 반복 없음
                ps.setNull(5, Types.VARCHAR);    // next_fire_at
            }

            // SQL 실행 (INSERT 수행)
            ps.executeUpdate();
            System.out.println("[DB] Task 추가 완료: " + title);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 기존 Task를 수정하는 메서드 (UPDATE)
     *
     * @param task Task 객체 (id 포함)
     */
    public void updateTask(Task task) {
        String sql = "UPDATE tasks SET title=?, priority=?, due_at=?, is_recurring=?, next_fire_at=? WHERE id=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1번째 ? → 제목
            ps.setString(1, task.title);
            // 2번째 ? → 우선순위
            ps.setInt(2, task.priority);

            // 3번째 ? → 마감일 (있으면 값, 없으면 null)
            if (task.dueAt != null) {
                ps.setString(3, task.dueAt);
            } else {
                ps.setNull(3, Types.VARCHAR);
            }

            // 4번째 ? → 반복 여부
            ps.setInt(4, task.isRecurring);

            // 5번째 ? → 알림 시간 (있으면 값, 없으면 null)
            if (task.nextFireAt != null) {
                ps.setString(5, task.nextFireAt);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            // 6번째 ? → WHERE id=?
            ps.setInt(6, task.id);

            // SQL 실행 (UPDATE 수행)
            ps.executeUpdate();
            System.out.println("[DB] Task 업데이트 완료: " + task.id);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 특정 Task 객체를 삭제하는 메서드 (DELETE)
     *
     * @param task Task 객체 (id 필요)
     */
    public void deleteTask(Task task) {
        // Task가 없거나 id가 0이면 삭제 불가
        if (task == null || task.id == 0) {
            throw new IllegalArgumentException("삭제할 Task의 id가 필요합니다.");
        }
        String sql = "DELETE FROM tasks WHERE id = ?";  // DELETE 문법

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1번째 ? → 삭제할 Task의 id
            ps.setInt(1, task.id);

            // SQL 실행 (DELETE 수행)
            int rows = ps.executeUpdate();

            // 삭제된 행 수 확인
            if (rows == 0) {
                System.out.println("[DB] 삭제할 행이 없습니다. id=" + task.id);
            } else {
                System.out.println("[DB] Task 삭제 완료. id=" + task.id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("deleteTask 실패: " + e.getMessage(), e);
        }
    }

    /**
     * id만으로 Task를 삭제하는 편의 메서드 (DELETE)
     *
     * @param id 삭제할 Task id
     */
    public void deleteById(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("[DB] Task 삭제 완료. id=" + id);

        } catch (SQLException e) {
            throw new RuntimeException("deleteById 실패: " + e.getMessage(), e);
        }
    }


    //특정날짜의 할 일 가져오기
    public List<Task> listByDate(LocalDate date) {
        String ymd = date.toString(); // "YYYY-MM-DD"
        String sql = """
                    SELECT id, title, priority, due_at, is_recurring, next_fire_at, created_at
                    FROM tasks
                    WHERE due_at IS NOT NULL AND substr(due_at, 1, 10) = ?
                    ORDER BY priority, due_at
                """;

        List<Task> out = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ymd);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task t = new Task();
                    t.id = rs.getInt("id");
                    t.title = rs.getString("title");
                    t.priority = rs.getInt("priority");
                    t.dueAt = rs.getString("due_at");
                    t.isRecurring = rs.getInt("is_recurring");
                    t.nextFireAt = rs.getString("next_fire_at");
                    t.createdAt = rs.getString("created_at");
                    out.add(t);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * 한 달치 달력에 표시할 "날짜별 할 일 개수"를 가져오는 메서드
     *
     * @param ym YearMonth 객체 (예: 2025-09)
     * @return Map<LocalDate, Integer> 날짜별 할 일 개수
     */
    public Map<LocalDate, Integer> getDailyCountsForMonth(YearMonth ym) {

        // 예: YearMonth = 2025-09 → "2025-09" (문자열)
        String ymPrefix = ym.toString();

        // SQL: 해당 달(YYYY-MM)로 시작하는 due_at 날짜만 가져오기
        String sql = """
                SELECT substr(due_at,1,10) AS ymd, COUNT(*) AS cnt
                FROM tasks
                WHERE due_at LIKE ? || '%'
                GROUP BY ymd
                """;

        // 결과를 담을 Map (날짜별 → 할 일 개수)
        Map<LocalDate, Integer> map = new HashMap<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // "YYYY-MM%" 조건 넣기
            ps.setString(1, ymPrefix);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // DB에서 문자열("YYYY-MM-DD") → LocalDate로 변환
                    LocalDate date = LocalDate.parse(rs.getString("ymd"));
                    // 해당 날짜의 할 일 개수
                    int count = rs.getInt("cnt");

                    // Map에 저장
                    map.put(date, count);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 날짜별 할 일 개수 반환
        return map;
    }

    /**
     * 반복 업무 저장 (주간 반복)
     *
     * @param title         제목 (필수)
     * @param priority      우선순위 (1=중요, 2=보통, 3=낮음)
     * @param daysMask      요일 비트마스크 (일=bit0 … 토=bit6) — 최소 1비트는 켜져 있어야 함
     * @param recurStart    반복 시작일 ("YYYY-MM-DD")
     * @param recurUntil    반복 종료일 (없으면 null → 무기한)
     * @param intervalWeeks 몇 주 간격으로 반복할지 (기본 1주)
     * @param timeHHmm      시간 문자열 ("HH:mm" 권장, 비우면 종일) — 지금 단계에선 저장만, 트리거 계산은 다음 단계
     */
    public void addRecurringTask(String title,
                                 int priority,
                                 int daysMask,
                                 String recurStart,
                                 String recurUntil,
                                 int intervalWeeks,
                                 String timeHHmm) {

        // --- 1) 입력값 가벼운 검증 ---
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제목(title)은 필수입니다.");
        }
        if (priority < 1 || priority > 3) {
            throw new IllegalArgumentException("priority는 1~3 범위여야 합니다.");
        }
        if (daysMask == 0) {
            throw new IllegalArgumentException("반복 요일(daysMask)이 최소 하나는 선택되어야 합니다.");
        }
        if (recurStart == null || recurStart.isBlank()) {
            throw new IllegalArgumentException("recurStart(반복 시작일)은 필수입니다. 예) 2025-09-14");
        }
        if (intervalWeeks < 1) {
            intervalWeeks = 1; // 방어적 기본값
        }

        // --- 2) INSERT SQL (is_recurring=1, due_at/next_fire_at은 지금은 NULL로 둠) ---
        final String sql = """
        INSERT INTO tasks
            (title, priority, is_recurring, recur_days, recur_start, recur_until, recur_interval, due_at, next_fire_at)
        VALUES
            (?,     ?,        1,           ?,          ?,          ?,           ?,             NULL,  NULL)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            ps.setString(i++, title);
            ps.setInt(i++, priority);
            ps.setInt(i++, daysMask);
            ps.setString(i++, recurStart);

            // recur_until 은 null 가능
            if (recurUntil != null && !recurUntil.isBlank()) {
                ps.setString(i++, recurUntil);
            } else {
                ps.setNull(i++, Types.VARCHAR);
            }

            ps.setInt(i++, intervalWeeks);

            // 실행
            ps.executeUpdate();
            System.out.println("[DB] 반복 업무 추가 완료: title=" + title +
                    ", mask=" + daysMask + ", start=" + recurStart +
                    (recurUntil != null ? ", until=" + recurUntil : "") +
                    ", every " + intervalWeeks + " week(s)");

        } catch (SQLException e) {
            throw new RuntimeException("addRecurringTask 실패: " + e.getMessage(), e);
        }
    }

}



