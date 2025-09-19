package Client.panel.login;

import Client.MainFrame;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import Client.panel.login.component.*;
import Client.panel.login.util.*;
import Client.util.AsyncFX;
import Client.panel.login.net.RequestSender;
import javafx.stage.Modality; // 添加缺失导入
import Client.util.UIUtil;

import java.lang.reflect.Type;
import java.util.Map;

/** 登录界面（固定尺寸与显式参数布局版本）
 *  @author Msgo-srAm
 */
public class LoginClientFX extends Application {
    // ================= 可调参数（仅修改这些常量即可微调布局） =================
    private static final double ROOT_WIDTH = 375;
    private static final double ROOT_HEIGHT = 575;

    private static final double LOGO_SIZE = 64;
    private static final double LOGO_TOP = 60;

    private static final double WELCOME_TOP = 176;
    private static final double WELCOME_EXTRA_TRANSLATE = 0;

    private static final double FORM_TOP = 306;
    private static final double SUBTITLE_OFFSET = 38;

    // 进度条位置（加载动画显示时）
    private static final double PROGRESS_TOP = 291;
    private static final double PROGRESS_LEFT = 112;

    // ========================================================================

    private final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String,Object>>(){}.getType();

    private UsernameInput cardInput;
    private PasswordInput passwordInput;
    private LoginButton loginButton;
    private Label statusLabel;
    private Label forgetLabel;
    private ProgressBar progressBar;
    private AnchorPane root;
    private AnchorPane formContainer;
    private ImageView logo;
    private Label welcomeLabel;
    private Label subtitleLabel;
    private javafx.scene.layout.HBox authButtonsRow;
    private FilledButton nextButton;
    private FilledButton cancelButton;
    // 外部找回密码模式支持
    private boolean externalRecoveryMode = false;
    private Stage externalStageRef;
    private String externalCardNumber; // 新增：外部传入的一卡通号
    private boolean inAuthMode = false;
    private boolean inNewPasswordMode = false;
    private String currentCardNumber;
    private boolean authRequestInProgress = false;
    private boolean passwordResetInProgress = false;
    private Rectangle focusReq;

