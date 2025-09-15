package Client;

import Client.login.component.LogoutHandler;
import Client.login.LoginClientFX;
import Client.studentmgmt.admin.StudentAdminPanel;
import Client.studentmgmt.student.StudentSelfPanel;
import Client.timetable.TimetablePanel;
import Client.courseselect.CourseSelectPanel;
import Client.teacherclass.MyClassroomPanel;
import Client.coursemgmt.admin.CourseAdminPanel;
import Client.finance.FinancePanel; // 新增导入
import Client.DeepSeekChat.AIChatPanel; // 新增 AI 助手面板导入
import Client.store.StoreMainPanel; // 新增：校园商店面板
import Client.library.LibraryMainPanel; // 新增：导入图书馆面板
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
 * 应用主界面：左侧功能导�� + 右侧内容区（当前只包含学籍管理模块）。
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
    private static final String SIDEBAR_COLOR = "#6EFF7E";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final String SECONDARY_TEXT_COLOR = "#666666";
    // 新增：顶部栏固定高度（使右上角三个按钮与顶部栏上下边界重合）
    private static final double TOP_BAR_HEIGHT = 48.0;

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
        // 增加底部分割线
        topBar.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8,0,0,2);" +
                " -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1px 0;");
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
        Image iconLostCardAdmin = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/解挂挂失.png")));

        Button stuManageBtn = new Button();
        stuManageBtn.setPrefWidth(40);
        stuManageBtn.setPrefHeight(40);
        stuManageBtn.setMinWidth(40);
        stuManageBtn.setMinHeight(40);
        stuManageBtn.setMaxWidth(40);
        stuManageBtn.setMaxHeight(40);
        setSelectedButtonStyle(stuManageBtn);
        stuManageBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        stuManageBtn.setAlignment(Pos.CENTER);

        Button courseMgmtBtn = new Button();
        courseMgmtBtn.setPrefWidth(40);
        courseMgmtBtn.setPrefHeight(40);
        courseMgmtBtn.setMinWidth(40);
        courseMgmtBtn.setMinHeight(40);
        courseMgmtBtn.setMaxWidth(40);
        courseMgmtBtn.setMaxHeight(40);
        resetButtonStyle(courseMgmtBtn);
        courseMgmtBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        courseMgmtBtn.setAlignment(Pos.CENTER);

        Button timetableBtn = new Button();
        timetableBtn.setPrefWidth(40);
        timetableBtn.setPrefHeight(40);
        timetableBtn.setMinWidth(40);
        timetableBtn.setMinHeight(40);
        timetableBtn.setMaxWidth(40);
        timetableBtn.setMaxHeight(40);
        resetButtonStyle(timetableBtn);
        timetableBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        timetableBtn.setAlignment(Pos.CENTER);

        Button courseSelectBtn = new Button();
        courseSelectBtn.setPrefWidth(40);
        courseSelectBtn.setPrefHeight(40);
        courseSelectBtn.setMinWidth(40);
        courseSelectBtn.setMinHeight(40);
        courseSelectBtn.setMaxWidth(40);
        courseSelectBtn.setMaxHeight(40);
        resetButtonStyle(courseSelectBtn);
        courseSelectBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        courseSelectBtn.setAlignment(Pos.CENTER);

        Button myClassroomBtn = new Button();
        myClassroomBtn.setPrefWidth(40);
        myClassroomBtn.setPrefHeight(40);
        myClassroomBtn.setMinWidth(40);
        myClassroomBtn.setMinHeight(40);
        myClassroomBtn.setMaxWidth(40);
        myClassroomBtn.setMaxHeight(40);
        resetButtonStyle(myClassroomBtn);
        myClassroomBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        myClassroomBtn.setAlignment(Pos.CENTER);

        Button financeBtn = new Button();
        financeBtn.setPrefWidth(40);
        financeBtn.setPrefHeight(40);
        financeBtn.setMinWidth(40);
        financeBtn.setMinHeight(40);
        financeBtn.setMaxWidth(40);
        financeBtn.setMaxHeight(40);
        resetButtonStyle(financeBtn);
        financeBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        financeBtn.setAlignment(Pos.CENTER);

        Button storeBtn = new Button();
        storeBtn.setPrefWidth(40);
        storeBtn.setPrefHeight(40);
        storeBtn.setMinWidth(40);
        storeBtn.setMinHeight(40);
        storeBtn.setMaxWidth(40);
        storeBtn.setMaxHeight(40);
        resetButtonStyle(storeBtn);
        storeBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        storeBtn.setAlignment(Pos.CENTER);

        Button aiAssistBtn = new Button();
        aiAssistBtn.setPrefWidth(40);
        aiAssistBtn.setPrefHeight(40);
        aiAssistBtn.setMinWidth(40);
        aiAssistBtn.setMinHeight(40);
        aiAssistBtn.setMaxWidth(40);
        aiAssistBtn.setMaxHeight(40);
        resetButtonStyle(aiAssistBtn);
        aiAssistBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        aiAssistBtn.setAlignment(Pos.CENTER);

        // 新增：图书馆按钮
        Button libraryBtn = new Button();
        libraryBtn.setPrefWidth(40);
        libraryBtn.setPrefHeight(40);
        libraryBtn.setMinWidth(40);
        libraryBtn.setMinHeight(40);
        libraryBtn.setMaxWidth(40);
        libraryBtn.setMaxHeight(40);
        resetButtonStyle(libraryBtn);
        libraryBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        libraryBtn.setAlignment(Pos.CENTER);

        // 新增：一卡通挂失按钮
        Button reportLossBtn = new Button();
        reportLossBtn.setPrefWidth(40);
        reportLossBtn.setPrefHeight(40);
        reportLossBtn.setMinWidth(40);
        reportLossBtn.setMinHeight(40);
        reportLossBtn.setMaxWidth(40);
        reportLossBtn.setMaxHeight(40);
        resetButtonStyle(reportLossBtn);
        reportLossBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        reportLossBtn.setAlignment(Pos.CENTER);
        Image iconReportLoss = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/挂失.png")));
        attachIconAndRememberText(reportLossBtn, iconReportLoss);
        setRightTooltip(reportLossBtn, "一卡通挂失");

        // 新增：挂失管理按钮
        Button lostCardAdminBtn = new Button();
        lostCardAdminBtn.setPrefWidth(40);
        lostCardAdminBtn.setPrefHeight(40);
        lostCardAdminBtn.setMinWidth(40);
        lostCardAdminBtn.setMinHeight(40);
        lostCardAdminBtn.setMaxWidth(40);
        lostCardAdminBtn.setMaxHeight(40);
        resetButtonStyle(lostCardAdminBtn);
        lostCardAdminBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        lostCardAdminBtn.setAlignment(Pos.CENTER);
        attachIconAndRememberText(lostCardAdminBtn, iconLostCardAdmin);
        setRightTooltip(lostCardAdminBtn, "挂失管理");


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
        navButtons.add(lostCardAdminBtn);
        navButtons.add(reportLossBtn); // 新增：一卡通挂失按钮
        navButtons.add(lostCardAdminBtn); // 新增：挂失管理按钮

        // 为每个按钮添加图标与保存原文案
        attachIconAndRememberText(stuManageBtn, iconStudent);
        attachIconAndRememberText(courseMgmtBtn, iconCourseMgmt);
        attachIconAndRememberText(timetableBtn, iconTimetable);
        attachIconAndRememberText(courseSelectBtn, iconCourseSelect);
        attachIconAndRememberText(myClassroomBtn, iconClassroom);
        attachIconAndRememberText(financeBtn, iconFinance);
        attachIconAndRememberText(storeBtn, iconStore);
        attachIconAndRememberText(aiAssistBtn, iconAI);
        attachIconAndRememberText(libraryBtn, iconLibrary); // 新增：图书馆图标

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

        // 中心内容容器（StackPane，便于后续叠加遮罩/弹层）
        centerContainer = new StackPane();
        // 增加顶部分割线和左侧分割线
        centerContainer.setStyle("-fx-background-color: #F6F8FA; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04),12,0,0,2);"
                + " -fx-border-color: #e2e8f0; -fx-border-width: 1px 0 0 1px;");
        StackPane.setMargin(centerContainer, new Insets(0));
        mainLayout.setCenter(centerContainer);

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
                leftBar.getChildren().add(lostCardAdminBtn); // 新增：挂失管理
            }
            if ("student".equals(userType)) {
                leftBar.getChildren().addAll(timetableBtn, courseSelectBtn);
            }
            if ("teacher".equals(userType)) {
                leftBar.getChildren().add(myClassroomBtn);
            }
            // 三类角色统一添加交易管理
            if ("student".equals(userType) || "admin".equals(userType) || "teacher".equals(userType)) {
                leftBar.getChildren().add(financeBtn);
                leftBar.getChildren().add(storeBtn); // 新增：校园商店
            }
            // AI 助手按钮添加
            leftBar.getChildren().add(aiAssistBtn);
            // 新增：图书馆按钮添加（对所有用户类型开放）
            leftBar.getChildren().add(libraryBtn);


            // 初次默认选中
            if ("student".equals(userType) || "admin".equals(userType)) {
                currentSelectedButton = stuManageBtn;
            } else if ("teacher".equals(userType)) {
                currentSelectedButton = myClassroomBtn;
                setSelectedButtonStyle(myClassroomBtn);
            }

            stuManageBtn.setOnAction(e -> {
                if (currentSelectedButton == stuManageBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(stuManageBtn);
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
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(courseMgmtBtn);
                currentSelectedButton = courseMgmtBtn;
                setCenterContent(new CourseAdminPanel());
            });

            // 学生-我的课表
            timetableBtn.setOnAction(e -> {
                if (currentSelectedButton == timetableBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(timetableBtn);
                currentSelectedButton = timetableBtn;
                setCenterContent(new TimetablePanel(cardNumber));
            });

            // 学生-选课
            courseSelectBtn.setOnAction(e -> {
                if (currentSelectedButton == courseSelectBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(courseSelectBtn);
                currentSelectedButton = courseSelectBtn;
                setCenterContent(new CourseSelectPanel(Integer.valueOf(cardNumber)));
            });

            // 教师-我的课堂
            myClassroomBtn.setOnAction(e -> {
                if (currentSelectedButton == myClassroomBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(myClassroomBtn);
                currentSelectedButton = myClassroomBtn;
                setCenterContent(new MyClassroomPanel(cardNumber));
            });

            // 新增：交易管理
            financeBtn.setOnAction(e -> {
                if (currentSelectedButton == financeBtn) return;
                if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(financeBtn);
                currentSelectedButton = financeBtn;
                setCenterContent(new FinancePanel(cardNumber, userType));
            });

            // 新增：校园商店事件
            storeBtn.setOnAction(e -> {
                if (currentSelectedButton == storeBtn) return;
                if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(storeBtn);
                currentSelectedButton = storeBtn;
                setCenterContent(new StoreMainPanel(cardNumber, userType));
            });

            // 新增：AI 助手
            aiAssistBtn.setOnAction(e -> {
                if (currentSelectedButton == aiAssistBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(aiAssistBtn);
                currentSelectedButton = aiAssistBtn;
                setCenterContent(new AIChatPanel(cardNumber));
            });

            // 新增：图书馆事件
            libraryBtn.setOnAction(e -> {
                if (currentSelectedButton == libraryBtn) {
                    return;
                }
                if (currentSelectedButton != null) {
                    resetButtonStyle(currentSelectedButton);
                }
                setSelectedButtonStyle(libraryBtn);
                currentSelectedButton = libraryBtn;
                // 创建图书馆主面板并显示在右侧，传递一卡通号
                setCenterContent(new LibraryMainPanel(cardNumber));
            });

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
                                info.setContentText(resp);
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

            // 管理员-挂失管理
            lostCardAdminBtn.setOnAction(e -> {
                if (currentSelectedButton == lostCardAdminBtn) return;
                if (currentSelectedButton != null) resetButtonStyle(currentSelectedButton);
                setSelectedButtonStyle(lostCardAdminBtn);
                currentSelectedButton = lostCardAdminBtn;
                setCenterContent(new Client.finance.LostCardAdminPanel());
            });

            // ===== 修改：使用透明 Region 占据剩余垂直空间（去除占位文字与背景） =====
            Region functionSpacer = new Region();
            VBox.setVgrow(functionSpacer, Priority.ALWAYS);
            leftBar.getChildren().add(functionSpacer);

            // 新增：左下角"修改��码"按钮
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
            ImageView pwdView = new ImageView(pwdIcon);
            pwdView.setFitWidth(28);
            pwdView.setFitHeight(28);
            pwdView.setPreserveRatio(true);
            changePwdSidebarBtn.setGraphic(pwdView);
            // 样式与功能区按钮一致
            changePwdSidebarBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";");
            // 悬停阴影
            changePwdSidebarBtn.setOnMouseEntered(e -> setButtonHoverShadow(changePwdSidebarBtn));
            changePwdSidebarBtn.setOnMouseExited(e -> changePwdSidebarBtn.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-effect: none; -fx-font-size: 15px; -fx-text-fill: " + TEXT_COLOR + ";"));
            // 事件
            changePwdSidebarBtn.setOnAction(e -> new LoginClientFX().openAsRecovery(stage, cardNumber));
            // Tooltip
            setRightTooltip(changePwdSidebarBtn, "修改密码");
            // 新增：一卡通挂失按钮添加
            leftBar.getChildren().add(reportLossBtn);

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
            ImageView logoutView = new ImageView(logoutIcon);
            logoutView.setFitWidth(28);
            logoutView.setFitHeight(28);
            logoutView.setPreserveRatio(true);
            logoutSidebarBtn.setGraphic(logoutView);
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
            Label noFunctionTitle = new Label("欢迎使用智慧���园系统");
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
        bar.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8,0,0,2);");

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
        userInfo.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        // 左侧组合容器，提供水平内边距
        HBox leftGroup = new HBox(6);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        leftGroup.setPadding(new Insets(0, 6, 0, 6));


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
        minBtn.setOnMouseEntered(e -> minBtn.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 0;"));
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
                boolean toMax = !stage.isMaximized();
                stage.setMaximized(toMax);
                // 切换图标
                if (toMax) {
                    maxView.setImage(imgRestore);
                } else {
                    maxView.setImage(imgMax);
                }
            }
        });
        // 悬停变灰色
        maxBtn.setOnMouseEntered(e -> maxBtn.setStyle("-fx-background-color: #e2e8f0; -fx-background-radius: 0;"));
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
        btn.setGraphic(iv);
        btn.setGraphicTextGap(8);
    }

    private void setSelectedButtonStyle(Button button) {
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: dropshadow(gaussian, #1E1F22, 10, 0, 0, 2);" +
                        "-fx-font-size: 15px; -fx-font-weight: bold; " +
                        "-fx-text-fill: " + PRIMARY_COLOR + ";"
        );
    }

    private void resetButtonStyle(Button button) {
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: none;" +
                        "-fx-font-size: 15px; " +
                        "-fx-text-fill: " + TEXT_COLOR + ";"
        );
    }

    private void setButtonHoverShadow(Button button) {
        button.setStyle(
                "-fx-background-color: " + SIDEBAR_COLOR + ";" +
                        "-fx-effect: dropshadow(gaussian, #1E1F22, 10, 0, 0, 2);" + // 光效颜色与悬停阴影一致
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
        // 默认选中按钮也展示悬停阴影
        if (currentSelectedButton != null) {
            setSelectedButtonStyle(currentSelectedButton);
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

