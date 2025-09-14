package Client.timetable;

import Client.ClientNetworkHelper;
import Server.model.Request;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import Client.util.EventBus;

import java.util.*;

/**
 * 学生端-我的课表（对接后端实现）
 * 展示当前学期课表，支持多节连上课程显示
 */
public class TimetablePanel extends BorderPane {
    // 颜色定义
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String BG_COLOR = "#f8fbff";
    private static final String CARD_BG = "#ffffff";
    private static final String HEADER_BG = PRIMARY_COLOR;
    private static final String HEADER_TEXT = "#ffffff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final String SUB_TEXT = "#666666";
    private static final String BORDER_COLOR = "#e2e8f0";

    // 课程颜色池（确保视觉区分度）
    private static final String[] COURSE_COLORS = {
            "#4e8cff", "#6EFF7E", "#ff9f43", "#ff6b6b", "#a55eea",
            "#fd9644", "#26de81", "#fc5c65", "#778ca3", "#fed330"
    };

    // 时间槽定义
    private static final String[] TIME_SLOTS = {
            "08:00-08:45", "08:50-09:35", "09:50-10:35", "10:40-11:25",
            "11:30-12:15", "14:00-14:45", "14:50-15:35", "15:50-16:35",
            "16:40-17:25", "17:30-18:15", "19:00-19:45", "19:50-20:35", "20:40-21:25"
    };

    private static final String[] DAYS_OF_WEEK = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final String cardNumber;
    private final List<Map<String, Object>> courses = new ArrayList<>();
    private final Map<String, List<CourseSlot>> scheduleMap = new HashMap<>();
    private final List<CourseSlot> unscheduledCourses = new ArrayList<>();
    private GridPane timetableGrid;
    private Label loadingLabel;

    public TimetablePanel(String cardNumber) {
        this.cardNumber = cardNumber;
        initializeUI();
        loadTimetableData();
    }

    private void initializeUI() {
        setStyle("-fx-background-color: " + BG_COLOR + ";");
        // 去掉外边距以最大化可用垂直空间
        setPadding(new Insets(0));

        // 不在顶部显示标题，节省垂直空间以显示更多表格内容

        // 加载提示（先放在 center）
        loadingLabel = new Label("正在加载课表...");
        loadingLabel.setStyle("-fx-text-fill: " + SUB_TEXT + "; -fx-font-size: 12px;");
        VBox container = new VBox(0);
        container.setPadding(new Insets(0));
        container.getChildren().add(loadingLabel);
        setCenter(container);

        // 订阅全局选课变更事件，以便在其他面板（如选课界面）执行选/退后自动刷新课表
        try {
            EventBus.addListener("student:courseChanged", this::loadTimetableData);
        } catch (Exception ignored) {}
    }