    private static final double LOGIN_BUTTON_WIDTH = 250;
    private static final double AUTH_BUTTON_GAP = 10;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
        UIUtil.applyLogoToStage(stage);
        // 外部模式下改为模态窗口（保留透明样式）
        if (externalRecoveryMode && externalStageRef != null) {
            // 修复：应将 owner 设为 externalStageRef 本身，而不是其 owner
            stage.initOwner(externalStageRef);
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        root = new AnchorPane();
        root.setStyle("-fx-background-radius:10px;-fx-background-color:#ffffff; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),25,0,0,4);");
        Scene scene = new Scene(root, ROOT_WIDTH, ROOT_HEIGHT);
        scene.setFill(Color.TRANSPARENT);

        Header header = new Header(24, (int)ROOT_WIDTH);
        header.setTransparent(true);
        header.setDragHandler(new DragHandler(stage));
        root.getChildren().add(header);

        ImageView bg = loadImage("/Image/BGI.png");
        if (bg != null) {
            bg.setFitWidth(ROOT_WIDTH); bg.setFitHeight(ROOT_HEIGHT); bg.setOpacity(0.8);
            root.getChildren().add(0, bg);
        }

        logo = loadImage("/Image/Logo.png");
        if (logo != null) {
            logo.setFitWidth(LOGO_SIZE);
            logo.setFitHeight(LOGO_SIZE);
            logo.setPreserveRatio(true);
            root.getChildren().add(logo);
        }

        welcomeLabel = new Label("Welcome,");
        welcomeLabel.setFont(Resources.ROBOTO_BOLD_LARGE);
        welcomeLabel.setTextFill(Resources.FONT_COLOR);
        root.getChildren().add(welcomeLabel);

        subtitleLabel = new Label("SEUer!");
        subtitleLabel.setFont(Resources.ROBOTO_BOLD);
        subtitleLabel.setTextFill(Resources.FONT_COLOR);
        root.getChildren().add(subtitleLabel);

        formContainer = new AnchorPane();
        VBox formBox = new VBox(); formBox.setSpacing(34);
        cardInput = new UsernameInput("Card Number", true);
        passwordInput = new PasswordInput("Password");
        VBox inputGroup = new VBox(cardInput, passwordInput); inputGroup.setSpacing(23);
        forgetLabel = new Label("                                                     Forget Password?");
        forgetLabel.setFont(Resources.ROBOTO_LIGHT); forgetLabel.setTextFill(Resources.DISABLED);
        forgetLabel.setOnMouseEntered(e -> new ColorTransition(forgetLabel, Duration.seconds(0.2), Resources.SECONDARY).play());
        forgetLabel.setOnMouseExited(e -> new ColorTransition(forgetLabel, Duration.seconds(0.2), Resources.DISABLED).play());
        // 移除旧的模态对话框调用，这里不绑定任何事件，在bindEvents中统一绑定
        VBox inputArea = new VBox(inputGroup, forgetLabel); inputArea.setSpacing(5);
        loginButton = new LoginButton();
        statusLabel = new Label(" "); statusLabel.setFont(Resources.ROBOTO_LIGHT); statusLabel.setTextFill(Resources.DISABLED);
        // 将 block 改为局部变量（仅用于组装布局），不保留为字段
        VBox block = new VBox(inputArea, loginButton, statusLabel); block.setSpacing(16);
        // 创建身份认证模式按钮区（默认隐藏，插入到 loginButton 的位置）
        authButtonsRow = buildAuthButtonsRow();
        authButtonsRow.setVisible(false); authButtonsRow.setManaged(false);
        block.getChildren().add(1, authButtonsRow);
        formBox.getChildren().add(block);
        formContainer.getChildren().add(formBox);
        AnchorPane.setLeftAnchor(formBox, 63.0);
        root.getChildren().add(formContainer);

        progressBar = new ProgressBar();
        var css = getClass().getResource("/Css/ProgressBar.css");
        if (css != null) { progressBar.getStylesheets().add(css.toExternalForm()); }
        progressBar.setOpacity(0);

        // 将透明矩形作为字段，便于在任意阶段请求焦点以“失焦”输入框
        focusReq = new Rectangle(ROOT_WIDTH, ROOT_HEIGHT, Color.TRANSPARENT);
        focusReq.setFocusTraversable(true);
        focusReq.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> focusReq.requestFocus());
        root.getChildren().add(0, focusReq);

        bindEvents();

        stage.setScene(scene);
        stage.show();

