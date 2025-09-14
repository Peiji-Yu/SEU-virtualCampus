package Client.coursemgmt.admin;

import Client.ClientNetworkHelper;
import Server.model.Request;
import Server.model.Response;
import Server.model.course.Course;
import Server.model.course.TeachingClass;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

        // 列表容器
        courseListContainer = new VBox(16);
        courseListContainer.setPadding(new Insets(16, 20, 20, 20));
        courseListContainer.setPrefWidth(1000);

        scrollPane = new ScrollPane(courseListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button refreshBtn = new Button("刷新");
        refreshBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadCourseData());
        HBox btnBox = new HBox(refreshBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(8, 16, 12, 16));

        VBox body = new VBox(statusBox, scrollPane, btnBox);
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
                    // 解析课程列表为通用 Map 列表
                    List<Map<String, Object>> courseList = new ArrayList<>();
                    Object dataObj = resp.getData();
                    if (dataObj instanceof List) {
                        courseList.addAll((List<Map<String, Object>>) dataObj);
                    } else if (dataObj != null) {
                        courseList.addAll(new Gson().fromJson(new Gson().toJson(dataObj), List.class));
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
            List<TeachingClass> tcs = tcsByCourse.getOrDefault(courseId, Collections.emptyList());

            VBox courseCard = new VBox(10);
            courseCard.setPadding(new Insets(12));
            courseCard.setPrefWidth(980);
            courseCard.setMaxWidth(Double.MAX_VALUE);
            courseCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 10;");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label(courseId + "  " + courseName);
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

            header.getChildren().addAll(title, spacer, addClassBtn);

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
            });

            courseCard.getChildren().addAll(header, details);
            courseListContainer.getChildren().add(courseCard);
        }
    }

    // 创建教学班卡片，带编辑/删除操作
    private Node createTeachingClassCard(TeachingClass tc, FlowPane parent) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-radius: 6; -fx-border-color: #e6eef8; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: #f7f9fc;");
        card.setPrefHeight(150);

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

        Label schedule = new Label("时间: " + (tc.getSchedule() == null ? "未设置" : tc.getSchedule()));
        schedule.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        Label place = new Label("地点: " + (tc.getPlace() == null ? "未设置" : tc.getPlace()));
        place.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        Label capacity = new Label("容量: " + tc.getCapacity());
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

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

    // 其余原有的增删改课程方法仍可复用（如果需要可继续扩展）
}

