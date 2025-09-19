package Client;

import Client.panel.login.component.LogoutHandler;
import Client.panel.login.LoginClientFX;
import Client.panel.student.admin.StudentAdminPanel;
import Client.panel.student.student.StudentSelfPanel;
import Client.panel.course.courseselect.timetable.TimetablePanel;
import Client.panel.course.courseselect.CourseSelectMainPanel;
import Client.panel.course.teacherclass.MyClassroomPanel;
import Client.panel.course.coursemgmt.CourseAdminMainPanel;
import Client.panel.finance.FinancePanel; // 新增导入
import Client.panel.chat.AIChatPanel; // 新增 AI 助手面板导入
import Client.panel.store.StoreMainPanel; // 新增：校园商店面板
import Client.panel.library.LibraryMainPanel; // 新增：导入图书馆面板
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle; // 新增导入
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node; // 新增
import java.util.*;

/**
 * 应用主界面：左侧功能导航 + 右侧内容区（当前只包含学籍管理模块）。
 * 根据 userType(student/admin/teacher) 动态加载不同面板。
 * 注意：仅调整结构与注释，不修改原有UI与行为。
 * @author Msgo-srAm
 */
public class MainFrame {
    // -------------------- 常量样式定义 --------------------
    private static final String PRIMARY_COLOR = "#4e8cff";
    private static final String DANGER_COLOR = "#ff6b6b";
    private static final String DANGER_HOVER_COLOR = "#ff5252";
    private static final String BACKGROUND_COLOR = "#f8fbff";
    private static final String SIDEBAR_COLOR = "#ffffff";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final String SECONDARY_TEXT_COLOR = "#666666";
    // 新增：顶部栏固定高度（使右上角三个按钮与顶部栏上下边界重合）
    private static final double TOP_BAR_HEIGHT = 48.0;
    private double prevX, prevY, prevW, prevH;
    private boolean isMaximized = false;
    // 折叠侧边栏尺寸
    private static final double SIDEBAR_EXPANDED_WIDTH = 180;
    // 调小收起宽度到能显示图标（含左右padding≈30 + 图标≈20，预留6）
    private static final double SIDEBAR_COLLAPSED_WIDTH = 56;

    // -------------------- 实例字段 --------------------
    private Stage stage;
    private final String cardNumber;

    // 用户类型 student / teacher / admin / unknown
    private final String userType;

    // 当前选中导航按钮（用于复位样式）
    private Button currentSelectedButton;

    // 新增：中心内容容器字段
    private StackPane centerContainer;

    // 新增：窗口拖动偏移
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    // 新增：窗口缩放相关字段
    private boolean resizing = false;
    private double resizeStartX, resizeStartY;
    private double resizeStartStageX, resizeStartStageY, resizeStartWidth, resizeStartHeight;
    private ResizeDirection resizeDir = ResizeDirection.NONE;
    private static final int RESIZE_MARGIN = 8;

    // 边缘方向枚举
    private enum ResizeDirection {
        NONE, LEFT, RIGHT, TOP, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    // 新增：功能解释悬浮层
    private StackPane hoverTipLayer;
    private Label hoverTipLabel;
    private Pane hoverTipBox;

    public MainFrame(String cardNumber) {
        this.cardNumber = cardNumber;
        this.userType = getUserType(cardNumber);
    }

