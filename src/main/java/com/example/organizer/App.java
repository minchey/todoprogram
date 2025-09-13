package com.example.organizer;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Todo Program 메인 실행 클래스
 * - 왼쪽: 달력 (날짜 클릭 시 할 일 목록 모달 열기)
 * - 오른쪽: 할 일 추가 패널 (제목/우선순위/날짜 입력 후 추가)
 */
public class App extends Application {

    private GridPane calendarGrid;          // 달력 격자
    private YearMonth currentMonth = YearMonth.now(); // 현재 달
    private TaskDao dao = new TaskDao();    // DB 접근 DAO

    @Override
    public void start(Stage stage) {
        Database.migrate(); // DB 스키마 초기화/마이그레이션 실행

        // 달력 그리드 기본 세팅
        calendarGrid = new GridPane();
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);
        calendarGrid.setPadding(new Insets(10));

        // 이번 달 달력 렌더링
        renderCalendar(currentMonth);

        // 달력 스크롤 (달력이 커지면 스크롤 가능)
        ScrollPane calendarScroll = new ScrollPane(calendarGrid);
        calendarScroll.setFitToWidth(true);
        calendarScroll.setFitToHeight(true);
        calendarScroll.setPrefViewportWidth(680);   // 달력 영역 폭
        calendarScroll.setPrefViewportHeight(560);  // 달력 영역 높이

        // 오른쪽 할 일 추가 패널
        VBox rightPanel = buildRightPanel();

        // 메인 레이아웃: 좌측 달력 + 우측 패널
        HBox main = new HBox(16, calendarScroll, rightPanel);
        main.setPadding(new Insets(12));
        HBox.setHgrow(calendarScroll, Priority.ALWAYS); // 달력이 남는 폭 차지
        rightPanel.setPrefWidth(300);                   // 오른쪽 폭 고정
        rightPanel.setMinWidth(280);

        // 씬 생성 및 Stage 설정
        Scene scene = new Scene(main, 1024, 640);
        stage.setTitle("Todo Program");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * 달력 렌더링
     * - YearMonth를 받아서 달력 그리드(GridPane)를 채움
     * - 오늘 날짜는 하이라이트
     * - 날짜별 할 일 개수는 점(●)으로 표시
     */
    private void renderCalendar(YearMonth ym) {
        // 1) 기존 내용/제약 초기화 (누적 방지)
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // 2) 날짜별 할 일 개수 맵 (점 표시용)
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
        // 우리 헤더는 월~일 → 월=0 … 일=6 으로 맞춤
        int firstCol = (first.getDayOfWeek().getValue() + 6) % 7;
        int length = ym.lengthOfMonth();

        int row = 1;   // 0행은 요일, 1행부터 날짜 시작
        int col = firstCol;

        // 5) 날짜 셀 채우기
        for (int day = 1; day <= length; day++) {
            LocalDate date = ym.atDay(day);

            // 날짜 버튼
            Button btn = new Button(String.valueOf(day));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(60);
            btn.setOnAction(e -> openDayTasksModal(date));

            // 점(●) — 일정 개수 표현 (최대 3개)
            int cnt = counts.getOrDefault(date, 0);
            Label dot = new Label(cnt > 0 ? "●".repeat(Math.min(cnt, 3)) : "");
            dot.setStyle("-fx-opacity: 0.7; -fx-font-size: 10px;");

            // 셀 컨테이너 (날짜 버튼 + 점)
            VBox cell = new VBox(4, btn, dot);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setPadding(new Insets(4));
            cell.setStyle("-fx-border-color: #ddd; -fx-background-color: #fafafa;");

            // 오늘 날짜 하이라이트
            if (date.equals(LocalDate.now())) {
                cell.setStyle("-fx-border-color: #3b82f6; -fx-background-color: #eef5ff;");
            }

            calendarGrid.add(cell, col, row);

            // 다음 칸으로 이동
            col++;
            if (col == 7) { col = 0; row++; }
        }

        // 6) 열 균등 분할
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100.0 / 7.0);
        calendarGrid.getColumnConstraints().addAll(cc, cc, cc, cc, cc, cc, cc);

