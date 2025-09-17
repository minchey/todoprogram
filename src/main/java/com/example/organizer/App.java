package com.example.organizer;

// ===== JavaFX =====
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

// ===== Java =====
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 메인 UI
 * - 왼쪽: 오늘 할 일(단발 + 반복)  ← TaskCell 렌더링 (완료/미완료 토글 + 삭제)
 * - 중앙: 달력(미완료=회색점, 완료=초록점, 반복=빨간점)
 * - 오른쪽: 새 할 일(단발/반복) 등록
 */
public class App extends Application {
    // ---------- 필드 ----------
    private GridPane calendarGrid;
    private YearMonth currentMonth = YearMonth.now();
    private final TaskDao dao = new TaskDao();

    private Label monthLabel;
    private ListView<Task> todayList;   // 오늘 할 일(단발 + 반복) - Task 객체로!

    // ---------- 진입 ----------
    @Override
    public void start(Stage stage) {
        Database.migrate();

        calendarGrid = new GridPane();
        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);
        calendarGrid.setPadding(new Insets(10));

        // 첫 렌더
        renderCalendar(currentMonth);

        // 중앙 달력 스크롤
        ScrollPane calendarScroll = new ScrollPane(calendarGrid);
        calendarScroll.setFitToWidth(true);
        calendarScroll.setFitToHeight(true);
        calendarScroll.setPrefViewportWidth(680);
        calendarScroll.setPrefViewportHeight(560);

        VBox rightPanel = buildRightPanel();
        rightPanel.setPrefWidth(300);

        VBox todayPanel = buildTodayPanel();
        todayPanel.setPrefWidth(280);

        HBox centerRow = new HBox(16, todayPanel, calendarScroll, rightPanel);
        centerRow.setPadding(new Insets(12));
        HBox.setHgrow(calendarScroll, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(buildMonthBar());
        root.setCenter(centerRow);

        Scene scene = new Scene(root, 1200, 680);
        stage.setTitle("Todo Program");
        stage.setScene(scene);
        stage.show();
    }

