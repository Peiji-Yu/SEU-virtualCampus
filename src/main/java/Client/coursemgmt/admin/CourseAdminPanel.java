package Client.coursemgmt.admin;

import Client.ClientNetworkHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.google.gson.reflect.TypeToken;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员-课程管理（前端演示版，无后端）。
 * 功能：搜索（按学生/教师/教学班）、表格展示、增删改、刷新。
 * @author Copilot
 */
public class CourseAdminPanel extends VBox {
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String PRIMARY_HOVER_COLOR = "#3d7bff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final double BUTTON_WIDTH = 110;

    // 后端一致课程模型
    public static class Course {
        public String courseId;     // 课程编号
        public String courseName;   // 课程名
        public String school;       // 开设学院
        public float credit;        // 学分
        // 新增字段
        public List<String> students = new ArrayList<>(); // 学生列表
        public String teacher = ""; // 教师
        public String clazz = "";   // 教学班
        public String room = ""; // 教室
        public int capacity = 0;   // 容量
        public String schedule = ""; // 上课时间
        public Course() {}
        public Course(String courseId, String courseName, String school, float credit) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.school = school;
            this.credit = credit;
        }
    }

    private final List<Course> allData = new ArrayList<>(); // 后端数据
    private TableView<Course> table;

    private void addControlBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("输入课程编号/名称/学院...");
        ComboBox<String> searchType = new ComboBox<>();
        searchType.getItems().addAll("全部", "编号", "名称", "学院");
        searchType.setValue("全部");
        Button searchBtn = new Button("搜索");
        setPrimaryButtonStyle(searchBtn);
        uniformButtonWidth(searchBtn);
        searchBtn.setOnAction(e -> doSearchBackend(searchType.getValue(), searchField.getText()));
        Button addBtn = new Button("新增课程");
        setPrimaryButtonStyle(addBtn);
        uniformButtonWidth(addBtn);
        addBtn.setOnAction(e -> showEditDialog(null));
        Button editBtn = new Button("编辑课程");
        setPrimaryButtonStyle(editBtn);
        uniformButtonWidth(editBtn);
        editBtn.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showWarn("请先选择要编辑的课程"); return; }
            showEditDialog(selected);
        });
        Button delBtn = new Button("删除课程");
        setPrimaryButtonStyle(delBtn);
        uniformButtonWidth(delBtn);
        delBtn.setOnAction(e -> {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { showWarn("请先选择要删除的课程"); return; }
            doDeleteCourse(selected.courseId);
        });
        Button refreshBtn = new Button("刷新");
        setPrimaryButtonStyle(refreshBtn);
        uniformButtonWidth(refreshBtn);
        refreshBtn.setOnAction(e -> refreshFromAll());
        bar.getChildren().addAll(searchType, searchField, searchBtn, addBtn, editBtn, delBtn, refreshBtn);
        getChildren().add(1, bar);
    }

    private void doSearchBackend(String type, String value) {
        new Thread(() -> {
            try {
                String resp = null;
                if ("全部".equals(type) || value.trim().isEmpty()) {
                    resp = ClientNetworkHelper.getAllCourses();
                } else if ("编号".equals(type)) {
                    resp = ClientNetworkHelper.getCourseById(value.trim());
                } else if ("名称".equals(type)) {
                    resp = ClientNetworkHelper.getCourseByName(value.trim());
                } else if ("学院".equals(type)) {
                    resp = ClientNetworkHelper.getCourseBySchool(value.trim());
                }
                java.lang.reflect.Type typeToken = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, typeToken);
                Object codeObj = result.get("code");
                int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
                Platform.runLater(() -> {
                    if (code == 200) {
                        java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                        List<Map<String, Object>> courseList = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(result.get("data")), listType);
                        List<Course> filtered = new ArrayList<>();
                        if (courseList != null) {
                            for (Map<String, Object> course : courseList) {
                                Course c = new Course();
                                c.courseId = (String) course.getOrDefault("courseId", "");
                                c.courseName = (String) course.getOrDefault("courseName", "");
                                c.school = (String) course.getOrDefault("school", "");
                                c.credit = ((Number) course.getOrDefault("credit", 0)).floatValue();
                                filtered.add(c);
                            }
                        }
                        table.getItems().setAll(filtered);
                    } else {
                        showAlert(Alert.AlertType.ERROR, "搜索失败", "code=" + code);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "搜索失败", e.getMessage()));
            }
        }).start();
    }

    private void showEditDialog(Course course) {
        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle(course == null ? "新增课程" : "编辑课程");
        dialog.setHeaderText(null);
        ButtonType okType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));
        TextField idField = new TextField(); idField.setPromptText("课程编号");
        TextField nameField = new TextField(); nameField.setPromptText("课程名称");
        TextField schoolField = new TextField(); schoolField.setPromptText("开设学院");
        TextField creditField = new TextField(); creditField.setPromptText("学分");
        if (course != null) {
            idField.setText(course.courseId); idField.setDisable(true);
            nameField.setText(course.courseName);
            schoolField.setText(course.school);
            creditField.setText(String.valueOf(course.credit));
        }
        grid.add(new Label("课程编号:"), 0, 0); grid.add(idField, 1, 0);
        grid.add(new Label("课程名称:"), 0, 1); grid.add(nameField, 1, 1);
        grid.add(new Label("开设学院:"), 0, 2); grid.add(schoolField, 1, 2);
        grid.add(new Label("学分:"), 0, 3); grid.add(creditField, 1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okType) {
                String id = idField.getText().trim();
                String name = nameField.getText().trim();
                String school = schoolField.getText().trim();
                float credit;
                try { credit = Float.parseFloat(creditField.getText().trim()); } catch (Exception e) { credit = 0; }
                if (id.isEmpty() || name.isEmpty() || school.isEmpty() || credit <= 0) {
                    showAlert(Alert.AlertType.WARNING, "数据校验", "请填写完整且合法的课程信息");
                    return null;
                }
                return new Course(id, name, school, credit);
            }
            return null;
        });
        Optional<Course> result = dialog.showAndWait();
        result.ifPresent(c -> {
            if (course == null) doAddCourse(c); else doUpdateCourse(c);
        });
    }

    private void doAddCourse(Course c) {
        new Thread(() -> {
            try {
                String resp = ClientNetworkHelper.addCourse(c.courseId, c.courseName, c.school, c.credit);
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                Object codeObj = result.get("code");
                int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
                Platform.runLater(() -> {
                    if (code == 200) {
                        showAlert(Alert.AlertType.INFORMATION, "新增成功", "课程已添加");
                        refreshFromAll();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "新增失败", "code=" + code);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "新增失败", e.getMessage()));
            }
        }).start();
    }

    private void doUpdateCourse(Course c) {
        new Thread(() -> {
            try {
                String resp = ClientNetworkHelper.updateCourse(c.courseId, c.courseName, c.school, c.credit);
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                Object codeObj = result.get("code");
                int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
                Platform.runLater(() -> {
                    if (code == 200) {
                        showAlert(Alert.AlertType.INFORMATION, "编辑成功", "课程信息已更新");
                        refreshFromAll();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "编辑失败", "code=" + code);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "编辑失败", e.getMessage()));
            }
        }).start();
    }

    private void doDeleteCourse(String courseId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定要删除该课程吗？", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            new Thread(() -> {
                try {
                    String resp = ClientNetworkHelper.deleteCourse(courseId);
                    Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                    Object codeObj = result.get("code");
                    int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
                    Platform.runLater(() -> {
                        if (code == 200) {
                            showAlert(Alert.AlertType.INFORMATION, "删除成功", "课程已删除");
                            refreshFromAll();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "删除失败", "code=" + code);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "删除失败", e.getMessage()));
                }
            }).start();
        }
    }

    // 构造方法中调用 addControlBar()
    public CourseAdminPanel() {
        setPadding(new Insets(18));
        setSpacing(10);
        init();
        addControlBar();
        loadAll(); // 初始化时加载后端数据
    }

    private void init() {
        Label title = new Label("课程管理");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");
        // 表格初始化
        table = new TableView<>();
        TableColumn<Course, String> idCol = new TableColumn<>("课程编号");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().courseId));
        TableColumn<Course, String> nameCol = new TableColumn<>("课程名称");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().courseName));
        TableColumn<Course, String> schoolCol = new TableColumn<>("开设学院");
        schoolCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().school));
        TableColumn<Course, String> creditCol = new TableColumn<>("学分");
        creditCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().credit)));
        table.getColumns().addAll(idCol, nameCol, schoolCol, creditCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        getChildren().addAll(title, table);
    }

    private void loadAll() {
        new Thread(() -> {
            try {
                String resp = ClientNetworkHelper.getAllCourses();
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                Object codeObj = result.get("code");
                int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
                if (code == 200) {
                    List<Map<String, Object>> courseList = (List<Map<String, Object>>) result.get("data");
                    Platform.runLater(() -> {
                        allData.clear();
                        for (Map<String, Object> course : courseList) {
                            Course c = new Course();
                            c.courseId = (String) course.getOrDefault("courseId", "");
                            c.courseName = (String) course.getOrDefault("courseName", "");
                            c.school = (String) course.getOrDefault("school", "");
                            c.credit = ((Number) course.getOrDefault("credit", 0)).floatValue();
                            allData.add(c);
                        }
                        table.getItems().setAll(allData);
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "课程数据加载失败: code=" + code);
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "课程数据加载失败: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void doSearch(String type, String value, boolean fuzzy){
        // 前端过滤演示
        new Thread(() -> {
            try {
                List<Course> filtered;
                String v = value==null?"":value.trim();
                if (type==null || "all".equals(type) || v.isEmpty()) {
                    filtered = new ArrayList<>(allData);
                } else if ("byStudent".equals(type)) {
                    filtered = allData.stream().filter(c -> matchList(c.students, v, fuzzy)).collect(Collectors.toList());
                } else if ("byTeacher".equals(type)) {
                    filtered = allData.stream().filter(c -> matchStr(c.teacher, v, fuzzy)).collect(Collectors.toList());
                } else if ("byClazz".equals(type)) {
                    filtered = allData.stream().filter(c -> matchStr(c.clazz, v, fuzzy)).collect(Collectors.toList());
                } else {
                    filtered = new ArrayList<>(allData);
                }
                Platform.runLater(() -> table.getItems().setAll(filtered));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "搜索失败", e.getMessage()));
            }
        }).start();
    }

    private boolean matchStr(String s, String v, boolean fuzzy){
        if (s==null) return false; return fuzzy ? s.contains(v) : s.equals(v);
    }
    private boolean matchList(List<String> list, String v, boolean fuzzy){
        if (list==null) return false; for(String s:list){ if (matchStr(s,v,fuzzy)) return true; } return false;
    }

    public void refreshFromAll(){
        // 编辑/删除后刷新表格，但保留当前简单筛选：直接重载全部
        loadAll();
    }

    private void setPrimaryButtonStyle(Button b){
        b.setPrefHeight(40);
        b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);");
        b.setOnMouseEntered(e-> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_HOVER_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(61,123,255,0.4), 10,0,0,3);"));
        b.setOnMouseExited(e-> b.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-background-color: "+PRIMARY_COLOR+"; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8,0,0,2);"));
    }
    private void uniformButtonWidth(Button b){b.setPrefWidth(BUTTON_WIDTH);b.setMinWidth(BUTTON_WIDTH);b.setMaxWidth(BUTTON_WIDTH);}

    private void showWarn(String msg){ showAlert(Alert.AlertType.WARNING, "选择提示", msg); }
    private void showAlert(Alert.AlertType type,String title,String msg){Alert a=new Alert(type);a.setTitle(title);a.setHeaderText(null);a.setContentText(msg);a.showAndWait();}

    private void seedDemo(){
        allData.clear();
        Course c1 = new Course(); c1.courseId="CS101"; c1.courseName="数据结构"; c1.school="计算机学院"; c1.credit=3;
        Course c2 = new Course(); c2.courseId="CS202"; c2.courseName="计算机网络"; c2.school="网络学院"; c2.credit=2;
        Course c3 = new Course(); c3.courseId="MA110"; c3.courseName="线性代数"; c3.school="数学学院"; c3.credit=4;
        allData.addAll(Arrays.asList(c1,c2,c3));
    }
}
