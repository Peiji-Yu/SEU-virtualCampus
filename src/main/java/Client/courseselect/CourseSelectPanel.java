package Client.courseselect;

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

        // 先填充演示数据，再构建列表
        seedDemo();

        VBox container = new VBox(12);
        container.setFillWidth(true);
        container.getChildren().add(buildHeader());
        container.getChildren().add(buildListCard());

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(sp);
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

        // 避免在缩放时被压缩到看不见
        name.setMaxWidth(Double.MAX_VALUE);
        room.setMaxWidth(Double.MAX_VALUE);
        sel.setMaxWidth(Double.MAX_VALUE);
        cap.setMaxWidth(Double.MAX_VALUE);

        Button action = new Button();
        action.setMinWidth(90);
        GridPane.setHalignment(action, HPos.RIGHT);
        action.setOnAction(e -> onToggle(c, sel));
        refreshButton(c, action);

        row.add(name, 0, 0);
        row.add(room, 1, 0);
        row.add(sel, 2, 0);
        row.add(cap, 3, 0);
        row.add(action, 4, 0);

        return row;
    }

    private void onToggle(Course c, Label selLabel) {
        boolean selected = mySelected.contains(c.id);
        if (!selected) {
            if (c.selected >= c.capacity) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "该课程已满");
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }
            mySelected.add(c.id);
            c.selected++;
        } else {
            mySelected.remove(c.id);
            if (c.selected > 0) c.selected--;
        }
        selLabel.setText(String.valueOf(c.selected));
        // 更新当前行按钮样式
        GridPane row = (GridPane) selLabel.getParent();
        Button btn = (Button) row.getChildren().get(row.getChildren().size() - 1);
        refreshButton(c, btn);
    }

    private void refreshButton(Course c, Button action) {
        boolean selected = mySelected.contains(c.id);
        if (selected) {
            action.setText("退选");
            action.setStyle("-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; -fx-background-radius: 6;");
            action.setDisable(false);
        } else {
            action.setText("选课");
            action.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-background-radius: 6;");
            action.setDisable(c.selected >= c.capacity);
        }
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

    private void seedDemo() {
        // 演示数据（增加更多课程）
        courses.clear();
        courses.addAll(Arrays.asList(
                new Course("C001", "高等数学（上）", "教四-201", 45, 50),
                new Course("C002", "大学英语（II）", "教二-105", 50, 50),
                new Course("C003", "数据结构", "机房-A301", 32, 40),
                new Course("C004", "线性代数", "教一-308", 28, 40),
                new Course("C005", "计算机网络", "实验-204", 18, 30),
                new Course("C006", "操作系统", "教三-207", 27, 40),
                new Course("C007", "数据库系统", "教五-110", 36, 40),
                new Course("C008", "概率论与数理统计", "教四-305", 22, 50),
                new Course("C009", "软件工程", "教二-209", 19, 40),
                new Course("C010", "马克思主义基本原理", "文科楼-402", 41, 50)
        ));
    }
}
