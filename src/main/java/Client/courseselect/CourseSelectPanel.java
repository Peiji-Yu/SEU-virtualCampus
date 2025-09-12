package Client.courseselect;

import Client.ClientNetworkHelper;
import javafx.geometry.Insets;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * 学生-选课（前端演示版，无后端交互）。
 * 展示课程名、教室、已选人数、课容量，右侧提供“选课/退选”按钮。
 */
public class CourseSelectPanel extends BorderPane {
    private static final String PRIMARY_COLOR = "#4e8cff"; // 与“我的课表”表头一致
    private static final String DANGER_COLOR = "#ff6b6b";
    private static final String BG = "#f8fbff";
    private static final String CARD = "#ffffff";
    private static final String TEXT = "#2a4d7b";
    private static final String SUB = "#555b66";
    private static final String BORDER = "#c0c9da";

    private static class Course {
        final String id;
        final String name;
        final String room;
        int selected;
        final int capacity;

        Course(String id, String name, String room, int selected, int capacity) {
            this.id = id; this.name = name; this.room = room; this.selected = selected; this.capacity = capacity;
        }
    }

    private final String cardNumber;
    private final List<Course> courses = new ArrayList<>();
    private final Set<String> mySelected = new HashSet<>();

    public CourseSelectPanel(String cardNumber) {
        this.cardNumber = cardNumber;
        setStyle("-fx-background-color: " + BG + ";");
        setPadding(new Insets(10));
        loadCoursesFromServer();
    }

