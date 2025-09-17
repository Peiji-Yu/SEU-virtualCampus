package Client.coursemgmt.admin;

import Server.model.Response;
import Server.model.course.TeachingClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;

import java.util.*;

/**
 * 管理员课程管理面板 - 卡片式展示，点击课程展开教学班（类似学生选课界面）
 */
public class CourseAdminPanel extends BorderPane {

    private VBox courseListContainer;
    private ScrollPane scrollPane;
    private Label statusLabel;
    private final Map<String, TeachingClass> teachingClassMap = new HashMap<>();
    // 缓存最近一次获取到的课程数据，便于本地搜索/过滤
    private List<Map<String, Object>> lastCourseList = new ArrayList<>();
    private Map<String, List<TeachingClass>> lastTcsByCourse = new HashMap<>();

    // 在初始化 UI 之前定义工具方法，避免静态分析器找不到方法
    private void performSearch(String type, String keyword) {
        if ((keyword == null || keyword.isEmpty()) && (lastCourseList == null || lastCourseList.isEmpty())) {
            // 没有数据可展示
            Platform.runLater(() -> displayCoursesByCourse(new ArrayList<>(), Collections.emptyMap()));
            return;
        }

        if (keyword == null || keyword.isEmpty()) {
            // 关键词为空，展示全部
            Platform.runLater(() -> displayCoursesByCourse(new ArrayList<>(lastCourseList), new HashMap<>(lastTcsByCourse)));
            statusLabel.setText("共加载 " + lastCourseList.size() + " 门课程");
            return;
        }

        String kwLower = keyword.toLowerCase();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> course : lastCourseList) {
            String courseId = course.get("courseId") == null ? "" : String.valueOf(course.get("courseId"));
            String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
            Object schoolObj = course.get("college") != null ? course.get("college") : course.get("school");
            String college = schoolObj == null ? "" : String.valueOf(schoolObj);

            boolean match = false;
            switch (type) {
                case "学院":
                    match = college.toLowerCase().contains(kwLower);
                    break;
                case "课程代码":
                    match = courseId.toLowerCase().contains(kwLower);
                    break;
                case "课程名称":
                    match = courseName.toLowerCase().contains(kwLower);
                    break;
                default:
                    // 全部：在课程代码/名称/学院中搜索
                    match = courseId.toLowerCase().contains(kwLower)
                            || courseName.toLowerCase().contains(kwLower)
                            || college.toLowerCase().contains(kwLower);
            }

            if (match) filtered.add(course);
        }