    /**
     * 显示主界面窗口。
     */
    public void show() {
        stage = new Stage();
        // 去除自带标题栏
        stage.initStyle(StageStyle.UNDECORATED);
        // 设置窗口图标
        try {
            var url = MainFrame.class.getResource("/Image/Logo.png");
            if (url != null) { stage.getIcons().add(new Image(url.toExternalForm())); }
        } catch (Exception ignore) {}
        stage.setTitle("智慧校园");
        // 调整窗口尺寸以适应更多列
        stage.setMinWidth(1300);
        stage.setMinHeight(830);

        // ===== 最外层根容器（铺满窗口） =====
        StackPane rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: linear-gradient(to bottom right,#f1f6ff,#ffffff);");
        // 移除圆角裁剪

        // 主功能面板：内部使用 BorderPane 分区（顶部、左侧功能栏、中心内容区）
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPrefSize(1300, 780);
        AnchorPane anchorWrapper = new AnchorPane(mainLayout);
        AnchorPane.setTopAnchor(mainLayout, 0.0);
        AnchorPane.setBottomAnchor(mainLayout, 0.0);
        AnchorPane.setLeftAnchor(mainLayout, 0.0);
        AnchorPane.setRightAnchor(mainLayout, 0.0);
        rootStack.getChildren().add(anchorWrapper);

        // 顶部用户栏容器
        HBox topBar = buildTopBar();
        VBox topContainer = new VBox(topBar);
        topContainer.setFillWidth(true);
        mainLayout.setTop(topContainer);

        // 左侧导航栏
        VBox leftBar = new VBox(18);
        leftBar.setPadding(new Insets(15));
        leftBar.setAlignment(Pos.TOP_CENTER);
        // 增加顶部和右侧分割线
        leftBar.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
                + " -fx-border-color: #e2e8f0; -fx-border-width: 1px 1px 0 0;");
        leftBar.setPrefHeight(Double.MAX_VALUE);
        leftBar.setMaxHeight(Double.MAX_VALUE);
        leftBar.setFillWidth(true);

        // 统一图标资源（分别加载各自功能图标）
        Image iconStudent = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/学生.png")));
        Image iconCourseMgmt = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/课程.png")));
        Image iconFinance = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/交易记录.png")));
        Image iconStore = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/商店.png")));
        Image iconAI = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/deepseek.png")));
        Image iconClassroom = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/课堂.png")));
        Image iconTimetable = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/表格.png")));
        Image iconCourseSelect = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/选课.png")));
        Image iconLibrary = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/图书馆.png"))); // 新增图书馆图标

        Image iconStudent_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/学生.png")));
        Image iconCourseMgmt_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/课程.png")));
        Image iconFinance_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/交易记录.png")));
        Image iconStore_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/商店.png")));
        Image iconAI_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/deepseek.png")));
        Image iconClassroom_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/课堂.png")));
        Image iconTimetable_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/表格.png")));
        Image iconCourseSelect_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/选课.png")));
        Image iconLibrary_1 = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar_choose/图书馆.png")));

        // 按钮声明（确保在后续使用前已定义）
        Button stuManageBtn = new Button();
        Button courseMgmtBtn = new Button();
        Button timetableBtn = new Button();
        Button courseSelectBtn = new Button();
        Button myClassroomBtn = new Button();
        Button financeBtn = new Button();
        Button storeBtn = new Button();
        Button aiAssistBtn = new Button();
        Button libraryBtn = new Button();
        Button teacherTimetableBtn = new Button(); // 新增教师课表按钮

        // 统一尺寸与显示
        List<Button> allBtns = Arrays.asList(stuManageBtn, courseMgmtBtn, timetableBtn, courseSelectBtn,
                myClassroomBtn, financeBtn, storeBtn, aiAssistBtn, libraryBtn, teacherTimetableBtn);
        for (Button b : allBtns) {
            b.setPrefWidth(40); b.setPrefHeight(40);
            b.setMinWidth(40); b.setMinHeight(40);
            b.setMaxWidth(40); b.setMaxHeight(40);
            b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            b.setAlignment(Pos.CENTER);
        }

        // 初始化按钮图标（默认使用普通图标）
        attachIconAndRememberText(stuManageBtn, iconStudent);
        attachIconAndRememberText(courseMgmtBtn, iconCourseMgmt);
        attachIconAndRememberText(timetableBtn, iconTimetable);
        attachIconAndRememberText(courseSelectBtn, iconCourseSelect);
        attachIconAndRememberText(myClassroomBtn, iconClassroom);
        attachIconAndRememberText(financeBtn, iconFinance);
        attachIconAndRememberText(storeBtn, iconStore);
        attachIconAndRememberText(aiAssistBtn, iconAI);
        attachIconAndRememberText(libraryBtn, iconLibrary);
        attachIconAndRememberText(teacherTimetableBtn, iconTimetable); // 教师课表按钮图标
        // reportLossBtn 已单独设置图标

        // 使用 map 维护每个按钮对应的普通/选中图标，方便统一切换
        Map<Button, Image> normalIcon = new HashMap<>();
        Map<Button, Image> selectedIcon = new HashMap<>();
        normalIcon.put(stuManageBtn, iconStudent); selectedIcon.put(stuManageBtn, iconStudent_1);
        normalIcon.put(courseMgmtBtn, iconCourseMgmt); selectedIcon.put(courseMgmtBtn, iconCourseMgmt_1);
        normalIcon.put(timetableBtn, iconTimetable); selectedIcon.put(timetableBtn, iconTimetable_1);
        normalIcon.put(courseSelectBtn, iconCourseSelect); selectedIcon.put(courseSelectBtn, iconCourseSelect_1);
        normalIcon.put(myClassroomBtn, iconClassroom); selectedIcon.put(myClassroomBtn, iconClassroom_1);
        normalIcon.put(financeBtn, iconFinance); selectedIcon.put(financeBtn, iconFinance_1);
        normalIcon.put(storeBtn, iconStore); selectedIcon.put(storeBtn, iconStore_1);
        normalIcon.put(aiAssistBtn, iconAI); selectedIcon.put(aiAssistBtn, iconAI_1);
        normalIcon.put(libraryBtn, iconLibrary); selectedIcon.put(libraryBtn, iconLibrary_1);
        normalIcon.put(teacherTimetableBtn, iconTimetable); selectedIcon.put(teacherTimetableBtn, iconTimetable_1); // 教师课表按钮图标
        // 将样式初始化为未选中状态
        resetButtonStyle(stuManageBtn, normalIcon.get(stuManageBtn));
        resetButtonStyle(courseMgmtBtn, normalIcon.get(courseMgmtBtn));
        resetButtonStyle(timetableBtn, normalIcon.get(timetableBtn));
        resetButtonStyle(courseSelectBtn, normalIcon.get(courseSelectBtn));
        resetButtonStyle(myClassroomBtn, normalIcon.get(myClassroomBtn));
        resetButtonStyle(financeBtn, normalIcon.get(financeBtn));
        resetButtonStyle(storeBtn, normalIcon.get(storeBtn));
        resetButtonStyle(aiAssistBtn, normalIcon.get(aiAssistBtn));
        resetButtonStyle(libraryBtn, normalIcon.get(libraryBtn));
        resetButtonStyle(teacherTimetableBtn, normalIcon.get(teacherTimetableBtn)); // 教师课表按钮样式

        // ===== 简化事件处理：统一通过 mapping 切换图标与样式 =====
        stuManageBtn.setOnAction(e -> {
            if (currentSelectedButton == stuManageBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(stuManageBtn, selectedIcon.get(stuManageBtn));
            currentSelectedButton = stuManageBtn;
            if ("student".equals(userType)) setCenterContent(new StudentSelfPanel(cardNumber)); else setCenterContent(new StudentAdminPanel());
        });

        courseMgmtBtn.setOnAction(e -> {
            if (currentSelectedButton == courseMgmtBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(courseMgmtBtn, selectedIcon.get(courseMgmtBtn));
            currentSelectedButton = courseMgmtBtn;
            setCenterContent(new CourseAdminMainPanel());
        });

        timetableBtn.setOnAction(e -> {
            if (currentSelectedButton == timetableBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(timetableBtn, selectedIcon.get(timetableBtn));
            currentSelectedButton = timetableBtn;
            setCenterContent(new TimetablePanel(cardNumber));
        });

        courseSelectBtn.setOnAction(e -> {
            if (currentSelectedButton == courseSelectBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(courseSelectBtn, selectedIcon.get(courseSelectBtn));
            currentSelectedButton = courseSelectBtn;
            setCenterContent(new CourseSelectMainPanel(Integer.valueOf(cardNumber)));
        });

        myClassroomBtn.setOnAction(e -> {
            if (currentSelectedButton == myClassroomBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(myClassroomBtn, selectedIcon.get(myClassroomBtn));
            currentSelectedButton = myClassroomBtn;
            setCenterContent(new MyClassroomPanel(cardNumber));
        });

        financeBtn.setOnAction(e -> {
            if (currentSelectedButton == financeBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(financeBtn, selectedIcon.get(financeBtn));
            currentSelectedButton = financeBtn;
            setCenterContent(new FinancePanel(cardNumber, userType));
        });

        storeBtn.setOnAction(e -> {
            if (currentSelectedButton == storeBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(storeBtn, selectedIcon.get(storeBtn));
            currentSelectedButton = storeBtn;
            setCenterContent(new StoreMainPanel(cardNumber, userType));
        });

        aiAssistBtn.setOnAction(e -> {
            if (currentSelectedButton == aiAssistBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(aiAssistBtn, selectedIcon.get(aiAssistBtn));
            currentSelectedButton = aiAssistBtn;
            setCenterContent(new AIChatPanel(cardNumber));
        });

        libraryBtn.setOnAction(e -> {
            if (currentSelectedButton == libraryBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(libraryBtn, selectedIcon.get(libraryBtn));
            currentSelectedButton = libraryBtn;
            setCenterContent(new LibraryMainPanel(cardNumber));
        });

        teacherTimetableBtn.setOnAction(e -> {
            if (currentSelectedButton == teacherTimetableBtn) return;
            if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
            setSelectedButtonStyle(teacherTimetableBtn, selectedIcon.get(teacherTimetableBtn));
            currentSelectedButton = teacherTimetableBtn;
            setCenterContent(new Client.panel.course.teacherclass.timetable.TeacherTimetablePanel(cardNumber));
        });

        // 收集所有需要随折叠切换文字的按钮（不再包含退出登录）
        List<Button> navButtons = new ArrayList<>();
        navButtons.add(stuManageBtn);
        navButtons.add(courseMgmtBtn);
        navButtons.add(timetableBtn);
        navButtons.add(courseSelectBtn);
        navButtons.add(myClassroomBtn);
        navButtons.add(financeBtn); // 交易管理
        navButtons.add(storeBtn);   // 校园商店
        navButtons.add(aiAssistBtn); // AI 助手
        navButtons.add(libraryBtn);  // 新增：图书馆
        navButtons.add(teacherTimetableBtn); // 教师课表按钮

        // 为每个按钮添加图标与保存原文案
        attachIconAndRememberText(stuManageBtn, iconStudent);
        attachIconAndRememberText(courseMgmtBtn, iconCourseMgmt);
        attachIconAndRememberText(timetableBtn, iconTimetable);
        attachIconAndRememberText(courseSelectBtn, iconCourseSelect);
        attachIconAndRememberText(myClassroomBtn, iconClassroom);
        attachIconAndRememberText(financeBtn, iconFinance);
        attachIconAndRememberText(storeBtn, iconStore);
        attachIconAndRememberText(aiAssistBtn, iconAI);
        attachIconAndRememberText(libraryBtn, iconLibrary);
        attachIconAndRememberText(teacherTimetableBtn, iconTimetable); // 教师课表按钮图标

        // 为每个功能区按钮设置右侧 tooltip
        setRightTooltip(stuManageBtn, "学籍管理");
        setRightTooltip(courseMgmtBtn, "课程管理");
        setRightTooltip(timetableBtn, "我的课表");
        setRightTooltip(courseSelectBtn, "选课");
        setRightTooltip(myClassroomBtn, "我的课堂");
        setRightTooltip(financeBtn, "交易管理");
        setRightTooltip(storeBtn, "校园商店");
        setRightTooltip(aiAssistBtn, "AI助手");
        setRightTooltip(libraryBtn, "图书管理"); // 新增：图书馆提示
        setRightTooltip(teacherTimetableBtn, "教师课表"); // 教师课表按钮提示

        // 中心内容容器（StackPane，便于后续叠加遮罩/弹层）
        centerContainer = new StackPane();
        // 增加顶部分割线和左侧分割线
        centerContainer.setStyle("-fx-background-color: #F6F8FA; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04),12,0,0,2);"
                + " -fx-border-color: #e2e8f0; -fx-border-width: 1px 0 0 1px;");
        StackPane.setMargin(centerContainer, new Insets(0));
        mainLayout.setCenter(centerContainer);

        // 初始化 hover tip（避免未初始化警告与潜在 NPE）
        hoverTipLayer = new StackPane();
        hoverTipLayer.setMouseTransparent(true);
        hoverTipBox = new Pane();
        hoverTipBox.setVisible(false);
        hoverTipLabel = new Label("");
        hoverTipLabel.setVisible(false);
        hoverTipBox.getChildren().add(hoverTipLabel);
        hoverTipLayer.getChildren().add(hoverTipBox);
        // 将 hover 层添加到 rootStack 的最上层
        rootStack.getChildren().add(hoverTipLayer);

        // 右侧内容初始载入
        if ("student".equals(userType)) {
            setCenterContent(new StudentSelfPanel(cardNumber));
        } else if ("admin".equals(userType)) {
            setCenterContent(new StudentAdminPanel());
        } else if ("teacher".equals(userType)) {
            setCenterContent(new MyClassroomPanel(cardNumber));
        }

        // 学生与管理员显示"学籍管理"按钮；学生显示"我的课表""选课"；教师显示"我的课堂"；管理员显示"课程管理"
        if ("student".equals(userType) || "admin".equals(userType) || "teacher".equals(userType)) {
            if ("student".equals(userType) || "admin".equals(userType)) {
                leftBar.getChildren().add(stuManageBtn);
            }
            if ("admin".equals(userType)) {
                leftBar.getChildren().add(courseMgmtBtn);
            }
            // 删除学生的“我的课表”按钮，只保留选课按钮
            if ("student".equals(userType)) {
                leftBar.getChildren().addAll(courseSelectBtn);
            }
            if ("teacher".equals(userType)) {
                leftBar.getChildren().add(myClassroomBtn);
                leftBar.getChildren().add(teacherTimetableBtn); // 教师课表按钮
            }
            // 三类角色统一添加交易管理
            if ("student".equals(userType) || "admin".equals(userType) || "teacher".equals(userType)) {
                leftBar.getChildren().add(financeBtn);
                leftBar.getChildren().add(storeBtn); // 新增：校园商店
            }
            // 新增：图书馆按钮添加（对所有用户类型开放）
            leftBar.getChildren().add(libraryBtn);
            // AI 助手按钮添加
            leftBar.getChildren().add(aiAssistBtn);

            // 初次默认选中
            if ("student".equals(userType) || "admin".equals(userType)) {
                currentSelectedButton = stuManageBtn;
            } else if ("teacher".equals(userType)) {
                currentSelectedButton = myClassroomBtn;
            }

            // 确保初始选中按钮显示为高亮图标
            if (currentSelectedButton != null) {
                Image sel = selectedIcon.get(currentSelectedButton);
                if (sel != null) setSelectedButtonStyle(currentSelectedButton, sel);
            }

            stuManageBtn.setOnAction(e -> {
                if (currentSelectedButton == stuManageBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                }
                setSelectedButtonStyle(stuManageBtn, selectedIcon.get(stuManageBtn));
                currentSelectedButton = stuManageBtn;
                if ("student".equals(userType)) {
                    setCenterContent(new StudentSelfPanel(cardNumber));
                } else {
                    setCenterContent(new StudentAdminPanel());
                }
            });

            // 管理员-课程管理
            courseMgmtBtn.setOnAction(e -> {
                if (currentSelectedButton == courseMgmtBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                }
                setSelectedButtonStyle(courseMgmtBtn, selectedIcon.get(courseMgmtBtn));
                currentSelectedButton = courseMgmtBtn;
                setCenterContent(new CourseAdminMainPanel());
            });

            // 学生-选课
            courseSelectBtn.setOnAction(e -> {
                if (currentSelectedButton == courseSelectBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                }
                setSelectedButtonStyle(courseSelectBtn, selectedIcon.get(courseSelectBtn));
                currentSelectedButton = courseSelectBtn;
                setCenterContent(new CourseSelectMainPanel(Integer.valueOf(cardNumber)));
            });

            // 教师-我的课堂
            myClassroomBtn.setOnAction(e -> {
                if (currentSelectedButton == myClassroomBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                }
                setSelectedButtonStyle(myClassroomBtn, selectedIcon.get(myClassroomBtn));
                currentSelectedButton = myClassroomBtn;
                setCenterContent(new MyClassroomPanel(cardNumber));
            });

            // 新增：交易管理
            financeBtn.setOnAction(e -> {
                if (currentSelectedButton == financeBtn) return;
                if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                setSelectedButtonStyle(financeBtn, selectedIcon.get(financeBtn));
                currentSelectedButton = financeBtn;
                setCenterContent(new FinancePanel(cardNumber, userType));
            });

            // 新增：校园商店事件
            storeBtn.setOnAction(e -> {
                if (currentSelectedButton == storeBtn) return;
                if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                setSelectedButtonStyle(storeBtn, selectedIcon.get(storeBtn));
                currentSelectedButton = storeBtn;
                setCenterContent(new StoreMainPanel(cardNumber, userType));
            });

            // 新增：AI 助手
            aiAssistBtn.setOnAction(e -> {
                if (currentSelectedButton == aiAssistBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                }
                setSelectedButtonStyle(aiAssistBtn, selectedIcon.get(aiAssistBtn));
                currentSelectedButton = aiAssistBtn;
                setCenterContent(new AIChatPanel(cardNumber));
            });

            // 新增：图书馆事件
            libraryBtn.setOnAction(e -> {
                if (currentSelectedButton == libraryBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                }
                setSelectedButtonStyle(libraryBtn, selectedIcon.get(libraryBtn));
                currentSelectedButton = libraryBtn;
                // 创建图书馆主面板并显示在右侧，传递一卡通号
                setCenterContent(new LibraryMainPanel(cardNumber));
            });

            // 新增：教师课表事件
            teacherTimetableBtn.setOnAction(e -> {
                if (currentSelectedButton == teacherTimetableBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton, normalIcon.get(currentSelectedButton));
                }
                setSelectedButtonStyle(teacherTimetableBtn, selectedIcon.get(teacherTimetableBtn));
                currentSelectedButton = teacherTimetableBtn;
                setCenterContent(new Client.panel.course.teacherclass.timetable.TeacherTimetablePanel(cardNumber));
            });

            // ===== 修改：使用透明 Region 占据剩余垂直空间（去除占位文字与背景） =====
            Region functionSpacer = new Region();
            VBox.setVgrow(functionSpacer, Priority.ALWAYS);
            leftBar.getChildren().add(functionSpacer);


            // 新增：一卡通挂失按钮添加
            // 左下角退出登录按钮（保持原样式与逻辑）
            Button reportLossBtn = new Button();
            reportLossBtn.setPrefWidth(40);
            reportLossBtn.setPrefHeight(40);
            reportLossBtn.setMinWidth(40);
            reportLossBtn.setMinHeight(40);
            reportLossBtn.setMaxWidth(40);
            reportLossBtn.setMaxHeight(40);
            reportLossBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            reportLossBtn.setAlignment(Pos.CENTER);
            Image reportLossIcon = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/挂失.png")));
            // 使用统一方法设置图标（带固定占位 wrapper）
            attachIconAndRememberText(reportLossBtn, reportLossIcon);
            reportLossBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";");
            reportLossBtn.setOnMouseEntered(e -> setButtonHoverShadow(reportLossBtn));
            reportLossBtn.setOnMouseExited(e -> reportLossBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";"));
            reportLossBtn.setOnAction(e -> LogoutHandler.handleLogout(stage));
            setRightTooltip(reportLossBtn, "一卡通挂失");
            leftBar.getChildren().add(reportLossBtn);

            // 新增：一卡通挂失事件
            reportLossBtn.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("一卡通挂失");
                alert.setHeaderText(null);
                alert.setContentText("确定要挂失您的一卡通吗？");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    // 发送挂失请求
                    new Thread(() -> {
                        try {
                            String resp = ClientNetworkHelper.reportLoss(cardNumber);
                            // 可根据resp弹窗提示
                            javafx.application.Platform.runLater(() -> {
                                Alert info = new Alert(Alert.AlertType.INFORMATION);
                                info.setTitle("挂失结果");
                                info.setHeaderText(null);
                                info.setContentText("挂失成功");
                                info.showAndWait();
                            });
                        } catch (Exception ex) {
                            javafx.application.Platform.runLater(() -> {
                                Alert err = new Alert(Alert.AlertType.ERROR);
                                err.setTitle("挂失失败");
                                err.setHeaderText(null);
                                err.setContentText("网络异常或服务器无响应");
                                err.showAndWait();
                            });
                        }
                    }).start();
                }
            });

            // 新增：左下角"修改密码"按钮
            Button changePwdSidebarBtn = new Button();
            changePwdSidebarBtn.setPrefWidth(40);
            changePwdSidebarBtn.setPrefHeight(40);
            changePwdSidebarBtn.setMinWidth(40);
            changePwdSidebarBtn.setMinHeight(40);
            changePwdSidebarBtn.setMaxWidth(40);
            changePwdSidebarBtn.setMaxHeight(40);
            changePwdSidebarBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            changePwdSidebarBtn.setAlignment(Pos.CENTER);
            // 设置图标
            Image pwdIcon = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/修改密码.png")));
            // 使用统一方法设置图标（带固定占位 wrapper）
            attachIconAndRememberText(changePwdSidebarBtn, pwdIcon);
            // 样式与功能区按钮一致
            changePwdSidebarBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";");
            // 悬停阴影
            changePwdSidebarBtn.setOnMouseEntered(e -> setButtonHoverShadow(changePwdSidebarBtn));
            changePwdSidebarBtn.setOnMouseExited(e -> changePwdSidebarBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";"));
            // 事件
            changePwdSidebarBtn.setOnAction(e -> new LoginClientFX().openAsRecovery(stage, cardNumber));
            // Tooltip
            setRightTooltip(changePwdSidebarBtn, "修改密码");
            // 添加到左侧底部（退出登录按钮之上）
            leftBar.getChildren().add(changePwdSidebarBtn);

            // 左下角退出登录按钮（保持原样式与逻辑）
            Button logoutSidebarBtn = new Button();
            logoutSidebarBtn.setPrefWidth(40);
            logoutSidebarBtn.setPrefHeight(40);
            logoutSidebarBtn.setMinWidth(40);
            logoutSidebarBtn.setMinHeight(40);
            logoutSidebarBtn.setMaxWidth(40);
            logoutSidebarBtn.setMaxHeight(40);
            logoutSidebarBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            logoutSidebarBtn.setAlignment(Pos.CENTER);
            Image logoutIcon = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/退出.png")));
            // 使用统一方法设置图标（带固定占位 wrapper）
            attachIconAndRememberText(logoutSidebarBtn, logoutIcon);
            logoutSidebarBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";");
            logoutSidebarBtn.setOnMouseEntered(e -> setButtonHoverShadow(logoutSidebarBtn));
            logoutSidebarBtn.setOnMouseExited(e -> logoutSidebarBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";"));
            logoutSidebarBtn.setOnAction(e -> LogoutHandler.handleLogout(stage));
            setRightTooltip(logoutSidebarBtn, "退出登录");
            leftBar.getChildren().add(logoutSidebarBtn);
            // ===== 修改结束 =====
        } else {
            // 其他类型暂无功能，显示提示
            VBox noFunctionBox = new VBox(20);
            noFunctionBox.setAlignment(Pos.CENTER);
            noFunctionBox.setStyle("-fx-background-color: " + BACKGROUND_COLOR + "; -fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2); -fx-padding: 40;");
            Label noFunctionTitle = new Label("欢迎使用智慧校园系统");
            noFunctionTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");
            Label noFunctionDesc = new Label("您当前的账户类型暂无可用功能模块");
            noFunctionDesc.setStyle("-fx-font-size: 16px; -fx-text-fill: " + SECONDARY_TEXT_COLOR + ";");
            noFunctionBox.getChildren().addAll(noFunctionTitle, noFunctionDesc);
            setCenterContent(noFunctionBox);
        }

