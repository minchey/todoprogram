package com.example.organizer;


// ✅ JavaFX imports (UI 전부)
import javafx.application.Application;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;   // ← HBox.setHgrow(..., Priority.ALWAYS) 때문에 필요
import javafx.scene.layout.VBox;

import javafx.stage.Stage;             // 모달/다이얼로그 창

// ✅ 자바 표준 라이브러리
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;


/**
 * 메인 UI 클래스 (JavaFX Application)
 * - 왼쪽: 오늘 할 일 리스트
 * - 상단: 전달/다음달 이동 버튼과 현재 월 표시
 * - 중앙: 달력(GridPane)
 * - 오른쪽: 할 일 추가 패널
 */
public class App extends Application {

    // ================= 필드 =================
    private GridPane calendarGrid;                // 달력 그리드
    private YearMonth currentMonth = YearMonth.now(); // 현재 표시 중인 월
    private final TaskDao dao = new TaskDao();    // DB 접근용 DAO
    private Label monthLabel;                     // 상단에 "YYYY년 M월" 표시
    private ListView<String> todayList;           // 오늘 할 일 표시용 리스트뷰

    // ================= 메인 실행 =================
    @Override
    public void start(Stage stage) {
        Database.migrate(); // DB 마이그레이션 (테이블/컬럼 보강)

        // 달력 그리드 초기화
        calendarGrid = new GridPane();
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);
        calendarGrid.setPadding(new Insets(10));

        // 첫 렌더링
        renderCalendar(currentMonth);

        // 달력 스크롤(화면 크기 작을 때 대비)
        ScrollPane calendarScroll = new ScrollPane(calendarGrid);
        calendarScroll.setFitToWidth(true);
        calendarScroll.setFitToHeight(true);
        calendarScroll.setPrefViewportWidth(680);
        calendarScroll.setPrefViewportHeight(560);

        // 오른쪽: 새 할일 입력 패널
        VBox rightPanel = buildRightPanel();
        rightPanel.setPrefWidth(300);
        rightPanel.setMinWidth(280);

        // 왼쪽: 오늘 할 일 패널 (내부에서 todayList 생성 + 첫 로드)
        VBox todayPanel = buildTodayPanel();
        todayPanel.setPrefWidth(240);
        todayPanel.setMinWidth(200);

        // 중앙 배치: (왼)오늘 / (중)달력 / (오)입력
        HBox centerRow = new HBox(16, todayPanel, calendarScroll, rightPanel);
        centerRow.setPadding(new Insets(12));
        HBox.setHgrow(calendarScroll, Priority.ALWAYS);

        // BorderPane 레이아웃
        BorderPane root = new BorderPane();
        root.setTop(buildMonthBar());   // ⬅️ 누락되어 에러가 났던 부분: 월 이동 바
        root.setCenter(centerRow);

