package Client;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import Server.model.Request;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NewPasswordFX extends Application {
    private String cardNumber;
    private Gson gson = new Gson();
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private TextField passwordField;
    private Button okButton;
    private Button cancelButton;
    private Label statusLabel;

    public NewPasswordFX() {}
    public NewPasswordFX(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("重置密码");
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 40, 10, 40));
        grid.setAlignment(Pos.CENTER);

        Label pwdLabel = new Label("新密码:");
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入新密码");
        okButton = new Button("确定");
        cancelButton = new Button("取消");
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #3570c7; -fx-font-size: 13px;");

        grid.add(pwdLabel, 0, 0);
        grid.add(passwordField, 1, 0);
        grid.add(okButton, 0, 1);
        grid.add(cancelButton, 1, 1);
        grid.add(statusLabel, 1, 2);

        okButton.setOnAction(e -> onResetPwd(stage));
        cancelButton.setOnAction(e -> stage.close());

        Scene scene = new Scene(grid, 350, 150);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void onResetPwd(Stage stage) {
        String newPwd = passwordField.getText().trim();
        if (newPwd.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "新密码不能为空！");
            return;
        }
        okButton.setDisable(true);
        statusLabel.setText("正在请求服务器...");
        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("password", newPwd);
                data.put("cardNumber", Integer.parseInt(cardNumber));
                Request request = new Request("resetPwd", data);
                String response = sendRequestToServer(request);
                Platform.runLater(() -> handleResetPwdResponse(response, stage));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    okButton.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "错误", "请求失败: " + ex.getMessage());
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
            byte[] jsonData = jsonRequest.getBytes(StandardCharsets.UTF_8);
            dos.writeInt(jsonData.length);
            dos.write(jsonData);
            dos.flush();
            int responseLength = dis.readInt();
            byte[] responseData = new byte[responseLength];
            dis.readFully(responseData);
            return new String(responseData, StandardCharsets.UTF_8);
        } finally {
            try { if (dis != null) dis.close(); } catch (IOException e) {}
            try { if (dos != null) dos.close(); } catch (IOException e) {}
            try { if (socket != null) socket.close(); } catch (IOException e) {}
        }
    }

    private void handleResetPwdResponse(String response, Stage stage) {
        okButton.setDisable(false);
        try {
            Map resp = gson.fromJson(response, Map.class);
            Object codeObj = resp.get("code");
            Object msgObj = resp.get("message");
            if (codeObj != null && ((Double)codeObj).intValue() == 200 && "success".equals(msgObj)) {
                showAlert(Alert.AlertType.INFORMATION, "成功", "密码重置成功！");
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "失败", "密码重置失败！\n" + response);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "解析错误", "服务器响应解析失败！\n" + response);
        }
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type, content);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}

