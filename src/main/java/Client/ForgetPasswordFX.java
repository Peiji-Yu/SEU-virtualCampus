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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import Server.model.Request;

public class ForgetPasswordFX extends Application {
    private Button submitButton;
    private TextField cardField;
    private TextField idField;
    private Label statusLabel;
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private Gson gson = new Gson();

    @Override
    public void start(Stage stage) {
        stage.setTitle("找回密码");
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 40, 10, 40));
        grid.setAlignment(Pos.CENTER);

        Label cardLabel = new Label("一卡通号:");
        cardField = new TextField();
        cardField.setPromptText("请输入一卡通号");
        Label idLabel = new Label("身份证号:");
        idField = new TextField();
        idField.setPromptText("请输入身份证号");
        submitButton = new Button("确定");
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #3570c7; -fx-font-size: 13px;");

        grid.add(cardLabel, 0, 0);
        grid.add(cardField, 1, 0);
        grid.add(idLabel, 0, 1);
        grid.add(idField, 1, 1);
        grid.add(submitButton, 1, 2);
        grid.add(statusLabel, 1, 3);

        submitButton.setOnAction(e -> onSubmit());

        Scene scene = new Scene(grid, 350, 200);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void onSubmit() {
        String cardNum = cardField.getText().trim();
        String idNum = idField.getText().trim();
        if (cardNum.isEmpty() || idNum.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "一卡通号和身份证号不能为空！");
            return;
        }
        submitButton.setDisable(true);
        statusLabel.setText("正在请求服务器...");
        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", Integer.parseInt(cardNum));
                data.put("id", idNum);
                Request request = new Request("getSelf", data);
                String response = sendRequestToServer(request);
                Platform.runLater(() -> handleForgetPwdResponse(response));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
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

    private void handleForgetPwdResponse(String response) {
        submitButton.setDisable(false);
        try {
            Map resp = gson.fromJson(response, Map.class);
            Object codeObj = resp.get("code");
            Object msgObj = resp.get("message");
            String cardNum = cardField.getText().trim();
            if (codeObj != null && ((Double)codeObj).intValue() == 200 && "success".equals(msgObj)) {
                showAlert(Alert.AlertType.INFORMATION, "成功", "身份验证成功，请设置新密码。");
                // 关闭当前窗口，打开新密码界面
                Stage stage = (Stage) submitButton.getScene().getWindow();
                stage.close();
                NewPasswordFX newPwdFX = new NewPasswordFX(cardNum);
                newPwdFX.start(new Stage());
            } else {
                showAlert(Alert.AlertType.ERROR, "失败", "身份验证失败！\n" + response);
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