        // ---- 侧边栏容器（无展开/收起动画） ----
        StackPane sidebarContainer = new StackPane(leftBar);
        sidebarContainer.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
        sidebarContainer.setPrefWidth(SIDEBAR_COLLAPSED_WIDTH);
        sidebarContainer.setMaxWidth(SIDEBAR_COLLAPSED_WIDTH);
        sidebarContainer.prefHeightProperty().bind(mainLayout.heightProperty());
        sidebarContainer.maxHeightProperty().bind(mainLayout.heightProperty());
        leftBar.prefHeightProperty().bind(sidebarContainer.heightProperty());
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarContainer.widthProperty());
        clip.heightProperty().bind(sidebarContainer.heightProperty());
        sidebarContainer.setClip(clip);

        mainLayout.setLeft(sidebarContainer);

        Scene scene = new Scene(rootStack, 1500, 780);

        // 新增：窗口边缘拖动缩放
        scene.setOnMouseMoved(e -> {
            ResizeDirection dir = getResizeDirection(e, stage);
            switch (dir) {
                case LEFT: scene.setCursor(javafx.scene.Cursor.W_RESIZE); break;
                case RIGHT: scene.setCursor(javafx.scene.Cursor.E_RESIZE); break;
                case TOP: scene.setCursor(javafx.scene.Cursor.N_RESIZE); break;
                case BOTTOM: scene.setCursor(javafx.scene.Cursor.S_RESIZE); break;
                case TOP_LEFT: scene.setCursor(javafx.scene.Cursor.NW_RESIZE); break;
                case TOP_RIGHT: scene.setCursor(javafx.scene.Cursor.NE_RESIZE); break;
                case BOTTOM_LEFT: scene.setCursor(javafx.scene.Cursor.SW_RESIZE); break;
                case BOTTOM_RIGHT: scene.setCursor(javafx.scene.Cursor.SE_RESIZE); break;
                default: scene.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });

        scene.setOnMousePressed(e -> {
            resizeDir = getResizeDirection(e, stage);
            if (resizeDir != ResizeDirection.NONE) {
                resizing = true;
                resizeStartX = e.getScreenX();
                resizeStartY = e.getScreenY();
                resizeStartStageX = stage.getX();
                resizeStartStageY = stage.getY();
                resizeStartWidth = stage.getWidth();
                resizeStartHeight = stage.getHeight();
                e.consume();
            }
        });

        scene.setOnMouseDragged(e -> {
            if (resizing && resizeDir != ResizeDirection.NONE) {
                double dx = e.getScreenX() - resizeStartX;
                double dy = e.getScreenY() - resizeStartY;
                double minW = stage.getMinWidth();
                double minH = stage.getMinHeight();
                switch (resizeDir) {
                    case LEFT:
                        double newW = Math.max(minW, resizeStartWidth - dx);
                        stage.setWidth(newW);
                        stage.setX(resizeStartStageX + (resizeStartWidth - newW));
                        break;
                    case RIGHT:
                        stage.setWidth(Math.max(minW, resizeStartWidth + dx));
                        break;
                    case TOP:
                        double newH = Math.max(minH, resizeStartHeight - dy);
                        stage.setHeight(newH);
                        stage.setY(resizeStartStageY + (resizeStartHeight - newH));
                        break;
                    case BOTTOM:
                        stage.setHeight(Math.max(minH, resizeStartHeight + dy));
                        break;
                    case TOP_LEFT:
                        newW = Math.max(minW, resizeStartWidth - dx);
                        newH = Math.max(minH, resizeStartHeight - dy);
                        stage.setWidth(newW);
                        stage.setHeight(newH);
                        stage.setX(resizeStartStageX + (resizeStartWidth - newW));
                        stage.setY(resizeStartStageY + (resizeStartHeight - newH));
                        break;
                    case TOP_RIGHT:
                        newW = Math.max(minW, resizeStartWidth + dx);
                        newH = Math.max(minH, resizeStartHeight - dy);
                        stage.setWidth(newW);
                        stage.setHeight(newH);
                        stage.setY(resizeStartStageY + (resizeStartHeight - newH));
                        break;
                    case BOTTOM_LEFT:
                        newW = Math.max(minW, resizeStartWidth - dx);
                        newH = Math.max(minH, resizeStartHeight + dy);
                        stage.setWidth(newW);
                        stage.setHeight(newH);
                        stage.setX(resizeStartStageX + (resizeStartWidth - newW));
                        break;
                    case BOTTOM_RIGHT:
                        newW = Math.max(minW, resizeStartWidth + dx);
                        newH = Math.max(minH, resizeStartHeight + dy);
                        stage.setWidth(newW);
                        stage.setHeight(newH);
                        break;
                }
                e.consume();
            }
        });

        scene.setOnMouseReleased(e -> {
            resizing = false;
            resizeDir = ResizeDirection.NONE;
        });

        stage.setScene(scene);
        stage.show();

        // 初次进入为收起状态，隐藏文字；靠近左缘或移入侧栏展开
        applySidebarText(navButtons, false);

    }

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        // 顶栏高度固定，去除垂直内边距，保证右上角按钮与上下边界重合
        bar.setMinHeight(TOP_BAR_HEIGHT);
        bar.setPrefHeight(TOP_BAR_HEIGHT);
        bar.setMaxHeight(TOP_BAR_HEIGHT);
        bar.setPadding(new Insets(0, 0, 0, 0));
        bar.setAlignment(Pos.CENTER_LEFT);
//      bar.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8,0,0,2);");
        bar.setStyle("-fx-background-color: #176B3A; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8,0,0,2);");

