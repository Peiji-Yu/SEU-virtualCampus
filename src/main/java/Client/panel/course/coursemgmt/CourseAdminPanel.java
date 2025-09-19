package Client.panel.course.coursemgmt;

import Client.panel.course.coursemgmt.card.CourseCard;
import Client.panel.course.coursemgmt.dialog.AddTeachingClassDialog;
import Client.panel.course.coursemgmt.dialog.StudentListDialog;
import Client.panel.course.coursemgmt.service.CourseCrud;
import Client.panel.course.coursemgmt.service.CourseService;
import Client.model.course.TeachingClass;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
        btn.setStyle("-fx-background-color: #1D8C4F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;");
        btn.setPrefHeight(40);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #1D8C4F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16;"));
    }

    public CourseAdminPanel() {
        initializeUI();
        loadCourseData();
    }

    private void initializeUI() {
        Label titleLabel = new Label("课程管理");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: black;");

        // 顶部标题栏：标题左侧，刷新图标放在右侧
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(16));
        titleBox.setStyle("-fx-background-color: #FFFFFF;");

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
        refreshIconBtn.setTooltip(new Tooltip("刷新课程"));

        Button addCourseBtn;
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream("/Image/增加.png"));
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(28);
            iv.setFitHeight(28);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            addCourseBtn = new javafx.scene.control.Button();
            addCourseBtn.setGraphic(iv);
        } catch (Exception ex) {
            // 资源加载失败时回退为文字按钮
            addCourseBtn = new javafx.scene.control.Button("添加课程");
        }
        addCourseBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        addCourseBtn.setOnAction(e -> CourseCrud.showAddCourseDialog(this));
        addCourseBtn.setTooltip(new Tooltip("添加课程"));

        titleBox.getChildren().addAll(titleLabel, refreshIconBtn, spacer, addCourseBtn);
        setTop(titleBox);

        statusLabel = new Label("正在加载课程数据...");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666666;");
        HBox statusBox = new HBox(statusLabel);
        statusBox.setPadding(new Insets(8, 16, 8, 16));


        // 使用与学籍管理一致的视觉样式（白色圆角卡片 + 阴影），但保留原有功能
        ComboBox<String> searchType = new ComboBox<>(FXCollections.observableArrayList("全部", "学院", "课程代码", "课程名称"));
        searchType.setValue("全部");
        searchType.setPrefWidth(95);
        searchType.setPrefHeight(40);
        searchType.setStyle("-fx-background-radius: 4; -fx-border-radius: 4;"+
                " -fx-border-color: #1D8C4F; -fx-border-width: 1; -fx-background-color: #ffffff;"+
                " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");

        TextField searchField = createStyledTextField("");
        Button searchBtn = new Button("搜索");
        Button clearBtn = new Button("清除");

        HBox searchBox = new HBox(12);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(12, 16, 12, 16));
        searchBox.setStyle("-fx-background-color: #FFFFFF;");

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
        courseListContainer = new VBox(20);
        courseListContainer.setPadding(new Insets(16, 24, 24, 24));
        courseListContainer.setPrefWidth(1000);

        scrollPane = new ScrollPane(courseListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setPrefViewportHeight(720);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);

        VBox body = new VBox(statusBox, searchBox, scrollPane);
        body.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        body.prefWidthProperty().bind(this.widthProperty().subtract(40));
        body.prefHeightProperty().bind(this.heightProperty().subtract(40));

        setCenter(body);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #FFFFFF;");
    }

    public void loadCourseData() {
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

    // 管理员查看教学班已选学生名单的模态窗口
    public void showStudentListDialog(String teachingClassUuid, String title) {
        StudentListDialog.show(this, teachingClassUuid, title);
    }

    public void showAddClassForCourse(String courseId) {
        AddTeachingClassDialog.showForCourse(this, courseId);
    }

    public TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 16px; -fx-pref-height: 45px; " +
                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent;" +
                "-fx-padding: 0 10px;"
        );
        return field;
    }
}
