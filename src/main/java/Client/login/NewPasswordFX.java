package Client.login;

import Client.ClientNetworkHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import Server.model.Request;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 找回密码第二步：重置新密码窗口 (已迁移至 login 子包)。
 * 流程：
 * 1. ForgetPasswordFX 校验成功后传入 cardNumber。
 * 2. 用户输入新密码 -> 后台线程发送 resetPwd 请求。
 * 3. 根据服务器返回 code 提示并可关闭窗口。
 * 设计：仅做非空校验；网络调用放入后台线程；UI 更新通过 Platform.runLater。
 * 可扩展：密码复杂度校验、显示/隐藏密码、重复输入确认、节流防刷。
 * 作者：@Msgo-srAm
 */
public class NewPasswordFX extends Application {
    private final String cardNumber;
    private final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String,Object>>(){}.getType();
    private TextField passwordField;
    private Button okButton;
    private Label statusLabel;

    public NewPasswordFX(String cardNumber) {this.cardNumber = cardNumber;}
    public NewPasswordFX() {this.cardNumber = null;}

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
        Button cancelButton = new Button("取消");
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
        new Thread(() -> doResetPwdRequest(newPwd, stage)).start();
    }

    private void doResetPwdRequest(String newPwd, Stage stage) {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("password", newPwd);
            if (cardNumber != null) {data.put("cardNumber", Integer.parseInt(cardNumber));}
            String resp = ClientNetworkHelper.send(new Request("resetPwd", data));
            Platform.runLater(() -> handleResetPwdResponse(resp, stage));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                okButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "错误", "请求失败: " + ex.getMessage());
            });
        }
    }

    private void handleResetPwdResponse(String response, Stage stage) {
        okButton.setDisable(false);
        try {
            Map<String,Object> respMap = gson.fromJson(response, MAP_TYPE);
            Object codeObj = respMap==null?null:respMap.get("code");
            Object msgObj = respMap==null?null:respMap.get("message");
            int code = (codeObj instanceof Number) ? ((Number) codeObj).intValue() : -1;
            String msg = msgObj == null ? "" : String.valueOf(msgObj);
            if (code == 200) {
                showAlert(Alert.AlertType.INFORMATION, "成功", (msg.isEmpty()? "密码重置成功" : msg));
                stage.close();
            } else if (code == 400) {
                showAlert(Alert.AlertType.ERROR, "失败", (msg.isEmpty()? "请求失败" : msg));
            } else if (code == 500) {
                showAlert(Alert.AlertType.ERROR, "服务器错误", (msg.isEmpty()? "服务器内部错误" : msg));
            } else {
                showAlert(Alert.AlertType.ERROR, "未知响应", "未识别的返回: code=" + code + "\n原始: " + response);
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