        // 左侧Logo
        ImageView logoView = null;
        try {
            var url = MainFrame.class.getResource("/Image/Logo.png");
            if (url != null) {
                Image logoImg = new Image(url.toExternalForm());
                logoView = new ImageView(logoImg);
                logoView.setFitHeight(32);
                logoView.setPreserveRatio(true);
            }
        } catch (Exception ignore) {}

        String roleCN;
        switch (userType) {
            case "student": roleCN = "学生"; break;
            case "teacher": roleCN = "教师"; break;
            case "admin": roleCN = "管理员"; break;
            default: roleCN = "未知";
        }
        Label userInfo = new Label(roleCN + " " + cardNumber);
//      userInfo.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        userInfo.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");

        // 左侧组合容器，提供水平内边距
        HBox leftGroup = new HBox(20);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        leftGroup.setPadding(new Insets(0, 0, 0, 11));


        // 只显示 logo 和用户信息
        if (logoView != null) {
            leftGroup.getChildren().addAll(logoView, userInfo);
        } else {
            leftGroup.getChildren().addAll(userInfo);
        }

        // 中部可拖动区域（占满剩余空间）
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 新增：窗口控制按钮组（与顶部栏等高正方形）
        HBox windowBtnBox = new HBox(0);
        windowBtnBox.setAlignment(Pos.CENTER_RIGHT);

