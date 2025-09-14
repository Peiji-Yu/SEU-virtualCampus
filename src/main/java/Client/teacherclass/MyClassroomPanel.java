package Client.teacherclass;

import Client.ClientNetworkHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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

    // 传入构造参数为当前用户的一卡通号（字符串），内部先解析为真实姓名再请求教师教学班
    private final String teacherCardNumber;
    private String teacherName; // 解析自服务器的姓名，若解析失败回退为 cardNumber
    private final ObservableList<Course> myCourses = FXCollections.observableArrayList();

    private final ListView<Course> courseListView = new ListView<>();
    private final Label courseTitle = new Label();
    private final Label courseMeta = new Label();
    private final Label courseStats = new Label();
    private final TableView<Student> studentTable = new TableView<>();

    public MyClassroomPanel(String teacherCardNumber) {
        this.teacherCardNumber = teacherCardNumber;
        this.teacherName = teacherCardNumber; // 临时占位，稍后尝试解析为真实姓名
        setStyle("-fx-background-color: " + BG + ";");
        setPadding(new Insets(10));
        setPrefSize(900, 600);
        setLeft(buildCourseListPane());
        setCenter(buildCourseDetailPane());
        try {
            loadMyCoursesFromServer();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "加载课程失败: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadMyCoursesFromServer() throws Exception {
        new Thread(() -> {
            try {
                // 直接将当前一卡通号发给服务端，服务端负责将 cardNumber->用户名(name) 映射后查询教学班
                String resp = ClientNetworkHelper.getTeachingClassesByTeacherCardNumber(teacherCardNumber);
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> classList = (List<Map<String, Object>>) result.get("data");
                    javafx.application.Platform.runLater(() -> {
                        myCourses.clear();
                        for (Map<String, Object> tc : classList) {
                            String uuid = (String) tc.get("uuid");
                            String courseId = (String) tc.get("courseId");
                            String place = (String) tc.get("place");
                            int capacity = ((Double) tc.get("capacity")).intValue();
                            // 如果服务端返回了 selectedCount，则用占位学生填充列表以显示正确的已选人数
                            int selectedCount = 0;
                            if (tc.containsKey("selectedCount") && tc.get("selectedCount") != null) {
                                Object sc = tc.get("selectedCount");
                                if (sc instanceof Number) selectedCount = ((Number) sc).intValue();
                                else {
                                    try { selectedCount = Integer.parseInt(String.valueOf(sc)); } catch (Exception ignored) {}
                                }
                            }
                            List<Student> placeholders = new ArrayList<>();
                            for (int i = 0; i < Math.max(0, selectedCount); i++) placeholders.add(new Student("", "", "", ""));
                            Course course = new Course(uuid, courseId, place, capacity, new ArrayList<>());
                            // 将占位学生加入 course.students，以便显示计数
                            if (!placeholders.isEmpty()) course.students.addAll(placeholders);
                            myCourses.add(course);
                        }
                        courseListView.setItems(myCourses);
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "教学班加载失败: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadStudentsForClass(newVal.id);
                courseTitle.setText(newVal.name);
                courseMeta.setText("教室: " + newVal.room + "  容量: " + newVal.capacity);
            }
        });
    }

    private void loadStudentsForClass(String teachingClassUuid) {
        new Thread(() -> {
            try {
                String resp = ClientNetworkHelper.getTeachingClassStudents(teachingClassUuid);
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("data");
                    javafx.application.Platform.runLater(() -> {
                        ObservableList<Student> studentList = FXCollections.observableArrayList();
                        for (Map<String, Object> stu : students) {
                            // 处理 studentNumber 可能是 Double/Integer/Long 或 String 的情况，避免出现 9023231.0
                            Object stuNumObj = stu.get("studentNumber");
                            String sid;
                            if (stuNumObj == null) {
                                sid = "";
                            } else if (stuNumObj instanceof String) {
                                sid = (String) stuNumObj;
                            } else if (stuNumObj instanceof Number) {
                                // 使用 BigDecimal 避免科学计数法或小数尾部 .0
                                sid = new java.math.BigDecimal(stuNumObj.toString()).toPlainString();
                                // 去掉小数点后多余的零（例如 "9023231.0" -> "9023231"）
                                if (sid.indexOf('.') >= 0) {
                                    sid = sid.replaceAll("\\.?0+$", "");
                                }
                            } else {
                                sid = String.valueOf(stuNumObj);
                            }
                            String sname = (String) stu.get("name");
                            String major = (String) stu.get("major");
                            String clazz = (String) stu.get("school");
                            studentList.add(new Student(sid, sname, major, clazz));
                        }
                        studentTable.setItems(studentList);
                        // 更新对应 Course 的 students 列表，以便左侧课程列表显示正确的已选人数
                        Course cur = null;
                        for (Course c : myCourses) {
                            if (teachingClassUuid.equals(c.id)) { cur = c; break; }
                        }
                        if (cur != null) {
                            // 将 Course 中的 ObservableList 内容替换为最新的学生列表
                            cur.students.setAll(studentList);
                            // 强制 ListView 刷新显示（选中行除外），以更新右侧计数等
                            courseListView.refresh();
                        }
                        courseStats.setText("已选人数: " + studentList.size());
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "学生列表加载失败: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private Node buildCourseListPane() {
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
            if (n != null) {
                updateRight(n);
            }
        });

        VBox.setVgrow(courseListView, Priority.ALWAYS);
        box.getChildren().addAll(title, courseListView);
        return box;
    }

    private Node buildCourseDetailPane() {
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
        // 该列实际绑定的是学生的学院信息（从后端字段 school 获取），因此显示为“学院"
        TableColumn<Student, String> c4 = new TableColumn<>("学院");
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
}
