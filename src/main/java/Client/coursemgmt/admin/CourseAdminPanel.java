package Client.coursemgmt.admin;

import Client.ClientNetworkHelper;
import Server.model.Request;
import Server.model.Response;
import Server.model.course.TeachingClass;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
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

    public CourseAdminPanel() {
        initializeUI();
        loadCourseData();
    }

    private void initializeUI() {
        Label titleLabel = new Label("课程管理");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-text-fill: #2a4d7b; -fx-font-weight: bold;");

        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(16));
        titleBox.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1px 0;");
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
        searchBox.setStyle("-fx-background-color: #ffffff;");

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
        scrollPane.setPrefViewportHeight(600); // 默认高度，方便在较小窗口中也能看到更多行
        courseListContainer.setPrefHeight(600); // 使内部容器在初始状态下有更高的可视高度
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadCourseData());
        Button addCourseBtn = new Button("新增课程");
        addCourseBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addCourseBtn.setOnAction(e -> showAddCourseDialog());
        HBox btnBox = new HBox(8, refreshBtn, addCourseBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(8, 16, 12, 16));

        VBox body = new VBox(statusBox, searchBox, scrollPane, btnBox);
        body.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12;");
        body.setPadding(new Insets(12));

        setCenter(body);
        setPadding(new Insets(12));
        setStyle("-fx-background-color: #f8fbff;");
    }

    private void loadCourseData() {
        statusLabel.setText("正在加载课程数据...");
        courseListContainer.getChildren().clear();
        new Thread(() -> {
            try {
                Request req = new Request("getAllCourses", new HashMap<>());
                String respStr = ClientNetworkHelper.send(req);
                Response resp = new Gson().fromJson(respStr, Response.class);
                if (resp.getCode() == 200) {
                    // 解析课程列表为通用 Map 列表（使用 TypeToken 避免 unchecked 警告）
                    List<Map<String, Object>> courseList = new ArrayList<>();
                    Object dataObj = resp.getData();
                    if (dataObj != null) {
                        com.google.gson.reflect.TypeToken<List<Map<String, Object>>> token = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){};
                        List<Map<String, Object>> parsed = new Gson().fromJson(new Gson().toJson(dataObj), token.getType());
                        if (parsed != null) courseList.addAll(parsed);
                    }

                    // 为每个课程获取教学班
                    List<TeachingClass> allTcs = new ArrayList<>();
                    for (Map<String, Object> course : courseList) {
                        String courseId = String.valueOf(course.get("courseId"));
                        Map<String, Object> data = new HashMap<>();
                        data.put("courseId", courseId);
                        Request tcReq = new Request("getTeachingClassesByCourseId", data);
                        String tcRespStr = ClientNetworkHelper.send(tcReq);
                        Response tcResp = new Gson().fromJson(tcRespStr, Response.class);
                        if (tcResp.getCode() == 200 && tcResp.getData() != null) {
                            Object tcData = tcResp.getData();
                            if (tcData instanceof List) {
                                List<TeachingClass> list = new Gson().fromJson(new Gson().toJson(tcData), new com.google.gson.reflect.TypeToken<List<TeachingClass>>(){}.getType());
                                allTcs.addAll(list);
                            } else {
                                TeachingClass tc = new Gson().fromJson(new Gson().toJson(tcData), TeachingClass.class);
                                allTcs.add(tc);
                            }
                        }
                    }

                    // 按课程分组
                    Map<String, List<TeachingClass>> tcsByCourse = new HashMap<>();
                    for (TeachingClass tc : allTcs) {
                        if (tc == null || tc.getCourseId() == null) continue;
                        tcsByCourse.computeIfAbsent(tc.getCourseId(), k -> new ArrayList<>()).add(tc);
                        if (tc.getUuid() != null) teachingClassMap.put(tc.getUuid().trim().toLowerCase(), tc);
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
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("加载失败: " + resp.getMessage());
                        Alert a = new Alert(Alert.AlertType.ERROR, "加载课程失败: " + resp.getMessage(), ButtonType.OK);
                        a.showAndWait();
                    });
                }
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

            VBox courseCard = new VBox(10);
            courseCard.setPadding(new Insets(12));
            courseCard.setPrefWidth(980);
            courseCard.setMaxWidth(Double.MAX_VALUE);
            courseCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 10;");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            // 在标题中额外显示学分与开设学院，格式友好展示
            String titleText = courseId + "  " + courseName;
            if (!courseCredit.isEmpty() || !college.isEmpty()) {
                titleText += "  (" + (courseCredit.isEmpty() ? "?" : courseCredit) + "学分";
                if (!college.isEmpty()) titleText += ", " + college;
                titleText += ")";
            }
            Label title = new Label(titleText);
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button addClassBtn = new Button("新增教学班");
            addClassBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
            addClassBtn.setOnAction(e -> {
                // 点击新增教学班：打开添加对话框并预填 courseId
                // reuse existing showAddClassDialog, but that uses selected course from table; here we'll implement inline simple dialog
                showAddClassForCourse(courseId);
            });

            Button editCourseBtn = new Button("编辑课程");
            editCourseBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");
            editCourseBtn.setOnAction(e -> showEditCourseDialog(course));

            Button delCourseBtn = new Button("删除课程");
            delCourseBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
            delCourseBtn.setOnAction(e -> deleteCourseConfirmed(course));

            header.getChildren().addAll(title, spacer, addClassBtn, editCourseBtn, delCourseBtn);

            FlowPane details = new FlowPane();
            details.setHgap(12);
            details.setVgap(12);
            details.setPadding(new Insets(10, 0, 0, 0));
            details.prefWrapLengthProperty().bind(courseCard.widthProperty().subtract(32));
            details.setVisible(false);
            details.setManaged(false);

            for (TeachingClass tc : tcs) {
                details.getChildren().add(createTeachingClassCard(tc, details));
            }

            // 如果该课程没有教学班则展示提示
            if (tcs.isEmpty()) {
                Label no = new Label("该课程当前无教学班");
                no.setStyle("-fx-text-fill: #888888;");
                details.getChildren().add(no);
            }

            header.setOnMouseClicked(e -> {
                boolean showing = details.isVisible();
                details.setVisible(!showing);
                details.setManaged(!showing);
                // Force a layout pass so FlowPane recalculates row heights immediately.
                // Use applyCss()/layout() on the FX thread to ensure synchronous reflow (more robust than requestLayout()).
                Platform.runLater(() -> {
                    try {
                        details.applyCss();
                        details.layout();
                        courseListContainer.applyCss();
                        courseListContainer.layout();
                    } catch (Exception ex) {
                        // fallback to requestLayout if direct layout fails
                        details.requestLayout();
                        courseListContainer.requestLayout();
                    }
                });
            });

            courseCard.getChildren().addAll(header, details);
            courseListContainer.getChildren().add(courseCard);
        }

        // After building the course list, ensure the container reflows to avoid clipped rows
        courseListContainer.requestLayout();
    }

    // 创建教学班卡片，带编辑/删除操作
    private Node createTeachingClassCard(TeachingClass tc, FlowPane parent) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        // 使用蓝色底，与学生选课界面保持一致
        String normalStyle = "-fx-background-radius: 6; -fx-border-color: #cfe4ff; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: linear-gradient(to bottom, #eaf4ff, #dceeff);";
        String hoverStyle = "-fx-background-radius: 6; -fx-border-color: #9fd0ff; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: linear-gradient(to bottom, #d6eeff, #c0e6ff); -fx-effect: dropshadow(gaussian, rgba(30,80,150,0.12), 8,0,0,2);";
        card.setStyle(normalStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));
        // Let the card compute its height from content instead of forcing a fixed height.
        // Fixed heights sometimes cause FlowPane rows to be clipped until a later repaint (e.g. on hover).
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMinHeight(Region.USE_COMPUTED_SIZE);

        try {
            card.prefWidthProperty().bind(parent.widthProperty()
                    .subtract(32)
                    .subtract(parent.getHgap() * 3)
                    .divide(4));
            card.minWidthProperty().bind(card.prefWidthProperty().multiply(0.75));
            card.maxWidthProperty().bind(card.prefWidthProperty().multiply(1.05));
        } catch (Exception ignored) {
            card.setPrefWidth(240);
        }

        Label teacher = new Label("教师: " + (tc.getTeacherName() == null ? "未知" : tc.getTeacherName()));
        teacher.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        teacher.setWrapText(true);

        Label schedule = new Label("时间: " + (tc.getSchedule() == null ? "未设置" : tc.getSchedule()));
        schedule.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        schedule.setWrapText(true);

        Label place = new Label("地点: " + (tc.getPlace() == null ? "未设置" : tc.getPlace()));
        place.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        place.setWrapText(true);

        // 显示格式为 已选/总容量，例如 "容量: 45/50"，并防御 null
        int sel = tc.getSelectedCount() == null ? 0 : tc.getSelectedCount();
        int cap = tc.getCapacity() == null ? 0 : tc.getCapacity();
        Label capacity = new Label("容量: " + sel + "/" + cap);
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        capacity.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewListBtn = new Button("查看名单");
        viewListBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff;");
        viewListBtn.setOnAction(e -> showStudentListDialog(tc.getUuid(), tc.getCourseId() + " " + (tc.getCourse() == null ? "" : tc.getCourse().getCourseName())));

        // 新增：添加学生按钮
        Button addStudentBtn = new Button("添加学生");
        addStudentBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        addStudentBtn.setOnAction(e -> {
            TextInputDialog inputDialog = new TextInputDialog();
            inputDialog.setTitle("添加学生到教学班");
            inputDialog.setHeaderText("请输入学生一卡通号");
            inputDialog.setContentText("一卡通号:");
            Optional<String> result = inputDialog.showAndWait();
            result.ifPresent(cardNumber -> {
                String cardNum = cardNumber == null ? "" : cardNumber.trim();
                if (cardNum.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "一卡通号不能为空", ButtonType.OK);
                    a.showAndWait();
                    return;
                }
                // 校验为纯数字
                if (!cardNum.matches("\\d+")) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "一卡通号必须为纯数字", ButtonType.OK);
                    a.showAndWait();
                    return;
                }
                new Thread(() -> {
                    try {
                        Map<String, Object> data = new HashMap<>();
                        // 按 selectCourse 规范发送 Long 类型
                        long cardLong = Long.parseLong(cardNum);
                        data.put("cardNumber", cardLong);
                        data.put("teachingClassUuid", tc.getUuid());
                        Request req = new Request("selectCourse", data);
                        String resp = ClientNetworkHelper.send(req);
                        Response rr = new Gson().fromJson(resp, Response.class);
                        Platform.runLater(() -> {
                            if (rr.getCode() == 200) {
                                Alert a = new Alert(Alert.AlertType.INFORMATION, "添加成功", ButtonType.OK);
                                a.showAndWait();
                                loadCourseData();
                            } else {
                                Alert a = new Alert(Alert.AlertType.ERROR, "添加失败: " + rr.getMessage(), ButtonType.OK);
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
        });

        Button editBtn = new Button("编辑");
        editBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");
        editBtn.setOnAction(e -> showEditClassDialogFor(tc));

        Button delBtn = new Button("删除");
        delBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        delBtn.setOnAction(e -> deleteTeachingClassConfirmed(tc));

        HBox btnRow = new HBox(8, spacer, viewListBtn, addStudentBtn, editBtn, delBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(teacher, schedule, place, capacity, btnRow);

        // Ensure layout is recalculated immediately to avoid clipping issues in FlowPane
        card.requestLayout();
        return card;
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
        public SimpleStringProperty cardNumberProperty() { return cardNumber; }
        public SimpleStringProperty sidProperty() { return sid; }
        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty majorProperty() { return major; }
        public SimpleStringProperty schoolProperty() { return school; }
    }

    // 管理员查看教学班已选学生名单的模态窗口
    private void showStudentListDialog(String teachingClassUuid, String title) {
        if (teachingClassUuid == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "教学班 UUID 为空，无法查看名单"); a.showAndWait();
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
        TableColumn<StudentRow, String> c1 = new TableColumn<>("学号"); c1.setCellValueFactory(d -> d.getValue().sidProperty()); c1.setPrefWidth(140);
        TableColumn<StudentRow, String> c2 = new TableColumn<>("姓名"); c2.setCellValueFactory(d -> d.getValue().nameProperty()); c2.setPrefWidth(120);
        TableColumn<StudentRow, String> c3 = new TableColumn<>("专业"); c3.setCellValueFactory(d -> d.getValue().majorProperty()); c3.setPrefWidth(160);
        TableColumn<StudentRow, String> c4 = new TableColumn<>("学院"); c4.setCellValueFactory(d -> d.getValue().schoolProperty()); c4.setPrefWidth(140);
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
                            Request req = new Request("dropCourse", data);
                            String resp = ClientNetworkHelper.send(req);
                            Response rr = new Gson().fromJson(resp, Response.class);
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
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(dropBtn);
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
                String resp = ClientNetworkHelper.getTeachingClassStudents(teachingClassUuid);
                Map<String, Object> result = new com.google.gson.Gson().fromJson(resp, Map.class);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    List<Map<String, Object>> students = (List<Map<String, Object>>) result.get("data");
                    ObservableList<StudentRow> rows = FXCollections.observableArrayList();
                    if (students != null) {
                        for (Map<String, Object> stu : students) {
                            // cardNumber 可能为数字或字符串；避免科学计数法（例如 2.13232556E8），使用 BigDecimal.toPlainString()
                            Object cardObj = stu.get("cardNumber");
                            String cardNum;
                            if (cardObj == null) {
                                cardNum = "";
                            } else if (cardObj instanceof String) {
                                cardNum = (String) cardObj;
                            } else if (cardObj instanceof Number) {
                                cardNum = new java.math.BigDecimal(cardObj.toString()).toPlainString();
                                if (cardNum.indexOf('.') >= 0) cardNum = cardNum.replaceAll("\\.?0+$", "");
                            } else {
                                cardNum = String.valueOf(cardObj);
                            }

                            Object stuNumObj = stu.get("studentNumber");
                            String sid;
                            if (stuNumObj == null) sid = "";
                            else if (stuNumObj instanceof String) sid = (String) stuNumObj;
                            else if (stuNumObj instanceof Number) {
                                sid = new java.math.BigDecimal(stuNumObj.toString()).toPlainString();
                                if (sid.indexOf('.') >= 0) sid = sid.replaceAll("\\.?0+$", "");
                            } else sid = String.valueOf(stuNumObj);

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

    private void showAddClassForCourse(String courseId) {
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.setTitle("新增教学班");
        dialog.setHeaderText("为课程 " + courseId + " 添加教学班");

        TextField teacherField = new TextField();
        // 新：以可编辑的 day 选择 + time 输入 + 列表方式管理多个上课日/时间
        javafx.scene.control.ComboBox<String> dayChoice = new javafx.scene.control.ComboBox<>(FXCollections.observableArrayList("周一","周二","周三","周四","周五","周六","周日"));
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
            String display = day.trim() + (time.isEmpty() ? "" : " " + time);
            // 内部存储格式为 day||time 以便序列化
            scheduleList.getItems().add(day.trim() + "||" + time);
            // 显示友好文本
            int idx = scheduleList.getItems().size() - 1;
            scheduleList.getSelectionModel().clearSelection();
            // 清空输入
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
                    Request r = new Request("addTeachingClass", data);
                    String resp = ClientNetworkHelper.send(r);
                    Response rr = new Gson().fromJson(resp, Response.class);
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

    private void showEditClassDialogFor(TeachingClass selected) {
        if (selected == null) return;
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.setTitle("编辑教学班");
        dialog.setHeaderText("编辑教学班信息");

        TextField teacherField = new TextField(selected.getTeacherName());
        // 编辑对话框中使用相同的多条 schedule 管理控件
        javafx.scene.control.ComboBox<String> dayChoice = new javafx.scene.control.ComboBox<>(FXCollections.observableArrayList("周一","周二","周三","周四","周五","周六","周日"));
        dayChoice.setEditable(true);
        TextField timeInput = new TextField();
        timeInput.setPromptText("例如: 9-11节 或 09:00-10:40");
        Button addScheduleBtn = new Button("添加到列表");
        ListView<String> scheduleList = new ListView<>(FXCollections.observableArrayList());
        scheduleList.setPrefHeight(120);
        Button removeScheduleBtn = new Button("移除所选");
        TextField placeField = new TextField(selected.getPlace());
        TextField capacityField = new TextField(String.valueOf(selected.getCapacity()));

        // 解析已有 schedule（可能为 JSON 对象或 JSON 数组）并填充列表
        try {
            String raw = selected.getSchedule();
            if (raw == null || raw.trim().isEmpty()) {
                // nothing
            } else {
                com.google.gson.Gson g = new com.google.gson.Gson();
                String t = raw.trim();
                if (t.startsWith("{")) {
                    // 旧的对象形式 {"周一":"9-11节", ...} 或 {"周六":["1-2节","6-7节"]}
                    Map<?, ?> m = g.fromJson(t, Map.class);
                    if (m != null) {
                        for (Map.Entry<?, ?> en : m.entrySet()) {
                            String d = en.getKey() == null ? "" : String.valueOf(en.getKey()).trim();
                            Object val = en.getValue();
                            if (d.isEmpty() || val == null) continue;
                            if (val instanceof java.util.List) {
                                for (Object it : (java.util.List<?>) val) {
                                    if (it == null) continue;
                                    String ti = String.valueOf(it).trim();
                                    if (!ti.isEmpty()) scheduleList.getItems().add(d + "||" + ti);
                                }
                            } else {
                                String ti = String.valueOf(val).trim();
                                // 如果是逗号分隔的多段，则拆开
                                if (ti.contains(",")) {
                                    String[] parts = ti.split("\\\\s*,\\\\s*");
                                    for (String p : parts) {
                                        if (!p.trim().isEmpty()) scheduleList.getItems().add(d + "||" + p.trim());
                                    }
                                } else {
                                    scheduleList.getItems().add(d + "||" + ti);
                                }
                            }
                        }
                    }
                } else if (t.startsWith("[")) {
                    // 新的数组形式 [{"day":"周一","time":"9-11节"}, ...]
                    java.util.List<Map> arr = g.fromJson(t, java.util.List.class);
                    if (arr != null) {
                        for (Map it : arr) {
                            if (it == null) continue;
                            Object od = it.get("day");
                            Object ot = it.get("time");
                            String d = od == null ? "" : String.valueOf(od).trim();
                            String ti = ot == null ? "" : String.valueOf(ot).trim();
                            if (d.isEmpty()) continue;
                            if (ti.contains(",")) {
                                String[] parts = ti.split("\\\\s*,\\\\s*");
                                for (String p : parts) if (!p.trim().isEmpty()) scheduleList.getItems().add(d + "||" + p.trim());
                            } else {
                                scheduleList.getItems().add(d + "||" + ti);
                            }
                        }
                    }
                } else {
                    // 退回兼容：把原始字符串作为单条项
                    scheduleList.getItems().add(t + "||");
                }
            }
        } catch (Exception ignored) {}

        addScheduleBtn.setOnAction(e -> {
            String day = dayChoice.getEditor().getText();
            if (day == null || day.trim().isEmpty()) {
                Alert a = new Alert(Alert.AlertType.ERROR, "请填写上课日（如 周一）", ButtonType.OK);
                a.showAndWait();
                return;
            }
            String time = timeInput.getText() == null ? "" : timeInput.getText().trim();
            scheduleList.getItems().add(day.trim() + "||" + time);
            timeInput.clear();
        });
        removeScheduleBtn.setOnAction(e -> {
            int sel = scheduleList.getSelectionModel().getSelectedIndex();
            if (sel >= 0) scheduleList.getItems().remove(sel);
        });

        // 构建布局并填充控件（与新增对话框保持一致的布局）
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
        ButtonType saveType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                try {
                    TeachingClass tc = new TeachingClass();
                    tc.setUuid(selected.getUuid());
                    tc.setCourseId(selected.getCourseId());
                    tc.setTeacherName(teacherField.getText());
                    // 将 scheduleList 条目转换为后端可接受的对象形式：Map<String, String>
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
                    tc.setSelectedCount(selected.getSelectedCount());
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
             new Thread(() -> {
                 try {
                     Map<String, Object> data = new HashMap<>();
                     data.put("uuid", tc.getUuid());
                     data.put("teacherName", tc.getTeacherName());
                     // tc.getSchedule() is a JSON string representing an object; parse it so the request sends an object
                     try {
                         Map<String, Object> scheduleObj = new com.google.gson.Gson().fromJson(tc.getSchedule(), Map.class);
                         data.put("schedule", scheduleObj == null ? new HashMap<>() : scheduleObj);
                     } catch (Exception ignore) {
                         data.put("schedule", tc.getSchedule());
                     }
                     // log payload for debugging
                     System.out.println("[CourseAdmin] addTeachingClass payload: " + new com.google.gson.Gson().toJson(data));
                     data.put("place", tc.getPlace());
                     data.put("capacity", tc.getCapacity());
                     Request r = new Request("updateTeachingClass", data);
                     String resp = ClientNetworkHelper.send(r);
                     Response rr = new Gson().fromJson(resp, Response.class);
                     Platform.runLater(() -> {
                         if (rr.getCode() == 200) {
                             Alert a = new Alert(Alert.AlertType.INFORMATION, "更新成功", ButtonType.OK);
                             a.showAndWait();
                             loadCourseData();
                         } else {
                             Alert a = new Alert(Alert.AlertType.ERROR, "更新失败: " + rr.getMessage(), ButtonType.OK);
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

    private void deleteTeachingClassConfirmed(TeachingClass tc) {
        if (tc == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定删除该教学班吗？", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("uuid", tc.getUuid());
                    Request req = new Request("deleteTeachingClass", data);
                    String resp = ClientNetworkHelper.send(req);
                    Response rr = new Gson().fromJson(resp, Response.class);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "删除成功", ButtonType.OK);
                            a.showAndWait();
                            loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "删除失败: " + rr.getMessage(), ButtonType.OK);
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
        }
    }

    // 新增/编辑/删除 课程（course）
    private void showAddCourseDialog() {
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("新增课程");
        dialog.setHeaderText("添加新课程");

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField creditField = new TextField();
        TextField collegeField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("课程编号:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("课程名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("学分:"), 0, 2);
        grid.add(creditField, 1, 2);
        grid.add(new Label("开设学院:"), 0, 3);
        grid.add(collegeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                String id = idField.getText() == null ? "" : idField.getText().trim();
                String name = nameField.getText() == null ? "" : nameField.getText().trim();
                if (id.isEmpty() || name.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "课程编号和课程名称为必填项", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
                try {
                    Map<String, Object> m = new HashMap<>();
                    m.put("courseId", id);
                    m.put("courseName", name);
                    String creditText = creditField.getText();
                    if (creditText != null && !creditText.trim().isEmpty()) {
                        // 支持小数学分
                        double c = Double.parseDouble(creditText.trim());
                        // 同时发送两种命名以兼容不同后端实现
                        m.put("courseCredit", c);
                        m.put("credit", c);
                    } else {
                        m.put("courseCredit", null);
                        m.put("credit", null);
                    }
                    String school = collegeField.getText() == null ? "" : collegeField.getText().trim();
                    m.put("college", school);
                    m.put("school", school);
                    return m;
                } catch (NumberFormatException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "学分必须为数字", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Map<String, Object>> res = dialog.showAndWait();
        res.ifPresent(data -> {
            new Thread(() -> {
                try {
                    Request req = new Request("addCourse", data);
                    String resp = ClientNetworkHelper.send(req);
                    Response rr = new Gson().fromJson(resp, Response.class);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "新增课程成功", ButtonType.OK);
                            a.showAndWait();
                            loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "新增课程失败: " + rr.getMessage(), ButtonType.OK);
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

    private void showEditCourseDialog(Map<String, Object> course) {
        if (course == null) return;
        String courseId = String.valueOf(course.get("courseId"));
        String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
        // 兼容后端不同命名（优先支持 courseCredit/college，回退到 credit/school）
        Object credObj = course.get("courseCredit") != null ? course.get("courseCredit") : course.get("credit");
        Object schoolObj = course.get("college") != null ? course.get("college") : course.get("school");
        String courseCredit = credObj == null ? "" : String.valueOf(credObj);
        String college = schoolObj == null ? "" : String.valueOf(schoolObj);

        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("编辑课程");
        dialog.setHeaderText("编辑课程 " + courseId);

        TextField idField = new TextField(courseId);
        idField.setDisable(true);
        TextField nameField = new TextField(courseName);
        TextField creditField = new TextField(courseCredit);
        TextField collegeField = new TextField(college);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("课程编号:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("课程名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("学分:"), 0, 2);
        grid.add(creditField, 1, 2);
        grid.add(new Label("开设学院:"), 0, 3);
        grid.add(collegeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                String name = nameField.getText() == null ? "" : nameField.getText().trim();
                if (name.isEmpty()) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "课程名称不能为空", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
                try {
                    Map<String, Object> m = new HashMap<>();
                    m.put("courseId", courseId);
                    m.put("courseName", name);
                    String creditText = creditField.getText();
                    if (creditText != null && !creditText.trim().isEmpty()) {
                        double c = Double.parseDouble(creditText.trim());
                        m.put("courseCredit", c);
                        m.put("credit", c);
                    } else {
                        m.put("courseCredit", null);
                        m.put("credit", null);
                    }
                    String school = collegeField.getText() == null ? "" : collegeField.getText().trim();
                    m.put("college", school);
                    m.put("school", school);
                    return m;
                } catch (NumberFormatException ex) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "学分必须为数字", ButtonType.OK);
                    a.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Map<String, Object>> res = dialog.showAndWait();
        res.ifPresent(data -> {
            new Thread(() -> {
                try {
                    Request req = new Request("updateCourse", data);
                    String resp = ClientNetworkHelper.send(req);
                    Response rr = new Gson().fromJson(resp, Response.class);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "更新课程成功", ButtonType.OK);
                            a.showAndWait();
                            loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "更新课程失败: " + rr.getMessage(), ButtonType.OK);
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

    private void deleteCourseConfirmed(Map<String, Object> course) {
        if (course == null) return;
        String courseId = String.valueOf(course.get("courseId"));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "确定删除课程 " + courseId + " 吗？这会同时删除该课程下的教学班。", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("courseId", courseId);
                    Request req = new Request("deleteCourse", data);
                    String resp = ClientNetworkHelper.send(req);
                    Response rr = new Gson().fromJson(resp, Response.class);
                    Platform.runLater(() -> {
                        if (rr.getCode() == 200) {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "删除课程成功", ButtonType.OK);
                            a.showAndWait();
                            loadCourseData();
                        } else {
                            Alert a = new Alert(Alert.AlertType.ERROR, "删除课程失败: " + rr.getMessage(), ButtonType.OK);
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
        }
    }

    // 本地搜索/过滤课程（使用 lastCourseList）
    private void applySearch(String type, String keyword) {
        if (lastCourseList == null || lastCourseList.isEmpty()) {
            statusLabel.setText("请先刷新以加载课程数据");
            return;
        }
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        if (type == null) type = "全部";
        if (type.equals("全部") || k.isEmpty()) {
            displayCoursesByCourse(lastCourseList, lastTcsByCourse);
            statusLabel.setText("显示全部，共 " + lastCourseList.size() + " 门课程");
            return;
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> course : lastCourseList) {
            String courseId = String.valueOf(course.get("courseId"));
            String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
            Object credObj = course.get("courseCredit") != null ? course.get("courseCredit") : course.get("credit");
            Object schoolObj = course.get("college") != null ? course.get("college") : course.get("school");
            String college = schoolObj == null ? "" : String.valueOf(schoolObj);

            boolean match = false;
            switch (type) {
                case "学院":
                    match = college.toLowerCase().contains(k);
                    break;
                case "课程代码":
                    match = courseId.toLowerCase().contains(k);
                    break;
                case "课程名称":
                    match = courseName.toLowerCase().contains(k);
                    break;
                default:
                    match = courseId.toLowerCase().contains(k) || courseName.toLowerCase().contains(k) || college.toLowerCase().contains(k);
            }
            if (match) filtered.add(course);
        }

        // 构建对应的教学班映射（只保留筛选后的课程）
        Map<String, List<TeachingClass>> filteredTcs = new HashMap<>();
        for (Map<String, Object> c : filtered) {
            String id = String.valueOf(c.get("courseId"));
            List<TeachingClass> list = lastTcsByCourse.get(id);
            if (list != null) filteredTcs.put(id, new ArrayList<>(list));
        }

        displayCoursesByCourse(filtered, filteredTcs);
        statusLabel.setText("搜索结果：共 " + filtered.size() + " 门课程");
    }

    // 在本地缓存上执行搜索并更新显示
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

    // 其余原有的增删改课程方法仍可复用（如果需要可继续扩展）

    // 统一样式化主按钮（用于搜索/清除等按钮）
    private void stylePrimaryButton(Button btn) {
        if (btn == null) return;
        btn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        btn.setPrefHeight(40);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #0056b3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;"));
    }
}
