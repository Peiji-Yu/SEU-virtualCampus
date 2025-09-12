package Client.timetable;

import Client.ClientNetworkHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 学生端-我的课表（前端静态实现，暂不对接后端）。
 * 展示当前周（周一至周日）13小节课表。
 */
public class TimetablePanel extends BorderPane {
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String BG_COLOR = "#f8fbff";
    private static final String CARD_BG = "#ffffff";
    // 加深边框颜色以提升对比度
    private static final String BORDER_COLOR = "#c0c9da";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final String SUB_TEXT = "#555b66";
    // 表头专用颜色（让表头显著）
    private static final String HEADER_BG = PRIMARY_COLOR;
    private static final String HEADER_TEXT = "#ffffff";
    // 不再使用 TIME_BG，改为与表头一致
    // private static final String TIME_BG = "#eef3ff";
    private static final String COURSE_BG = "#b7cbff"; // 课程块更深底色

    // 仅保存时间范围，节次用索引+1生成
    private static final String[] TIME_SLOTS = new String[]{
            "08:00~08:45",
            "08:50~09:35",
            "09:50~10:35",
            "10:49~11:25",
            "11:30~12:15",
            "14:00~14:45",
            "14:50~15:35",
            "15:50~16:35",
            "16:40~17:25",
            "17:30~18:15",
            "19:00~19:45",
            "19:50~20:35",
            "20:40~21:25"
    };

    private static final String[] DAY_TITLES = {
            "周一", "周二", "周三", "周四", "周五", "周六", "周日"
    };

    // 简单课程模型（仅用于前端演示）
    private static class Course {
        final int dayIndex; // 0=周一 ... 6=周日
        final int startSlot; // 1..13
        final int length; // 连上几节
        final String name;
        final String location;
        final String teacher;

        Course(int dayIndex, int startSlot, int length, String name, String location, String teacher) {
            this.dayIndex = dayIndex;
            this.startSlot = startSlot;
            this.length = length;
            this.name = name;
            this.location = location;
            this.teacher = teacher;
        }
    }

    private final String cardNumber;
    private final List<Map<String, Object>> selectedCourses = new ArrayList<>();
    private final Map<String, List<Map<String, Object>>> scheduleMap = new HashMap<>();

    public TimetablePanel(String cardNumber) {
        this.cardNumber = cardNumber;
        setStyle("-fx-background-color: " + BG_COLOR + ";");
        setPadding(new Insets(10));
        loadTimetableFromServer();
    }

