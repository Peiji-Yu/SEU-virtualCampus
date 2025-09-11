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
import Client.store.StorePanel; // 新增：校园商店面板
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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
    private static final String SIDEBAR_COLOR = "#6EFF7E";
    private static final String TEXT_COLOR = "#2a4d7b";
    private static final String SECONDARY_TEXT_COLOR = "#666666";

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

    public MainFrame(String cardNumber) {
        this.cardNumber = cardNumber;
        this.userType = getUserType(cardNumber);
    }

    /**
     * 显示主界面窗口。
     */
    public void show() {
        stage = new Stage();
        // 设置窗口图标
        try {
            var url = MainFrame.class.getResource("/Image/Logo.png");
            if (url != null) { stage.getIcons().add(new Image(url.toExternalForm())); }
        } catch (Exception ignore) {}
        stage.setTitle("智慧校园");
        // 调整窗口尺寸以适应更多列
        stage.setMinWidth(1300);
        stage.setMinHeight(750);

        // ===== 最外层根容器（铺满窗口） =====
        StackPane rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color: linear-gradient(to bottom right,#f1f6ff,#ffffff);");

        // 主功能面板：内部使用 BorderPane 分区（顶部、左侧功能栏、中心内容区）
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPrefSize(1300, 750);
        AnchorPane anchorWrapper = new AnchorPane(mainLayout);
        AnchorPane.setTopAnchor(mainLayout, 0.0);
        AnchorPane.setBottomAnchor(mainLayout, 0.0);
        AnchorPane.setLeftAnchor(mainLayout, 0.0);
        AnchorPane.setRightAnchor(mainLayout, 0.0);
        rootStack.getChildren().add(anchorWrapper);

        // 顶部用户栏容器
        HBox topBar = buildTopBar();
        VBox topContainer = new VBox(topBar); // 后续如需加入二级工具条可追加
        topContainer.setFillWidth(true);
        mainLayout.setTop(topContainer);

        // 左侧导航栏（原 leftBar 与折叠容器 sidebarContainer 将被加入功能栏容器）
        VBox leftBar = new VBox(18);
        leftBar.setPadding(new Insets(15));
        leftBar.setAlignment(Pos.TOP_CENTER);
        // 修改：去除左上圆角，仅保留左下外侧圆角
        leftBar.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-background-radius: 0 0 0 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        leftBar.setPrefHeight(Double.MAX_VALUE);
        leftBar.setMaxHeight(Double.MAX_VALUE);
        leftBar.setFillWidth(true);

        // 统一图标资源（分别加载各自功能图标）
        Image iconStudent = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/学生.png")));
        Image iconCourseMgmt = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/课程.png")));
        Image iconFinance = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/交易记录.png")));
        Image iconStore = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/商店.png")));
        Image iconAI = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/deepseek-copy.png")));
        Image iconClassroom = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/课堂.png")));
        Image iconTimetable = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/表格.png")));
        Image iconCourseSelect = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/functionbar/选课.png")));

        Button stuManageBtn = new Button("学籍管理");
        stuManageBtn.setPrefWidth(130);
        stuManageBtn.setMaxWidth(Double.MAX_VALUE);
        stuManageBtn.setPrefHeight(45);
        setSelectedButtonStyle(stuManageBtn);

        // 管理员-课程管理按钮
        Button courseMgmtBtn = new Button("课程管理");
        courseMgmtBtn.setPrefWidth(130);
        courseMgmtBtn.setMaxWidth(Double.MAX_VALUE);
        courseMgmtBtn.setPrefHeight(45);
        resetButtonStyle(courseMgmtBtn);

        // 学生-我的课表按钮
        Button timetableBtn = new Button("我的课表");
        timetableBtn.setPrefWidth(130);
        timetableBtn.setMaxWidth(Double.MAX_VALUE);
        timetableBtn.setPrefHeight(45);
        resetButtonStyle(timetableBtn);

        // 学生-选课按钮
        Button courseSelectBtn = new Button("选课");
        courseSelectBtn.setPrefWidth(130);
        courseSelectBtn.setMaxWidth(Double.MAX_VALUE);
        courseSelectBtn.setPrefHeight(45);
        resetButtonStyle(courseSelectBtn);

        // 教师-我的课堂按钮
        Button myClassroomBtn = new Button("我的课堂");
        myClassroomBtn.setPrefWidth(130);
        myClassroomBtn.setMaxWidth(Double.MAX_VALUE);
        myClassroomBtn.setPrefHeight(45);
        resetButtonStyle(myClassroomBtn);

        // 新增：交易管理按钮（所有已知三类角色可见）
        Button financeBtn = new Button("交易管理");
        financeBtn.setPrefWidth(130);
        financeBtn.setMaxWidth(Double.MAX_VALUE);
        financeBtn.setPrefHeight(45);
        resetButtonStyle(financeBtn);

        // 新增：校园商店按钮
        Button storeBtn = new Button("校园商店");
        storeBtn.setPrefWidth(130);
        storeBtn.setMaxWidth(Double.MAX_VALUE);
        storeBtn.setPrefHeight(45);
        resetButtonStyle(storeBtn);

        // 新增：AI 助手按钮（所有角色显示）
        Button aiAssistBtn = new Button("AI助手");
        aiAssistBtn.setPrefWidth(130);
        aiAssistBtn.setMaxWidth(Double.MAX_VALUE);
        aiAssistBtn.setPrefHeight(45);
        resetButtonStyle(aiAssistBtn);

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

        // 为每个按钮添加图标与保存原文案
        attachIconAndRememberText(stuManageBtn, iconStudent);
        attachIconAndRememberText(courseMgmtBtn, iconCourseMgmt);
        attachIconAndRememberText(timetableBtn, iconTimetable);
        attachIconAndRememberText(courseSelectBtn, iconCourseSelect);
        attachIconAndRememberText(myClassroomBtn, iconClassroom);
        attachIconAndRememberText(financeBtn, iconFinance);
        attachIconAndRememberText(storeBtn, iconStore);
        attachIconAndRememberText(aiAssistBtn, iconAI);

        // 中心内容容器（StackPane，便于后续叠加遮罩/弹层）
        centerContainer = new StackPane();
        // 修改：去除与顶部栏、侧边栏相交的圆角，仅保留右下外侧圆角
        centerContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 0 0 12 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04),12,0,0,2);");
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

        // 学生与管理员显示“学籍管理”按钮；学生显示“我的课表”“选课”；教师显示“我的课堂”；管理员显示“课程管理”
        if ("student".equals(userType) || "admin".equals(userType) || "teacher".equals(userType)) {
            if ("student".equals(userType) || "admin".equals(userType)) {
                leftBar.getChildren().add(stuManageBtn);
            }
            if ("admin".equals(userType)) {
                leftBar.getChildren().add(courseMgmtBtn);
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
                setCenterContent(new CourseSelectPanel(cardNumber));
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
                setCenterContent(new StorePanel(cardNumber, userType));
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

            // ===== 修改：使用透明 Region 占据剩余垂直空间（去除占位文字与背景） =====
            Region functionSpacer = new Region();
            VBox.setVgrow(functionSpacer, Priority.ALWAYS);
            leftBar.getChildren().add(functionSpacer);
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

        // ---- 折叠侧边栏容器与动画 ----
        StackPane sidebarContainer = new StackPane(leftBar);final DoubleProperty sidebarWidth = new SimpleDoubleProperty(SIDEBAR_COLLAPSED_WIDTH); // 默认收起
        sidebarContainer.minWidthProperty().bind(sidebarWidth);
        sidebarContainer.prefWidthProperty().bind(sidebarWidth);
        sidebarContainer.maxWidthProperty().bind(sidebarWidth);
        // 让侧栏容器占满 BorderPane 的垂直空间，从而 leftBar 能填满左侧剩余高度
        sidebarContainer.prefHeightProperty().bind(mainLayout.heightProperty());
        sidebarContainer.maxHeightProperty().bind(mainLayout.heightProperty());
        // leftBar 高度跟随容器，保证不出现空隙
        leftBar.prefHeightProperty().bind(sidebarContainer.heightProperty());
        // 内容裁剪，避免收起时外溢
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarWidth);
        clip.heightProperty().bind(sidebarContainer.heightProperty());
        sidebarContainer.setClip(clip);

        final Timeline[] currentAnim = new Timeline[1];
        Runnable expand = () -> {
            if (currentAnim[0] != null) currentAnim[0].stop();
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(200), new KeyValue(sidebarWidth, SIDEBAR_EXPANDED_WIDTH, Interpolator.EASE_BOTH))
            );
            tl.setOnFinished(e -> applySidebarText(navButtons, true));
            currentAnim[0] = tl; tl.play();
        };
        Runnable collapse = () -> {
            if (currentAnim[0] != null) currentAnim[0].stop();
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(200), new KeyValue(sidebarWidth, SIDEBAR_COLLAPSED_WIDTH, Interpolator.EASE_BOTH))
            );
            tl.setOnFinished(e -> applySidebarText(navButtons, false));
            currentAnim[0] = tl; tl.play();
        };
        // 移出后延时收起，避免误触
        final PauseTransition hideDelay = new PauseTransition(Duration.millis(600));
        hideDelay.setOnFinished(e -> collapse.run());

        sidebarContainer.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            hideDelay.stop();
            expand.run();
        });
        sidebarContainer.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            // 若鼠标离开侧边栏，延时收起
            hideDelay.playFromStart();
        });

        mainLayout.setLeft(sidebarContainer);

        Scene scene = new Scene(rootStack, 1500, 780);
        // 场景左缘热区：鼠标靠近左侧时自动展开
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (e.getSceneX() <= 6 && sidebarWidth.get() <= SIDEBAR_COLLAPSED_WIDTH + 0.5) {
                hideDelay.stop();
                expand.run();
            }
        });

        stage.setScene(scene);
        stage.show();

        // 初次进入为收起状态，隐藏文字；靠近左缘或移入侧栏展开
        applySidebarText(navButtons, false);
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        // 修改：仅保留顶部外侧两个圆角，上边圆角，下边相接为 0
        bar.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12 12 0 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8,0,0,2);");

        String roleCN;
        switch (userType) {
            case "student": roleCN = "学生"; break;
            case "teacher": roleCN = "教师"; break;
            case "admin": roleCN = "管理员"; break;
            default: roleCN = "未知";
        }
        // 显示为“角色 卡号”，不加“用户：”与句号
        Label userInfo = new Label(roleCN + " " + cardNumber);
        userInfo.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button changePwdBtn = new Button("修改密码");
        changePwdBtn.setPrefHeight(34);
        changePwdBtn.setStyle("-fx-font-size: 13px; -fx-background-radius: 8; -fx-background-color: #4e8cff; -fx-text-fill: white;");
        changePwdBtn.setOnAction(e -> new LoginClientFX().openAsRecovery(stage, cardNumber));

        Button logoutTopBtn = new Button("退出登录");
        logoutTopBtn.setPrefHeight(34);
        setDangerButtonStyle(logoutTopBtn);
        logoutTopBtn.setOnMouseEntered(e -> setDangerButtonHoverStyle(logoutTopBtn));
        logoutTopBtn.setOnMouseExited(e -> setDangerButtonStyle(logoutTopBtn));
        logoutTopBtn.setOnAction(e -> LogoutHandler.handleLogout(stage));

        bar.getChildren().addAll(userInfo, spacer, changePwdBtn, logoutTopBtn);
        return bar;
    }


    private void attachIconAndRememberText(Button btn, Image icon){
        if (btn.getUserData() == null) {
            btn.setUserData(btn.getText());
        }
        ImageView iv = new ImageView(icon);
        iv.setFitWidth(18);
        iv.setFitHeight(18);
        iv.setPreserveRatio(true);
        btn.setGraphic(iv);
        btn.setGraphicTextGap(8);
    }

    private void applySidebarText(List<Button> buttons, boolean expanded){
        for (Button b : buttons) {
            Object ud = b.getUserData();
            String original = ud instanceof String ? (String) ud : b.getText();
            b.setText(expanded ? original : "");
            if (expanded) {
                b.setPrefWidth(130);
                b.setPrefHeight(45);
            } else {
                b.setPrefWidth(45);
                b.setPrefHeight(45);
                b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY); // 收起时只显示图标
                b.setAlignment(Pos.CENTER); // 图标居中
            }
            if (expanded) {
                b.setContentDisplay(ContentDisplay.LEFT); // 展开时图标在左
                b.setAlignment(Pos.CENTER_LEFT);
            }
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

    private void setSelectedButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-text-fill: " + PRIMARY_COLOR + ";");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-background-radius: 10; " +
                "-fx-text-fill: " + TEXT_COLOR + ";");
    }

    private void setDangerButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                "-fx-background-color: " + DANGER_COLOR + "; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,107,107,0.3), 8, 0, 0, 2);");
    }

    private void setDangerButtonHoverStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 10; " +
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
        StackPane.setAlignment(node, Pos.CENTER);
    }
}
