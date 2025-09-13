package com.example.organizer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * 메인 UI 클래스 (JavaFX Application)
 * - 왼쪽: 우선순위 필터 Nav
 * - 상단: 전달/다음달 이동 버튼과 현재 월 표시
 * - 중앙: 달력(GridPane)
 * - 오른쪽: 할 일 추가 패널
 */
public class App extends Application {

    // ================= 필드 =================
    private GridPane calendarGrid;        // 달력 그리드
    private YearMonth currentMonth = YearMonth.now(); // 현재 표시 중인 월
    private TaskDao dao = new TaskDao();  // DB 접근용 DAO
    private Label monthLabel;             // 상단에 "2025년 9월" 같은 표시

    // ================= 메인 실행 =================
    @Override
    public void start(Stage stage) {
        // DB 마이그레이션 (테이블 없으면 생성)
        Database.migrate();

        // 달력 그리드 초기화
        calendarGrid = new GridPane();
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);
        calendarGrid.setPadding(new Insets(10));

        // 이번 달 렌더링
        renderCalendar(currentMonth);

        // 스크롤 지원 (달력이 커졌을 때 대비)
        ScrollPane calendarScroll = new ScrollPane(calendarGrid);
        calendarScroll.setFitToWidth(true);
        calendarScroll.setFitToHeight(true);
        calendarScroll.setPrefViewportWidth(680);
        calendarScroll.setPrefViewportHeight(560);

        // 오른쪽 할 일 추가 패널
        VBox rightPanel = buildRightPanel();
        rightPanel.setPrefWidth(300);
        rightPanel.setMinWidth(280);

        // 중앙: 달력 + 오른쪽 패널
        HBox centerRow = new HBox(16, calendarScroll, rightPanel);
        centerRow.setPadding(new Insets(12));
        HBox.setHgrow(calendarScroll, Priority.ALWAYS);

        // BorderPane 레이아웃
        BorderPane root = new BorderPane();
        root.setTop(buildMonthBar());   // 상단 월 이동 바
        root.setLeft(buildNav());       // 왼쪽 Nav
        root.setCenter(centerRow);      // 중앙 (달력+우패널)

        // 장면 구성
        Scene scene = new Scene(root, 1100, 680);
        stage.setTitle("Todo Program");
        stage.setScene(scene);
        stage.show();
    }

    // ================= 달력 렌더링 =================
    /**
     * 주어진 YearMonth 기준으로 달력을 다시 그림
     */
    private void renderCalendar(YearMonth ym) {
        calendarGrid.getChildren().clear();

        // 날짜별 할 일 개수 맵 (점 표시용)
        Map<LocalDate, Integer> counts = dao.getDailyCountsForMonth(ym);

        // 요일 헤더
        String[] wk = {"일","월","화","수","목","금","토"};
        for (int i = 0; i < 7; i++) {
            Label head = new Label(wk[i]);
            head.setStyle("-fx-font-weight: bold;");
            if (i == 0) head.setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); // 일요일 헤더 빨강
            calendarGrid.add(head, i, 0);
        }

        // 이번 달 1일의 요일 계산 (일요일=0)
        LocalDate first = ym.atDay(1);
        int firstDow = first.getDayOfWeek().getValue() % 7; // 일=0, 월=1 … 토=6

        int length = ym.lengthOfMonth();
        int row = 1;
        int col = firstDow;

        // 날짜 버튼 + 할일 점 렌더링
        for (int day = 1; day <= length; day++) {
            LocalDate date = ym.atDay(day);

            // 날짜 버튼
            Button btn = new Button(String.valueOf(day));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(60);
            btn.setOnAction(e -> openDayTasksModal(date));

            // 👉 일요일이면 빨간색 적용
            if (date.getDayOfWeek().getValue() == 7) { // 일요일
                btn.setStyle("-fx-text-fill: red;");
            }

            // 할 일 개수 → 점 표시
            int cnt = counts.getOrDefault(date, 0);
            Label dot = new Label(cnt > 0 ? "●".repeat(Math.min(cnt, 3)) : "");
            dot.setStyle("-fx-opacity: 0.7; -fx-font-size: 10px;");


            // 🔴 반복 업무가 있는지 확인해서 빨간 동그라미 표시
            boolean hasRecurring = dao.hasRecurringOn(date);
            Label recurDot = new Label(hasRecurring ? "●" : "");
            recurDot.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

            // 날짜 셀 (VBox: 날짜 + 점)
            VBox cell = new VBox(4, btn, dot);
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

    // ================= 상단 월 이동 바 =================
    private HBox buildMonthBar() {
        Button prev = new Button("〈");
        Button next = new Button("〉");
        monthLabel = new Label(formatMonth(currentMonth));

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

        HBox box = new HBox(10, prev, monthLabel, next);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 0, 8, 0));
        return box;
    }

    // "2025년 9월" 형식 텍스트
    private String formatMonth(YearMonth ym) {
        return ym.getYear() + "년 " + ym.getMonthValue() + "월";
    }

    // ================= 왼쪽 Nav =================
    private ListView<String> buildNav() {
        ListView<String> nav = new ListView<>();
        nav.getItems().addAll("전체", "중요(1)", "보통(2)", "낮음(3)");
        nav.getSelectionModel().selectFirst();
        nav.setPrefWidth(140);
        return nav;
    }

