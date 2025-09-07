package Client.login.component;

import Client.ClientNetworkHelper;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 可嵌入的“找回密码-设置新密码”面板。
 */
public class NewPasswordPane extends StackPane {
    private final String cardNumber;
    private final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String,Object>>(){}.getType();

    private PasswordField passwordField;
    private Button okButton;
    private Button cancelButton;
    private Label statusLabel;

    private Runnable onCancel;
    private Runnable onSuccess;

    public NewPasswordPane(String cardNumber) {
        this.cardNumber = cardNumber;
        setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 4);");
        setPadding(new Insets(20, 32, 16, 32));
        setPrefSize(350, 180);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
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

        getChildren().add(grid);

        okButton.setOnAction(e -> onResetPwd());
        cancelButton.setOnAction(e -> { if (onCancel != null) onCancel.run(); });
    }

    public void setOnCancel(Runnable onCancel) { this.onCancel = onCancel; }
    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    private void onResetPwd() {
        String newPwd = passwordField.getText().trim();
        if (newPwd.isEmpty()) { setStatus("新密码不能为空！", true); return; }
        okButton.setDisable(true);
        setStatus("正在请求服务器...", false);
        new Thread(() -> doResetPwdRequest(newPwd)).start();
    }

    private void doResetPwdRequest(String newPwd) {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("password", newPwd);
            if (cardNumber != null) data.put("cardNumber", Integer.parseInt(cardNumber));
            String resp = ClientNetworkHelper.send(new Request("resetPwd", data));
            Platform.runLater(() -> handleResetPwdResponse(resp));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                okButton.setDisable(false);
                setStatus("请求失败: " + ex.getMessage(), true);
            });
        }
    }

    private void handleResetPwdResponse(String response) {
        okButton.setDisable(false);
        try {
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            Object codeObj = map==null?null:map.get("code");
            Object msgObj = map==null?null:map.get("message");
            int code = codeObj instanceof Number ? ((Number)codeObj).intValue() : -1;
            String msg = msgObj == null ? "" : String.valueOf(msgObj);
            if (code == 200) {
                setStatus(msg.isEmpty()? "密码重置成功" : msg, false);
                if (onSuccess != null) onSuccess.run();
            } else if (code == 400) {
                setStatus(msg.isEmpty()? "请求失败" : msg, true);
            } else if (code == 500) {
                setStatus(msg.isEmpty()? "服务器内部错误" : msg, true);
            } else {
                setStatus("未识别的返回: code=" + code + "\n原始: " + response, true);
            }
        } catch (Exception e) {
            setStatus("服务器响应解析失败！\n" + response, true);
        }
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill:#ff5f56; -fx-font-size:13px;" : "-fx-text-fill:#3570c7; -fx-font-size:13px;");
    }
}

