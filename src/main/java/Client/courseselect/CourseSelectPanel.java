package Client.courseselect;

import Client.ClientNetworkHelper;
import Client.util.EventBus;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 学生选课界面，重构版。后端数据类型安全，支持多教学班，容量校验，冲突检测。
 * @author Copilot
 */
public class CourseSelectPanel extends BorderPane {

    private final int studentId;
    private VBox courseListContainer;
    private ScrollPane scrollPane;
    private Label statusLabel;
    // 视图切换按钮：全部课程 / 已选课程
    private Button allCoursesBtn;
    private Button selectedCoursesBtn;
    private final Map<String, TeachingClass> teachingClassMap;
    private List<StudentTeachingClass> selectedClasses;
    // 防止重复提交的临时集合（线程安全）
    private final Set<String> pendingSelections = Collections.synchronizedSet(new HashSet<>());
    // 已选教学班 UUID 集合，兼容后端返回 TeachingClass 或 StudentTeachingClass 两种格式
    private final Set<String> selectedUuids = Collections.synchronizedSet(new HashSet<>());
    // 当前是否显示已选课程视图
    private boolean showingSelectedView = false;

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

        // 视图切换按钮（全部课程 / 已选课程）
        allCoursesBtn = new Button("全部课程");
        selectedCoursesBtn = new Button("已选课程");
        allCoursesBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
        selectedCoursesBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff; -fx-font-weight: bold;");
        allCoursesBtn.setOnAction(e -> {
            allCoursesBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
            selectedCoursesBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff; -fx-font-weight: bold;");
            showingSelectedView = false;
            loadCourseData();
        });
        selectedCoursesBtn.setOnAction(e -> {
            selectedCoursesBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
            allCoursesBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff; -fx-font-weight: bold;");
            showingSelectedView = true;
            loadSelectedCourses();
        });
        HBox viewToggle = new HBox(8, allCoursesBtn, selectedCoursesBtn);
        viewToggle.setPadding(new Insets(0, 20, 10, 20));

