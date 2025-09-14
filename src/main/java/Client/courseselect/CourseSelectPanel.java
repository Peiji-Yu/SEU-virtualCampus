package Client.courseselect;

import Client.ClientNetworkHelper;
import Server.model.Request;
import Server.model.Response;
import Server.model.course.StudentTeachingClass;
import Server.model.course.TeachingClass;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.util.*;

/**
 * 学生选课界面，重构版。后端数据类型安全，支持多教学班，容量校验，冲突检测。
 * @author Copilot
 */
public class CourseSelectPanel extends BorderPane {

    private final int studentId;
    private VBox courseListContainer;
    private ScrollPane scrollPane;
    private Label statusLabel;
    private final Map<Integer, TeachingClass> teachingClassMap;
    private List<StudentTeachingClass> selectedClasses;
    // 防止重复提交的临时集合（线程安全）
    private final Set<String> pendingSelections = Collections.synchronizedSet(new HashSet<>());
    // 已选教学班 UUID 集合，兼容后端返回 TeachingClass 或 StudentTeachingClass 两种格式
    private final Set<String> selectedUuids = Collections.synchronizedSet(new HashSet<>());

    public CourseSelectPanel(int studentId) {
        this.studentId = studentId;
        this.teachingClassMap = new HashMap<>();
        this.selectedClasses = new ArrayList<>();
        initializeUI();
        loadCourseData();
    }

    private void initializeUI() {
        // 顶部标题
        Label titleLabel = new Label("课程选课");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setPadding(new Insets(20));
        titleBox.setAlignment(Pos.CENTER);

        // 状态标签
        statusLabel = new Label("正在加载课程数据...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");
        HBox statusBox = new HBox(statusLabel);
        statusBox.setPadding(new Insets(0, 20, 10, 20));

        // 课程列表容器（增大间距与默认宽度，作为更大表格的基础）
        courseListContainer = new VBox(20);
        courseListContainer.setPadding(new Insets(16, 24, 24, 24));
        courseListContainer.setPrefWidth(1000); // 建议的默认宽度

        scrollPane = new ScrollPane(courseListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setPrefViewportHeight(720); // 增大视口高度，显示更多内容
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // 刷新按钮
        Button refreshButton = new Button("刷新课程列表");
        refreshButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> loadCourseData());

        HBox buttonBox = new HBox(refreshButton);
        buttonBox.setPadding(new Insets(10, 20, 20, 20));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox mainContainer = new VBox(titleBox, statusBox, scrollPane, buttonBox);
        mainContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12;");

        this.setCenter(mainContainer);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: #f8fbff;");
    }