// ================= 오른쪽 패널 =================
    /**
     * 오른쪽 입력 패널 (새 할일 등록 UI)
     * - 제목, 우선순위
     * - [반복 업무] 체크박스 + 요일 토글(일~토)
     * - 시작 날짜 / 시간
     * - 추가 버튼 (단발/반복 분기 저장)
     */
    private VBox buildRightPanel() {
        // (1) 제목 입력 필드
        TextField titleField = new TextField();
        titleField.setPromptText("예) 보고서 작성");

        // (2) 우선순위 선택 (1=중요, 2=보통, 3=낮음)
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("중요(1)", "보통(2)", "낮음(3)");
        priorityBox.getSelectionModel().select(1); // 기본값: 보통(2)

        // (3) 반복 업무 여부 체크박스
        CheckBox recurringChk = new CheckBox("반복 업무");

        // (4) 반복 요일 선택(일~토) - ToggleButton으로 on/off
        HBox daysRow = new HBox(6);
        ToggleButton[] dayToggles = {
                new ToggleButton("일"), new ToggleButton("월"), new ToggleButton("화"),
                new ToggleButton("수"), new ToggleButton("목"), new ToggleButton("금"), new ToggleButton("토")
        };
        daysRow.getChildren().addAll(dayToggles);

        // 반복 업무가 아닐 땐 요일 선택을 잠가둔다.
        daysRow.setDisable(true);
        recurringChk.selectedProperty().addListener((obs, oldVal, isOn) -> {
            // 체크박스 상태에 맞춰 토글 행 활성/비활성
            daysRow.setDisable(!isOn);
        });

        // (5) 시작 날짜 (반복/단발 공통으로 '기준 날짜')
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("YYYY-MM-DD");

        // (6) 시간 (선택) - "HH:mm"를 권장. 비우면 종일로 처리.
        TextField timeField = new TextField();
        timeField.setPromptText("예) 14:00 (비우면 종일)");

        // (7) 저장(추가) 버튼
        Button addBtn = new Button("추가");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            // --- 입력값 수집/검증 ---
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.isEmpty()) {
                showAlert("제목을 입력하세요.");
                return;
            }

            // 우선순위 인덱스: 0=중요(1), 1=보통(2), 2=낮음(3)
            int priority = switch (priorityBox.getSelectionModel().getSelectedIndex()) {
                case 0 -> 1; case 2 -> 3; default -> 2;
            };

            LocalDate selectedDate = datePicker.getValue(); // null 허용
            String timeText = timeField.getText() == null ? "" : timeField.getText().trim();

            // 단발 일정 저장 시 사용할 dueAt 문자열 조립
            // - 날짜만 있으면 "YYYY-MM-DD"
            // - 시간까지 있으면 "YYYY-MM-DD HH:mm"
            String dueAt = null;
            if (selectedDate != null) {
                if (timeText.isEmpty()) {
                    dueAt = selectedDate.toString();
                } else {
                    // 시간 형식 러프 검증 (정확 검증은 LocalTime.parse로 가능)
                    // 포트폴리오에선 단순 문자열 저장 + UI 안내로 충분
                    dueAt = selectedDate.toString() + " " + timeText;
                }
            }

            // --- 단발/반복 분기 ---
            if (!recurringChk.isSelected()) {
                // ✅ (A) 단발 업무: 기존 메서드로 저장
                dao.addTask(title, priority, dueAt);

            } else {
                // ✅ (B) 반복 업무: 요일 비트마스크 계산 + 반복 메서드로 저장
                int mask = buildDaysMask(dayToggles); // 일=bit0 … 토=bit6
                if (mask == 0) {
                    showAlert("반복 요일을 하나 이상 선택하세요.");
                    return;
                }

                // 반복 시작일: 선택된 날짜가 있으면 그 날부터, 없으면 오늘부터
                String recurStart = (selectedDate != null ? selectedDate.toString() : LocalDate.now().toString());

                // 반복 종료일은 지금 단계에선 미사용 → null (무기한 반복)
                String recurUntil = null;

                // 간격(주 단위): 지금은 1주 반복만 제공
                int intervalWeeks = 1;

                // 선택한 시간(옵션). ""이면 종일로 처리(DAO에서 next_fire_at 계산 시 활용 가능)
                String timeHHmm = timeText;

                // ※ 여기서 호출하는 addRecurringTask(...) 는 다음 단계에서 DAO에 구현
                dao.addRecurringTask(title, priority, mask, recurStart, recurUntil, intervalWeeks, timeHHmm);
            }

            // --- 저장 후 UI 정리 & 달력 갱신 ---
            renderCalendar(currentMonth);  // 점(●) 갱신 포함
            titleField.clear();
            timeField.clear();
            datePicker.setValue(null);
            recurringChk.setSelected(false); // 체크 해제 → 요일 토글 잠김
        });

        // (8) 패널 배치 (위에서 아래로 순서대로)
        VBox box = new VBox(10,
                new Label("새 할일"),
                new Label("할 일 제목"), titleField,
                new Label("우선순위"), priorityBox,
                new Separator(),                 // 시각적 구분선
                recurringChk, daysRow,           // ✅ 반복 관련 UI
                new Label("시작 날짜"), datePicker,
                new Label("시간 (HH:mm, 선택)"), timeField,
                addBtn
        );
        box.setPadding(new Insets(10));
        return box;
    }

    /**
     * 요일 토글(일~토)을 비트마스크(int)로 변환
     * - 인덱스 0=일 → bit0
     * - 인덱스 6=토 → bit6
     * - 선택된 요일만 OR(|) 하여 누적
     *
     * 예) 월/수/금 선택 → 0b0101010 (10진수 42)
     */
    private int buildDaysMask(ToggleButton[] toggles) {
        int mask = 0;
        for (int i = 0; i < 7; i++) {
            if (toggles[i].isSelected()) {
                mask |= (1 << i);
            }
        }
        return mask;
    }

    /**
     * 간단 알림창 유틸
     * - 정보 메시지를 모달로 띄운 뒤 OK 누르면 닫힘
     */
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }



    // ================= 특정 날짜 클릭 시 모달 =================
    private void openDayTasksModal(LocalDate date) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("할 일 (" + date + ")");

        ListView<String> list = new ListView<>();
        dao.listByDate(date).forEach(t ->
                list.getItems().add(t.title + " (우선순위 " + t.priority + ")" + " 마감기한 : " + t.dueAt));

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
