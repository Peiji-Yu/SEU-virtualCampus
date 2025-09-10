package Client;

import Client.login.component.LogoutHandler;
import Client.login.LoginClientFX;
import Client.studentmgmt.admin.StudentAdminPanel;
import Client.studentmgmt.student.StudentSelfPanel;
import Client.timetable.TimetablePanel;
import Client.courseselect.CourseSelectPanel;
import Client.teacherclass.MyClassroomPanel;
import Client.coursemgmt.admin.CourseAdminPanel;
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
    private static final String SIDEBAR_COLOR = "#e8f2ff";
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

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // 顶部用户栏
        HBox topBar = buildTopBar();
        root.setTop(topBar);

        // 左侧导航栏
        VBox leftBar = new VBox(18);
        leftBar.setPadding(new Insets(15));
        leftBar.setAlignment(Pos.TOP_CENTER);
        leftBar.setStyle("-fx-background-color: " + SIDEBAR_COLOR + "; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // 统一图标资源
        Image navIcon = new Image(Objects.requireNonNull(MainFrame.class.getResourceAsStream("/Image/Logo.png")));

        Button stuManageBtn = new Button("学籍管理");
        stuManageBtn.setPrefWidth(130);
        stuManageBtn.setPrefHeight(45);
        setSelectedButtonStyle(stuManageBtn);

        // 管理员-课程管理按钮
        Button courseMgmtBtn = new Button("课程管理");
        courseMgmtBtn.setPrefWidth(130);
        courseMgmtBtn.setPrefHeight(45);
        resetButtonStyle(courseMgmtBtn);

        // 学生-我的课表按钮
        Button timetableBtn = new Button("我的课表");
        timetableBtn.setPrefWidth(130);
        timetableBtn.setPrefHeight(45);
        resetButtonStyle(timetableBtn);

        // 学生-选课按钮
        Button courseSelectBtn = new Button("选课");
        courseSelectBtn.setPrefWidth(130);
        courseSelectBtn.setPrefHeight(45);
        resetButtonStyle(courseSelectBtn);

        // 教师-我的课堂按钮
        Button myClassroomBtn = new Button("我的课堂");
        myClassroomBtn.setPrefWidth(130);
        myClassroomBtn.setPrefHeight(45);
        resetButtonStyle(myClassroomBtn);

        // 收集所有需要随折叠切换文字的按钮（不再包含退出登录）
        List<Button> navButtons = new ArrayList<>();
        navButtons.add(stuManageBtn);
        navButtons.add(courseMgmtBtn);
        navButtons.add(timetableBtn);
        navButtons.add(courseSelectBtn);
        navButtons.add(myClassroomBtn);

        // 为每个按钮添加图标与保存原文案
        attachIconAndRememberText(stuManageBtn, navIcon);
        attachIconAndRememberText(courseMgmtBtn, navIcon);
        attachIconAndRememberText(timetableBtn, navIcon);
        attachIconAndRememberText(courseSelectBtn, navIcon);
        attachIconAndRememberText(myClassroomBtn, navIcon);

        // 右侧内容初始载入
        if ("student".equals(userType)) {
            root.setCenter(new StudentSelfPanel(cardNumber));
        } else if ("admin".equals(userType)) {
            root.setCenter(new StudentAdminPanel());
        } else if ("teacher".equals(userType)) {
            root.setCenter(new MyClassroomPanel(cardNumber));
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
                    root.setCenter(new StudentSelfPanel(cardNumber));
                } else {
                    root.setCenter(new StudentAdminPanel());
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
                root.setCenter(new CourseAdminPanel());
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
                root.setCenter(new TimetablePanel(cardNumber));
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
                root.setCenter(new CourseSelectPanel(cardNumber));
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
                root.setCenter(new MyClassroomPanel(cardNumber));
            });
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
            root.setCenter(noFunctionBox);
        }

        // ---- 折叠侧边栏容器与动画 ----
        StackPane sidebarContainer = new StackPane(leftBar);final DoubleProperty sidebarWidth = new SimpleDoubleProperty(SIDEBAR_COLLAPSED_WIDTH); // 默认收起
        sidebarContainer.minWidthProperty().bind(sidebarWidth);
        sidebarContainer.prefWidthProperty().bind(sidebarWidth);
        sidebarContainer.maxWidthProperty().bind(sidebarWidth);
        // 内容裁剪，避免收起时外溢
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarWidth);
        clip.heightProperty().bind(root.heightProperty());
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

        root.setLeft(sidebarContainer);

        Scene scene = new Scene(root, 1500, 780);
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
        bar.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8,0,0,2);");

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
                "-fx-border-color: " + PRIMARY_COLOR + "; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-background-color: " + BACKGROUND_COLOR + "; -fx-text-fill: " + PRIMARY_COLOR + "; " +
                "-fx-effect: dropshadow(gaussian, rgba(78,140,255,0.3), 8, 0, 0, 2);");
    }

    private void resetButtonStyle(Button button) {
        button.setStyle("-fx-font-size: 15px; -fx-background-radius: 10; " +
                "-fx-background-color: white; -fx-text-fill: " + TEXT_COLOR + "; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 1);");
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
}
