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
 * 找回密码第一步：身份验证窗口 (已迁移至 login 子包)。
 * 职责：
 * 1. 采集一卡通号 + 身份证号提交后端校验。
 * 2. 成功后跳转到 NewPasswordFX。
 * 3. 使用后台线程发送网络请求，避免阻塞 UI 线程。
 * 设计说明：保持原逻辑不变，仅调整包结构便于分类管理。
 * 后续可扩展：短信/邮箱验证码、多步校验、节流防刷等。
 * 线程安全：网络调用在后台线程执行，UI 更新通过 Platform.runLater 切回 FX 线程。
 * 输入校验：仅做非空与数字转换，后端兜底严格校验。
 * 作者：@Msgo-srAm
 */
public class ForgetPasswordFX extends Application {
    private Button submitButton;
    private TextField cardField;
    private TextField idField;
    private Label statusLabel;
    private final Gson gson = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String,Object>>(){}.getType();

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
        new Thread(() -> doForgetPwdRequest(cardNum, idNum)).start();
    }

    private void doForgetPwdRequest(String cardNum, String idNum) {
        try {
            Map<String,Object> data = new HashMap<>();
            data.put("cardNumber", Integer.parseInt(cardNum));
            data.put("id", idNum);
            String resp = ClientNetworkHelper.send(new Request("forgetPwd", data));
            Platform.runLater(() -> handleForgetPwdResponse(resp, cardNum));
        } catch (Exception ex) {
            Platform.runLater(() -> {
                submitButton.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "错误", "请求失败: " + ex.getMessage());
            });
        }
    }

    private void handleForgetPwdResponse(String response, String cardNum) {
        submitButton.setDisable(false);
        try {
            Map<String,Object> map = gson.fromJson(response, MAP_TYPE);
            int code = map!=null && map.get("code") instanceof Number ? ((Number)map.get("code")).intValue() : -1;
            if (code == 200) {
                showAlert(Alert.AlertType.INFORMATION, "成功", "身份验证成功，请设置新密码。");
                Stage stage = (Stage) submitButton.getScene().getWindow();
                stage.close();
                new NewPasswordFX(cardNum).start(new Stage());
            } else if (code == 400) {
                showAlert(Alert.AlertType.ERROR, "失败", "身份验证失败！");
            } else if (code == 500) {
                showAlert(Alert.AlertType.ERROR, "服务器错误", "服务器内部错误，请稍后再试！");
            } else {
                showAlert(Alert.AlertType.ERROR, "未知响应", "服务器响应: " + response);
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

