package Client.login.component;

import Client.ClientNetworkHelper;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 可嵌入的“找回密码-身份验证”面板。
 * 使用 onCancel/onSuccess 回调与宿主通讯。
 */
public class ForgetPasswordPane extends StackPane {
    private final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String,Object>>(){}.getType();

    private Button submitButton;
    private Button cancelButton;
    private TextField cardField;
    private TextField idField;
    private Label statusLabel;

    private Runnable onCancel;
    private Consumer<String> onSuccess; // 参数为 cardNumber

    public ForgetPasswordPane() {
        setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 4);");
        setPadding(new Insets(20, 32, 16, 32));
        setPrefSize(350, 220);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);

        Label cardLabel = new Label("一卡通号:");
        cardField = new TextField();
        cardField.setPromptText("请输入一卡通号");

        Label idLabel = new Label("身份证号:");
        idField = new TextField();
        idField.setPromptText("请输入身份证号");

        submitButton = new Button("确定");
        cancelButton = new Button("取消");
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #3570c7; -fx-font-size: 13px;");

        grid.add(cardLabel, 0, 0);
        grid.add(cardField, 1, 0);
        grid.add(idLabel, 0, 1);
        grid.add(idField, 1, 1);
        grid.add(submitButton, 0, 2);
        grid.add(cancelButton, 1, 2);
        grid.add(statusLabel, 1, 3);

        getChildren().add(grid);

        submitButton.setOnAction(e -> onSubmit());
        cancelButton.setOnAction(e -> { if (onCancel != null) onCancel.run(); });
    }

    public Region getPreferredRegion() { return this; }

    public void setOnCancel(Runnable onCancel) { this.onCancel = onCancel; }
    public void setOnSuccess(Consumer<String> onSuccess) { this.onSuccess = onSuccess; }

    private void onSubmit() {
        String cardNum = cardField.getText().trim();
        String idNum = idField.getText().trim();
        if (cardNum.isEmpty() || idNum.isEmpty()) {
            setStatus("一卡通号和身份证号不能为空！", true);
            return;
        }
        submitButton.setDisable(true);
        setStatus("正在请求服务器...", false);
        new Thread(() -> doForgetPwdRequest(cardNum, idNum)).start();
    }

    private void doForgetPwdRequest(String cardNum, String idNum) {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("cardNumber", Integer.parseInt(cardNum));
            data.put("id", idNum);
            String resp = Client.ClientNetworkHelper.send(new Request("forgetPwd", data));
            Platform.runLater(() -> handleForgetPwdResponse(resp, cardNum));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                submitButton.setDisable(false);
                setStatus("请求失败: " + ex.getMessage(), true);
            });
        }
    }

    private void handleForgetPwdResponse(String response, String cardNum) {
        submitButton.setDisable(false);
        try {
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            int code = map!=null && map.get("code") instanceof Number ? ((Number)map.get("code")).intValue() : -1;
            if (code == 200) {
                setStatus("身份验证成功，请设置新密码。", false);
                if (onSuccess != null) onSuccess.accept(cardNum);
            } else if (code == 400) {
                setStatus("身份验证失败！", true);
            } else if (code == 500) {
                setStatus("服务器内部错误，请稍后再试！", true);
            } else {
                setStatus("服务器响应: " + response, true);
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