        final List<Map<String, Object>> toShow = filtered;
        Platform.runLater(() -> {
            displayCoursesByCourse(toShow, lastTcsByCourse);
            statusLabel.setText("搜索到 " + toShow.size() + " 门课程");
        });
    }

    // 统一样式化主按钮（用于搜索/清除等按钮）
    private void stylePrimaryButton(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        btn.setPrefHeight(40);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #0056b3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;"));
    }

    public CourseAdminPanel() {
        initializeUI();
        loadCourseData();
    }

    private void initializeUI() {
        Label titleLabel = new Label("课程管理");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 顶部标题栏：标题左侧，刷新图标放在右侧
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(16));
        titleBox.setStyle("-fx-background-color: #F6F8FA; -fx-border-color: #F6F8FA; -fx-border-width: 0 0 0 0;");

        // 占位区域以将刷新图标推到右侧
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        javafx.scene.control.Button refreshIconBtn;
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream("/Image/刷新.png"));
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(28);
            iv.setFitHeight(28);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            refreshIconBtn = new javafx.scene.control.Button();
            refreshIconBtn.setGraphic(iv);
        } catch (Exception ex) {
            // 资源加载失败时回退为文字按钮
            refreshIconBtn = new javafx.scene.control.Button("刷新");
        }
        refreshIconBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        refreshIconBtn.setOnAction(e -> loadCourseData());
        refreshIconBtn.setTooltip(new Tooltip("刷新课程数据"));

        titleBox.getChildren().addAll(titleLabel, spacer, refreshIconBtn);
        setTop(titleBox);

        statusLabel = new Label("正在加载课程数据...");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666666;");
        HBox statusBox = new HBox(statusLabel);
        statusBox.setPadding(new Insets(8, 16, 8, 16));


        // 使用与学籍管理一致的视觉样式（白色圆角卡片 + 阴影），但保留原有功能
        ComboBox<String> searchType = new ComboBox<>(FXCollections.observableArrayList("全部", "学院", "课程代码", "课程名称"));
        searchType.setValue("全部");
        searchType.setPrefWidth(140);
        searchType.setPrefHeight(40);
        searchType.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1;");

        TextField searchField = new TextField();
        searchField.setPromptText("输入搜索关键词（回车或点击搜索）");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e6ed; -fx-border-width: 1; -fx-font-size: 14px;");

        Button searchBtn = new Button("搜索");
        Button clearBtn = new Button("清除");

        HBox searchBox = new HBox(12);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(12, 16, 12, 16));
        searchBox.setStyle("-fx-background-color: #F6F8FA;");

        Label searchLabel = new Label("搜索条件:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-font-size: 14px;");

        // 按学籍管理风格统一按钮外观
        stylePrimaryButton(searchBtn);
        stylePrimaryButton(clearBtn);

        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchLabel, searchType, searchField, searchBtn, clearBtn);

        // 搜索按钮与回车支持：使用本地缓存 lastCourseList 进行过滤，避免重新请求服务器
        searchBtn.setOnAction(e -> {
            String kw = searchField.getText() == null ? "" : searchField.getText().trim();
            String type = searchType.getValue() == null ? "全部" : searchType.getValue();
            performSearch(type, kw);
        });
        // 回车触发搜索
        searchField.setOnAction(e -> {
            String kw = searchField.getText() == null ? "" : searchField.getText().trim();
            String type = searchType.getValue() == null ? "全部" : searchType.getValue();
            performSearch(type, kw);
        });
        // 清除按钮重置为全部
        clearBtn.setOnAction(e -> {
            searchField.clear();
            // 恢复全部展示
            Platform.runLater(() -> displayCoursesByCourse(new ArrayList<>(lastCourseList), new HashMap<>(lastTcsByCourse)));
            statusLabel.setText("共加载 " + lastCourseList.size() + " 门课程");
        });

        // 列表容器
        courseListContainer = new VBox(16);
        courseListContainer.setPadding(new Insets(16, 20, 20, 20));
        courseListContainer.setPrefWidth(1000);

        scrollPane = new ScrollPane(courseListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        // 允许搜索结果/课程列表向下延伸并占满可用空间：优先让 ScrollPane 在父容器中垂直扩展
        scrollPane.setPrefViewportHeight(600);
        courseListContainer.setPrefHeight(600);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 底部仅保留“新增课程”按钮，刷新已移动到顶部标题栏
        Button addCourseBtn = new Button("新增课程");
        addCourseBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addCourseBtn.setOnAction(e -> CourseCrud.showAddCourseDialog(this));
        HBox btnBox = new HBox(8, addCourseBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(8, 16, 12, 16));

        VBox body = new VBox(statusBox, searchBox, scrollPane, btnBox);
        body.setStyle("-fx-background-color: #F6F8FA; -fx-background-radius: 12;");
        body.setPadding(new Insets(12));

        setCenter(body);
        setPadding(new Insets(12));
        setStyle("-fx-background-color: #F6F8FA;");
    }

    void loadCourseData() {
        statusLabel.setText("正在加载课程数据...");
        courseListContainer.getChildren().clear();
        new Thread(() -> {
            try {
                List<Map<String, Object>> courseList = CourseService.fetchAllCourses();

                // 为每个课程获取教学班
                List<TeachingClass> allTcs = new ArrayList<>();
                for (Map<String, Object> course : courseList) {
                    String courseId = String.valueOf(course.get("courseId"));
                    List<TeachingClass> list = CourseService.fetchTeachingClassesByCourseId(courseId);
                    if (list != null) { allTcs.addAll(list); }
                }

                // 按课程分组
                Map<String, List<TeachingClass>> tcsByCourse = new HashMap<>();
                for (TeachingClass tc : allTcs) {
                    if (tc == null || tc.getCourseId() == null) {
                        continue;
                    }
                    tcsByCourse.computeIfAbsent(tc.getCourseId(), k -> new ArrayList<>()).add(tc);
                    if (tc.getUuid() != null) {
                        teachingClassMap.put(tc.getUuid().trim().toLowerCase(), tc);
                    }
                }

                // 缓存用于本地搜索
                lastCourseList.clear();
                lastCourseList.addAll(courseList);
                lastTcsByCourse.clear();
                lastTcsByCourse.putAll(tcsByCourse);

                final List<Map<String, Object>> courseListFinal = courseList;
                final Map<String, List<TeachingClass>> tcsByCourseFinal = tcsByCourse;

                Platform.runLater(() -> {
                    displayCoursesByCourse(courseListFinal, tcsByCourseFinal);
                    statusLabel.setText("共加载 " + courseListFinal.size() + " 门课程");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("网络错误: " + e.getMessage());
                    Alert a = new Alert(Alert.AlertType.ERROR, "网络异常: " + e.getMessage(), ButtonType.OK);
                    a.showAndWait();
                });
            }
        }).start();
    }

    // 按课程分组显示；点击课程头部展开/收起教学班
    private void displayCoursesByCourse(List<Map<String, Object>> courseList, Map<String, List<TeachingClass>> tcsByCourse) {
        courseListContainer.getChildren().clear();
        if (courseList == null || courseList.isEmpty()) {
            Label none = new Label("暂无课程");
            none.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
            courseListContainer.getChildren().add(none);
            return;
        }

        for (Map<String, Object> course : courseList) {
            String courseId = String.valueOf(course.get("courseId"));
            String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
            // 新增字段：学分和学院，兼容后端不同命名（优先支持 courseCredit/college，回退到 credit/school）
            Object credObj = course.get("courseCredit") != null ? course.get("courseCredit") : course.get("credit");
            Object schoolObj = course.get("college") != null ? course.get("college") : course.get("school");
            String courseCredit = credObj == null ? "" : String.valueOf(credObj);
            String college = schoolObj == null ? "" : String.valueOf(schoolObj);
            List<TeachingClass> tcs = tcsByCourse.getOrDefault(courseId, Collections.emptyList());

            // 使用拆分出的 CourseCard 组件来渲染课程卡片
            CourseCard cc = new CourseCard(course, tcs, this);
            courseListContainer.getChildren().add(cc);
        }

        // After building the course list, ensure the container reflows to avoid clipped rows
        courseListContainer.requestLayout();
    }

    // 本地简单学生行模型（用于显示名单），包含实际 cardNumber 以便退选操作
    private static class StudentRow {
        private final SimpleStringProperty cardNumber;
        private final SimpleStringProperty sid;
        private final SimpleStringProperty name;
        private final SimpleStringProperty major;
        private final SimpleStringProperty school;

        StudentRow(String cardNumber, String sid, String name, String major, String school) {
            this.cardNumber = new SimpleStringProperty(cardNumber == null ? "" : cardNumber);
            this.sid = new SimpleStringProperty(sid == null ? "" : sid);
            this.name = new SimpleStringProperty(name == null ? "" : name);
            this.major = new SimpleStringProperty(major == null ? "" : major);
            this.school = new SimpleStringProperty(school == null ? "" : school);
        }

        public SimpleStringProperty cardNumberProperty() {
            return cardNumber;
        }

        public SimpleStringProperty sidProperty() {
            return sid;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty majorProperty() {
            return major;
        }

        public SimpleStringProperty schoolProperty() {
            return school;
        }
    }

    // 管理员查看教学班已选学生名单的模态窗口
    void showStudentListDialog(String teachingClassUuid, String title) {
        if (teachingClassUuid == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "教学班 UUID 为空，无法查看名单");
            a.showAndWait();
            return;
        }
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("已选学生 - " + (title == null ? teachingClassUuid : title));

        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        Label status = new Label("正在加载名单...");
        status.setStyle("-fx-text-fill: #666666;");

        TableView<StudentRow> tv = new TableView<>();
        TableColumn<StudentRow, String> c1 = new TableColumn<>("学号");
        c1.setCellValueFactory(d -> d.getValue().sidProperty());
        c1.setPrefWidth(140);
        TableColumn<StudentRow, String> c2 = new TableColumn<>("姓名");
        c2.setCellValueFactory(d -> d.getValue().nameProperty());
        c2.setPrefWidth(120);
        TableColumn<StudentRow, String> c3 = new TableColumn<>("专业");
        c3.setCellValueFactory(d -> d.getValue().majorProperty());
        c3.setPrefWidth(160);
        TableColumn<StudentRow, String> c4 = new TableColumn<>("学院");
        c4.setCellValueFactory(d -> d.getValue().schoolProperty());
        c4.setPrefWidth(140);
        // 操作列：退选
        TableColumn<StudentRow, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(100);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button dropBtn = new Button("退选");

            {
                dropBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
                dropBtn.setOnAction(e -> {
                    StudentRow sr = getTableRow() == null ? null : (StudentRow) getTableRow().getItem();
                    if (sr == null) return;
                    Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "确认将该学生从本教学班退选吗？", ButtonType.OK, ButtonType.CANCEL);
                    Optional<ButtonType> r = conf.showAndWait();
                    if (!(r.isPresent() && r.get() == ButtonType.OK)) return;
                    // 异步发起退课请求（使用 teachingClassUuid 从外层捕获）
                    dropBtn.setDisable(true);
                    new Thread(() -> {
                        try {
                            Map<String, Object> data = new HashMap<>();
                            // 服务端期望 data.cardNumber 为 Double（服务器端直接 cast 为 Double），
                            // 因此这里优先把 cardNumber 字符串解析为数值并以 Double 发送，避免出现字符串导致的 ClassCastException
                            String cardStr = sr.cardNumberProperty().get();
                            if (cardStr == null || cardStr.trim().isEmpty()) {
                                Platform.runLater(() -> {
                                    Alert a = new Alert(Alert.AlertType.ERROR, "无法解析一卡通号（为空），已取消退选操作", ButtonType.OK);
                                    a.showAndWait();
                                    dropBtn.setDisable(false);
                                });
                                return;
                            }
                            Double cardDouble = null;
                            try {
                                java.math.BigDecimal bd = new java.math.BigDecimal(cardStr.trim());
                                cardDouble = bd.doubleValue();
                            } catch (Exception parseEx) {
                                try {
                                    long lv = Long.parseLong(cardStr.trim());
                                    cardDouble = (double) lv;
                                } catch (Exception ignore) {
                                    // 无法解析为数字，提示用户并返回
                                    Platform.runLater(() -> {
                                        Alert a = new Alert(Alert.AlertType.ERROR, "无法解析一卡通号: " + cardStr + "，已取消退选操作", ButtonType.OK);
                                        a.showAndWait();
                                        dropBtn.setDisable(false);
                                    });
                                    return;
                                }
                            }
                            data.put("cardNumber", cardDouble);
                            data.put("teachingClassUuid", teachingClassUuid);
                            Response rr = CourseService.sendDropCourse(cardDouble, teachingClassUuid);
                            Platform.runLater(() -> {
                                if (rr.getCode() == 200) {
                                    tv.getItems().remove(sr);
                                    status.setText("共 " + tv.getItems().size() + " 名学生");
                                    // 刷新课程数据以更新容量显示
                                    loadCourseData();
                                } else {
                                    Alert a = new Alert(Alert.AlertType.ERROR, "退选失败: " + rr.getMessage(), ButtonType.OK);
                                    a.showAndWait();
                                    dropBtn.setDisable(false);
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> {
                                Alert a = new Alert(Alert.AlertType.ERROR, "网络错误: " + ex.getMessage(), ButtonType.OK);
                                a.showAndWait();
                                dropBtn.setDisable(false);
                            });
                        }
                    }).start();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(dropBtn);
            }
        });
        tv.getColumns().addAll(c1, c2, c3, c4, actionCol);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        root.getChildren().addAll(status, tv);

        Scene scene = new Scene(root, 620, 420);
        dialog.setScene(scene);
        dialog.show();

        // 异步请求并填充名单
        new Thread(() -> {
            try {
                Map<String, Object> result = CourseService.getTeachingClassStudentsRaw(teachingClassUuid);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("data");
                    ObservableList<StudentRow> rows = FXCollections.observableArrayList();
                    if (students != null) {
                        for (Object stuObj : students) {
                            if (!(stuObj instanceof Map)) continue;
                            Map<?, ?> stu = (Map<?, ?>) stuObj;
                            Object cardObj = stu.get("cardNumber");
                            String cardNum;
                            if (cardObj == null) {
                                cardNum = "";
                            } else if (cardObj instanceof String) {
                                cardNum = (String) cardObj;
                            } else if (cardObj instanceof Number) {
                                cardNum = new java.math.BigDecimal(cardObj.toString()).toPlainString();
                                if (cardNum.indexOf('.') >= 0) { cardNum = cardNum.replaceAll("\\.?0+$", ""); }
                            } else {
                                cardNum = String.valueOf(cardObj);
                            }

                            Object stuNumObj = stu.get("studentNumber");
                            String sid;
                            if (stuNumObj == null) {
                                sid = "";
                            } else if (stuNumObj instanceof String) {
                                sid = (String) stuNumObj;
                            } else if (stuNumObj instanceof Number) {
                                sid = new java.math.BigDecimal(stuNumObj.toString()).toPlainString();
                                if (sid.indexOf('.') >= 0) { sid = sid.replaceAll("\\.?0+$", ""); }
                            } else {
                                sid = String.valueOf(stuNumObj);
                            }

                            String sname = stu.get("name") == null ? "" : String.valueOf(stu.get("name"));
                            String major = stu.get("major") == null ? "" : String.valueOf(stu.get("major"));
                            String school = stu.get("school") == null ? "" : String.valueOf(stu.get("school"));
                            rows.add(new StudentRow(cardNum, sid, sname, major, school));
                        }
                    }
                    Platform.runLater(() -> {
                        status.setText("共 " + rows.size() + " 名学生");
                        tv.setItems(rows);
                    });
                } else {
                    Platform.runLater(() -> status.setText("加载失败: " + String.valueOf(result.get("message"))));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> status.setText("网络错误: " + ex.getMessage()));
            }
        }).start();
    }

    void showAddClassForCourse(String courseId) {
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.setTitle("新增教学班");
        dialog.setHeaderText("为课程 " + courseId + " 添加教学班");

        TextField teacherField = new TextField();
        // 新：以可编辑的 day 选择 + time 输入 + 列表方式管理多个上课日/时间
        javafx.scene.control.ComboBox<String> dayChoice = new javafx.scene.control.ComboBox<>(FXCollections.observableArrayList("周一", "周二", "周三", "周四", "周五", "周六", "周日"));
        dayChoice.setEditable(true);
        dayChoice.setPrefWidth(140);
        TextField timeInput = new TextField();
        timeInput.setPromptText("例如: 9-11节 或 09:00-10:40");
        Button addScheduleBtn = new Button("添加到列表");
        ListView<String> scheduleList = new ListView<>(FXCollections.observableArrayList());
        scheduleList.setPrefHeight(120);
        Button removeScheduleBtn = new Button("移除所选");
        TextField placeField = new TextField();
        TextField capacityField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("教师姓名:"), 0, 0);
        grid.add(teacherField, 1, 0);
        grid.add(new Label("时间安排 (可添加多条):"), 0, 1);
        HBox schedInputRow = new HBox(8, dayChoice, timeInput, addScheduleBtn, removeScheduleBtn);
        schedInputRow.setAlignment(Pos.CENTER_LEFT);
        grid.add(schedInputRow, 1, 1);
        grid.add(scheduleList, 1, 2);
        grid.add(new Label("地点:"), 0, 3);
        grid.add(placeField, 1, 3);
        grid.add(new Label("容量:"), 0, 4);
        grid.add(capacityField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        // 添加 schedule 按钮逻辑
        addScheduleBtn.setOnAction(e -> {
            String day = dayChoice.getEditor().getText();
            if (day == null || day.trim().isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, "请填写上课日（如 周一）", ButtonType.OK);
                a.showAndWait();
                return;
            }
            String time = timeInput.getText() == null ? "" : timeInput.getText().trim();
            // 内部存储格式为 day||time 以便序列化
            scheduleList.getItems().add(day.trim() + "||" + time);
            scheduleList.getSelectionModel().clearSelection();
            timeInput.clear();
        });
        removeScheduleBtn.setOnAction(e -> {
            int sel = scheduleList.getSelectionModel().getSelectedIndex();
            if (sel >= 0) scheduleList.getItems().remove(sel);
        });

        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                try {
                    TeachingClass tc = new TeachingClass();
                    tc.setUuid(UUID.randomUUID().toString());
                    tc.setCourseId(courseId);
                    tc.setTeacherName(teacherField.getText());
                    // 将 scheduleList 条目转换为后端可接受的对象形式：Map<String, String>，同一天多条用逗号分隔
                    Map<String, String> schedMap = new LinkedHashMap<>();
                    for (String item : scheduleList.getItems()) {
                        if (item == null) continue;
                        String[] parts = item.split("\\|\\|", 2);
                        String d = parts.length >= 1 ? parts[0].trim() : "";
                        String t = parts.length >= 2 ? parts[1].trim() : "";
                        if (d.isEmpty()) continue;
                        if (t == null) t = "";
                        String prev = schedMap.get(d);
                        if (prev == null || prev.trim().isEmpty()) {
                            schedMap.put(d, t);
                        } else {
                            // 避免重复
                            List<String> list = new ArrayList<>(Arrays.asList(prev.split(",")));
                            if (!t.isEmpty() && !list.contains(t)) {
                                list.add(t);
                                schedMap.put(d, String.join(",", list));
                            }
                        }
                    }
                    tc.setSchedule(new com.google.gson.Gson().toJson(schedMap));
                    tc.setPlace(placeField.getText());
                    tc.setCapacity(Integer.parseInt(capacityField.getText()));
                    tc.setSelectedCount(0);
                    return tc;
                } catch (NumberFormatException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "容量必须为整数", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<TeachingClass> res = dialog.showAndWait();
        res.ifPresent(tc -> {
            // 调用网络接口添加
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("uuid", tc.getUuid());
                    data.put("courseId", tc.getCourseId());
                    data.put("teacherName", tc.getTeacherName());
                    // 把 tc.getSchedule()（JSON 字符串）解析为对象后再发送，避免发送字符串或数组导致后端校验失败
                    try {
                        Map<String, Object> scheduleObj = new com.google.gson.Gson().fromJson(tc.getSchedule(), Map.class);
                        data.put("schedule", scheduleObj == null ? new HashMap<>() : scheduleObj);
                    } catch (Exception ignore) {
                        data.put("schedule", tc.getSchedule());
                    }
                    data.put("place", tc.getPlace());
                    data.put("capacity", tc.getCapacity());
                    Response rr = CourseService.addTeachingClass(data);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "新增成功", ButtonType.OK);
                            a.showAndWait();
                            loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "新增失败: " + rr.getMessage(), ButtonType.OK);
                            a.showAndWait();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.ERROR, "网络异常: " + ex.getMessage(), ButtonType.OK);
                        a.showAndWait();
                    });
                }
            }).start();
        });
    }
}
