package com.example.organizer;


//DB 데이터 <-> 자바 코드 변환용 클래스
public class Task {
    public int id;          // 고유 ID (PRIMARY KEY)
    public String title;    // 할 일 제목
    public int priority;    // 우선순위 (1=High, 2=Medium, 3=Low)
    public String dueAt;    // 마감 시각 (문자열로 저장: "2025-09-12T09:00")
    public int isRecurring; // 반복 여부 (0=단발, 1=반복)
    public String nextFireAt; // 다음 알림 시간
    public String createdAt;  // 등록된 시간 (DB에서 자동 채워짐)
}