        // 7) 행 높이 (보기 좋게 균등 배치)
        RowConstraints rcHeader = new RowConstraints(); // 헤더 행
        calendarGrid.getRowConstraints().add(rcHeader);

        RowConstraints rc = new RowConstraints();
        rc.setMinHeight(80);
        // 최대 6주치 → 넉넉히 6개 추가
        calendarGrid.getRowConstraints().addAll(rc, rc, rc, rc, rc, rc);
    }

    /**
     * 오른쪽 "할 일 추가" 패널 생성
     * - 제목/우선순위/날짜/시간 입력 후 추가 버튼 클릭 시 DB 저장
     */
    private VBox buildRightPanel() {
        Label titleLbl = new Label("할 일 제목");
        TextField titleField = new TextField();
        titleField.setPromptText("예) 보고서 작성");

        Label priLbl = new Label("우선순위");
        ComboBox<String> priCombo = new ComboBox<>();
        priCombo.getItems().addAll("중요(1)", "보통(2)", "낮음(3)");
        priCombo.getSelectionModel().select(1); // 기본 보통(2)

        Label dateLbl = new Label("마감 날짜");
        DatePicker datePicker = new DatePicker();

        Label timeLbl = new Label("시간 (HH:mm, 선택)");
        TextField timeField = new TextField();
        timeField.setPromptText("예) 14:00 (비우면 종일)");

        Button addBtn = new Button("추가");
        addBtn.setMaxWidth(Double.MAX_VALUE);

        Label tip = new Label("• 시간 비우면 종일 처리\n• 추가 후 달력 점이 갱신됩니다.");
        tip.setStyle("-fx-font-size: 11px; -fx-opacity: .75;");

        // 추가 버튼 이벤트
        addBtn.setOnAction(e -> {
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.isEmpty()) {
                alert("제목을 입력하세요.");
                return;
            }

            int priority = switch (priCombo.getSelectionModel().getSelectedIndex()) {
                case 0 -> 1; case 2 -> 3; default -> 2;
            };

            LocalDate d = datePicker.getValue(); // null 가능
            String dueAt = null;

            // dueAt 문자열 생성
            if (d != null) {
                String time = timeField.getText() == null ? "" : timeField.getText().trim();
                if (time.isEmpty()) {
                    dueAt = d.toString(); // 날짜만
                } else {
                    try {
                        java.time.LocalTime lt = java.time.LocalTime.parse(time); // "HH:mm"
                        java.time.LocalDateTime dt = java.time.LocalDateTime.of(d, lt);
                        dueAt = dt.toString(); // "YYYY-MM-DDTHH:mm"
                    } catch (Exception ex) {
                        alert("시간 형식이 올바르지 않습니다. 예) 09:30");
                        return;
                    }
                }
            }

            // DB에 추가
            dao.addTask(title, priority, dueAt);

            // 입력 초기화
            titleField.clear();
            timeField.clear();

            // 달력 새로고침
            renderCalendar(currentMonth);
            alert("추가되었습니다.");
        });

        VBox box = new VBox(8,
                new Label("새 할 일"),
                titleLbl, titleField,
                priLbl, priCombo,
                dateLbl, datePicker,
                timeLbl, timeField,
                addBtn, tip
        );
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #f7f7fb; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
        return box;
    }

    /**
     * 특정 날짜 클릭 시 할 일 목록을 모달로 띄우는 메서드
     */
    private void openDayTasksModal(LocalDate date) {
        List<Task> tasks = dao.listByDate(date);

        StringBuilder sb = new StringBuilder();
        for (Task t : tasks) {
            sb.append("- ").append(t.title)
                    .append(" (우선순위 ").append(t.priority).append(")\n");
        }
        if (tasks.isEmpty()) sb.append("등록된 할 일이 없습니다.");

        Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        a.setHeaderText(date.toString() + " 할 일");
        a.showAndWait();
    }

    /**
     * 간단 알림창
     */
    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }
}