        Image imgMin = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/titlebar/Minimize-2.png")));
        Image imgMax = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/titlebar/Maximize-1.png")));
        Image imgRestore = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/titlebar/Maximize-3.png")));
        Image imgClose = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/titlebar/关闭.png")));

        Button minBtn = new Button();
        minBtn.setMinSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        minBtn.setPrefSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        minBtn.setMaxSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        // 去除圆角
        minBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;");
        minBtn.setTooltip(new Tooltip("最小化"));
        ImageView minView = new ImageView(imgMin);
        minView.setFitWidth(10); minView.setFitHeight(10); minView.setPreserveRatio(true);
        minBtn.setGraphic(minView);
        minBtn.setOnAction(e -> {
            if (stage != null) stage.setIconified(true);
        });
        // 悬停变灰色
        minBtn.setOnMouseEntered(e -> minBtn.setStyle("-fx-background-color: #CED0D6; -fx-background-radius: 0;"));
        minBtn.setOnMouseExited(e -> minBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;"));

        Button maxBtn = new Button();
        maxBtn.setMinSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        maxBtn.setPrefSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        maxBtn.setMaxSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        // 去除圆角
        maxBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;");
        maxBtn.setTooltip(new Tooltip("最大化/还原"));
        ImageView maxView = new ImageView(imgMax);
        maxView.setFitWidth(10); maxView.setFitHeight(10); maxView.setPreserveRatio(true);
        maxBtn.setGraphic(maxView);
        maxBtn.setOnAction(e -> {
            if (stage != null) {
                if (!isMaximized) {
                    // 保存当前位置和大小
                    prevX = stage.getX();
                    prevY = stage.getY();
                    prevW = stage.getWidth();
                    prevH = stage.getHeight();

                    // 最大化到可见区域（不覆盖任务栏）
                    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                    stage.setX(bounds.getMinX());
                    stage.setY(bounds.getMinY());
                    stage.setWidth(bounds.getWidth());
                    stage.setHeight(bounds.getHeight());

                    maxView.setImage(imgRestore); // 换成还原图标
                } else {
                    // 还原
                    stage.setX(prevX);
                    stage.setY(prevY);
                    stage.setWidth(prevW);
                    stage.setHeight(prevH);

                    maxView.setImage(imgMax); // 换成最大化图标
                }
                isMaximized = !isMaximized;
            }
        });
        // 悬停变灰色
        maxBtn.setOnMouseEntered(e -> maxBtn.setStyle("-fx-background-color: #CED0D6; -fx-background-radius: 0;"));
        maxBtn.setOnMouseExited(e -> maxBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;"));

        Button closeBtn = new Button();
        closeBtn.setMinSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        closeBtn.setPrefSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        closeBtn.setMaxSize(TOP_BAR_HEIGHT, TOP_BAR_HEIGHT);
        // 去除圆角
        closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;");
        closeBtn.setTooltip(new Tooltip("关闭"));
        ImageView closeView = new ImageView(imgClose);
        closeView.setFitWidth(10); closeView.setFitHeight(10); closeView.setPreserveRatio(true);
        closeBtn.setGraphic(closeView);
        closeBtn.setOnAction(e -> {
            if (stage != null) stage.close();
        });
        // 悬停变红色
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #ff5252; -fx-background-radius: 0;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 0;"));

        windowBtnBox.getChildren().addAll(minBtn, maxBtn, closeBtn);

        // 顶栏内容：左组、拖动空白、右侧按钮
        bar.getChildren().addAll(leftGroup, spacer, windowBtnBox);

        // 新增：用户栏空白处拖动窗口（仅 spacer 区域可拖动）
        bar.setOnMousePressed(e -> {
            if (e.getTarget() == spacer) {
                dragOffsetX = e.getSceneX();
                dragOffsetY = e.getSceneY();
            } else {
                dragOffsetX = -1;
                dragOffsetY = -1;
            }
        });
        bar.setOnMouseDragged(e -> {
            if (dragOffsetX >= 0 && dragOffsetY >= 0) {
                Stage s = stage;
                if( s != null) {
                    s.setX(e.getScreenX() - dragOffsetX);
                    s.setY(e.getScreenY() - dragOffsetY);
                }
            }
        });

        return bar;
    }


    private void attachIconAndRememberText(Button btn, Image icon){
        if (btn.getUserData() == null) {
            btn.setUserData(btn.getText());
        }
        ImageView iv = new ImageView(icon);
        iv.setFitWidth(28); // 图标宽度扩大
        iv.setFitHeight(28); // 图标高度扩大
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        // 使用固定尺寸的容器包裹 ImageView，保证不同图片（有透明边距或实际像素不同）时图标占位一致
        StackPane graphicWrapper = new StackPane(iv);
        graphicWrapper.setPrefSize(28, 28);
        graphicWrapper.setMinSize(28, 28);
        graphicWrapper.setMaxSize(28, 28);
        graphicWrapper.setAlignment(Pos.CENTER);
        btn.setGraphic(graphicWrapper);
        btn.setGraphicTextGap(8);
    }

    private void setSelectedButtonStyle(Button button, Image icon) {
        if (button == null) return;
        // 选中时不使用阴影，仅变色并替换为高亮图标
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: none;" +
                        "-fx-font-size: 15px; -fx-font-weight: bold; " +
                        "-fx-text-fill: " + PRIMARY_COLOR + ";"
        );
        if (icon != null) attachIconAndRememberText(button, icon);
    }

