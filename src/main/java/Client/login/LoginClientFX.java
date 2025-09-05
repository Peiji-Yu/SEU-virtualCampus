package Client.login;

import Client.ClientNetworkHelper;
import Client.MainFrame;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import Server.model.Request;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录界面：负责采集用户凭证并调用后端进行认证。
 * 已迁移至 login 子包，功能保持不变。
 * 特点：
 * 1. 使用后台线程执行网络请求，避免阻塞 JavaFX UI 线程。
 * 2. 对服务器返回的 code 做健壮解析，失败时回退到字符串包含判断（兼容早期格式）。
 * 3. 不缓存登录状态，成功后直接打开 MainFrame 并关闭当前窗口。
 * 4. 保留结构便于后期扩展（记住密码 / 多角色 / 验证码等）。
 * @author Msgo-srAm
 */
public class LoginClientFX extends Application {
    private final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String,Object>>(){}.getType();
    private TextField cardNumberField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button forgetPwdButton;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("虚拟校园登录");
        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(30, 40, 30, 40));
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setStyle(
            "-fx-background-radius: 18;" +
            "-fx-background-color: linear-gradient(to bottom right, #e3f0ff, #f8fbff);" +
            "-fx-effect: dropshadow(gaussian, #b3c6e7, 12, 0.3, 0, 4);"
        );
        Label titleLabel = new Label("虚拟校园系统登录");
        titleLabel.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;" +
            "-fx-effect: dropshadow(gaussian, #b3c6e7, 2, 0.2, 0, 1);"
        );
        GridPane formPane = new GridPane();
        formPane.setHgap(14);
        formPane.setVgap(16);
        formPane.setPadding(new Insets(18, 0, 18, 0));
        formPane.setAlignment(Pos.CENTER);
        Label cardNumberLabel = new Label("一卡通号:");
        cardNumberLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #2a4d7b;");
        cardNumberField = new TextField();
        cardNumberField.setPromptText("请输入一卡通号");
        cardNumberField.setStyle(
            "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #b3c6e7;" +
            "-fx-font-size: 14px; -fx-padding: 6 10 6 10;"
        );
        Label passwordLabel = new Label("密码:");
        passwordLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #2a4d7b;");
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setStyle(
            "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #b3c6e7;" +
            "-fx-font-size: 14px; -fx-padding: 6 10 6 10;"
        );
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        loginButton = new Button("登录");
        loginButton.setStyle(
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #4e8cff;" +
            "-fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 28 8 28;" +
            "-fx-effect: dropshadow(gaussian, #b3c6e7, 2, 0.2, 0, 1);"
        );
        loginButton.setOnMouseEntered(e -> loginButton.setStyle(
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #3570c7;" +
            "-fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 28 8 28;" +
            "-fx-effect: dropshadow(gaussian, #b3c6e7, 2, 0.2, 0, 1);"
        ));
        loginButton.setOnMouseExited(e -> loginButton.setStyle(
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-background-color: #4e8cff;" +
            "-fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 28 8 28;" +
            "-fx-effect: dropshadow(gaussian, #b3c6e7, 2, 0.2, 0, 1);"
        ));
        loginButton.setOnAction(e -> performLogin());
        forgetPwdButton = new Button("忘记密码");
        forgetPwdButton.setStyle(
            "-fx-font-size: 15px; -fx-background-color: #e3f0ff; -fx-text-fill: #3570c7;" +
            "-fx-background-radius: 8; -fx-padding: 8 18 8 18; -fx-border-color: #b3c6e7; -fx-border-radius: 8;"
        );
        forgetPwdButton.setOnMouseEntered(e -> forgetPwdButton.setStyle(
            "-fx-font-size: 15px; -fx-background-color: #d0e2ff; -fx-text-fill: #3570c7;" +
            "-fx-background-radius: 8; -fx-padding: 8 18 8 18; -fx-border-color: #b3c6e7; -fx-border-radius: 8;"
        ));
        forgetPwdButton.setOnMouseExited(e -> forgetPwdButton.setStyle(
            "-fx-font-size: 15px; -fx-background-color: #e3f0ff; -fx-text-fill: #3570c7;" +
            "-fx-background-radius: 8; -fx-padding: 8 18 8 18; -fx-border-color: #b3c6e7; -fx-border-radius: 8;"
        ));
        forgetPwdButton.setOnAction(e -> {
            try { new ForgetPasswordFX().start(new Stage()); } catch (Exception ex) { showError("无法打开找回密码窗口: " + ex.getMessage()); }
        });
        buttonBox.getChildren().addAll(loginButton, forgetPwdButton);
        formPane.add(cardNumberLabel, 0, 0);
        formPane.add(cardNumberField, 1, 0);
        formPane.add(passwordLabel, 0, 1);
        formPane.add(passwordField, 1, 1);
        formPane.add(buttonBox, 1, 2);
        statusLabel = new Label(" ");
        statusLabel.setStyle("-fx-text-fill: #3570c7; -fx-font-size: 13px;");
        statusLabel.setAlignment(Pos.CENTER);
        mainBox.getChildren().addAll(titleLabel, formPane, statusLabel);
        passwordField.setOnAction(e -> performLogin());
        Scene scene = new Scene(mainBox, 400, 250);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void performLogin() {
        String cardNumber = cardNumberField.getText().trim();
        String password = passwordField.getText();
        if (cardNumber.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("一卡通号和密码不能为空!");
            return;
        }
        loginButton.setDisable(true);
        statusLabel.setStyle("-fx-text-fill: blue;");
        statusLabel.setText("正在连接服务器...");
        new Thread(() -> doLoginRequest(cardNumber, password)).start();
    }

    private void doLoginRequest(String cardNumber, String password) {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("cardNumber", Integer.parseInt(cardNumber));
            data.put("password", password);
            Request req = new Request("login", data);
            String respJson = ClientNetworkHelper.send(req);
            Platform.runLater(() -> handleServerResponse(respJson, cardNumber));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("登录失败: " + ex.getMessage());
                loginButton.setDisable(false);
            });
        }
    }

    private void handleServerResponse(String response, String cardNumber) {
        loginButton.setDisable(false);
        try {
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            Object codeObj = map == null ? null : map.get("code");
            int code = (codeObj instanceof Number)? ((Number)codeObj).intValue() : -1;
            if (code == 200) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("登录成功!");
                Platform.runLater(() -> {
                    Stage currentStage = (Stage) loginButton.getScene().getWindow();
                    currentStage.close();
                    new MainFrame(cardNumber).show();
                });
            } else if (code == 400) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("登录失败: 一卡通号或密码错误");
            } else if (code == 500) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("服务器内部错误");
            } else {
                statusLabel.setStyle("-fx-text-fill: blue;");
                statusLabel.setText("服务器响应: " + response);
            }
        } catch (Exception e) {
            if (response.contains("\"code\":200")) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("登录成功!");
                Platform.runLater(() -> {
                    Stage currentStage = (Stage) loginButton.getScene().getWindow();
                    currentStage.close();
                    new MainFrame(cardNumber).show();
                });
            } else if (response.contains("\"code\":400")) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("登录失败: 一卡通号或密码错误");
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("解析服务器响应时出错: " + e.getMessage());
            }
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg);
        alert.setHeaderText("错误");
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}