        VBox mainContainer = new VBox(titleBox, statusBox, viewToggle, scrollPane, buttonBox);
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
                            if (tc != null && tc.getUuid() != null) teachingClassMap.put(tc.getUuid().trim().toLowerCase(), tc);
                        }
                        // 显示课程为一级卡片，展开后显示该课程的教学班卡片
                        displayCoursesByCourse(courseListCopy, teachingClassesByCourseCopy);
                        // 显示按课程分组的课程数量，而不是教学班总数
                        statusLabel.setText("共加载 " + courseListCopy.size() + " 门课程，按课程分组显示");
                        // 保证切换按钮样式与当前视图一致
                        allCoursesBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
                        selectedCoursesBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff; -fx-font-weight: bold;");
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

            // 详情容器改为可换行的 FlowPane，最多每行显示 4 个教学班卡片（每卡片宽度约 320）
            FlowPane details = new FlowPane();
            details.setHgap(12);
            details.setVgap(12);
            details.setPadding(new Insets(10, 0, 0, 0));
            // 设置换行宽度为课程卡片内部可用宽度（响应式绑定到 courseCard 宽度），
            // 并将单个教学班卡片宽度减小到 ~230，使每行能放下4个卡片： 4*230 + 3*12 = 956 <= 可用宽度
            details.prefWrapLengthProperty().bind(courseCard.widthProperty().subtract(32));
            // 默认不显示，除非该课程包含已选教学班
            details.setVisible(false);
            details.setManaged(false);
            final FlowPane detailsFinal = details;

            for (TeachingClass tc : tcs) {
                // 传入父 FlowPane，使卡片可以根据父容器宽度动态计算宽度，保证一行最多显示 4 个
                details.getChildren().add(createTeachingClassCard(tc, details));
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
    private Node createTeachingClassCard(TeachingClass tc, FlowPane parent) {
        boolean isSelected = isCourseSelected(tc.getUuid());
        // 判断是否与任一已选教学班冲突（仅对未选中的教学班进行高亮）
        boolean isConflict = false;
        try {
            if (!isSelected && tc != null && tc.getSchedule() != null && !selectedUuids.isEmpty()) {
                for (String selUuid : selectedUuids) {
                    if (selUuid == null) continue;
                    TeachingClass existTc = teachingClassMap.get(selUuid.trim().toLowerCase());
                    if (existTc == null) continue;
                    if (schedulesConflict(existTc.getSchedule(), tc.getSchedule())) {
                        isConflict = true;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        // 默认样式
        card.setStyle("-fx-background-radius: 6; -fx-border-color: #e6eef8; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: #f7f9fc;");
        if (isSelected) {
            // 已选：绿色背景
            card.setStyle("-fx-background-radius: 6; -fx-border-color: #cfe8d8; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: #e6f6ec;");
        } else if (isConflict) {
            // 冲突：红色提示背景（不改变按钮逻辑）
            card.setStyle("-fx-background-radius: 6; -fx-border-color: #f5c2c7; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-color: #fdecec;");
        }
        card.setAlignment(Pos.TOP_LEFT);
        // 固定高度让每行卡片纵向对齐，按钮能靠底部显示
        card.setPrefHeight(160);

        // 绑定卡片宽度到父容器，确保四列均匀分布
        try {
            card.prefWidthProperty().bind(parent.widthProperty()
                    .subtract(32)
                    .subtract(parent.getHgap() * 3)
                    .divide(4));
            card.minWidthProperty().bind(card.prefWidthProperty().multiply(0.75));
            card.maxWidthProperty().bind(card.prefWidthProperty().multiply(1.05));
        } catch (Exception ex) {
            card.setPrefWidth(230);
            card.setMinWidth(200);
        }

        VBox info = new VBox(8);
        Label teacher = new Label("教师: " + (tc.getTeacherName() == null ? "未知" : tc.getTeacherName()));
        teacher.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        VBox scheduleBox = new VBox(2);
        scheduleBox.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        String scheduleJson = tc.getSchedule();
        if (scheduleJson != null && !scheduleJson.trim().isEmpty()) {
            try {
                java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
                Map<String, String> scheduleMap = new Gson().fromJson(scheduleJson, mapType);
                if (scheduleMap != null && !scheduleMap.isEmpty()) {
                    for (Map.Entry<String, String> e : scheduleMap.entrySet()) {
                        String formatted = formatScheduleValue(e.getValue());
                        Label dayLine = new Label(e.getKey() + ": " + formatted);
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

        // 用可伸缩区域把按钮推到卡片底部
        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        Button selectButton = new Button();
        final Button selectButtonFinal = selectButton;
        boolean isFull = tc.getSelectedCount() >= tc.getCapacity();
        boolean isPending = pendingSelections.contains(tc.getUuid());

        if (isSelected) {
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
        selectButton.setMaxWidth(Double.MAX_VALUE);
        HBox buttonRow = new HBox(selectButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(info, vSpacer, buttonRow);
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
                        // 先更新本地已选集合，保证后续界面渲染以当前视图为准
                        if (tc != null && tc.getUuid() != null) selectedUuids.remove(tc.getUuid().trim().toLowerCase());
                        // 刷新当前视图：如果当前在已选课程视图，则刷新已选列表，否则刷新全部课程列表
                        if (showingSelectedView) {
                            loadSelectedCourses();
                        } else {
                            loadCourseData();
                        }
                        // 通知全局事件：学生选课发生变化（用于课表等面板自动刷新）
                        EventBus.post("student:courseChanged");
                        // 在刷新之后再给出提示，避免样式被 alert 打断后意外恢复到默认
                        showAlert("成功", "退课成功！", Alert.AlertType.INFORMATION);
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

    // 把后端的 schedule 值格式化为可读文本。
    // 支持原始时间范围（含 ':'）以及新的节次格式如 "1-2节"、"3节"。
    // 当识别为节次时，会尝试将节次转换为近似的时间区间（映射表可根据学校实际时间调整）。
    private String formatScheduleValue(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        // 如果已经是时间范围（包含 ':'），直接返回原样
        if (s.contains(":")) return s;

        // 简单映射：节次 -> 时间段。可根据实际校历调整。
        String[] periodStart = new String[]{
                "", // 0 占位
                "08:00", "08:50", "10:00", "10:50",
                "14:00", "14:50", "15:50", "16:40",
                "19:00", "19:50", "20:10", "20:55"
        };
        String[] periodEnd = new String[]{
                "",
                "08:45", "09:35", "10:45", "11:30",
                "14:45", "15:35", "16:35", "17:25",
                "19:45", "20:35", "20:50", "21:40"
        };

        // 匹配范围，例如 "1-2节" 或 "1-2"
        Pattern rangePat = Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)\\s*节?$");
        Matcher m = rangePat.matcher(s);
        if (m.find()) {
            try {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                if (a < 1) a = 1;
                if (b < 1) b = 1;
                if (a >= periodStart.length) a = periodStart.length - 1;
                if (b >= periodEnd.length) b = periodEnd.length - 1;
                if (a > b) { int tmp=a; a=b; b=tmp; }
                String start = periodStart[a];
                String end = periodEnd[b];
                if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                    return s + " (" + start + "-" + end + ")";
                }
            } catch (Exception ignored) {}
        }

        // 匹配单节 "3节" 或 "3"
        Pattern singlePat = Pattern.compile("^(\\d+)\\s*节?$");
        m = singlePat.matcher(s);
        if (m.find()) {
            try {
                int p = Integer.parseInt(m.group(1));
                if (p < 1) p = 1;
                if (p >= periodStart.length) p = periodStart.length - 1;
                String start = periodStart[p];
                String end = periodEnd[p];
                if (start != null && !start.isEmpty() && end != null && !end.isEmpty()) {
                    return s + " (" + start + "-" + end + ")";
                }
            } catch (Exception ignored) {}
        }

        // 退回原始字符串
        return s;
    }

    // 创建已选课程卡片，显示详细信息并提供退课按钮
    private Node createSelectedCourseCard(TeachingClass tc) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #e6f6ec; -fx-background-radius: 6; -fx-border-color: #cfe8d8; -fx-border-width: 1; -fx-border-radius: 6;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(6);
        Label title = new Label(tc.getCourseId() + "  " + (tc.getCourse() != null ? tc.getCourse().getCourseName() : ""));
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;");
        Label teacher = new Label("教师: " + (tc.getTeacherName() == null ? "" : tc.getTeacherName()));
        teacher.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        VBox scheduleBox = new VBox(2);
        try {
            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
            Map<String, String> scheduleMap = new Gson().fromJson(tc.getSchedule(), mapType);
            if (scheduleMap != null) {
                for (Map.Entry<String, String> e : scheduleMap.entrySet()) {
                    String formatted = formatScheduleValue(e.getValue());
                    Label line = new Label(e.getKey() + ": " + formatted);
                    line.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
                    scheduleBox.getChildren().add(line);
                }
            }
        } catch (Exception ignored) {}

        Label place = new Label("地点: " + (tc.getPlace() == null ? "" : tc.getPlace()));
        place.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");
        info.getChildren().addAll(title, teacher, scheduleBox, place);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button dropBtn = new Button("退课");
        dropBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        dropBtn.setOnAction(e -> {
            dropBtn.setDisable(true);
            pendingSelections.add(tc.getUuid());
            dropCourse(tc, dropBtn);
        });
        dropBtn.setPrefWidth(90);

        card.getChildren().addAll(info, spacer, dropBtn);
        return card;
    }

    private void loadSelectedCourses() {
        statusLabel.setText("正在加载已选课程...");
        courseListContainer.getChildren().clear();

        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", studentId);
                Request request = new Request("getStudentSelectedCourses", data);
                String responseStr = ClientNetworkHelper.send(request);
                Response response = new Gson().fromJson(responseStr, Response.class);

                if (response.getCode() == 200) {
                    Object d = response.getData();
                    List<TeachingClass> list = new ArrayList<>();
                    if (d instanceof List) {
                        String json = new Gson().toJson(d);
                        list = new Gson().fromJson(json, new com.google.gson.reflect.TypeToken<List<TeachingClass>>(){}.getType());
                    }

                    // 更新内存中的 selectedUuids 与 selectedClasses，使状态一致
                    selectedUuids.clear();
                    selectedClasses = new ArrayList<>();
                    for (TeachingClass t : list) {
                        if (t != null && t.getUuid() != null) selectedUuids.add(t.getUuid().trim().toLowerCase());
                        StudentTeachingClass stc = new StudentTeachingClass();
                        stc.setTeachingClassUuid(t == null ? null : t.getUuid());
                        selectedClasses.add(stc);
                    }

                    final List<TeachingClass> finalList = list;
                    Platform.runLater(() -> {
                        displaySelectedCoursesList(finalList);
                        statusLabel.setText("已选课程: " + finalList.size());
                        selectedCoursesBtn.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
                        allCoursesBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4e8cff; -fx-font-weight: bold;");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("加载已选课程失败: " + response.getMessage());
                        showAlert("错误", "加载已选课程失败: " + response.getMessage(), Alert.AlertType.ERROR);
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

    private void displaySelectedCoursesList(List<TeachingClass> list) {
        courseListContainer.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label no = new Label("您尚未选任何课程");
            no.setStyle("-fx-font-size: 16px; -fx-text-fill: #666666;");
            courseListContainer.getChildren().add(no);
            return;
        }

        for (TeachingClass tc : list) {
            courseListContainer.getChildren().add(createSelectedCourseCard(tc));
        }
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
                        loadCourseData();
                        // 通知全局事件：学生选课发生变化（用于课表等面板自动刷新）
                        EventBus.post("student:courseChanged");
                    } else {
                        showAlert("选课失败", response.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("错误", "网络连接失败: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        }).start();
    }

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
                        loadCourseData();
                        // 通知全局事件：学生选课发生变化（用于课表等面板自动刷新）
                        EventBus.post("student:courseChanged");
                    } else {
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

    // 客户端用于表示时间段的简单类型
    private static class TimeRange {
        java.time.LocalTime start;
        java.time.LocalTime end;
        TimeRange(java.time.LocalTime s, java.time.LocalTime e) { start = s; end = e; }
    }

    // 客户端解析 schedule JSON 为 Map<day, List<TimeRange>>，兼容节次格式
    private Map<String, List<TimeRange>> parseScheduleClient(String scheduleJson) {
        Map<String, List<TimeRange>> map = new HashMap<>();
        if (scheduleJson == null || scheduleJson.trim().isEmpty()) return map;
        try {
            java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
            Map<String, String> raw = new Gson().fromJson(scheduleJson, mapType);
            if (raw == null) return map;

            String[] periodStart = new String[]{"", "08:00","08:50","10:00","10:50","14:00","14:50","15:50","16:40","19:00","19:50","20:10","20:55"};
            String[] periodEnd = new String[]{"", "08:45","09:35","10:45","11:30","14:45","15:35","16:35","17:25","19:45","20:35","20:50","21:40"};

            for (Map.Entry<String, String> e : raw.entrySet()) {
                String day = e.getKey();
                String val = e.getValue();
                if (val == null) continue;
                String[] parts = val.split("[,;]\\s*");
                List<TimeRange> ranges = new ArrayList<>();
                for (String p : parts) {
                    String rawPart = p.replace('：', ':').replace('－', '-').replace('—', '-').replace('–', '-').trim();
                    if (rawPart.isEmpty()) continue;
                    if (rawPart.contains(":")) {
                        String[] se = rawPart.split("-"); if (se.length != 2) continue;
                        String startStr = se[0].trim(); String endStr = se[1].trim();
                        java.time.LocalTime s = null, t = null;
                        java.time.format.DateTimeFormatter[] fmts = new java.time.format.DateTimeFormatter[] {
                                java.time.format.DateTimeFormatter.ofPattern("H:mm"), java.time.format.DateTimeFormatter.ofPattern("HH:mm") };
                        for (java.time.format.DateTimeFormatter fmt : fmts) {
                            if (s == null) try { s = java.time.LocalTime.parse(startStr, fmt); } catch (Exception ignored) {}
                            if (t == null) try { t = java.time.LocalTime.parse(endStr, fmt); } catch (Exception ignored) {}
                            if (s != null && t != null) break;
                        }
                        if (s != null && t != null) ranges.add(new TimeRange(s, t));
                        continue;
                    }
                    java.util.regex.Pattern rangePat = java.util.regex.Pattern.compile("^(\\d+)\\s*-\\s*(\\d+)\\s*节?$");
                    java.util.regex.Matcher m = rangePat.matcher(rawPart);
                    if (m.find()) {
                        try { int a = Integer.parseInt(m.group(1)); int b = Integer.parseInt(m.group(2));
                            if (a < 1) a = 1; if (b < 1) b = 1;
                            if (a >= periodStart.length) a = periodStart.length - 1;
                            if (b >= periodEnd.length) b = periodEnd.length - 1;
                            if (a > b) { int tmp=a; a=b; b=tmp; }
                            String ss = periodStart[a]; String ee = periodEnd[b];
                            if (ss != null && ee != null && !ss.isEmpty() && !ee.isEmpty()) ranges.add(new TimeRange(java.time.LocalTime.parse(ss), java.time.LocalTime.parse(ee)));
                            continue; } catch (Exception ignored) {}
                    }
                    java.util.regex.Pattern singlePat = java.util.regex.Pattern.compile("^(\\d+)\\s*节?$");
                    m = singlePat.matcher(rawPart);
                    if (m.find()) {
                        try { int pnum = Integer.parseInt(m.group(1)); if (pnum < 1) pnum = 1; if (pnum >= periodStart.length) pnum = periodStart.length - 1;
                            String ss = periodStart[pnum]; String ee = periodEnd[pnum];
                            if (ss != null && ee != null && !ss.isEmpty() && !ee.isEmpty()) ranges.add(new TimeRange(java.time.LocalTime.parse(ss), java.time.LocalTime.parse(ee)));
                            continue; } catch (Exception ignored) {}
                    }
                    String[] se = rawPart.split("-"); if (se.length == 2) {
                        String startStr = se[0].trim(); String endStr = se[1].trim();
                        java.time.LocalTime s = null, t = null;
                        java.time.format.DateTimeFormatter[] fmts = new java.time.format.DateTimeFormatter[] { java.time.format.DateTimeFormatter.ofPattern("H:mm"), java.time.format.DateTimeFormatter.ofPattern("HH:mm") };
                        for (java.time.format.DateTimeFormatter fmt : fmts) {
                            if (s == null) try { s = java.time.LocalTime.parse(startStr, fmt); } catch (Exception ignored) {}
                            if (t == null) try { t = java.time.LocalTime.parse(endStr, fmt); } catch (Exception ignored) {}
                            if (s != null && t != null) break;
                        }
                        if (s != null && t != null) ranges.add(new TimeRange(s, t));
                    }
                }
                if (!ranges.isEmpty()) map.put(e.getKey(), ranges);
            }
        } catch (Exception ex) { }
        return map;
    }

    private boolean schedulesConflict(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        Map<String, List<TimeRange>> m1 = parseScheduleClient(s1);
        Map<String, List<TimeRange>> m2 = parseScheduleClient(s2);
        for (String day : m1.keySet()) {
            if (!m2.containsKey(day)) continue;
            List<TimeRange> r1 = m1.get(day);
            List<TimeRange> r2 = m2.get(day);
            for (TimeRange a : r1) for (TimeRange b : r2) {
                if (a.start.isBefore(b.end) && b.start.isBefore(a.end)) return true;
            }
        }
        return false;
    }
}