        // 장면 구성
        Scene scene = new Scene(root, 1200, 680);
        stage.setTitle("Todo Program");
        stage.setScene(scene);
        stage.show();
    }

    // ================= 상단 월 이동 바 =================
    /**
     * 전달/다음달 버튼 + 현재 월 라벨을 담은 바 생성
     * - 버튼 클릭 시 currentMonth 갱신, 달력 재렌더링, 오늘 리스트(선택) 갱신
     */
    private HBox buildMonthBar() {
        Button prev = new Button("〈");
        Button next = new Button("〉");
        monthLabel = new Label(formatMonth(currentMonth)); // "2025년 9월"

        prev.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            monthLabel.setText(formatMonth(currentMonth));
            renderCalendar(currentMonth);
            // 필요 시 오늘 리스트는 그대로 두고 싶으면 아래 호출은 주석 처리
            refreshTodayTasks();
        });

        next.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            monthLabel.setText(formatMonth(currentMonth));
            renderCalendar(currentMonth);
            refreshTodayTasks();
        });

        HBox box = new HBox(10, prev, monthLabel, next);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 0, 8, 0));
        return box;
    }

    /** "YYYY년 M월" 형식으로 변환 */
    private String formatMonth(YearMonth ym) {
        return ym.getYear() + "년 " + ym.getMonthValue() + "월";
    }

    // ================= 달력 렌더링 =================
    /**
     * 주어진 YearMonth 기준으로 달력을 다시 그림
     * - 헤더(일~토)
     * - 각 날짜 버튼 + 단발 업무 개수 점 + 반복업무 빨간 점
     */
    private void renderCalendar(YearMonth ym) {
        calendarGrid.getChildren().clear();

        // 날짜별 단발 할 일 개수 (점 표시용)
        Map<LocalDate, Integer> counts = dao.getDailyCountsForMonth(ym);

        // 요일 헤더
        String[] wk = {"일", "월", "화", "수", "목", "금", "토"};
        for (int i = 0; i < 7; i++) {
            Label head = new Label(wk[i]);
            head.setStyle("-fx-font-weight: bold;");
            if (i == 0) head.setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); // 일요일 빨강
            calendarGrid.add(head, i, 0);
        }

        // 이번 달 1일의 요일 계산 (일요일=0)
        LocalDate first = ym.atDay(1);
        int firstDow = first.getDayOfWeek().getValue() % 7; // 일=0, 월=1 … 토=6

        int length = ym.lengthOfMonth();
        int row = 1;
        int col = firstDow;

        // 날짜 셀 렌더링
        for (int day = 1; day <= length; day++) {
            LocalDate date = ym.atDay(day);

            // 날짜 버튼
            Button btn = new Button(String.valueOf(day));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(60);
            btn.setOnAction(e -> openDayTasksModal(date));

            // 일요일은 빨간 글자
            if (date.getDayOfWeek().getValue() == 7) {
                btn.setStyle("-fx-text-fill: red;");
            }

            // 단발 업무 개수 → 회색 점 (최대 3개까지만 표시)
            int cnt = counts.getOrDefault(date, 0);
            Label dot = new Label(cnt > 0 ? "●".repeat(Math.min(cnt, 3)) : "");
            dot.setStyle("-fx-opacity: 0.7; -fx-font-size: 10px;");

            // 반복 업무 여부 → 빨간 점 1개
            boolean hasRecurring = dao.hasRecurringOn(date);
            Label recurDot = new Label(hasRecurring ? "●" : "");
            recurDot.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

            // 셀 배치
            VBox cell = new VBox(4, btn, dot, recurDot);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setPadding(new Insets(4));
            cell.setStyle("-fx-border-color: #ddd; -fx-background-color: #fafafa;");

            calendarGrid.add(cell, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }

        // 열 균등 분할
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100.0 / 7.0);
        calendarGrid.getColumnConstraints().setAll(cc, cc, cc, cc, cc, cc, cc);
    }

    // ================= 오늘 할 일 패널 =================
    /** 왼쪽 Today 패널 생성: todayList 초기화 + 데이터 첫 로드 */
    private VBox buildTodayPanel() {
        todayList = new ListView<>();
        refreshTodayTasks(); // 첫 로드

        Label title = new Label("오늘 할 일 (" + LocalDate.now() + ")");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox box = new VBox(10, title, todayList);
        box.setPadding(new Insets(10));
        return box;
    }

    /** 오늘(LocalDate.now()) 기준으로 단발+반복 업무를 읽어 리스트뷰에 반영 */
    private void refreshTodayTasks() {
        if (todayList == null) return;
        todayList.getItems().clear();

        LocalDate today = LocalDate.now();

        // 단발 일정
        dao.listByDate(today).forEach(t ->
                todayList.getItems().add("[단발] " + t.title + " (우선순위 " + t.priority + ")")
        );

        // 반복 일정
        dao.listRecurringByDate(today).forEach(t ->
                todayList.getItems().add("[반복] " + t.title + " (우선순위 " + t.priority + ")")
        );
    }

    // ================= 오른쪽 입력 패널 =================
    /**
     * 새 할일 등록 패널
     * - 제목/우선순위/반복 여부(요일 토글)/시작 날짜/시간
     * - 저장 시 단발/반복 분기하여 DAO 호출
     * - 저장 후 달력 + 오늘 리스트 갱신
     */
    private VBox buildRightPanel() {
        // (1) 제목
        TextField titleField = new TextField();
        titleField.setPromptText("예) 보고서 작성");

        // (2) 우선순위
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("중요(1)", "보통(2)", "낮음(3)");
        priorityBox.getSelectionModel().select(1); // 기본: 보통(2)

        // (3) 반복 여부 + 요일 토글
        CheckBox recurringChk = new CheckBox("반복 업무");

        HBox daysRow = new HBox(6);
        ToggleButton[] dayToggles = {
                new ToggleButton("일"), new ToggleButton("월"), new ToggleButton("화"),
                new ToggleButton("수"), new ToggleButton("목"), new ToggleButton("금"), new ToggleButton("토")
        };
        daysRow.getChildren().addAll(dayToggles);
        daysRow.setDisable(true); // 기본 비활성화
        recurringChk.selectedProperty().addListener((obs, oldVal, isOn) ->
                daysRow.setDisable(!isOn)
        );

        // (4) 날짜/시간
        DatePicker datePicker = new DatePicker();                 // 기준 날짜(단발=마감, 반복=시작)
        TextField timeField = new TextField();                    // HH:mm (선택)
        timeField.setPromptText("예) 14:00 (비우면 종일)");

        // (5) 추가 버튼
        Button addBtn = new Button("추가");
        addBtn.setOnAction(e -> {
            // 입력 수집/검증
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.isEmpty()) {
                showAlert("제목을 입력하세요.");
                return;
            }

            // 0=중요(1), 1=보통(2), 2=낮음(3)
            int priority = switch (priorityBox.getSelectionModel().getSelectedIndex()) {
                case 0 -> 1; case 2 -> 3; default -> 2;
            };

            LocalDate selectedDate = datePicker.getValue();
            String timeText = timeField.getText() == null ? "" : timeField.getText().trim();

            // 단발 일정 저장시 dueAt (YYYY-MM-DD 또는 YYYY-MM-DD HH:mm)
            String dueAt = null;
            if (selectedDate != null) {
                dueAt = timeText.isEmpty()
                        ? selectedDate.toString()
                        : selectedDate.toString() + " " + timeText;
            }

            if (!recurringChk.isSelected()) {
                // (A) 단발 업무
                dao.addTask(title, priority, dueAt);
            } else {
                // (B) 반복 업무
                int mask = buildDaysMask(dayToggles); // 일=bit0 … 토=bit6
                if (mask == 0) {
                    showAlert("반복 요일을 하나 이상 선택하세요.");
                    return;
                }
                String recurStart = (selectedDate != null ? selectedDate.toString() : LocalDate.now().toString());
                String recurUntil = null;         // 무기한
                int intervalWeeks = 1;            // 1주 간격
                dao.addRecurringTask(title, priority, mask, recurStart, recurUntil, intervalWeeks, timeText);
            }

            // 저장 후 UI 갱신
            renderCalendar(currentMonth); // 달력 점/표시 갱신
            refreshTodayTasks();          // 오늘 리스트 갱신

            // 입력 리셋
            titleField.clear();
            timeField.clear();
            datePicker.setValue(null);
            recurringChk.setSelected(false); // 요일 토글 잠김
        });

        // 패널 구성
        VBox box = new VBox(10,
                new Label("새 할일"),
                new Label("할 일 제목"), titleField,
                new Label("우선순위"), priorityBox,
                new Separator(),
                recurringChk, daysRow,
                new Label("시작 날짜"), datePicker,
                new Label("시간 (HH:mm, 선택)"), timeField,
                addBtn
        );
        box.setPadding(new Insets(10));
        return box;
    }

    /** 요일 토글(일~토)을 비트마스크로 변환 (일=bit0 … 토=bit6) */
    private int buildDaysMask(ToggleButton[] toggles) {
        int mask = 0;
        for (int i = 0; i < 7; i++) {
            if (toggles[i].isSelected()) mask |= (1 << i);
        }
        return mask;
    }

    /** 간단 알림창 */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ================= 특정 날짜 클릭 시 모달 =================
    /** 달력의 날짜 클릭 시 해당 날짜의 단발/반복 업무를 팝업으로 보여줌 */
    private void openDayTasksModal(LocalDate date) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("할 일 (" + date + ")");

        ListView<String> list = new ListView<>();
        // 단발 일정
        dao.listByDate(date).forEach(t ->
                list.getItems().add("[단발] " + t.title + " (우선순위 " + t.priority + ") 마감: " + t.dueAt)
        );
        // 반복 일정
        dao.listRecurringByDate(date).forEach(t ->
                list.getItems().add("[반복] " + t.title + " (우선순위 " + t.priority + ")")
        );

        Button close = new Button("닫기");
        close.setOnAction(e -> dialog.close());

        VBox box = new VBox(10, new Label(date.toString()), list, close);
        box.setPadding(new Insets(12));

        dialog.setScene(new Scene(box, 420, 360));
        dialog.showAndWait();
    }

    // ================= main =================
    public static void main(String[] args) {
        launch(args);
    }
}
