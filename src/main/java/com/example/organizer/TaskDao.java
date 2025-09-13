package com.example.organizer;

import java.sql.*;

public class TaskDao {

    /**
     * DB에 새로운 할 일을 추가하는 메서드 (CREATE)
     * @param title     할 일 제목
     * @param priority  우선순위 (1=High, 2=Medium, 3=Low)
     * @param dueAt     마감일 (없으면 null)
     */
    public void addTask(String title, int priority, String dueAt){
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

        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * 기존 Task를 수정하는 메서드 (UPDATE)
     * @param task Task 객체 (id 포함)
     */
    public void updateTask(Task task){
        String sql = "UPDATE tasks SET title=?, priority=?, due_at=?, is_recurring=?, next_fire_at=? WHERE id=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){

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

        } catch(SQLException e){
            e.printStackTrace();
        }
    }

    /**
     * 특정 Task 객체를 삭제하는 메서드 (DELETE)
     * @param task Task 객체 (id 필요)
     */
    public void deleteTask(Task task){
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
}
