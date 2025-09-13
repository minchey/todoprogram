package com.example.organizer;

import java.sql.*;

public class TaskDao {

    /**
     * 새로운 할 일을 DB에 추가
     * @param title     할 일 제목
     * @param priority  우선순위 (1=High, 2=Medium, 3=Low)
     * @param dueAt     마감일 (없으면 null)
     */

    public void addTask(String title, int priority, String dueAt){
        String sql = "INSERT INTO tasks(title, priority, due_at, is_recurring, next_fire_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1,title);
            ps.setInt(2,priority);

            if(dueAt != null){
                ps.setString(3,dueAt);
                ps.setInt(4,0);
                ps.setString(5,dueAt);
            }else {
                ps.setNull(3, Types.VARCHAR); //due_at 없음
                ps.setNull(4,0);
                ps.setNull(5, Types.VARCHAR); //next_fire_at 없음
            }

            ps.executeUpdate();
            System.out.println("[DB] Task 추가 완료: " + title);

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void updateTask(Task task){
        String sql = "UPDATE tasks SET title = ?, priority =?, due_at =?, is_recurring=?, next_fire_at=? WHERE id=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)){

            ps.setString(1,task.title);
            ps.setInt(2,task.priority);

            if (task.dueAt != null) {
                ps.setString(3, task.dueAt);
            } else {
                ps.setNull(3, java.sql.Types.VARCHAR);
            }

            ps.setInt(4, task.isRecurring);

            if (task.nextFireAt != null) {
                ps.setString(5, task.nextFireAt);
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }

            ps.setInt(6, task.id);

            ps.executeUpdate();
            System.out.println("[DB] Task 업데이트 완료: " + task.id);

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void deleteTask(Task task){
        if (task == null || task.id == 0) {
            throw new IllegalArgumentException("삭제할 Task의 id가 필요합니다.");
        }
        String sql = "DELETE FROM task WHERE = id=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, task.id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.out.println("[DB] 삭제할 행이 없습니다. id=" + task.id);
            } else {
                System.out.println("[DB] Task 삭제 완료. id=" + task.id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("deleteTask 실패: " + e.getMessage(), e);
        }
    }
}
