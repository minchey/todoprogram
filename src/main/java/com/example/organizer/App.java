package com.example.organizer;

import javafx.application.Application;  // JavaFX 앱의 진입점을 정의하는 부모 클래스
import javafx.stage.Stage;              // 최상위 윈도우(창) 클래스
import javafx.scene.Scene;              // 하나의 장면(Scene) - 화면 레이아웃을 담는 컨테이너
import javafx.scene.control.Label;      // UI 요소 중 하나: 텍스트 표시용

// JavaFX 앱은 반드시 Application 클래스를 상속해야 함
public class App extends Application{
    @Override
    public void start(Stage stage){
        Database.migrate();
        // JavaFX가 프로그램 실행 시 자동으로 호출하는 메서드
        // stage: 최상위 창(윈도우)
        Label label = new Label("Hello Organizer!");     // 화면에 보일 텍스트 라벨 하나 생성
        Scene scene = new Scene(label,400,200);      // 장면(Scene) 생성: 라벨을 올려놓고, 가로 400px 세로 200px 크기로 설정
        stage.setScene(scene);                              // stage(윈도우)에 scene(장면)을 붙임
        stage.setTitle("TodoProgram");                      // 창의 제목 표시줄에 보일 문자열 설정
        stage.show();                                       // 실제 화면에 stage(창)를 띄움
        TaskDao dao = new TaskDao();
        dao.addTask("보고서 작성", 1, "2025-09-12T09:00"); // 마감 있는 업무
        dao.addTask("아이디어 메모", 2, null);             // 마감 없는 업무

    }

    public static void main(String[] args) {
        launch(args);                                        // Application.launch() → 내부적으로 JavaFX 앱 초기화 후 start(Stage) 호출
    }
}
