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

    /**
     * 특정 날짜에 반복업무가 있는지 확인
     */
    public boolean hasRecurringOn(LocalDate date) {
        String sql = """
        SELECT COUNT(*) FROM tasks
        WHERE is_recurring = 1
          AND (recur_days & ?) != 0            -- 요일 비트가 겹치면 true
          AND (recur_start IS NULL OR recur_start <= ?)
          AND (recur_until IS NULL OR recur_until >= ?)
    """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // DayOfWeek: 월=1 … 일=7  → 일=bit0, 월=bit1 … 토=bit6 으로 매핑
            int dow = date.getDayOfWeek().getValue();   // 1~7 (월~일)
            int dowMask = 1 << (dow % 7);               // 월(1)->2, … 일(7%7=0)->1

            String ymd = date.toString();               // "YYYY-MM-DD"
            ps.setInt(1, dowMask);
            ps.setString(2, ymd);
            ps.setString(3, ymd);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 특정 날짜(YYYY-MM-DD)에 "반복 업무"가 해당되는 목록을 조회한다.
     *
     * 동작 요약
     * 1) 날짜의 요일을 구한다 (Java: 월=1 … 일=7)
     * 2) DB에 저장된 반복 요일 비트마스크(recur_days)와 & 연산으로 일치 여부를 판단
     *    - 비트 규칙: 일=bit0, 월=bit1, … 토=bit6  (예: 월/수/금 → 0b0101010 = 42)
     * 3) 반복 유효 기간(recur_start ≤ date ≤ recur_until)도 함께 필터링
     * 4) 일치하는 반복 업무들의 최소 필드(id, title, priority, is_recurring)를 Task로 구성해 반환
     *
     * @param date 확인할 날짜 (예: LocalDate.of(2025, 9, 15))
     * @return 해당 날짜에 반복 규칙과 기간이 맞는 Task 목록(반복 업무만)
     */
    public List<Task> listRecurringByDate(LocalDate date) {
        // 조회 결과를 담을 리스트 준비
        List<Task> out = new ArrayList<>();

        // (A) 이 날짜의 요일 비트 준비 -----------------------------
        // Java 요일값: 월=1, 화=2, … 일=7
        int javaDow = date.getDayOfWeek().getValue();

        // 비트 인덱스 변환:
        //  - 우리가 정한 비트 규칙은 "일=bit0, 월=bit1, … 토=bit6"
        //  - Java의 일요일 값은 7이므로, 7 % 7 = 0 으로 만들어 bit0에 매핑
        //  - 월(1)→1%7=1→bit1, 화(2)→bit2 … 토(6)→bit6
        int maskBitIndex = javaDow % 7;

        // 최종 요일 마스크: 해당 날짜의 요일에 해당하는 1비트만 켠 값
        //  예) 월요일이면 1<<1 = 0b10(2), 일요일이면 1<<0 = 0b1(1)
        int dowMask = 1 << maskBitIndex;

        // (B) 하루 문자열(YYYY-MM-DD) 준비 → 기간 비교에 사용
        String ymd = date.toString();

        // (C) 반복 업무 조회 SQL -----------------------------------
        //  - is_recurring = 1 : 반복 업무만
        //  - (recur_days & ?) != 0 : 요일 비트가 겹치는(해당 요일에 수행되는) 것만
        //  - 기간 조건: 시작일이 비어있거나 시작일 ≤ date, 종료일이 비어있거나 date ≤ 종료일
        final String sql = """
        SELECT id, title, priority, is_recurring
        FROM tasks
        WHERE is_recurring = 1
          AND (recur_days & ?) != 0
          AND (recur_start IS NULL OR recur_start <= ?)
          AND (recur_until IS NULL OR recur_until >= ?)
        ORDER BY priority ASC, title ASC
    """;

        // (D) DB 연결/실행 -----------------------------------------
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 바인딩 #1: 요일 비트마스크
            ps.setInt(1, dowMask);

            // 바인딩 #2, #3: 기간 필터에 사용할 날짜 문자열(YYYY-MM-DD)
            ps.setString(2, ymd); // recur_start ≤ date
            ps.setString(3, ymd); // date ≤ recur_until

            // 실행 및 결과 매핑
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task t = new Task();

                    // 필요한 최소 필드만 매핑 (UI에 제목/우선순위/반복태그를 보여주기 위함)
                    t.id          = rs.getInt("id");
                    t.title       = rs.getString("title");
                    t.priority    = rs.getInt("priority");
                    t.isRecurring = rs.getInt("is_recurring"); // 1 고정

                    // 필요하면 여기서 추가 필드도 매핑 가능:
                    //   recur_days, recur_start, recur_until, recur_interval 등

                    out.add(t);
                }
            }

        } catch (SQLException e) {
            // 실사용에선 로깅 권장. 여기서는 콘솔에 출력 후 빈 목록 반환.
            e.printStackTrace();
        }

        return out;
    }

}