    private void loadTimetableData() {
        new Thread(() -> {
            try {
                Gson gson = new Gson();

                // 优先使用学生已选课程来构建个人课表（支持退选后即时刷新）
                boolean handled = false;
                if (cardNumber != null && !cardNumber.trim().isEmpty()) {
                    try {
                        // 构造请求，确保 cardNumber 以数字类型发送（服务器期望数字）
                        Map<String, Object> data = new HashMap<>();
                        try {
                            data.put("cardNumber", Integer.parseInt(cardNumber));
                        } catch (NumberFormatException nfe) {
                            // 如果不是纯数字，仍传字符串作为回退
                            data.put("cardNumber", cardNumber);
                        }
                        Request req = new Request("getStudentSelectedCourses", data);
                        String selResp = ClientNetworkHelper.send(req);
                        System.out.println("[Timetable] getStudentSelectedCourses raw: " + selResp);
                        Map<String, Object> selResult = gson.fromJson(selResp, Map.class);
                        if (selResult != null && Boolean.TRUE.equals(selResult.get("success"))) {
                            Object dataObj = selResult.get("data");
                            if (dataObj instanceof List) {
                                List<Map<String, Object>> tcs = (List<Map<String, Object>>) dataObj;
                                courses.clear();
                                scheduleMap.clear();
                                unscheduledCourses.clear();
                                for (Map<String, Object> tc : tcs) {
                                    String sched = tc.get("schedule") == null ? "" : String.valueOf(tc.get("schedule"));
                                    String place = tc.get("place") == null ? "" : String.valueOf(tc.get("place"));
                                    String teacher = tc.get("teacherName") == null ? (tc.get("teacher") == null ? "" : String.valueOf(tc.get("teacher"))) : String.valueOf(tc.get("teacherName"));
                                    String cname = "";
                                    Object courseObj = tc.get("course");
                                    if (courseObj instanceof Map) {
                                        Object cn = ((Map) courseObj).get("courseName");
                                        if (cn != null) cname = String.valueOf(cn);
                                    }
                                    if (cname.isEmpty()) cname = tc.get("courseId") == null ? "" : String.valueOf(tc.get("courseId"));

                                    parseScheduleEntry(cname, sched, place, teacher);
                                }
                                handled = true;
                                Platform.runLater(this::renderTimetable);
                            }
                        } else {
                            System.out.println("[Timetable] getStudentSelectedCourses returned no data or failed: " + selResp);
                        }
                    } catch (Exception ex) {
                        System.out.println("[Timetable] getStudentSelectedCourses error: " + ex.getMessage());
                    }
                }

                if (handled) return; // 已用已选课程构建课表，直接返回

                // 原来的回退逻辑：获取所有课程并解析（适用于非登录状态或调试）
                String response = ClientNetworkHelper.getAllCourses();
                System.out.println("[Timetable] raw response: " + response);
                Map<String, Object> result = null;
                try {
                    result = gson.fromJson(response, Map.class);
                } catch (Exception ex) {
                    System.err.println("[Timetable] parse response error: " + ex.getMessage());
                }

                if (result == null) {
                    final String resp = response;
                    Platform.runLater(() -> showError("加载失败: 无法解析服务器响应 -> " + (resp == null ? "null" : resp)));
                    return;
                }

                // 额外请求所有教学班，用于后端没有在 course.teachingClasses 返回时补充信息
                List<Map<String, Object>> allTeachingClasses = Collections.emptyList();
                try {
                    String tcResp = ClientNetworkHelper.getAllTeachingClasses();
                    Map<String, Object> tcResult = gson.fromJson(tcResp, Map.class);
                    if (tcResult != null && Boolean.TRUE.equals(tcResult.get("success"))) {
                        Object dataObj = tcResult.get("data");
                        if (dataObj instanceof List) {
                            allTeachingClasses = (List<Map<String, Object>>) dataObj;
                        }
                    } else {
                        System.out.println("[Timetable] getAllTeachingClasses returned no data or failed");
                    }
                } catch (Exception ignored) {
                    System.out.println("[Timetable] getAllTeachingClasses failed: " + ignored.getMessage());
                }

                // build index by courseId
                Map<String, List<Map<String, Object>>> tcByCourse = new HashMap<>();
                for (Map<String, Object> tc : allTeachingClasses) {
                    Object cid = tc.get("courseId");
                    if (cid == null) continue;
                    String courseId = String.valueOf(cid);
                    tcByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(tc);
                }

                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> courseList = (List<Map<String, Object>>) result.get("data");
                    courses.clear();
                    scheduleMap.clear();
                    unscheduledCourses.clear();

                    for (Map<String, Object> course : courseList) {
                        courses.add(course);
                        // 优先解析教学班内的 schedule（服务端 TeachingClass 存放在 teachingClasses 字段）
                        Object tcsObj = course.get("teachingClasses");
                        List<Map<String, Object>> tcs = null;
                        if (tcsObj instanceof List) {
                            tcs = (List<Map<String, Object>>) tcsObj;
                        } else {
                            // 如果 course 未返回教学班，尝试使用全局教学班索引补充
                            Object cidObj = course.get("courseId");
                            if (cidObj != null) {
                                String cid = String.valueOf(cidObj);
                                if (tcByCourse.containsKey(cid)) {
                                    tcs = tcByCourse.get(cid);
                                }
                            }
                        }

                        if (tcs != null) {
                            for (Map<String, Object> tc : tcs) {
                                String sched = tc.get("schedule") == null ? "" : String.valueOf(tc.get("schedule"));
                                String place = tc.get("place") == null ? "" : String.valueOf(tc.get("place"));
                                String teacher = tc.get("teacherName") == null ? (tc.get("teacher") == null ? "" : String.valueOf(tc.get("teacher"))) : String.valueOf(tc.get("teacherName"));
                                String cname = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
                                parseScheduleEntry(cname, sched, place, teacher);
                            }
                        } else {
                            // 兼容旧结构：直接从 course 顶层取 schedule/place/teacher
                            parseCourseSchedule(course);
                        }
                    }

                    Platform.runLater(this::renderTimetable);
                } else {
                    String msg = result.get("message") == null ? String.valueOf(result.get("code")) : String.valueOf(result.get("message"));
                    Platform.runLater(() -> showError("加载失败: " + msg));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("网络错误: " + e.getMessage()));
            }
        }).start();
    }

    private void parseCourseSchedule(Map<String, Object> course) {
        // 兼容不同类型的 schedule 字段（可能为 null、数字或字符串）
        Object schedObj = course.get("schedule");
        String schedule = schedObj == null ? "" : String.valueOf(schedObj).trim();
        String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
        String location = course.getOrDefault("place", "") == null ? "" : String.valueOf(course.getOrDefault("place", ""));
        String teacher = course.getOrDefault("teacher", "") == null ? "" : String.valueOf(course.getOrDefault("teacher", ""));

        if (schedule.isEmpty()) {
            // 没有具体 schedule，视为未排课
            unscheduledCourses.add(new CourseSlot(courseName, location, teacher, null, 0, 0, COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]));
            return;
        }

        boolean parsedAny = false;
        for (String day : DAYS_OF_WEEK) {
            if (!schedule.contains(day)) continue;
            String timePart = schedule.replace(day, "").trim();

            // 支持多种格式："1-2节"、"1-2"、"1节"、"1" 或 时间段 "08:00-09:40"
            String tp = timePart;
            if (tp.endsWith("节")) tp = tp.substring(0, tp.length() - 1).trim();

            int[] res = parseTimeToSlots(tp);
            if (res != null) {
                int startSlot = res[0];
                int duration = res[1];
                if (duration > 0 && startSlot >= 1) {
                    CourseSlot slot = new CourseSlot(courseName, location, teacher, day, startSlot, duration,
                            COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]);
                    scheduleMap.computeIfAbsent(day, k -> new ArrayList<>()).add(slot);
                    parsedAny = true;
                }
            } else {
                System.err.println("[Timetable] 无法解析时间片: '" + timePart + "' for course: " + courseName);
            }
        }

        if (!parsedAny) {
            // 如果无法解析出具体 week-day 节次，尝试按纯时间解析并加入未排课
            int[] res = parseTimeToSlots(schedule.replaceAll("节", "").trim());
            if (res != null) {
                // 时间存在但没有 day 信息，加入未排课，用 day=null 表示
                unscheduledCourses.add(new CourseSlot(courseName, location, teacher, null, res[0], res[1], COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]));
            } else {
                // 完全无法解析，加入未排课占位
                unscheduledCourses.add(new CourseSlot(courseName, location, teacher, null, 0, 0, COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]));
            }
        }
    }

    // 将类似 "1-2" / "1" / "08:00-09:40" 映射到节次 start & duration
    private int[] parseTimeToSlots(String tp) {
        if (tp == null) return null;
        tp = tp.trim();
        if (tp.isEmpty()) return null;
        // 1-2 或 1
        try {
            if (tp.matches("\\d+-\\d+")) {
                String[] parts = tp.split("-");
                int s = Integer.parseInt(parts[0].trim());
                int e = Integer.parseInt(parts[1].trim());
                if (s >= 1 && e >= s) return new int[]{s, e - s + 1};
            }
            if (tp.matches("\\d+")) {
                int s = Integer.parseInt(tp);
                if (s >= 1) return new int[]{s, 1};
            }
        } catch (NumberFormatException ignored) {}

        // 时间段 HH:mm-HH:mm，尝试匹配 TIME_SLOTS
        if (tp.matches("\\d{1,2}:\\d{2}\\s*-\\s*\\d{1,2}:\\d{2}")) {
            String[] parts = tp.split("-");
            String start = parts[0].trim();
            String end = parts[1].trim();
            // 找到第一个 slot whose start equals start (or contains start)
            int first = -1;
            int last = -1;
            for (int i = 0; i < TIME_SLOTS.length; i++) {
                String slot = TIME_SLOTS[i]; // e.g. "08:00-08:45"
                String[] se = slot.split("-");
                String s = se[0].trim();
                String e = se.length > 1 ? se[1].trim() : s;
                if (first == -1) {
                    if (s.equals(start) || start.startsWith(s) || s.startsWith(start)) {
                        first = i + 1; // slots are 1-based
                    }
                }
                if (first != -1) {
                    // if this slot end equals or overlaps end time, mark last
                    if (e.equals(end) || end.startsWith(e) || e.startsWith(end) || compareTime(e, end) >= 0) {
                        last = i + 1;
                        break;
                    }
                }
            }
            if (first != -1) {
                if (last == -1) last = first;
                return new int[]{first, last - first + 1};
            }
        }
        return null;
    }

    // 比较时间字符串 HH:mm，返回 negative if a < b, 0 if equal, positive if a > b
    private int compareTime(String a, String b) {
        try {
            String[] pa = a.split(":");
            String[] pb = b.split(":");
            int ha = Integer.parseInt(pa[0].trim());
            int ma = Integer.parseInt(pa[1].trim());
            int hb = Integer.parseInt(pb[0].trim());
            int mb = Integer.parseInt(pb[1].trim());
            return (ha * 60 + ma) - (hb * 60 + mb);
        } catch (Exception ex) {
            return 0;
        }
    }

    private void renderTimetable() {
        // 减少 container 间距并去掉上方空白，使表格尽量向上对齐
        VBox mainContainer = new VBox(0);
        mainContainer.setPadding(new Insets(0));

        // 创建课表网格
        GridPane timetableGrid = createTimetableGrid();
        // 精确计算 GridPane 高度并去掉额外顶部空白
        double estimatedRowHeight = 50;
        // 减小 GridPane 内边距，以便表格更靠上
        double gridPadTop = 4, gridPadBottom = 4;
        timetableGrid.setPadding(new Insets(gridPadTop, 10, gridPadBottom, 10));
        double gridMinH = (TIME_SLOTS.length + 1) * estimatedRowHeight + gridPadTop + gridPadBottom;
        timetableGrid.setMinHeight(gridMinH);
        this.timetableGrid = timetableGrid;
        populateTimetable();

        // 渲染完成后隐藏加载提示
        if (loadingLabel != null) loadingLabel.setVisible(false);

        ScrollPane scrollPane = new ScrollPane(timetableGrid);
        scrollPane.setFitToWidth(true);
        // 禁用额外填充并让视口高度恰好等于表格高度以便表格向上贴边
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPadding(new Insets(0));
        // 让视口高度与表格最小高度一致（包含 GridPane 内边距），确保表格顶部无额外空白
        scrollPane.setPrefViewportHeight(gridMinH);

        // 标题已固定在 top，center 只放置滚动区域
        mainContainer.getChildren().add(scrollPane);
        setCenter(mainContainer);
    }

    private GridPane createTimetableGrid() {
        GridPane grid = new GridPane();
        grid.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        grid.setPadding(new Insets(15));
        grid.setHgap(2);
        grid.setVgap(2);

        // 设置列约束（时间列较窄，课程列等宽）
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPrefWidth(100);
        grid.getColumnConstraints().add(timeCol);

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPrefWidth(120);
            col.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(col);
        }

        // 设置行约束（每行高度固定）
        for (int i = 0; i <= TIME_SLOTS.length; i++) {
            RowConstraints row = new RowConstraints();
            row.setPrefHeight(50);
            row.setVgrow(Priority.NEVER);
            grid.getRowConstraints().add(row);
        }

        return grid;
    }

    private void populateTimetable() {
        // 添加表头
        addHeaderCell(timetableGrid, 0, 0, "时间/日期");
        for (int i = 0; i < DAYS_OF_WEEK.length; i++) {
            addHeaderCell(timetableGrid, i + 1, 0, DAYS_OF_WEEK[i]);
        }

        // 添加时间行
        for (int timeSlot = 0; timeSlot < TIME_SLOTS.length; timeSlot++) {
            addTimeCell(timetableGrid, 0, timeSlot + 1, TIME_SLOTS[timeSlot]);
        }

        // 先填充空白单元格作为占位（便于后面课程覆盖）
        for (int day = 1; day <= 7; day++) {
            for (int time = 1; time <= TIME_SLOTS.length; time++) {
                if (getNodeFromGridPane(timetableGrid, day, time) == null) {
                    addEmptyCell(timetableGrid, day, time);
                }
            }
        }

        // 填充课程（在占位之上添加，确保占据多行时显示完整）
        for (int dayIdx = 0; dayIdx < DAYS_OF_WEEK.length; dayIdx++) {
            String day = DAYS_OF_WEEK[dayIdx];
            List<CourseSlot> dayCourses = scheduleMap.getOrDefault(day, new ArrayList<>());

            for (CourseSlot course : dayCourses) {
                int row = course.startSlot; // 直接从1开始
                int rowSpan = course.duration;

                if (row >= 1 && row <= TIME_SLOTS.length) {
                    // 在添加课程前移除被占位的空 Pane（如果存在），以免遮挡
                    Node existing = getNodeFromGridPane(timetableGrid, dayIdx + 1, row);
                    if (existing != null && existing instanceof Pane) {
                        timetableGrid.getChildren().remove(existing);
                    }
                    addCourseCell(timetableGrid, dayIdx + 1, row, rowSpan, course);
                }
            }
        }
    }

    private void addHeaderCell(GridPane grid, int col, int row, String text) {
        StackPane cell = new StackPane();
        cell.setStyle("-fx-background-color: " + HEADER_BG + "; -fx-background-radius: 6;");
        cell.setPadding(new Insets(10));

        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + HEADER_TEXT + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        label.setAlignment(Pos.CENTER);

        cell.getChildren().add(label);
        grid.add(cell, col, row);
    }

    private void addTimeCell(GridPane grid, int col, int row, String timeText) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.CENTER);
        cell.setStyle("-fx-background-color: #f1f6ff; -fx-background-radius: 4;");
        cell.setPadding(new Insets(5));

        Label slotLabel = new Label("第" + row + "节");
        slotLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");

        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + SUB_TEXT + ";");

        cell.getChildren().addAll(slotLabel, timeLabel);
        grid.add(cell, col, row);
    }

    private void addCourseCell(GridPane grid, int col, int row, int rowSpan, CourseSlot course) {
        VBox courseBox = new VBox(3);
        courseBox.setAlignment(Pos.TOP_LEFT);
        courseBox.setStyle("-fx-background-color: " + course.color + "; -fx-background-radius: 6; " +
                "-fx-border-color: " + darken(course.color, 0.2) + "; -fx-border-radius: 6; -fx-border-width: 1;");
        courseBox.setPadding(new Insets(8));

        // 课程名称（截断过长的名称）
        String displayName = course.name.length() > 12 ? course.name.substring(0, 10) + "..." : course.name;
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setWrapText(true);

        // 地点和教师
        Label infoLabel = new Label(course.location + "\n" + course.teacher);
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.9);");
        infoLabel.setWrapText(true);

        courseBox.getChildren().addAll(nameLabel, infoLabel);

        // 设置行跨度
        grid.add(courseBox, col, row, 1, rowSpan);

        // 添加悬停效果
        courseBox.setOnMouseEntered(e -> {
            courseBox.setStyle("-fx-background-color: " + darken(course.color, 0.1) + "; -fx-background-radius: 6; " +
                    "-fx-border-color: " + darken(course.color, 0.3) + "; -fx-border-radius: 6; -fx-border-width: 1;");
        });
        courseBox.setOnMouseExited(e -> {
            courseBox.setStyle("-fx-background-color: " + course.color + "; -fx-background-radius: 6; " +
                    "-fx-border-color: " + darken(course.color, 0.2) + "; -fx-border-radius: 6; -fx-border-width: 1;");
        });
    }

    private void addEmptyCell(GridPane grid, int col, int row) {
        Pane cell = new Pane();
        cell.setStyle("-fx-background-color: #fafafa; -fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0.5;");
        grid.add(cell, col, row);
    }

    private Node getNodeFromGridPane(GridPane gridPane, int col, int row) {
        for (Node node : gridPane.getChildren()) {
            Integer nodeColObj = GridPane.getColumnIndex(node);
            Integer nodeRowObj = GridPane.getRowIndex(node);
            Integer colSpanObj = GridPane.getColumnSpan(node);
            Integer rowSpanObj = GridPane.getRowSpan(node);
            int nodeCol = nodeColObj == null ? 0 : nodeColObj;
            int nodeRow = nodeRowObj == null ? 0 : nodeRowObj;
            int colSpan = colSpanObj == null ? 1 : colSpanObj;
            int rowSpan = rowSpanObj == null ? 1 : rowSpanObj;
            if (col >= nodeCol && col < nodeCol + colSpan && row >= nodeRow && row < nodeRow + rowSpan) {
                return node;
            }
        }
        return null;
    }

    // 显示错误信息在面板中央
    private void showError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 16px;");
        setCenter(errorLabel);
        if (loadingLabel != null) loadingLabel.setVisible(false);
    }

    private String darken(String hex, double amount) {
        Color color = Color.web(hex);
        return String.format("#%02x%02x%02x",
                (int)(color.getRed() * 255 * (1 - amount)),
                (int)(color.getGreen() * 255 * (1 - amount)),
                (int)(color.getBlue() * 255 * (1 - amount))
        );
    }

    // 将各种 day 表示（中文/英文缩写/英文全名/数字1-7）规范化为中文 "周一".."周日"
    private String normalizeDay(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        // 中文直接返回含周的形式
        if (s.startsWith("周") || s.startsWith("星期")) {
            if (s.length() >= 2) {
                // 处理 "星期一" 或 "周一"
                if (s.startsWith("星期")) return "周" + s.substring(2, 3);
                return s.startsWith("周") && s.length() >= 2 ? s.substring(0, 2) : null;
            }
        }
        // 数字形式 1..7 或 01..07
        try {
            int v = Integer.parseInt(s);
            if (v >= 1 && v <= 7) {
                return DAYS_OF_WEEK[v - 1];
            }
        } catch (Exception ignored) {}
        // 英文全称/缩写
        String lower = s.toLowerCase();
        switch (lower) {
            case "mon": case "monday": return "周一";
            case "tue": case "tues": case "tuesday": return "周二";
            case "wed": case "wednesday": return "周三";
            case "thu": case "thurs": case "thursday": return "周四";
            case "fri": case "friday": return "周五";
            case "sat": case "saturday": return "周六";
            case "sun": case "sunday": return "周日";
            default: return null;
        }
    }

    // 课程槽位数据类
    private static class CourseSlot {
        String name;
        String location;
        String teacher;
        String day;
        int startSlot;
        int duration;
        String color;

        CourseSlot(String name, String location, String teacher, String day,
                   int startSlot, int duration, String color) {
            this.name = name;
            this.location = location;
            this.teacher = teacher;
            this.day = day;
            this.startSlot = startSlot;
            this.duration = duration;
            this.color = color;
        }
    }

    // 解析单个教学班的 schedule 字符串并加入 scheduleMap
    private void parseScheduleEntry(String courseName, String schedule, String place, String teacher) {
        if (schedule == null) schedule = "";
        schedule = schedule.trim();
        if (schedule.isEmpty()) return;

        // 如果是 JSON 格式，尝试解析为 Map 或 List 并抽取常见字段
        if ((schedule.startsWith("{") && schedule.endsWith("}")) || (schedule.startsWith("[") && schedule.endsWith("]"))) {
            Gson gson = new Gson();
            try {
                if (schedule.startsWith("[")) {
                    // JSON 数组，解析为 List
                    List<Map<String, Object>> arr = gson.fromJson(schedule, List.class);
                    for (Map<String, Object> item : arr) {
                        if (item == null) continue;
                        // 尝试从 item 中抽取 day/time 或作为 time 值
                        String day = extractDayFromMap(item);
                        String time = extractTimeFromMap(item);
                        if (day != null && time != null) {
                            String combined = normalizeDay(day);
                            combined = (combined == null ? day : combined) + time;
                            Map<String, Object> tmp = new HashMap<>();
                            tmp.put("courseName", courseName);
                            tmp.put("schedule", combined);
                            tmp.put("place", place);
                            tmp.put("teacher", teacher);
                            parseCourseSchedule(tmp);
                        } else if (time != null) {
                            // 没有 day，只能作为未排课程（带时间）
                            int[] res = parseTimeToSlots(time);
                            if (res != null) {
                                unscheduledCourses.add(new CourseSlot(courseName, place, teacher, null, res[0], res[1], COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]));
                            }
                        }
                    }
                    return;
                } else {
                    Map<String, Object> s = gson.fromJson(schedule, Map.class);
                    // 处理空键或无名键的情况：取第一个非空值作为 time
                    String day = extractDayFromMap(s);
                    String time = extractTimeFromMap(s);

                    if (day == null && (time == null)) {
                        // 特殊：可能是 {"": "1-2节"} 或 {"": "08:00-09:40"}
                        for (Map.Entry<String, Object> e : s.entrySet()) {
                            String k = e.getKey();
                            String v = e.getValue() == null ? null : String.valueOf(e.getValue());
                            if (v == null) continue;
                            // 尝试从 value 中提取时间片
                            String extracted = extractTimeFromString(v);
                            if (extracted != null) {
                                time = extracted;
                                break;
                            }
                        }
                    }

                    if (day != null && time != null) {
                        String combined = normalizeDay(day);
                        combined = (combined == null ? day : combined) + time;
                        Map<String, Object> tmp = new HashMap<>();
                        tmp.put("courseName", courseName);
                        tmp.put("schedule", combined);
                        tmp.put("place", place);
                        tmp.put("teacher", teacher);
                        parseCourseSchedule(tmp);
                        System.out.println("[Timetable] parsed JSON schedule -> " + combined + " for " + courseName);
                        return;
                    }

                    if (time != null && day == null) {
                        int[] res = parseTimeToSlots(time);
                        if (res != null) {
                            unscheduledCourses.add(new CourseSlot(courseName, place, teacher, null, res[0], res[1], COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]));
                            return;
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("[Timetable] JSON schedule parse failed: " + ex.getMessage());
                // fallthrough to plain text handling
            }
        }

        // 不是 JSON 或 JSON 解析失败，按纯文本尝试解析（例如: "周一1-2节"、"周二5节"、"周三3-4"、或者只有时间段）
        // 先尝试把字符串中的时间片提取出来
        String extractedTime = extractTimeFromString(schedule);
        String dayFound = null;
        for (String d : DAYS_OF_WEEK) {
            if (schedule.contains(d)) { dayFound = d; break; }
            // 也支持数字/英文 day 放在字符串中
        }
        if (dayFound != null && extractedTime != null) {
            Map<String, Object> tmp = new HashMap<>();
            tmp.put("courseName", courseName);
            tmp.put("schedule", dayFound + extractedTime);
            tmp.put("place", place);
            tmp.put("teacher", teacher);
            parseCourseSchedule(tmp);
            return;
        }

        // 无 day 信息但有时间
        if (extractedTime != null) {
            int[] res = parseTimeToSlots(extractedTime);
            if (res != null) {
                unscheduledCourses.add(new CourseSlot(courseName, place, teacher, null, res[0], res[1], COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]));
                return;
            }
        }

        // 其它情况作为未排课占位
        unscheduledCourses.add(new CourseSlot(courseName, place, teacher, null, 0, 0, COURSE_COLORS[Math.abs(courseName.hashCode()) % COURSE_COLORS.length]));
    }

    // 尝试从 Map 中提取 day 字段（支持多种命名）
    private String extractDayFromMap(Map<String, Object> m) {
        if (m == null) return null;
        Object o = null;
        if (m.get("day") != null) o = m.get("day");
        else if (m.get("weekday") != null) o = m.get("weekday");
        else if (m.get("week") != null) o = m.get("week");
        else if (m.get("dayOfWeek") != null) o = m.get("dayOfWeek");
        if (o != null) return String.valueOf(o);
        // 也检查键是否本身是 day-like
        for (String k : m.keySet()) {
            String norm = normalizeDay(k);
            if (norm != null) return k;
        }
        return null;
    }

    // 尝试从 Map 中提取 time 字段或第一个匹配的时间值
    private String extractTimeFromMap(Map<String, Object> m) {
        if (m == null) return null;
        Object o = null;
        if (m.get("time") != null) o = m.get("time");
        else if (m.get("timeRange") != null) o = m.get("timeRange");
        else if (m.get("range") != null) o = m.get("range");
        if (o != null) return String.valueOf(o);
        // 其他可能：值本身可能是时间
        for (Object v : m.values()) {
            if (v == null) continue;
            String s = String.valueOf(v);
            String extracted = extractTimeFromString(s);
            if (extracted != null) return extracted;
        }
        return null;
    }

    // 从任意字符串中提取首个时间片样式（如 1-2, 1, 08:00-09:40），容忍非数字/乱码
    private String extractTimeFromString(String s) {
        if (s == null) return null;
        String t = s.trim();
        // 先尝试数字区间
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("(\\d{1,2}\\s*-\\s*\\d{1,2})");
        java.util.regex.Matcher m1 = p1.matcher(t);
        if (m1.find()) return m1.group(1).replaceAll("\\s", "");
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("(\\d{1,2})\\s*节");
        java.util.regex.Matcher m2 = p2.matcher(t);
        if (m2.find()) return m2.group(1);
        // 时间段 HH:MM-HH:MM
        java.util.regex.Pattern p3 = java.util.regex.Pattern.compile("(\\d{1,2}:\\d{2}\\s*-\\s*\\d{1,2}:\\d{2})");
        java.util.regex.Matcher m3 = p3.matcher(t);
        if (m3.find()) return m3.group(1).replaceAll("\\s", "");
        // 退化到查找简单数字范围例如 '1-2��' 中的 '1-2'
        java.util.regex.Pattern p4 = java.util.regex.Pattern.compile("(\\d{1,2}\\s*-\\s*\\d{1,2})");
        java.util.regex.Matcher m4 = p4.matcher(t);
        if (m4.find()) return m4.group(1).replaceAll("\\s", "");
        // 查找单个数字
        java.util.regex.Pattern p5 = java.util.regex.Pattern.compile("(\\d{1,2})");
        java.util.regex.Matcher m5 = p5.matcher(t);
        if (m5.find()) return m5.group(1);
        return null;
    }
}