    private void loadTimetableFromServer() {
        new Thread(() -> {
            try {
                String resp = ClientNetworkHelper.getStudentSelectedCourses(cardNumber);
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> courseList = (List<Map<String, Object>>) result.get("data");
                    selectedCourses.clear();
                    scheduleMap.clear();
                    for (Map<String, Object> tc : courseList) {
                        String courseName = (String) tc.get("courseId");
                        String place = (String) tc.get("place");
                        String scheduleJson = (String) tc.get("schedule");
                        Map<String, String> schedule = new com.google.gson.Gson().fromJson(scheduleJson, Map.class);
                        for (Map.Entry<String, String> entry : schedule.entrySet()) {
                            String day = entry.getKey();
                            String time = entry.getValue();
                            Map<String, Object> courseInfo = new HashMap<>();
                            courseInfo.put("courseName", courseName);
                            courseInfo.put("place", place);
                            courseInfo.put("time", time);
                            if (!scheduleMap.containsKey(day)) scheduleMap.put(day, new ArrayList<>());
                            scheduleMap.get(day).add(courseInfo);
                        }
                    }
                    javafx.application.Platform.runLater(this::renderTimetable);
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Label error = new Label("课表加载失败: " + e.getMessage());
                    setCenter(error);
                });
            }
        }).start();
    }

    private void renderTimetable() {
        // 构建课表UI，按scheduleMap渲染
        VBox container = new VBox(12);
        container.setFillWidth(true);
        container.getChildren().add(buildHeader());
        container.getChildren().add(buildTable());
        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(sp);
    }

    private Node buildHeader() {
        Label title = new Label("我的课表");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");
        return title;
    }

    private Region buildTable() {
        GridPane grid = new GridPane();
        grid.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");
        grid.setPadding(new Insets(10));
        grid.setHgap(8);
        grid.setVgap(8);
        // 表头
        for (int i = 0; i < DAY_TITLES.length; i++) {
            Label dayLabel = new Label(DAY_TITLES[i]);
            dayLabel.setStyle("-fx-background-color: " + HEADER_BG + "; -fx-text-fill: " + HEADER_TEXT + "; -fx-font-weight: bold;");
            grid.add(dayLabel, i + 1, 0);
        }
        for (int i = 0; i < TIME_SLOTS.length; i++) {
            Label timeLabel = new Label((i+1) + "节 " + TIME_SLOTS[i]);
            timeLabel.setStyle("-fx-text-fill: " + SUB_TEXT + ";");
            grid.add(timeLabel, 0, i + 1);
        }
        // 课程块
        for (int dayIdx = 0; dayIdx < DAY_TITLES.length; dayIdx++) {
            String day = DAY_TITLES[dayIdx];
            List<Map<String, Object>> courses = scheduleMap.getOrDefault(day, Collections.emptyList());
            for (Map<String, Object> course : courses) {
                String courseName = (String) course.get("courseName");
                String place = (String) course.get("place");
                String time = (String) course.get("time");
                Label courseLabel = new Label(courseName + "\n" + place + "\n" + time);
                courseLabel.setStyle("-fx-background-color: " + COURSE_BG + "; -fx-background-radius: 6; -fx-text-fill: " + TEXT_COLOR + ";");
                // 简单放在第1节，实际可根据time解析节次
                grid.add(courseLabel, dayIdx + 1, 1);
            }
        }
        return grid;
    }

    private LocalDate getCurrentMonday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<Course> demoCourses(LocalDate monday) {
        // 与日期无关，仅展示；若需要可根据周次切换不同数据
        return Arrays.asList(
                new Course(0, 1, 2, "高等数学", "教四-201", "王老师"),
                new Course(0, 6, 2, "大学英语", "教二-105", "李老师"),
                new Course(1, 3, 3, "数据结构", "机房-A301", "赵老师"),
                new Course(2, 11, 2, "线性代数", "教一-308", "周老师"),
                new Course(4, 8, 2, "计算机网络", "实验中心-204", "孙老师"),
                new Course(5, 12, 2, "形势与政策", "文科楼-402", "陈老师")
        );
    }

    private Map<String, Course> toSlotMap(List<Course> courses) {
        Map<String, Course> map = new HashMap<>();
        for (Course c : courses) {
            for (int i = 0; i < c.length; i++) {
                int slot = c.startSlot + i;
                if (slot >= 1 && slot <= 13 && c.dayIndex >= 0 && c.dayIndex < 7) {
                    map.put((c.dayIndex + 1) + "-" + slot, c);
                }
            }
        }
        return map;
    }

    // ---- 网格添加辅助 ----
    private void addCell(GridPane grid, int col, int row, Node node) {
        grid.add(node, col, row);
        // 可在此统一添加 hover 效果或通用样式
    }

    // ---- 单元格工厂 ----
    private StackPane headerCell(String text) {
        StackPane p = new StackPane();
        p.setAlignment(Pos.CENTER);
        p.setStyle(cellBaseStyle() + "-fx-background-color: " + HEADER_BG + ";");
        Label lb = new Label(text);
        lb.setTextFill(Color.web(HEADER_TEXT));
        // 明确内联字体大小，避免被全局 CSS 覆盖
        lb.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        p.getChildren().add(lb);
        return p;
    }

    private StackPane timeCell(int slotIndex, String timeRange) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 8, 4, 10));

        Label top = new Label("第" + slotIndex + "节");
        top.setTextFill(Color.web(HEADER_TEXT));
        top.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Label bottom = new Label(timeRange);
        bottom.setTextFill(Color.web(HEADER_TEXT));
        bottom.setStyle("-fx-font-size: 12px;");

        box.getChildren().addAll(top, bottom);

        StackPane p = new StackPane(box);
        p.setStyle(cellBaseStyle() + "-fx-background-color: " + HEADER_BG + ";");
        return p;
    }

    private StackPane emptyCell() {
        StackPane p = new StackPane();
        p.setAlignment(Pos.CENTER);
        // 空白格稍微提亮背景，配合更深边框提高可读性
        p.setStyle(cellBaseStyle() + "-fx-background-color: " + lighten(CARD_BG, 0.02) + ";");
        return p;
    }

    private StackPane courseCell(Course c) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(6));

        Label name = new Label(c.name);
        name.setTextFill(Color.web(TEXT_COLOR));
        name.setStyle("-fx-font-weight: bold;");

        Label meta = new Label(c.location + " · " + c.teacher);
        meta.setTextFill(Color.web(SUB_TEXT));
        meta.setFont(Font.font(12));

        box.getChildren().addAll(name, meta);

        StackPane p = new StackPane(box);
        // 课程块：更深底色 + 更粗左侧强调条
        p.setStyle(
                "-fx-background-color: " + COURSE_BG + ";" +
                "-fx-border-color: " + PRIMARY_COLOR + " " + BORDER_COLOR + " " + BORDER_COLOR + " " + BORDER_COLOR + ";" +
                "-fx-border-width: 0 1 1 6;"
        );
        return p;
    }

    private String cellBaseStyle() {
        // 使用更清晰的网格线颜色
        return "-fx-border-color: " + BORDER_COLOR + "; -fx-border-width: 0 1 1 0;" +
                "-fx-background-radius: 0;";
    }

    private String lighten(String hex, double amount) {
        Color c = Color.web(hex);
        double r = clamp(c.getRed() + amount);
        double g = clamp(c.getGreen() + amount);
        double b = clamp(c.getBlue() + amount);
        return String.format("#%02x%02x%02x", (int)(r*255), (int)(g*255), (int)(b*255));
    }

    private double clamp(double v) { return Math.min(1.0, Math.max(0.0, v)); }
}