        // 固定布局一次（延迟确保文本宽度可得）
        Platform.runLater(this::applyFixedLayout);
        // 外部找回模式：默认进入认证流程
        if (externalRecoveryMode) {
            Platform.runLater(this::enterAuthMode);
        }
    }

    private javafx.scene.layout.HBox buildAuthButtonsRow(){
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.setSpacing(AUTH_BUTTON_GAP);
        row.setMinWidth(LOGIN_BUTTON_WIDTH);
        double w = (LOGIN_BUTTON_WIDTH - AUTH_BUTTON_GAP) / 2.0;
        cancelButton = new FilledButton("Cancel", w, Resources.DISABLED, Resources.DISABLED, Resources.WHITE);
        nextButton = new FilledButton("Next", w, Resources.PRIMARY, Resources.SECONDARY, Resources.WHITE);
        // 在外部模式下，Cancel 关闭窗口；否则返回登录态
        if (externalRecoveryMode) {
            cancelButton.setOnAction(() -> {
                Stage s = (Stage) root.getScene().getWindow();
                s.close();
            });
        } else {
            cancelButton.setOnAction(this::exitAuthMode);
        }
        nextButton.setOnAction(this::performAuth);
        row.getChildren().addAll(cancelButton, nextButton);
        return row;
    }

    /** 固定布局：使用显式常量 */
    private void applyFixedLayout() {
        if (logo != null) {
            logo.setLayoutY(LOGO_TOP);
            logo.setLayoutX((ROOT_WIDTH - LOGO_SIZE) / 2.0);
        }
        if (welcomeLabel != null) {
            double w = welcomeLabel.prefWidth(-1);
            welcomeLabel.setLayoutY(WELCOME_TOP + WELCOME_EXTRA_TRANSLATE);
            welcomeLabel.setLayoutX((ROOT_WIDTH - w) / 2.0);
        }
        if (subtitleLabel != null) {
            double sw = subtitleLabel.prefWidth(-1);
            // 基于主标题位置 + 偏移
            subtitleLabel.setLayoutY(WELCOME_TOP + WELCOME_EXTRA_TRANSLATE + SUBTITLE_OFFSET);
            subtitleLabel.setLayoutX((ROOT_WIDTH - sw) / 2.0);
        }
        if (formContainer != null) {
            formContainer.setLayoutY(FORM_TOP);
            formContainer.setLayoutX(0);
        }
    }

    private void bindEvents() {
        Runnable loginAction = this::performLogin;
        loginButton.setOnLogin(loginAction);
        cardInput.setOnAction(loginAction);
        passwordInput.setOnAction(loginAction);
        // 直接绑定到内联身份认证模式
        forgetLabel.setOnMouseClicked(e -> enterAuthMode());
    }

    // 统一移除当前焦点，避免文本框处于选中/焦点态
    private void blurFocus(){ Platform.runLater(() -> { if (focusReq != null) { focusReq.requestFocus(); } }); }

    private void enterAuthMode(){
        if(inAuthMode) { return; }
        inAuthMode = true;
        welcomeLabel.setText("Authentication");
        // 使用系统字体以确保中文可见
        welcomeLabel.setFont(Resources.ROBOTO_BOLD_LARGE);
        subtitleLabel.setVisible(false);
        // 清空/预填输入并切换占位
        if (externalRecoveryMode && externalCardNumber != null && !externalCardNumber.isEmpty()) {
            cardInput.setText(externalCardNumber);
            cardInput.setEditable(false);
        } else {
            cardInput.clear();
            cardInput.setEditable(true);
        }
        passwordInput.clear();
        passwordInput.getPlaceHolder().setText("ID Number");
        // 隐藏 Forget Password 按钮
        forgetLabel.setVisible(false);
        forgetLabel.setManaged(false);
        // 计算按钮宽度，保证与其上的文本框左右对齐
        double fieldWidth = passwordInput.getBackgroundShape().getWidth();
        double each = (fieldWidth - AUTH_BUTTON_GAP) / 2.0;
        if (cancelButton != null) { cancelButton.setButtonWidth(each); }
        if (nextButton != null) { nextButton.setButtonWidth(each); }
        if (authButtonsRow != null) { authButtonsRow.setMinWidth(fieldWidth); authButtonsRow.setMaxWidth(fieldWidth); }
        // 切换按钮行
        loginButton.setVisible(false); loginButton.setManaged(false);
        authButtonsRow.setVisible(true); authButtonsRow.setManaged(true);
        statusLabel.setText(" "); statusLabel.setTextFill(Resources.DISABLED);
        // 回车触发 Next
        cardInput.setOnAction(this::performAuth);
        passwordInput.setOnAction(this::performAuth);
        // 重新居中标题
        Platform.runLater(this::applyFixedLayout);
        // 进入认证环节后移除任何输入框焦点/选中态
        blurFocus();
    }

    private void exitAuthMode(){
        if(!inAuthMode) { return; }
        inAuthMode = false;
        authRequestInProgress = false;
        // 恢复 Roboto 字体
        welcomeLabel.setText("Welcome,");
        welcomeLabel.setFont(Resources.ROBOTO_BOLD_LARGE);
        subtitleLabel.setVisible(true);
        // 清空输入并恢复占位
        cardInput.clear();
        passwordInput.clear();
        passwordInput.getPlaceHolder().setText("Password");
        // 显示 Forget Password 按钮
        forgetLabel.setVisible(true);
        forgetLabel.setManaged(true);
        authButtonsRow.setVisible(false); authButtonsRow.setManaged(false);
        loginButton.setVisible(true); loginButton.setManaged(true);
        statusLabel.setText(" "); statusLabel.setTextFill(Resources.DISABLED);
        // 回车触发 Login
        cardInput.setOnAction(this::performLogin);
        passwordInput.setOnAction(this::performLogin);
        // 重新居中标题
        Platform.runLater(this::applyFixedLayout);
        // 离开认证环节后移除焦点
        blurFocus();
    }

    // 身份认证"Next"动作
    private void performAuth(){
        // 防止重复请求
        if (authRequestInProgress) {
            return;
        }

        String cardNum = cardInput.getText()==null? "" : cardInput.getText().trim();
        String idNum = passwordInput.getPassword()==null? "" : passwordInput.getPassword().trim();
        if(cardNum.isEmpty() || idNum.isEmpty()) {
            showErrorDialog("一卡通号和身份证号不能为空");
            return;
        }

        authRequestInProgress = true;
        setAuthBusy(true);
        new Thread(() -> doAuthRequest(cardNum, idNum)).start();
    }

    private void setAuthBusy(boolean busy){
        cardInput.setDisable(busy);
        passwordInput.setDisable(busy);
        if(nextButton!=null) { nextButton.setBusy(busy); }
        if(cancelButton!=null) { cancelButton.setBusy(busy); }
    }

    private void doAuthRequest(String cardNum, String idNum){
        try{
            String resp = RequestSender.forgetPwd(cardNum, idNum);
            Platform.runLater(() -> handleAuthResponse(resp, cardNum));
        } catch (Exception ex){
            Platform.runLater(() -> {
                // 重置请求状态
                authRequestInProgress = false;
                setAuthBusy(false);
                // 判断异常类型，提供更准确的错误提示
                String errorMsg;
                if (ex.getMessage().contains("Connection refused") ||
                        ex.getMessage().contains("ConnectException") ||
                        ex.getMessage().contains("UnknownHostException") ||
                        ex instanceof java.net.ConnectException ||
                        ex instanceof java.net.UnknownHostException) {
                    errorMsg = "无法连接到服务器";
                } else {
                    errorMsg = "网络连接失败";
                }
                showErrorDialog(errorMsg);
            });
        }
    }

    private void handleAuthResponse(String response, String cardNum){
        // 重置请求状态
        authRequestInProgress = false;
        setAuthBusy(false);
        try{
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            int code = map!=null && map.get("code") instanceof Number ? ((Number)map.get("code")).intValue() : -1;
            if(code == 200){
                enterNewPasswordMode(cardNum);
            } else if(code == 400){
                showErrorDialog("身份验证失败！一卡通号或身份证号不正确");
            } else if(code == 500){
                showErrorDialog("服务器内部错误，请稍后再试");
            } else {
                showErrorDialog("验证失败，请检查输入信息");
            }
        } catch(Exception e){
            showErrorDialog("服务器响应异常，请稍后重试");
        }
    }

    // 进入新密码设置模式
    private void enterNewPasswordMode(String cardNumber) {
        if(inNewPasswordMode) { return; }
        inNewPasswordMode = true;
        inAuthMode = false;
        currentCardNumber = cardNumber;

        welcomeLabel.setText("New Password");
        welcomeLabel.setFont(javafx.scene.text.Font.font(32));
        subtitleLabel.setVisible(false);

        // 隐藏一卡通号输入框
        cardInput.setVisible(false);
        cardInput.setManaged(false);

        // 清空密码输入框并改为新密码输入
        passwordInput.clear();
        passwordInput.getPlaceHolder().setText("Password");

        // 继续隐藏 Forget Password 按钮
        forgetLabel.setVisible(false);
        forgetLabel.setManaged(false);

        // 修改按钮文本为保存和取消
        if (nextButton != null) { nextButton.setText("Save"); }
        if (cancelButton != null) { cancelButton.setText("Cancel"); }

        // 设置按钮事件
        if (nextButton != null) {
            nextButton.setOnAction(this::performPasswordReset);
        }
        if (cancelButton != null) {
            if (externalRecoveryMode) {
                cancelButton.setOnAction(() -> {
                    Stage s = (Stage) root.getScene().getWindow();
                    s.close();
                });
            } else {
                cancelButton.setOnAction(this::exitNewPasswordMode);
            }
        }

        // 回车触发保存
        passwordInput.setOnAction(this::performPasswordReset);

        Platform.runLater(this::applyFixedLayout);
        // 进入新密码环节后移除焦点
        blurFocus();
    }

    // 退出新密码设置模式，返回登录界面
    private void exitNewPasswordMode() {
        if(!inNewPasswordMode) { return; }
        inNewPasswordMode = false;
        passwordResetInProgress = false;
        currentCardNumber = null;

        welcomeLabel.setText("Welcome,");
        welcomeLabel.setFont(Resources.ROBOTO_BOLD_LARGE);
        subtitleLabel.setVisible(true);

        // 显示一卡通号输入框
        cardInput.setVisible(true);
        cardInput.setManaged(true);
        cardInput.clear();

        // 清空密码输入框并恢复占位符
        passwordInput.clear();
        passwordInput.getPlaceHolder().setText("Password");

        // 显示 Forget Password 按钮
        forgetLabel.setVisible(true);
        forgetLabel.setManaged(true);

        // 恢复登录按钮
        authButtonsRow.setVisible(false);
        authButtonsRow.setManaged(false);
        loginButton.setVisible(true);
        loginButton.setManaged(true);
        statusLabel.setText(" "); statusLabel.setTextFill(Resources.DISABLED);
        // 回车触发 Login
        cardInput.setOnAction(this::performLogin);
        passwordInput.setOnAction(this::performLogin);
        // 重新居中标题
        Platform.runLater(this::applyFixedLayout);
        // 离开新密码环节后移除焦点
        blurFocus();
    }

    // 执行密码重置
    private void performPasswordReset() {
        // 防止重复请求
        if (passwordResetInProgress) {
            return;
        }

        String newPassword = passwordInput.getPassword();
        if (newPassword == null || newPassword.trim().isEmpty()) {
            showErrorDialog("新密码不能为空");
            return;
        }

        passwordResetInProgress = true;
        setNewPasswordBusy(true);
        new Thread(() -> doPasswordResetRequest(currentCardNumber, newPassword.trim())).start();
    }

    private void setNewPasswordBusy(boolean busy) {
        passwordInput.setDisable(busy);
        if(nextButton != null) { nextButton.setBusy(busy); }
        if(cancelButton != null) { cancelButton.setBusy(busy); }
    }

    private void doPasswordResetRequest(String cardNumber, String newPassword) {
        try {
            String resp = RequestSender.resetPwd(cardNumber, newPassword);
            Platform.runLater(() -> handlePasswordResetResponse(resp));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                // 重置请求状态
                passwordResetInProgress = false;
                setNewPasswordBusy(false);
                showErrorDialog("重置密码失败: " + ex.getMessage());
            });
        }
    }

    private void handlePasswordResetResponse(String response) {
        // 重置请求状态
        passwordResetInProgress = false;
        setNewPasswordBusy(false);
        try {
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            int code = map!=null && map.get("code") instanceof Number ? ((Number)map.get("code")).intValue() : -1;
            if(code == 200) {
                // 成功：外部找回模式下，2秒后自动关闭窗口；否则返回登录界面
                if (externalRecoveryMode) {
                    AsyncFX.runLaterDelay(2000, () -> {
                        Stage s = (Stage) root.getScene().getWindow();
                        if (s != null) { s.close(); }
                    });
                } else {
                    AsyncFX.runLaterDelay(2000, this::exitNewPasswordMode);
                }
            } else if(code == 400) {
                showErrorDialog("密码重置失败！");
            } else if(code == 500) {
                showErrorDialog("服务器内部错误，请稍后再试！");
            } else {
                showErrorDialog("服务器响应: " + response);
            }
        } catch(Exception e) {
            showErrorDialog("服务器响应解析失败！\n" + response);
        }
    }

    private void showLoadingAnimation() {
        FadeAnimation.fadeOut(Duration.seconds(0.18), formContainer);
        if (!root.getChildren().contains(progressBar)) {
            root.getChildren().add(progressBar);
            AnchorPane.setLeftAnchor(progressBar, PROGRESS_LEFT);
            AnchorPane.setTopAnchor(progressBar, PROGRESS_TOP);
        }
        FadeAnimation.fadeIn(Duration.seconds(0.20), progressBar, Duration.seconds(0.15));
    }

    private void hideLoadingAnimation() {
        FadeAnimation.fadeOut(Duration.seconds(0.20), progressBar);
        FadeAnimation.fadeIn(Duration.seconds(0.22), formContainer, Duration.seconds(0.05));
    }

    private void performLogin() {
        String cardNumber = cardInput.getText().trim();
        String password = passwordInput.getPassword();
        if (cardNumber.isEmpty() || password.isEmpty()) {
            showErrorDialog("一卡通号和密码不能为空");
            return;
        }
        setBusy(true);
        showLoadingAnimation();
        new Thread(() -> doLoginRequest(cardNumber, password)).start();
    }

    private void setBusy(boolean busy) {
        loginButton.setBusy(busy);
        cardInput.setDisable(busy); passwordInput.setDisable(busy); forgetLabel.setDisable(busy);
    }

    private void doLoginRequest(String cardNumber, String password) {
        try {
            String resp = RequestSender.login(cardNumber, password);
            Platform.runLater(() -> handleServerResponse(resp, cardNumber));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                String errorMsg;
                if (ex.getMessage().contains("Connection refused") ||
                        ex.getMessage().contains("ConnectException") ||
                        ex.getMessage().contains("UnknownHostException") ||
                        ex instanceof java.net.ConnectException ||
                        ex instanceof java.net.UnknownHostException) {
                    errorMsg = "无法连接到服务器";
                } else {
                    errorMsg = "网络连接失败";
                }
                AsyncFX.runLaterDelay(2050, () -> {
                    showErrorDialog(errorMsg);
                    setBusy(false);
                    hideLoadingAnimation();
                });
            });
        }
    }

    private void handleServerResponse(String response, String cardNumber) {
        try {
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            Object codeObj = map == null ? null : map.get("code");
            int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
            if (code == 200) {
                Platform.runLater(() -> { ((Stage) loginButton.getScene().getWindow()).close(); new MainFrame(cardNumber).show(); });
            } else if (code == 400) {
                AsyncFX.runLaterDelay(2050, () -> {
                    showErrorDialog("一卡通号或密码错误");
                    setBusy(false);
                    hideLoadingAnimation();
                });
                return;
            } else {
                AsyncFX.runLaterDelay(2050, () -> {
                    showErrorDialog("服务器错误");
                    setBusy(false);
                    hideLoadingAnimation();
                });
                return;
            }
        } catch (Exception e) {
            if (response.contains("\"code\":200")) {
                Platform.runLater(() -> { ((Stage) loginButton.getScene().getWindow()).close(); new MainFrame(cardNumber).show(); });
            } else if (response.contains("\"code\":400")) {
                AsyncFX.runLaterDelay(2050, () -> {
                    showErrorDialog("一卡通号或密码错误");
                    setBusy(false);
                    hideLoadingAnimation();
                });
                return;
            } else {
                AsyncFX.runLaterDelay(2050, () -> {
                    showErrorDialog("服务器错误");
                    setBusy(false);
                    hideLoadingAnimation();
                });
                return;
            }
        }
        // 只有200成功的情况才会执行到这里
        setBusy(false);
        hideLoadingAnimation();
    }

    private void showErrorDialog(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle("登录错误");
        alert.setHeaderText(null);
        UIUtil.applyLogoToAlert(alert);
        alert.showAndWait();
    }

    private ImageView loadImage(String path) {
        try {
            var url = getClass().getResource(path);
            if (url == null) { return null; }
            Image img = new Image(url.toExternalForm());
            return new ImageView(img);
        } catch (Exception e) { return null; }
    }

    // 对外暴露：以找回密码模式打开新窗口（含一卡通号）
    public void openAsRecovery(Stage owner, String cardNumber) {
        this.externalRecoveryMode = true;
        this.externalStageRef = owner;
        this.externalCardNumber = cardNumber;
        Stage stage = new Stage();
        try {
            start(stage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // 兼容旧签名
    public void openAsRecovery(Stage owner) { openAsRecovery(owner, null); }

    public static void main(String[] args) { launch(args); }
}