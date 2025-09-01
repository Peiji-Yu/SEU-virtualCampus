package Client;

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
    private Label statusLabel;

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private JSONUtil jsonUtil = new JSONUtil();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("虚拟校园登录");

        // 标题
        Label titleLabel = new Label("虚拟校园系统登录");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 表单
        GridPane formPane = new GridPane();
        formPane.setHgap(10);
        formPane.setVgap(10);
        formPane.setPadding(new Insets(15, 0, 15, 0));

        Label cardNumberLabel = new Label("一卡通号:");
        cardNumberLabel.setStyle("-fx-font-size: 14px;");
        cardNumberField = new TextField();

        Label passwordLabel = new Label("密码:");
        passwordLabel.setStyle("-fx-font-size: 14px;");
        passwordField = new PasswordField();

        loginButton = new Button("登录");
        loginButton.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        formPane.add(cardNumberLabel, 0, 0);
        formPane.add(cardNumberField, 1, 0);
        formPane.add(passwordLabel, 0, 1);
        formPane.add(passwordField, 1, 1);
        formPane.add(loginButton, 1, 2);

        // 状态标签
        statusLabel = new Label(" ");
        statusLabel.setStyle("-fx-text-fill: blue;");
        statusLabel.setAlignment(Pos.CENTER);

        VBox mainBox = new VBox(10, titleLabel, formPane, statusLabel);
        mainBox.setPadding(new Insets(15));
        mainBox.setAlignment(Pos.CENTER);

        loginButton.setOnAction(e -> performLogin());

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

            String jsonRequest = jsonUtil.toJson(request);
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

        try {
            if (response.contains("success") || response.contains("true")) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("登录成功!");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "登录成功，欢迎使用虚拟校园系统!");
                alert.setHeaderText("成功");
                alert.showAndWait();
            } else if (response.contains("error") || response.contains("false")) {
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

    public static void main(String[] args) {
        launch(args);
    }
}