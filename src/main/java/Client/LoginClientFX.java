package Client;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import Server.model.Request;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class LoginClientFX extends Application {
    private TextField cardNumberField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button forgetPwdButton; // 新增忘记密码按钮
    private Label statusLabel;

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private Gson gson = new Gson();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("虚拟校园登录");

        // 主容器美化
        VBox mainBox = new VBox(18);
        mainBox.setPadding(new Insets(30, 40, 30, 40));
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setStyle(
            "-fx-background-radius: 18;" +
            "-fx-background-color: linear-gradient(to bottom right, #e3f0ff, #f8fbff);" +
            "-fx-effect: dropshadow(gaussian, #b3c6e7, 12, 0.3, 0, 4);"
        );

        // 标题
        Label titleLabel = new Label("虚拟校园系统登录");
        titleLabel.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2a4d7b;" +
            "-fx-effect: dropshadow(gaussian, #b3c6e7, 2, 0.2, 0, 1);"
        );

        // 表单
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

        // 登录和忘记密码按钮水平排列
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
        // 修改为打开新窗口
        forgetPwdButton.setOnAction(e -> {
            try {
                new ForgetPasswordFX().start(new Stage());
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "无法打开找回密码窗口: " + ex.getMessage());
                alert.setHeaderText("错误");
                alert.showAndWait();
            }
        });

        buttonBox.getChildren().addAll(loginButton, forgetPwdButton);

        // 修改表单按钮布局
        formPane.add(cardNumberLabel, 0, 0);
        formPane.add(cardNumberField, 1, 0);
        formPane.add(passwordLabel, 0, 1);
        formPane.add(passwordField, 1, 1);
        formPane.add(buttonBox, 1, 2); // 用HBox替换原来的loginButton

        // 状态标签美化
        statusLabel = new Label(" ");
        statusLabel.setStyle("-fx-text-fill: #3570c7; -fx-font-size: 13px;");
        statusLabel.setAlignment(Pos.CENTER);

        mainBox.getChildren().addAll(titleLabel, formPane, statusLabel);

        // 支持回车键登录
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

        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", Integer.parseInt(cardNumber));
                data.put("password", password);

                Request request = new Request("login", data);
                String response = sendRequestToServer(request);

                Platform.runLater(() -> handleServerResponse(response));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("登录失败: " + ex.getMessage());
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    private String sendRequestToServer(Request request) throws IOException {
        Socket socket = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;

        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            String jsonRequest = gson.toJson(request);
            byte[] jsonData = jsonRequest.getBytes("UTF-8");

            dos.writeInt(jsonData.length);
            dos.write(jsonData);
            dos.flush();

            int responseLength = dis.readInt();
            byte[] responseData = new byte[responseLength];
            dis.readFully(responseData);

            return new String(responseData, "UTF-8");
        } finally {
            try { if (dis != null) dis.close(); } catch (IOException e) {}
            try { if (dos != null) dos.close(); } catch (IOException e) {}
            try { if (socket != null) socket.close(); } catch (IOException e) {}
        }
    }

    private void handleServerResponse(String response) {
        loginButton.setDisable(false);
        System.out.println(response);
        try {
            if (response.contains("\"code\":200")) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("登录成功!");
                // 打开主界面
                String cardNumber = cardNumberField.getText().trim();
                Platform.runLater(() -> {
                    Stage currentStage = (Stage) loginButton.getScene().getWindow();
                    currentStage.close();
                    new MainFrame(cardNumber).show();
                });
            } else if (response.contains("\"code\":400")) {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("登录失败: 一卡通号或密码错误");
            } else {
                statusLabel.setStyle("-fx-text-fill: blue;");
                statusLabel.setText("服务器响应: " + response);
            }
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("解析服务器响应时出错: " + e.getMessage());
        }
    }

    // 忘记密码弹窗及请求逻辑
    public static void main(String[] args) {
        launch(args);
    }
}
