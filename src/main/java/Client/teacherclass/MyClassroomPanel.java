package Client.teacherclass;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * 教师端 - 我的课堂（前端静态实现）
 * 左侧：我负责的课程列表；右侧：课程详情 + 已选学生列表。
 */
public class MyClassroomPanel extends BorderPane {
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String BG = "#f8fbff";
    private static final String CARD = "#ffffff";
    private static final String TEXT = "#2a4d7b";
    private static final String SUB = "#555b66";
    private static final String BORDER = "#c0c9da";

    // 课程模型
    public static class Course {
        public final String id;
        public final String name;
        public final String room;
        public final int capacity;
        public final ObservableList<Student> students;
        public Course(String id, String name, String room, int capacity, List<Student> students) {
            this.id = id; this.name = name; this.room = room; this.capacity = capacity;
            this.students = FXCollections.observableArrayList(students);
        }
    }

    // 学生模型
    public static class Student {
        public final String sid;
        public final String sname;
        public final String major;
        public final String clazz;
        public Student(String sid, String sname, String major, String clazz) {
            this.sid = sid; this.sname = sname; this.major = major; this.clazz = clazz;
        }
    }

    private final String teacherCardNumber;
    private final ObservableList<Course> myCourses = FXCollections.observableArrayList();

    private final ListView<Course> courseListView = new ListView<>();
    private final Label courseTitle = new Label();
    private final Label courseMeta = new Label();
    private final Label courseStats = new Label();
    private final TableView<Student> studentTable = new TableView<>();

    public MyClassroomPanel(String teacherCardNumber) {
        this.teacherCardNumber = teacherCardNumber;
        setStyle("-fx-background-color: " + BG + ";");
        setPadding(new Insets(10));

        // 数据
        seedDemo();

        // 布局
        Node left = buildLeft();
        Node right = buildRight();
        SplitPane sp = new SplitPane(left, right);
        sp.setDividerPositions(0.28);
        setCenter(sp);

        // 默认选中第一门课
        if (!myCourses.isEmpty()) {
            courseListView.getSelectionModel().select(0);
            updateRight(myCourses.get(0));
        }
    }

    private Node buildLeft() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: " + CARD + "; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");

        Label title = new Label("我的课程");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");

        courseListView.setItems(myCourses);
        courseListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name + "\n" + item.room + "  (" + item.students.size() + "/" + item.capacity + ")");
                    setStyle("-fx-padding: 10; -fx-text-fill: " + TEXT + ";");
                }
            }
        });
        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) updateRight(n);
        });

        VBox.setVgrow(courseListView, Priority.ALWAYS);
        box.getChildren().addAll(title, courseListView);
        return box;
    }

    private Node buildRight() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + CARD + "; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 3);");

        // 顶部课程信息
        courseTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        courseMeta.setStyle("-fx-text-fill: " + SUB + ";");
        courseStats.setStyle("-fx-text-fill: " + SUB + ";");

        // 已选学生表
        TableColumn<Student, String> c1 = new TableColumn<>("学号");
        c1.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sid));
        c1.setPrefWidth(140);
        TableColumn<Student, String> c2 = new TableColumn<>("姓名");
        c2.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sname));
        c2.setPrefWidth(120);
        TableColumn<Student, String> c3 = new TableColumn<>("专业");
        c3.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().major));
        c3.setPrefWidth(160);
        TableColumn<Student, String> c4 = new TableColumn<>("班级");
        c4.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().clazz));
        c4.setPrefWidth(120);
        studentTable.getColumns().addAll(c1, c2, c3, c4);
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(studentTable, Priority.ALWAYS);

        card.getChildren().addAll(buildRightHeaderBar(), studentTable);
        return new StackPane(card);
    }

    private Node buildRightHeaderBar() {
        VBox header = new VBox(6);
        HBox line1 = new HBox(10);
        line1.setAlignment(Pos.CENTER_LEFT);
        HBox line2 = new HBox(10);
        line2.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> {
            // 前端静态数据，仅做简单重绘
            Course cur = courseListView.getSelectionModel().getSelectedItem();
            updateRight(cur);
        });
        refreshBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-background-radius: 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        line1.getChildren().addAll(courseTitle, spacer, refreshBtn);
        line2.getChildren().addAll(courseMeta, courseStats);
        header.getChildren().addAll(line1, line2);
        return header;
    }

    private void updateRight(Course c) {
        if (c == null) {
            courseTitle.setText("未选择课程");
            courseMeta.setText("");
            courseStats.setText("");
            studentTable.setItems(FXCollections.observableArrayList());
            return;
        }
        courseTitle.setText(c.name);
        courseMeta.setText("教室：" + c.room + "    课程编号：" + c.id);
        courseStats.setText("已选人数：" + c.students.size() + " / 容量：" + c.capacity);
        studentTable.setItems(c.students);
    }

    private void seedDemo() {
        // 根据教师卡号简单区分不同演示数据（无需真实关联）
        List<Student> s1 = Arrays.asList(
                new Student("220300001", "张三", "计算机科学与技术", "计科2101"),
                new Student("220300002", "李四", "计算机科学与技术", "计科2101"),
                new Student("220300103", "王五", "人工智能", "人工2102")
        );
        List<Student> s2 = Arrays.asList(
                new Student("220311001", "赵六", "信息安全", "信安2103"),
                new Student("220311045", "钱七", "信息安全", "信安2101")
        );
        List<Student> s3 = Arrays.asList(
                new Student("220322010", "周八", "软件工程", "软工2102"),
                new Student("220322066", "吴九", "软件工程", "软工2103"),
                new Student("220322099", "郑十", "软件工程", "软工2101"),
                new Student("220322120", "冯十一", "软件工程", "软工2101")
        );

        myCourses.clear();
        myCourses.addAll(FXCollections.observableArrayList(
                new Course("T001", "数据结构", "教四-201", 60, s1),
                new Course("T002", "计算机网络", "实验中心-204", 48, s2),
                new Course("T003", "操作系统", "教三-207", 56, s3)
        ));
    }
}