    private void loadCoursesFromServer() {
        new Thread(() -> {
            try {
                String resp = ClientNetworkHelper.getAllCourses();
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> courseList = (List<Map<String, Object>>) result.get("data");
                    courses.clear();
                    for (Map<String, Object> course : courseList) {
                        String courseId = (String) course.get("courseId");
                        String courseName = (String) course.get("courseName");
                        String school = (String) course.get("school");
                        double credit = (double) course.get("credit");
                        // 获取教学班
                        String tcResp = ClientNetworkHelper.getTeachingClassesByCourseId(courseId);
                        Map<String, Object> tcResult = new com.google.gson.Gson().fromJson(tcResp, Map.class);
                        if (Boolean.TRUE.equals(tcResult.get("success"))) {
                            Map<String, Object> tc = (Map<String, Object>) tcResult.get("data");
                            String place = (String) tc.get("place");
                            int selectedCount = ((Double) tc.get("selectedCount")).intValue();
                            int capacity = ((Double) tc.get("capacity")).intValue();
                            courses.add(new Course(courseId, courseName, place, selectedCount, capacity));
                        }
                    }
                    // 查询已选课程
                    String selResp = ClientNetworkHelper.getStudentSelectedCourses(cardNumber);
                    Map<String, Object> selResult = new com.google.gson.Gson().fromJson(selResp, Map.class);
                    if (Boolean.TRUE.equals(selResult.get("success"))) {
                        List<Map<String, Object>> selected = (List<Map<String, Object>>) selResult.get("data");
                        mySelected.clear();
                        for (Map<String, Object> tc : selected) {
                            mySelected.add((String) tc.get("uuid"));
                        }
                    }
                    // 刷新UI
                    javafx.application.Platform.runLater(() -> {
                        getChildren().clear();
                        VBox container = new VBox(12);
                        container.setFillWidth(true);
                        container.getChildren().add(buildHeader());
                        container.getChildren().add(buildListCard());
                        ScrollPane sp = new ScrollPane(container);
                        sp.setFitToWidth(true);
                        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                        setCenter(sp);
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "课程数据加载失败: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private Node buildHeader() {
        VBox box = new VBox(4);
        Label title = new Label("选课");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        Label sub = new Label("本页面为演示数据，未对接后端");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: " + SUB + ";");
        box.getChildren().addAll(title, sub);
        return box;
    }

    private Region buildListCard() {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: " + CARD + "; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");

        // 表头使用与“我的课表”相同主色，文字为白色
        GridPane header = rowGrid();
        header.setMaxWidth(Double.MAX_VALUE);
        header.setStyle(baseRowStyle() + "-fx-background-color: " + PRIMARY_COLOR + ";");
        header.add(colHeaderLabel("课程名"), 0, 0);
        header.add(colHeaderLabel("教室"), 1, 0);
        header.add(colHeaderLabel("已选人数"), 2, 0);
        header.add(colHeaderLabel("课容量"), 3, 0);
        header.add(colHeaderLabel("操作"), 4, 0);

        VBox list = new VBox();
        list.setFillWidth(true);
        list.getChildren().add(header);

        // 课程行
        for (Course c : courses) {
            list.getChildren().add(buildCourseRow(c));
        }

        card.getChildren().add(list);
        return card;
    }

    private Label colHeaderLabel(String text) {
        Label lb = new Label(text);
        lb.setTextFill(Color.WHITE);
        lb.setStyle("-fx-font-weight: bold;");
        return lb;
    }

    private GridPane buildCourseRow(Course c) {
        GridPane row = rowGrid();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle(baseRowStyle());
        Label name = colLabel(c.name, false);
        Label room = colLabel(c.room, false);
        Label sel = colLabel(String.valueOf(c.selected), false);
        Label cap = colLabel(String.valueOf(c.capacity), false);
        name.setMaxWidth(Double.MAX_VALUE);
        room.setMaxWidth(Double.MAX_VALUE);
        sel.setMaxWidth(Double.MAX_VALUE);
        Button actionBtn = new Button();
        boolean isSelected = mySelected.contains(c.id);
        actionBtn.setText(isSelected ? "退课" : "选课");
        actionBtn.setStyle("-fx-background-color: " + (isSelected ? DANGER_COLOR : PRIMARY_COLOR) + "; -fx-text-fill: white;");
        actionBtn.setOnAction(e -> {
            new Thread(() -> {
                try {
                    String resp;
                    if (isSelected) {
                        resp = ClientNetworkHelper.dropCourse(cardNumber, c.id);
                    } else {
                        resp = ClientNetworkHelper.selectCourse(cardNumber, c.id);
                    }
                    Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(result.get("success").equals(Boolean.TRUE) ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                                (String) result.get("message"));
                        alert.showAndWait();
                        loadCoursesFromServer();
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "操作失败: " + ex.getMessage());
                        alert.showAndWait();
                    });
                }
            }).start();
        });
        row.add(name, 0, 0);
        row.add(room, 1, 0);
        row.add(sel, 2, 0);
        row.add(cap, 3, 0);
        row.add(actionBtn, 4, 0);
        return row;
    }

    private GridPane rowGrid() {
        GridPane gp = new GridPane();
        gp.setPadding(new Insets(10, 14, 10, 14));
        gp.setHgap(8);

        // 五列：课程名/教室/已选/容量/操作
        ColumnConstraints c0 = new ColumnConstraints(); // 课程名
        c0.setMinWidth(180);
        c0.setHgrow(Priority.ALWAYS);
        ColumnConstraints c1 = new ColumnConstraints(); // 教室
        c1.setMinWidth(130);
        ColumnConstraints c2 = new ColumnConstraints(); // 已选
        c2.setMinWidth(90);
        ColumnConstraints c3 = new ColumnConstraints(); // 容量
        c3.setMinWidth(90);
        ColumnConstraints c4 = new ColumnConstraints(); // 操作按钮
        c4.setMinWidth(110);

        gp.getColumnConstraints().addAll(c0, c1, c2, c3, c4);
        return gp;
    }


    private String baseRowStyle() {
        return "-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;";
    }

    private Label colLabel(String text, boolean header) {
        Label lb = new Label(text);
        lb.setTextFill(Color.web(header ? TEXT : SUB));
        lb.setStyle(header ? "-fx-font-weight: bold;" : "");
        return lb;
    }
}