    private void loadCourseData() {
        statusLabel.setText("正在加载课程数据...");
        courseListContainer.getChildren().clear();

        new Thread(() -> {
            try {
                // 第一步：获取所有课程
                Request courseRequest = new Request("getAllCourses", new HashMap<>());
                String courseResponseStr = ClientNetworkHelper.send(courseRequest);
                Response courseResponse = new Gson().fromJson(courseResponseStr, Response.class);

                if (courseResponse.getCode() == 200) {
                    // 课程列表（声明为 final，避免 lambda 捕获问题）
                    final List<Map<String, Object>> courseList = new ArrayList<>();
                    Object dataObj = courseResponse.getData();
                    if (dataObj instanceof List) {
                        // 将返回的数据添加到已声明的列表中，而不是重新赋值
                        courseList.addAll((List<Map<String, Object>>) dataObj);
                    } else if (dataObj != null) {
                        courseList.addAll(new Gson().fromJson(new Gson().toJson(dataObj), List.class));
                    }

                    // 第二步：根据每个课程ID获取教学班
                    List<TeachingClass> allTeachingClasses = new ArrayList<>();
                    for (Map<String, Object> course : courseList) {
                        String courseId = String.valueOf(course.get("courseId"));
                        Map<String, Object> tcReqData = new HashMap<>();
                        tcReqData.put("courseId", courseId);
                        Request tcRequest = new Request("getTeachingClassesByCourseId", tcReqData);
                        String tcResponseStr = ClientNetworkHelper.send(tcRequest);
                        Response tcResponse = new Gson().fromJson(tcResponseStr, Response.class);
                        if (tcResponse.getCode() == 200 && tcResponse.getData() != null) {
                            Object tcDataObj = tcResponse.getData();
                            if (tcDataObj instanceof List) {
                                List<TeachingClass> tcList = new Gson().fromJson(new Gson().toJson(tcDataObj), new com.google.gson.reflect.TypeToken<List<TeachingClass>>(){}.getType());
                                allTeachingClasses.addAll(tcList);
                            } else {
                                TeachingClass tc = new Gson().fromJson(new Gson().toJson(tcDataObj), TeachingClass.class);
                                allTeachingClasses.add(tc);
                            }
                        }
                    }

                    // 第三步：获取学生已选课程
                    Map<String, Object> selectedReqData = new HashMap<>();
                    selectedReqData.put("cardNumber", studentId);
                    Request selectedRequest = new Request("getStudentSelectedCourses", selectedReqData);
                    String selectedResponseStr = ClientNetworkHelper.send(selectedRequest);
                    Response selectedResponse = new Gson().fromJson(selectedResponseStr, Response.class);
                    if (selectedResponse.getCode() == 200 && selectedResponse.getData() != null) {
                        String selectedJson = new Gson().toJson(selectedResponse.getData());
                        // 重置
                        selectedUuids.clear();
                        try {
                            // 尝试解析为通用 Map 列表，兼容 TeachingClass 或 StudentTeachingClass 返回格式
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, Object>>>(){}.getType();
                            List<Map<String, Object>> list = new Gson().fromJson(selectedJson, listType);
                            if (list != null) {
                                for (Map<String, Object> item : list) {
                                    if (item == null) continue;
                                    Object uuidObj = item.get("teachingClassUuid");
                                    if (uuidObj == null) uuidObj = item.get("uuid");
                                    if (uuidObj == null) uuidObj = item.get("teachingClass");
                                    if (uuidObj instanceof Map) {
                                        Object innerUuid = ((Map<?, ?>) uuidObj).get("uuid");
                                        if (innerUuid != null) selectedUuids.add(String.valueOf(innerUuid).trim().toLowerCase());
                                    } else if (uuidObj != null) {
                                        selectedUuids.add(String.valueOf(uuidObj).trim().toLowerCase());
                                    }
                                }
                                // 同步清空 selectedClasses，保持一致性
                                selectedClasses = new ArrayList<>();
                            }
                        } catch (Exception ex) {
                            // 解析失败时尝试作为 StudentTeachingClass 数组解析（回退）
                            try {
                                StudentTeachingClass[] selectedClassesArray = new Gson().fromJson(selectedJson, StudentTeachingClass[].class);
                                selectedClasses = Arrays.asList(selectedClassesArray);
                                for (StudentTeachingClass stc : selectedClasses) {
                                    if (stc != null && stc.getTeachingClassUuid() != null) selectedUuids.add(stc.getTeachingClassUuid().trim().toLowerCase());
                                    if (stc != null && stc.getTeachingClass() != null && stc.getTeachingClass().getUuid() != null) selectedUuids.add(stc.getTeachingClass().getUuid().trim().toLowerCase());
                                }
                            } catch (Exception ex2) {
                                // 忽略
                            }
                        }
                    }

                    // 按课程分组教学班
                    Map<String, List<TeachingClass>> teachingClassesByCourse = new HashMap<>();
                    for (TeachingClass tc : allTeachingClasses) {
                        if (tc == null || tc.getCourseId() == null) continue;
                        teachingClassesByCourse.computeIfAbsent(tc.getCourseId(), k -> new ArrayList<>()).add(tc);
                    }

                    final List<Map<String, Object>> courseListCopy = courseList;
                    final Map<String, List<TeachingClass>> teachingClassesByCourseCopy = teachingClassesByCourse;
                    final List<TeachingClass> allTeachingClassesCopy = allTeachingClasses;
                    final int totalCount = allTeachingClasses.size();

                    Platform.runLater(() -> {
                        // 调试输出已选教学班 UUID，便于排查 UI 是否未标识问题
                        System.out.println("[CourseSelectPanel] selectedUuids=" + selectedUuids);

                        teachingClassMap.clear();
                        for (TeachingClass tc : allTeachingClassesCopy) {
                            if (tc != null && tc.getUuid() != null) teachingClassMap.put(tc.getUuid().hashCode(), tc);
                        }
                        // 显示课程为一级卡片，展开后显示该课程的教学班卡片
                        displayCoursesByCourse(courseListCopy, teachingClassesByCourseCopy);
                        statusLabel.setText("共加载 " + totalCount + " 个教学班，按课程分组显示");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("加载失败: " + courseResponse.getMessage());
                        showAlert("错误", "加载课程数据失败: " + courseResponse.getMessage(), Alert.AlertType.ERROR);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("网络错误: " + e.getMessage());
                    showAlert("错误", "网络连接失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    // 按课程分组显示：点击课程卡片展开/收起下方教学班卡片
    private void displayCoursesByCourse(List<Map<String, Object>> courseList, Map<String, List<TeachingClass>> teachingClassesByCourse) {
        courseListContainer.getChildren().clear();

        if (courseList == null || courseList.isEmpty()) {
            Label noCoursesLabel = new Label("暂无可选课程");
            noCoursesLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666666;");
            courseListContainer.getChildren().add(noCoursesLabel);
            return;
        }

        for (Map<String, Object> course : courseList) {
            String courseId = String.valueOf(course.get("courseId"));
            String courseName = course.get("courseName") == null ? "" : String.valueOf(course.get("courseName"));
            List<TeachingClass> tcs = teachingClassesByCourse.getOrDefault(courseId, Collections.emptyList());

            // 课程卡片头部（增大卡片尺寸以形成更大的课程表格）
            VBox courseCard = new VBox(10);
            courseCard.setPadding(new Insets(16));
            courseCard.setPrefWidth(980);
            courseCard.setMaxWidth(Double.MAX_VALUE);
            courseCard.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 10;");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label(courseId + "  " + courseName);
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            header.getChildren().addAll(title, spacer);

            // 详情容器改为横向排列，包含该课程的所有教学班卡片（每个卡片加宽）
            HBox details = new HBox(12);
            details.setPadding(new Insets(10, 0, 0, 0));
            // 默认不显示，除非该课程包含已选教学班
            details.setVisible(false);
            details.setManaged(false);
            final HBox detailsFinal = details;

            for (TeachingClass tc : tcs) {
                details.getChildren().add(createTeachingClassCard(tc));
            }

            // 如果该课程中存在已选教学班，则默认展开
            boolean hasSelectedInCourse = false;
            for (TeachingClass tc : tcs) {
                if (tc != null && tc.getUuid() != null && selectedUuids.contains(tc.getUuid().trim().toLowerCase())) {
                    hasSelectedInCourse = true;
                    break;
                }
            }
            if (hasSelectedInCourse) {
                details.setVisible(true);
                details.setManaged(true);
            }

            // 点击头部切换展开/收起（不显示文字）
            header.setOnMouseClicked(e -> {
                boolean showing = detailsFinal.isVisible();
                detailsFinal.setVisible(!showing);
                detailsFinal.setManaged(!showing);
            });

            courseCard.getChildren().addAll(header, details);
            courseListContainer.getChildren().add(courseCard);
        }
    }

    // 创建单个教学班卡片（用于课程详情展开内显示）
    private Node createTeachingClassCard(TeachingClass tc) {
        boolean isSelected = isCourseSelected(tc.getUuid());
        HBox card = new HBox(12);
        card.setPadding(new Insets(12));
        String baseStyle = "-fx-background-radius: 6; -fx-border-color: #e6eef8; -fx-border-width: 1; -fx-border-radius: 6;";
        if (isSelected) {
            card.setStyle("-fx-background-color: #e6f6ec; " + baseStyle); // 绿底
        } else {
            card.setStyle("-fx-background-color: #f7f9fc; " + baseStyle);
        }
        card.setAlignment(Pos.CENTER_LEFT);

        // 建议卡片宽度，形成更明显的表格布局
        card.setPrefWidth(320);
        card.setMinWidth(280);

        VBox info = new VBox(8);
        Label teacher = new Label("教师: " + (tc.getTeacherName() == null ? "未知" : tc.getTeacherName()));
        teacher.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
        // 解析 schedule JSON
        VBox scheduleBox = new VBox(2);
        scheduleBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        String scheduleJson = tc.getSchedule();
        if (scheduleJson != null && !scheduleJson.trim().isEmpty()) {
            try {
                java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
                Map<String, String> scheduleMap = new Gson().fromJson(scheduleJson, mapType);
                if (scheduleMap != null && !scheduleMap.isEmpty()) {
                    for (Map.Entry<String, String> e : scheduleMap.entrySet()) {
                        Label dayLine = new Label(e.getKey() + ": " + e.getValue());
                        dayLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                        scheduleBox.getChildren().add(dayLine);
                    }
                } else {
                    Label dayLine = new Label(scheduleJson);
                    dayLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                    scheduleBox.getChildren().add(dayLine);
                }
            } catch (Exception ex) {
                Label dayLine = new Label(scheduleJson);
                dayLine.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                scheduleBox.getChildren().add(dayLine);
            }
        }
        Label timeTitle = new Label("时间:");
        timeTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666; -fx-font-weight: bold;");
        HBox timeRow = new HBox(6);
        timeRow.getChildren().addAll(timeTitle, scheduleBox);

        Label placeLabel = new Label("地点: " + (tc.getPlace() == null ? "" : tc.getPlace()));
        placeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        Label capacity = new Label("容量: " + tc.getSelectedCount() + "/" + tc.getCapacity());
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        info.getChildren().addAll(teacher, timeRow, placeLabel, capacity);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button selectButton = new Button();
        final Button selectButtonFinal = selectButton;
        boolean isFull = tc.getSelectedCount() >= tc.getCapacity();
        boolean isPending = pendingSelections.contains(tc.getUuid());

        if (isSelected) {
            // 已选：显示退课按钮
            selectButton.setText("退课");
            selectButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
            selectButton.setDisable(false);
            selectButton.setOnAction(e -> {
                selectButtonFinal.setDisable(true);
                pendingSelections.add(tc.getUuid());
                dropCourse(tc, selectButtonFinal);
            });
        } else if (isFull) {
            selectButton.setText("已满");
            selectButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
            selectButton.setDisable(true);
        } else if (isPending) {
            selectButton.setText("处理中");
            selectButton.setStyle("-fx-background-color: #999999; -fx-text-fill: white; -fx-font-weight: bold;");
            selectButton.setDisable(true);
        } else {
            selectButton.setText("选课");
            selectButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
            selectButton.setOnAction(e -> {
                selectButtonFinal.setDisable(true);
                pendingSelections.add(tc.getUuid());
                selectCourse(tc, selectButtonFinal);
            });
        }
        selectButton.setPrefWidth(90);

        card.getChildren().addAll(info, spacer, selectButton);
        return card;
    }

    // 退课（带按钮引用，失败时恢复按钮）
    private void dropCourse(TeachingClass tc, Button selectButton) {
        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", studentId);
                data.put("teachingClassUuid", tc.getUuid());

                Request request = new Request("dropCourse", data);
                String responseStr = ClientNetworkHelper.send(request);
                Response response = new Gson().fromJson(responseStr, Response.class);

                Platform.runLater(() -> {
                    pendingSelections.remove(tc.getUuid());
                    if (response.getCode() == 200) {
                        showAlert("成功", "退课成功！", Alert.AlertType.INFORMATION);
                        loadCourseData();
                    } else {
                        selectButton.setDisable(false);
                        showAlert("退课失败", response.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pendingSelections.remove(tc.getUuid());
                    selectButton.setDisable(false);
                    showAlert("错误", "网络连接失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private boolean isCourseSelected(String teachingClassUuid) {
        if (teachingClassUuid == null) return false;
        return selectedUuids.contains(teachingClassUuid.trim().toLowerCase());
    }

    private void selectCourse(TeachingClass tc) {
        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", studentId);
                data.put("teachingClassUuid", tc.getUuid());

                Request request = new Request("selectCourse", data);
                String responseStr = ClientNetworkHelper.send(request);
                Response response = new Gson().fromJson(responseStr, Response.class);

                Platform.runLater(() -> {
                    if (response.getCode() == 200) {
                        showAlert("成功", "选课成功！", Alert.AlertType.INFORMATION);
                        // 刷新课程列表
                        loadCourseData();
                    } else {
                        showAlert("选课失败", response.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "网络连接失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    // 重载：带按钮引用，用于在请求失败时恢复按钮状态
    private void selectCourse(TeachingClass tc, Button selectButton) {
        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", studentId);
                data.put("teachingClassUuid", tc.getUuid());

                Request request = new Request("selectCourse", data);
                String responseStr = ClientNetworkHelper.send(request);
                Response response = new Gson().fromJson(responseStr, Response.class);

                Platform.runLater(() -> {
                    pendingSelections.remove(tc.getUuid());
                    if (response.getCode() == 200) {
                        showAlert("成功", "选课成功！", Alert.AlertType.INFORMATION);
                        // 刷新课程列表
                        loadCourseData();
                    } else {
                        // 恢复按钮以允许重试
                        selectButton.setDisable(false);
                        showAlert("选课失败", response.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    pendingSelections.remove(tc.getUuid());
                    selectButton.setDisable(false);
                    showAlert("错误", "网络连接失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
