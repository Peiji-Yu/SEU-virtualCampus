package Client.login;

import Client.ClientNetworkHelper;
import Client.MainFrame;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.TranslateTransition;
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

import Client.login.component.*;
import Client.login.util.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/** 登录界面（固定尺寸与显式参数布局版本） */
public class LoginClientFX extends Application {
    // ================= 可调参数（仅修改这些常量即可微调布局） =================
    private static final double ROOT_WIDTH = 375;
    private static final double ROOT_HEIGHT = 575;

    private static final double LOGO_SIZE = 64;          // Logo 宽高
    private static final double LOGO_TOP = 60;           // Logo 顶部 Y

    private static final double WELCOME_TOP = 176;       // 欢迎文字顶部 Y
    private static final double WELCOME_EXTRA_TRANSLATE = 0; // 需要轻微垂直微调可改此值

    private static final double FORM_TOP = 306;          // 表单区域顶部 Y
    private static final double SUBTITLE_OFFSET = 38;    // 副标题相对主标题向下偏移

    // 进度条位置（加载动画显示时）
    private static final double PROGRESS_TOP = 291;
    private static final double PROGRESS_LEFT = 112;

    // 登录动画：Logo 下移距离
    private static final double LOGIN_ANIM_LOGO_TRANSLATE_Y = 105;
    private static final Duration LOGIN_ANIM_DURATION = Duration.seconds(0.6);
    private static final Duration LOGIN_ANIM_DELAY = Duration.seconds(0.2);

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
    private Label subtitleLabel; // 新增副标题

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.TRANSPARENT);
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
            bg.setFitWidth(ROOT_WIDTH); bg.setFitHeight(ROOT_HEIGHT); bg.setOpacity(0.17);
            root.getChildren().add(0, bg);
        }

        logo = loadImage("/Image/Logo.png");
        if (logo != null) {
            logo.setFitWidth(LOGO_SIZE); logo.setFitHeight(LOGO_SIZE); logo.setPreserveRatio(true);
            root.getChildren().add(logo);
        }

        welcomeLabel = new Label("welcome,");
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
        forgetLabel.setOnMouseClicked(e -> openForget());
        VBox inputArea = new VBox(inputGroup, forgetLabel); inputArea.setSpacing(5);
        loginButton = new LoginButton();
        statusLabel = new Label(" "); statusLabel.setFont(Resources.ROBOTO_LIGHT); statusLabel.setTextFill(Resources.DISABLED);
        VBox block = new VBox(inputArea, loginButton, statusLabel); block.setSpacing(16);
        formBox.getChildren().add(block);
        formContainer.getChildren().add(formBox);
        AnchorPane.setLeftAnchor(formBox, 63.0);
        root.getChildren().add(formContainer);

        progressBar = new ProgressBar();
        var css = getClass().getResource("/Css/ProgressBar.css");
        if (css != null) progressBar.getStylesheets().add(css.toExternalForm());
        progressBar.setOpacity(0);

        Rectangle focusReq = new Rectangle(ROOT_WIDTH, ROOT_HEIGHT, Color.TRANSPARENT);
        focusReq.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> focusReq.requestFocus());
        root.getChildren().add(0, focusReq);

        bindEvents();

        stage.setScene(scene);
        stage.show();

        // 固定布局一次（延迟确保文本宽度可得）
        Platform.runLater(this::applyFixedLayout);
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
            formContainer.setLayoutX(0); // 内部已设置左右留白 63
        }
    }

    private void bindEvents() {
        Runnable loginAction = this::performLogin;
        loginButton.setOnLogin(loginAction);
        cardInput.setOnAction(loginAction);
        passwordInput.setOnAction(loginAction);
    }

    private void showLoadingAnimation() {
        FadeAnimation.fadeOut(Duration.seconds(0.18), formContainer);
        if (!root.getChildren().contains(progressBar)) {
            root.getChildren().add(progressBar);
            AnchorPane.setLeftAnchor(progressBar, PROGRESS_LEFT);
            AnchorPane.setTopAnchor(progressBar, PROGRESS_TOP);
        }
        FadeAnimation.fadeIn(Duration.seconds(0.20), progressBar, Duration.seconds(0.15));
        if (logo != null) {
            TranslateTransition tt = new TranslateTransition(LOGIN_ANIM_DURATION, logo);
            tt.setByY(LOGIN_ANIM_LOGO_TRANSLATE_Y);
            tt.setDelay(LOGIN_ANIM_DELAY);
            tt.play();
        }
    }

    private void hideLoadingAnimation() {
        FadeAnimation.fadeOut(Duration.seconds(0.20), progressBar);
        FadeAnimation.fadeIn(Duration.seconds(0.22), formContainer, Duration.seconds(0.05));
    }

    private void performLogin() {
        String cardNumber = cardInput.getText().trim();
        String password = passwordInput.getPassword();
        if (cardNumber.isEmpty() || password.isEmpty()) { setStatus("一卡通号和密码不能为空", true); return; }
        setBusy(true);
        setStatus("正在连接服务器...", false);
        showLoadingAnimation();
        new Thread(() -> doLoginRequest(cardNumber, password)).start();
    }

    private void setBusy(boolean busy) {
        loginButton.setBusy(busy);
        cardInput.setDisable(busy); passwordInput.setDisable(busy); forgetLabel.setDisable(busy);
    }

    private void doLoginRequest(String cardNumber, String password) {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("cardNumber", Integer.parseInt(cardNumber));
            data.put("password", password);
            Request req = new Request("login", data);
            String resp = ClientNetworkHelper.send(req);
            Platform.runLater(() -> handleServerResponse(resp, cardNumber));
        } catch (Exception ex) {
            Platform.runLater(() -> { setBusy(false); hideLoadingAnimation(); setStatus("登录失败: " + ex.getMessage(), true); });
        }
    }

    private void handleServerResponse(String response, String cardNumber) {
        try {
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            Object codeObj = map == null ? null : map.get("code");
            int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
            if (code == 200) {
                setStatus("登录成功", false);
                Platform.runLater(() -> { ((Stage) loginButton.getScene().getWindow()).close(); new MainFrame(cardNumber).show(); });
            } else if (code == 400) { setStatus("一卡通号或密码错误", true); }
            else if (code == 500) { setStatus("服务器内部错误", true); }
            else { setStatus("服务器响应: " + response, true); }
        } catch (Exception e) {
            if (response.contains("\"code\":200")) {
                setStatus("登录成功", false);
                Platform.runLater(() -> { ((Stage) loginButton.getScene().getWindow()).close(); new MainFrame(cardNumber).show(); });
            } else if (response.contains("\"code\":400")) { setStatus("一卡通号或密码错误", true); }
            else { setStatus("解析响应失败: " + e.getMessage(), true); }
        } finally {
            setBusy(false);
            hideLoadingAnimation();
        }
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(error ? Color.valueOf("#ff5f56") : Resources.PRIMARY);
    }

    private void openForget() { try { new ForgetPasswordFX().start(new Stage()); } catch (Exception ex) { showErrorDialog("无法打开找回密码窗口: " + ex.getMessage()); } }

    private void showErrorDialog(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("错误");
        alert.showAndWait();
    }

    private ImageView loadImage(String path) {
        try { Image img = new Image(getClass().getResource(path).toExternalForm()); return new ImageView(img); } catch (Exception e) { return null; }
    }

    public static void main(String[] args) { launch(args); }
}
