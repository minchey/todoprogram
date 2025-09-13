package com.example.organizer;

/*
JavaFX 관련 → javafx.application, javafx.scene, javafx.stage, javafx.control, javafx.layout, javafx.geometry

날짜/시간 관련 → java.time.LocalDate, java.time.YearMonth

컬렉션(자료구조) → java.util.List, java.util.Map, java.util.HashMap

DB 관련 → java.sql.*
 */
import javafx.application.Application;  // JavaFX 앱의 진입점을 정의하는 부모 클래스
import javafx.stage.Stage;              // 최상위 윈도우(창) 클래스
import javafx.scene.Scene;              // 하나의 장면(Scene) - 화면 레이아웃을 담는 컨테이너
import javafx.scene.control.*;          // 튼(Button), 레이블(Label), 리스트(ListView), 입력창(TextField) 같은 UI 위젯 모음
import javafx.scene.layout.*;           // 화면 배치(Layout) 관련 클래스 모음 - BorderPane, VBox, HBox, GridPane 등
import javafx.geometry.Insets;          // 여백 설정에 필요
import javafx.geometry.Pos;             // 정렬 위치 같은 상수 제공 (ex Pos.CENTER)
import javafx.stage.Modality;           // 모달 대화상자용


import java.time.LocalDate;             // 특정한 날짜
import java.time.YearMonth;             // 연-월 만 저장 (달력 그릴 때 유용)
import java.util.List;                  // 여러개의 Task를 모아 담을 때 (하루 일정 리스트)
import java.util.Map;                   // 날짜별 일정 개수 저장
import java.util.HashMap;               // Map의 대표 구현체
import java.sql.*;                      //데이터베이스 연결





// JavaFX 앱은 반드시 Application 클래스를 상속해야 함
public class App extends Application{

    //dao: 데이터베이스와 대화하는 객체
    private final TaskDao dao = new TaskDao();

    // 지금 보고 있는 달
    private YearMonth currentMonth = YearMonth.now();

    // 달력 그리드와 현재 월 표시 라벨
    private GridPane calendarGrid;
    private Label monthLabel;

    @Override
    public void start(Stage stage){
        Database.migrate(); //db 테이블 생성 보장
        // JavaFX가 프로그램 실행 시 자동으로 호출하는 메서드

        // 왼쪽 NAV (카테고리 메뉴)
        ListView<String> nav = new ListView<>();
        nav.getItems().addAll("전체", "중요(1)", "보통(2)", "낮음(3)");
        nav.getSelectionModel().selectFirst();

        // 상단 월 이동 바
        Button prev = new Button("〈");
        Button next = new Button("〉");
        monthLabel = new Label(formatMonth(currentMonth));
        HBox monthBar = new HBox(10, prev, monthLabel, next);
        monthBar.setAlignment(Pos.CENTER);
        monthBar.setPadding(new Insets(8));

        // 달력 GridPane 생성
        calendarGrid = new GridPane();
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);
        calendarGrid.setPadding(new Insets(10));

        // 초기 달력 렌더링
        renderCalendar(currentMonth);

