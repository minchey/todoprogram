package com.example.organizer;

/**
 * DB <-> 자바 변환용 Task 모델
 * - DB의 INTEGER(0/1) 대신 코드에서는 boolean completed로 다룸
 */
public class Task {
    // === DB 컬럼 ===
    public int id;               // PRIMARY KEY
    public String title;         // 제목
    public int priority;         // 1=High, 2=Medium, 3=Low
    public String dueAt;         // "YYYY-MM-DD" 또는 "YYYY-MM-DD HH:mm"
    public int isRecurring;      // 0/1
    public String nextFireAt;    // 다음 알림
    public String createdAt;     // 생성시각
    public boolean completed;    // ✅ 완료 여부

    // === UI에서 쓰기 쉬운 헬퍼 ===
    public int getId() { return id; }
    public String getTitle() { return title; }
    public int getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean v) { this.completed = v; }

    @Override public String toString() {
        return String.format("[%s] %s(prio=%d, due=%s, done=%s)",
                (isRecurring==1?"반복":"단발"), title, priority, dueAt, completed);
    }
}