    private void resetButtonStyle(Button button, Image icon) {
        if (button == null) return;
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: none;" +
                        "-fx-font-size: 15px; " +
                        "-fx-text-fill: " + TEXT_COLOR + ";"
        );
        if (icon != null) attachIconAndRememberText(button, icon);
    }

    private void setButtonHoverShadow(Button button) {
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: dropshadow(gaussian, #1E1F22, 10, 0, 0, 2);" + // 光效颜色与悬停阴影一致
                        "-fx-font-size: 15px; " +
                        "-fx-text-fill: " + TEXT_COLOR + ";"
        );
    }

    // 重载：只设置选中样式但不替换图标
    private void setSelectedButtonStyle(Button button) {
        if (button == null) return;
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: none;" +
                        "-fx-font-size: 15px; -fx-font-weight: bold; " +
                        "-fx-text-fill: " + PRIMARY_COLOR + ";"
        );
    }

    // 重载：只重置样式但不替换图标
    private void resetButtonStyle(Button button) {
        if (button == null) return;
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: none;" +
                        "-fx-font-size: 15px; " +
                        "-fx-text-fill: " + TEXT_COLOR + ";"
        );
    }

    private void applySidebarText(List<Button> buttons, boolean expanded){
        for (Button b : buttons) {
            // 只保留图片，取消文字，并设置为正方形 40x40
            b.setText("");
            b.setPrefWidth(40);
            b.setPrefHeight(40);
            b.setMinWidth(40);
            b.setMinHeight(40);
            b.setMaxWidth(40);
            b.setMaxHeight(40);
            b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            b.setAlignment(Pos.CENTER);

            // 按钮背景色与功能区一致
            b.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none;");

            // 悬停阴影
            b.setOnMouseEntered(e -> {
                if (currentSelectedButton != b) setButtonHoverShadow(b);
            });
            b.setOnMouseExited(e -> {
                if (currentSelectedButton != b) resetButtonStyle(b);
            });
        }
    }

    // -------------------- 构建控件 / 样式方法 --------------------
    private Button buildLogoutButton() {
        Button logoutBtn = new Button("退出登录");
        logoutBtn.setPrefWidth(130);
        logoutBtn.setPrefHeight(45);
        setDangerButtonStyle(logoutBtn);
        logoutBtn.setOnMouseEntered(e -> setDangerButtonHoverStyle(logoutBtn));
        logoutBtn.setOnMouseExited(e -> setDangerButtonStyle(logoutBtn));
        logoutBtn.setOnAction(e -> LogoutHandler.handleLogout(stage));
        return logoutBtn;
    }


    private void setDangerButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,107,107,0.3), 8, 0, 0, 2);");
    }

    private void setDangerButtonHoverStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-background-color: " + DANGER_HOVER_COLOR + "; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,82,82,0.4), 10, 0, 0, 3);");
    }

    // -------------------- 工具方法 --------------------
    private String getUserType(String cardNumber) {
        if (cardNumber.length() == 9 && cardNumber.startsWith("2")) {
            return "student";
        }
        if (cardNumber.length() == 9 && cardNumber.startsWith("1")) {
            return "teacher";
        }
        try {
            int num = Integer.parseInt(cardNumber);
            if (num <= 1000) {
                return "admin";
            }
        } catch (Exception e) {
            // 忽略异常，返回未知类型
        }
        return "unknown";
    }

    // 新增缺失的方法：设置中心内容
    private void setCenterContent(Node node) {
        if (centerContainer == null || node == null) return;
        centerContainer.getChildren().setAll(node);
        // 将内容置顶（避免子面板在垂直方向居中产生上方空白）
        StackPane.setAlignment(node, Pos.TOP_CENTER);
    }

    // 判断鼠标是否在窗口边缘
    private ResizeDirection getResizeDirection(MouseEvent e, Stage stage) {
        double x = e.getSceneX();
        double y = e.getSceneY();
        double w = stage.getWidth();
        double h = stage.getHeight();
        boolean left = x <= RESIZE_MARGIN;
        boolean right = x >= w - RESIZE_MARGIN;
        boolean top = y <= RESIZE_MARGIN;
        boolean bottom = y >= h - RESIZE_MARGIN;
        if (top && left) return ResizeDirection.TOP_LEFT;
        if (top && right) return ResizeDirection.TOP_RIGHT;
        if (bottom && left) return ResizeDirection.BOTTOM_LEFT;
        if (bottom && right) return ResizeDirection.BOTTOM_RIGHT;
        if (left) return ResizeDirection.LEFT;
        if (right) return ResizeDirection.RIGHT;
        if (top) return ResizeDirection.TOP;
        if (bottom) return ResizeDirection.BOTTOM;
        return ResizeDirection.NONE;
    }

    // 显示悬浮解释窗口
    private void showHoverTip(Button btn, StackPane sidebarContainer) {
        Object ud = btn.getUserData();
        if (ud == null) return;
        hoverTipLabel.setText(ud.toString());

        // 计算解释窗口位置：在 sidebarContainer 右侧，按钮垂直居中
        javafx.geometry.Point2D sidebarScene = sidebarContainer.localToScene(0, 0);
        javafx.geometry.Point2D btnScene = btn.localToScene(0, 0);

        double x = sidebarScene.getX() + sidebarContainer.getWidth() + 8;
        double y = btnScene.getY() + (btn.getHeight() - hoverTipBox.getPrefHeight()) / 2.0;

        hoverTipBox.setLayoutX(x);
        hoverTipBox.setLayoutY(y);
        hoverTipBox.setVisible(true);
        hoverTipLabel.setVisible(true);
        hoverTipLayer.toFront();
        hoverTipBox.toFront();
    }

    // 隐藏悬浮解释窗口
    private void hideHoverTip() {
        hoverTipBox.setVisible(false);
        hoverTipLabel.setVisible(false);
    }

    /**
     * 设置按钮右侧显示的 Tooltip
     */
    private void setRightTooltip(Button btn, String text) {
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(200));
        tip.setHideDelay(Duration.millis(100));
        tip.setStyle("-fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 6 12 6 12;");
        // 设置 tooltip 显示在右侧
        tip.setOnShowing(e -> {
            javafx.stage.Window w = tip.getOwnerWindow();
            if (w != null && btn.getScene() != null) {
                javafx.geometry.Point2D btnPos = btn.localToScene(0, 0);
                double x = btn.getScene().getWindow().getX() + btnPos.getX() + btn.getWidth() + 10;
                double y = btn.getScene().getWindow().getY() + btnPos.getY() + btn.getHeight() / 2 - 18;
                tip.setAnchorX(x);
                tip.setAnchorY(y);
            }
        });
        btn.setTooltip(tip);
    }
}