        // 월 이동 버튼 이벤트
        prev.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            monthLabel.setText(formatMonth(currentMonth));
            renderCalendar(currentMonth);
        });
        next.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            monthLabel.setText(formatMonth(currentMonth));
            renderCalendar(currentMonth);
        });

        // 전체 레이아웃 (BorderPane)
        // stage: 최상위 창(윈도우)

        nav.setPrefWidth(140);
        BorderPane root = new BorderPane();
        root.setLeft(nav);
        root.setTop(monthBar);
        root.setCenter(new ScrollPane(calendarGrid));

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("Todo Program");
        stage.setScene(scene);
        stage.show();



    }

    //달력 표시 텍스트 포맷
    private String formatMonth(YearMonth ym) {
        return ym.getYear() + "년 " + ym.getMonthValue() + "월";
    }

    //달력 그리기
    // 월~일 헤더 기준 렌더 (빈칸/레이아웃 깨짐 방지 강화 버전)
    private void renderCalendar(YearMonth ym) {
        // 1) 기존 내용/제약 초기화 (누적 방지)
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // 2) 날짜별 할 일 개수(점 표시용) 미리 가져오기
        Map<LocalDate, Integer> counts = dao.getDailyCountsForMonth(ym);

        // 3) 요일 헤더 (0행)
        String[] wk = {"월","화","수","목","금","토","일"};
        for (int i = 0; i < 7; i++) {
            Label head = new Label(wk[i]);
            head.setStyle("-fx-font-weight: bold;");
            calendarGrid.add(head, i, 0);
        }

        // 4) 이번 달 1일 위치 계산
        LocalDate first = ym.atDay(1);
        // DayOfWeek.getValue(): 월=1 … 일=7
        // 우리 헤더가 월~일이므로 월=0 … 일=6 으로 맞춤
        int firstCol = (first.getDayOfWeek().getValue() + 6) % 7;

        int length = ym.lengthOfMonth();
        int row = 1;           // 0행은 요일 헤더, 1행부터 날짜
        int col = firstCol;

        // 5) 날짜 셀(버튼 + 점) 채우기
        for (int day = 1; day <= length; day++) {
            LocalDate date = ym.atDay(day);

            // 날짜 버튼
            Button btn = new Button(String.valueOf(day));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(60);
            btn.setOnAction(e -> openDayTasksModal(date));

            // 점(최대 3개)
            int cnt = counts.getOrDefault(date, 0);
            Label dot = new Label(cnt > 0 ? "●".repeat(Math.min(cnt, 3)) : "");
            dot.setStyle("-fx-opacity: 0.7; -fx-font-size: 10px;");

            // 셀 컨테이너
            VBox cell = new VBox(4, btn, dot);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setPadding(new Insets(4));
            cell.setStyle("-fx-border-color: #ddd; -fx-background-color: #fafafa;");

            // 오늘 날짜 하이라이트
            if (date.equals(LocalDate.now())) {
                cell.setStyle("-fx-border-color: #3b82f6; -fx-background-color: #eef5ff;");
            }

            calendarGrid.add(cell, col, row);

            // 다음 칸
            col++;
            if (col == 7) { col = 0; row++; } // 정확히 7에서 줄바꿈
        }

        // 6) 열 제약(1/7 균등) — 매 렌더마다 재설정
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100.0 / 7.0);
        calendarGrid.getColumnConstraints().addAll(cc, cc, cc, cc, cc, cc, cc);

        // 7) 행 제약(보기 좋은 높이) — 최대 6주 + 헤더 1행
        RowConstraints rcHeader = new RowConstraints();   // 헤더 행 (기본)
        calendarGrid.getRowConstraints().add(rcHeader);

        RowConstraints rc = new RowConstraints();
        rc.setMinHeight(80); // 필요 시 조절
        // 달이 4~6주라 넉넉히 6개 추가
        calendarGrid.getRowConstraints().addAll(rc, rc, rc, rc, rc, rc);
    }



    //날짜 클릭시 모달
    private void openDayTasksModal(LocalDate date) {
        List<Task> tasks = dao.listByDate(date);

        ListView<String> list = new ListView<>();
        for (Task t : tasks) {
            String pri = switch (t.priority) { case 1 -> "[H]"; case 3 -> "[L]"; default -> "[M]"; };
            list.getItems().add(pri + " " + t.title + (t.dueAt != null ? " (" + t.dueAt + ")" : ""));
        }
        if (list.getItems().isEmpty()) {
            list.getItems().add("일정이 없습니다.");
        }

        Button close = new Button("닫기");
        close.setOnAction(e -> close.getScene().getWindow().hide());

        VBox box = new VBox(10, new Label(date.toString()), list, close);
        box.setPadding(new Insets(12));

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("할 일 (" + date + ")");
        dialog.setScene(new Scene(box, 420, 360));
        dialog.showAndWait();
    }


    public static void main(String[] args) {
        launch(args);                                        // Application.launch() → 내부적으로 JavaFX 앱 초기화 후 start(Stage) 호출
    }
}
