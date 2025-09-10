package com.example.organizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

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
}
