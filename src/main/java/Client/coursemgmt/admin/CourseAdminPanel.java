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
        Label titleLabel = new Label("课程管理（管理员）");
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
        searchBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8,0,0,2);");

        Label searchLabel = new Label("搜索条件:");
        searchLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b; -fx-font-size: 14px;");

        // 按学籍管理风格统一按钮外观
        stylePrimaryButton(searchBtn);
        stylePrimaryButton(clearBtn);

        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchLabel, searchType, searchField, searchBtn, clearBtn);

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

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("编辑");
        editBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white;");
        editBtn.setOnAction(e -> showEditClassDialogFor(tc));

        Button delBtn = new Button("删除");
        delBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        delBtn.setOnAction(e -> deleteTeachingClassConfirmed(tc));

        btnRow.getChildren().addAll(spacer, editBtn, delBtn);

        card.getChildren().addAll(teacher, schedule, place, capacity, btnRow);

        // Ensure layout is recalculated immediately to avoid clipping issues in FlowPane
        card.requestLayout();
        return card;
    }

    // 弹出新增教学班对话框，并使用 courseId 预填
    private void showAddClassForCourse(String courseId) {
        Dialog<TeachingClass> dialog = new Dialog<>();
        dialog.setTitle("新增教学班");
        dialog.setHeaderText("为课程 " + courseId + " 添加教学班");

        TextField teacherField = new TextField();
        TextField scheduleField = new TextField();
        TextField placeField = new TextField();
        TextField capacityField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("教师姓名:"), 0, 0);
        grid.add(teacherField, 1, 0);
        grid.add(new Label("时间安排:"), 0, 1);
        grid.add(scheduleField, 1, 1);
        grid.add(new Label("地点:"), 0, 2);
        grid.add(placeField, 1, 2);
        grid.add(new Label("容量:"), 0, 3);
        grid.add(capacityField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        ButtonType addType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == addType) {
                try {
                    TeachingClass tc = new TeachingClass();
                    tc.setUuid(UUID.randomUUID().toString());
                    tc.setCourseId(courseId);
                    tc.setTeacherName(teacherField.getText());
                    tc.setSchedule(scheduleField.getText());
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
                    data.put("schedule", tc.getSchedule());
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
        TextField scheduleField = new TextField(selected.getSchedule());
        TextField placeField = new TextField(selected.getPlace());
        TextField capacityField = new TextField(String.valueOf(selected.getCapacity()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("教师姓名:"), 0, 0);
        grid.add(teacherField, 1, 0);
        grid.add(new Label("时间安排:"), 0, 1);
        grid.add(scheduleField, 1, 1);
        grid.add(new Label("地点:"), 0, 2);
        grid.add(placeField, 1, 2);
        grid.add(new Label("容量:"), 0, 3);
        grid.add(capacityField, 1, 3);

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
                    tc.setSchedule(scheduleField.getText());
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
                    data.put("schedule", tc.getSchedule());
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