    // ---------- 상단 월 이동 바 ----------
    private HBox buildMonthBar() {
        Button prev = new Button("〈");
        Button next = new Button("〉");
        monthLabel = new Label(formatMonth(currentMonth));

        prev.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            monthLabel.setText(formatMonth(currentMonth));
            refreshAll();
        });
        next.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            monthLabel.setText(formatMonth(currentMonth));
            refreshAll();
        });

        HBox box = new HBox(10, prev, monthLabel, next);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 0, 8, 0));
        return box;
    }

    private String formatMonth(YearMonth ym) {
        return ym.getYear() + "년 " + ym.getMonthValue() + "월";
    }

    // ---------- 달력 렌더링 ----------
    /**
     * - 회색 ●: 미완료 단발 count (최대 3개)
     * - 초록 ●: 완료 단발 count (최대 3개)
     * - 빨강 ●: 반복 존재
     */
    private void renderCalendar(YearMonth ym) {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();

        // 날짜별 [완료, 미완료] 카운트
        Map<LocalDate, int[]> counts = dao.getDailyDoneTodoCounts(ym);

        // 헤더
        String[] wk = {"일","월","화","수","목","금","토"};
        for (int i = 0; i < 7; i++) {
            Label head = new Label(wk[i]);
            head.setStyle("-fx-font-weight: bold;");
            if (i == 0) head.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            calendarGrid.add(head, i, 0);
        }

        LocalDate first = ym.atDay(1);
        int firstDow = first.getDayOfWeek().getValue() % 7;
        int length = ym.lengthOfMonth();
        int row = 1, col = firstDow;

        for (int day = 1; day <= length; day++) {
            LocalDate date = ym.atDay(day);

            Button btn = new Button(String.valueOf(day));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(60);
            btn.setOnAction(e -> openDayTasksModal(date));

            if (date.getDayOfWeek().getValue() == 7) {
                btn.setStyle("-fx-text-fill: red;");
            }

            int[] arr = counts.getOrDefault(date, new int[]{0, 0});
            int done = arr[0], todo = arr[1];

            Label todoDots = new Label(todo > 0 ? "●".repeat(Math.min(todo, 3)) : "");
            todoDots.setStyle("-fx-opacity: 0.75; -fx-font-size: 10px; -fx-text-fill: gray;");
            Label doneDots = new Label(done > 0 ? "●".repeat(Math.min(done, 3)) : "");
            doneDots.setStyle("-fx-opacity: 0.95; -fx-font-size: 10px; -fx-text-fill: green;");

            boolean hasRecurring = dao.hasRecurringOn(date);
            Label recurDot = new Label(hasRecurring ? "●" : "");
            recurDot.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

            VBox dotsBox = new VBox(2, new HBox(4, todoDots, doneDots), recurDot);
            dotsBox.setAlignment(Pos.CENTER);

            VBox cell = new VBox(4, btn, dotsBox);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setPadding(new Insets(4));
            cell.setStyle("-fx-border-color: #ddd; -fx-background-color: #fafafa;");
            calendarGrid.add(cell, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }

        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100.0 / 7.0);
        calendarGrid.getColumnConstraints().setAll(cc, cc, cc, cc, cc, cc, cc);
    }

    // 달력 + 오늘 리스트 동시 갱신
    private void refreshAll() {
        renderCalendar(currentMonth);
        refreshTodayTasks();
    }

    // ---------- 왼쪽: 오늘 패널 ----------
    private VBox buildTodayPanel() {
        todayList = new ListView<>();
        // ✅ 오늘 리스트는 Task 객체로 렌더링 + 삭제/토글 가능
        todayList.setCellFactory(v -> new TaskCell(dao, this::refreshAll));
        refreshTodayTasks();

        Label title = new Label("오늘 할 일 (" + LocalDate.now() + ")");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox box = new VBox(10, title, todayList);
        box.setPadding(new Insets(10));
        return box;
    }

    /**
     * 오늘(LocalDate.now()) 기준으로
     * - 단발: 오늘까지 마감 & 미완료(필요시 includeCompleted=true로 구현했다면 변경 가능)
     * - 반복: 오늘 요일/기간에 해당하는 모든 반복
     * 를 합쳐서 ListView<Task>에 넣는다.
     */
    private void refreshTodayTasks() {
        if (todayList == null) return;
        LocalDate today = LocalDate.now();

        List<Task> items = new ArrayList<>();
        // 단발(미완료 중심) — includeCompleted=false 가정
        items.addAll(dao.listDueUntil(today, /*includeCompleted*/ false));
        // 반복 — 반드시 추가!
        items.addAll(dao.listRecurringByDate(today));

        todayList.getItems().setAll(items);
    }

    // ---------- 오른쪽: 새 할 일 ----------
    private VBox buildRightPanel() {
        TextField titleField = new TextField();
        titleField.setPromptText("예) 보고서 작성");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("중요(1)", "보통(2)", "낮음(3)");
        priorityBox.getSelectionModel().select(1);

        CheckBox recurringChk = new CheckBox("반복 업무");

        HBox daysRow = new HBox(6);
        ToggleButton[] dayToggles = {
                new ToggleButton("일"), new ToggleButton("월"), new ToggleButton("화"),
                new ToggleButton("수"), new ToggleButton("목"), new ToggleButton("금"), new ToggleButton("토")
        };
        daysRow.getChildren().addAll(dayToggles);
        daysRow.setDisable(true);
        recurringChk.selectedProperty().addListener((obs, o, on) -> daysRow.setDisable(!on));

        DatePicker datePicker = new DatePicker();  // 단발=마감일, 반복=시작일
        TextField timeField = new TextField();     // HH:mm (선택)
        timeField.setPromptText("예) 14:00 (비우면 종일)");

        Button addBtn = new Button("추가");
        addBtn.setOnAction(e -> {
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.isEmpty()) { showInfo("제목을 입력하세요."); return; }

            int priority = switch (priorityBox.getSelectionModel().getSelectedIndex()) {
                case 0 -> 1; case 2 -> 3; default -> 2;
            };

            LocalDate selectedDate = datePicker.getValue();
            String timeText = timeField.getText() == null ? "" : timeField.getText().trim();

            // 단발 dueAt: "YYYY-MM-DD" 또는 "YYYY-MM-DD HH:mm"
            String dueAt = null;
            if (selectedDate != null) {
                dueAt = timeText.isEmpty()
                        ? selectedDate.toString()
                        : selectedDate.toString() + " " + timeText;
            }

            if (!recurringChk.isSelected()) {
                dao.addTask(title, priority, dueAt); // 단발
            } else {
                int mask = buildDaysMask(dayToggles); // 일=bit0 … 토=bit6
                if (mask == 0) { showInfo("반복 요일을 하나 이상 선택하세요."); return; }
                String recurStart = (selectedDate != null ? selectedDate.toString() : LocalDate.now().toString());
                String recurUntil = null;
                int intervalWeeks = 1;
                dao.addRecurringTask(title, priority, mask, recurStart, recurUntil, intervalWeeks, timeText);
            }

            // 갱신 + 리셋
            refreshAll();
            titleField.clear();
            timeField.clear();
            datePicker.setValue(null);
            recurringChk.setSelected(false);
        });

        VBox box = new VBox(10,
                new Label("새 할일"),
                new Label("할 일 제목"), titleField,
                new Label("우선순위"), priorityBox,
                new Separator(),
                recurringChk, daysRow,
                new Label("마감/시작 날짜"), datePicker,
                new Label("시간 (HH:mm, 선택)"), timeField,
                addBtn
        );
        box.setPadding(new Insets(10));
        return box;
    }

    private int buildDaysMask(ToggleButton[] toggles) {
        int mask = 0;
        for (int i = 0; i < 7; i++) if (toggles[i].isSelected()) mask |= (1 << i);
        return mask;
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ---------- 날짜 클릭 모달 ----------
    private void openDayTasksModal(LocalDate date) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("할 일 (" + date + ")");

        // ✅ 문자열 X, Task로 렌더링 + 삭제/토글 가능
        ListView<Task> list = new ListView<>();
        list.setCellFactory(v -> new ModalTaskCell(dao, () -> {
            // 토글/삭제 후 전체 리프레시
            refreshAll();
            // 모달 목록 즉시 갱신
            list.getItems().setAll(collectTasksFor(date));
        }));
        list.getItems().setAll(collectTasksFor(date));

        Button close = new Button("닫기");
        close.setOnAction(e -> dialog.close());

        VBox box = new VBox(10, new Label(date.toString()), list, close);
        box.setPadding(new Insets(12));

        dialog.setScene(new Scene(box, 480, 420));
        dialog.showAndWait();
    }

    // 모달에 보여줄 Task 수집(단발 + 반복)
    private List<Task> collectTasksFor(LocalDate date) {
        List<Task> items = new ArrayList<>();
        items.addAll(dao.listByDate(date));
        items.addAll(dao.listRecurringByDate(date));
        return items;
    }

    // ---------- 공용 헬퍼 ----------
    static String shortDate(String s) {
        if (s == null || s.isBlank()) return "";
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }
    static String shortTime(String s) {
        if (s == null || s.isBlank() || s.length() <= 10) return "";
        int idx = s.indexOf(' ');
        if (idx < 0 || s.length() < idx + 6) return "";
        return s.substring(idx + 1, idx + 6); // "HH:mm"
    }

    public static void main(String[] args) { launch(args); }

    // ==============================================================
    // 모달 셀: 완료/미완료 토글 + 삭제 버튼
    // ==============================================================
    private static class ModalTaskCell extends ListCell<Task> {
        private final TaskDao dao;
        private final Runnable onChanged;

        private final Circle circle = new Circle(6);
        private final Label title  = new Label();
        private final Label meta   = new Label();
        private final Label recurLabel = new Label();
        private final Button toggleBtn = new Button("완료/미완료");
        private final Button deleteBtn = new Button("삭제");
        private final HBox header = new HBox(8, circle, title, recurLabel, meta, toggleBtn, deleteBtn);
        private final VBox root = new VBox(2, header);

        ModalTaskCell(TaskDao dao, Runnable onChanged) {
            this.dao = dao;
            this.onChanged = onChanged;
            header.setAlignment(Pos.CENTER_LEFT);
            recurLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
            meta.setStyle("-fx-font-size: 11px;");
        }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) { setGraphic(null); return; }

            title.setText(task.title);
            title.setStyle("-fx-font-size: 14px;");

            // 🔁 반복 배지
            if (task.isRecurring == 1) {
                recurLabel.setText("🔁 반복");
                recurLabel.setVisible(true); recurLabel.setManaged(true);
            } else {
                recurLabel.setText("");
                recurLabel.setVisible(false); recurLabel.setManaged(false);
            }

            // 마감/시간/D-day
            String dueStr  = App.shortDate(task.dueAt);
            String timeStr = App.shortTime(task.dueAt);
            Integer dday   = ddayOf(task.dueAt);

            String dueLabel = dueStr.isEmpty() ? "" : (" / 마감: " + dueStr + (timeStr.isEmpty() ? "" : " " + timeStr));
            String dText    = (dday == null ? "" : " / D" + (dday == 0 ? "-DAY" : (dday > 0 ? "-" + dday : "+" + Math.abs(dday))));
            meta.setText("(우선순위 " + task.priority + ")" + dueLabel + dText);

            // 경고색
            if (!task.completed && dday != null) {
                if (dday < 0)      meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #d32f2f;");
                else if (dday == 0) meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #f57c00;");
                else               meta.setStyle("-fx-font-size: 11px;");
            } else if (task.completed) {
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #2e7d32;");
            }

            circle.setFill(task.completed ? Color.GREEN : Color.GRAY);

            // 완료/미완료 토글
            toggleBtn.setOnAction(e -> {
                boolean next = !task.completed;
                task.completed = next;
                circle.setFill(next ? Color.GREEN : Color.GRAY);
                try {
                    dao.updateCompleted(task.id, next);
                    if (onChanged != null) onChanged.run();
                } catch (Exception ex) {
                    task.completed = !next;
                    circle.setFill(task.completed ? Color.GREEN : Color.GRAY);
                    new Alert(Alert.AlertType.ERROR, "저장 실패: " + ex.getMessage()).showAndWait();
                }
            });

            // 삭제
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "이 일정을 삭제할까요?\n\n" + task.title, ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText("삭제 확인");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        try {
                            dao.deleteById(task.id);
                            if (onChanged != null) onChanged.run();
                            // 리스트에서 즉시 제거
                            if (getListView() != null) getListView().getItems().remove(task);
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR, "삭제 실패: " + ex.getMessage()).showAndWait();
                        }
                    }
                });
            });

            setGraphic(root);
        }

        private Integer ddayOf(String dueAt) {
            try {
                if (dueAt == null || dueAt.isBlank()) return null;
                LocalDate due = LocalDate.parse(App.shortDate(dueAt));
                LocalDate today = LocalDate.now();
                return (int) java.time.temporal.ChronoUnit.DAYS.between(today, due);
            } catch (Exception e) {
                return null;
            }
        }
    }

    // ==============================================================
    // 오늘 패널 셀: 완료/미완료 토글 + 삭제 버튼 + 🔁반복 배지
    // ==============================================================
    private static class TaskCell extends ListCell<Task> {
        private final TaskDao dao;
        private final Runnable onChanged;

        private final Circle circle = new Circle(6);
        private final Label title  = new Label();
        private final Label meta   = new Label();
        private final Label recurLabel = new Label();
        private final Label doneLabel = new Label();
        private final Button toggleBtn = new Button("완료/미완료");
        private final Button deleteBtn = new Button("삭제");

        private final HBox header = new HBox(8, circle, title, recurLabel, doneLabel, toggleBtn, deleteBtn);
        private final VBox root = new VBox(2, header, meta);

        TaskCell(TaskDao dao, Runnable onChanged) {
            this.dao = dao;
            this.onChanged = onChanged;
            header.setAlignment(Pos.CENTER_LEFT);
            root.setAlignment(Pos.CENTER_LEFT);
            recurLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
            meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        }
        TaskCell(TaskDao dao) { this(dao, null); }

        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) { setGraphic(null); return; }

            title.setText(task.title);

            // 🔁 반복 배지
            if (task.isRecurring == 1) {
                recurLabel.setText("🔁 반복");
                recurLabel.setVisible(true); recurLabel.setManaged(true);
            } else {
                recurLabel.setText("");
                recurLabel.setVisible(false); recurLabel.setManaged(false);
            }

            // 마감/시간/D-day
            String dueStr  = App.shortDate(task.dueAt);
            String timeStr = App.shortTime(task.dueAt);
            Integer dday   = ddayOf(task.dueAt);

            String dueLabel = dueStr.isEmpty() ? "" : (" / 마감: " + dueStr + (timeStr.isEmpty() ? "" : " " + timeStr));
            String dText    = (dday == null ? "" : " / D" + (dday == 0 ? "-DAY" : (dday > 0 ? "-" + dday : "+" + Math.abs(dday))));
            meta.setText("(우선순위 " + task.priority + ")" + dueLabel + dText);

            applyVisual(task.completed);

            // 경고색
            if (!task.completed && dday != null) {
                if (dday < 0)      meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #d32f2f;");
                else if (dday == 0) meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #f57c00;");
                else               meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
            } else if (task.completed) {
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #2e7d32;");
            } else {
                meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
            }

            // 완료/미완료 토글
            toggleBtn.setOnAction(e -> {
                boolean next = !task.completed;
                task.completed = next;
                applyVisual(next);
                try {
                    dao.updateCompleted(task.id, next);
                    if (onChanged != null) onChanged.run();
                } catch (Exception ex) {
                    task.completed = !next;
                    applyVisual(task.completed);
                    new Alert(Alert.AlertType.ERROR, "저장 실패: " + ex.getMessage()).showAndWait();
                }
            });

            // 삭제
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "이 일정을 삭제할까요?\n\n" + task.title, ButtonType.OK, ButtonType.CANCEL);
                confirm.setHeaderText("삭제 확인");
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        try {
                            dao.deleteById(task.id);
                            if (onChanged != null) onChanged.run();
                            if (getListView() != null) getListView().getItems().remove(task);
                        } catch (Exception ex) {
                            new Alert(Alert.AlertType.ERROR, "삭제 실패: " + ex.getMessage()).showAndWait();
                        }
                    }
                });
            });

            setGraphic(root);
        }

        private void applyVisual(boolean completed) {
            circle.setFill(completed ? Color.GREEN : Color.GRAY);
            doneLabel.setText(completed ? "완료" : "");
            doneLabel.setStyle("-fx-text-fill: " + (completed ? "#2e7d32" : "#555") + "; -fx-font-size: 11px;");
        }

        private Integer ddayOf(String dueAt) {
            try {
                if (dueAt == null || dueAt.isBlank()) return null;
                LocalDate due = LocalDate.parse(App.shortDate(dueAt));
                LocalDate today = LocalDate.now();
                return (int) java.time.temporal.ChronoUnit.DAYS.between(today, due);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
