package Client.login.component;

import Client.login.net.RequestSender;
import Client.login.util.Resources;
import Client.util.UIUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

/**
 * 修改密码弹窗（复用登录 Forget Password? 流程：身份验证 -> 设置新密码）。
 * 取消按钮仅关闭窗口，不返回登录。
 */
public class ChangePasswordDialog {
    private static final Gson GSON = new Gson();

    private boolean authBusy = false;
    private boolean resetBusy = false;
    private String currentCardNumber;

    public static void show(Stage owner) { new ChangePasswordDialog().open(owner); }

    private void open(Stage owner){
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("修改密码");
        UIUtil.applyLogoToStage(stage);

        // 标题
        Label title = new Label("找回/重置密码");
        title.setFont(Resources.ROBOTO_BOLD);
        title.setTextFill(Resources.FONT_COLOR);

        // Step 1: 认证输入（卡号 + 身份证）
        UsernameInput cardInput = new UsernameInput("Card Number", true);
        PasswordInput idInput = new PasswordInput("ID Number");

        // Step 2: 新密码
        PasswordInput newPwd = new PasswordInput("New Password");
        PasswordInput confirm = new PasswordInput("Confirm Password");
        newPwd.setVisible(false); newPwd.setManaged(false);
        confirm.setVisible(false); confirm.setManaged(false);

        // 按钮区
        HBox btns = new HBox(12); btns.setAlignment(Pos.CENTER_RIGHT);
        FilledButton primary = new FilledButton("Next", 110, Resources.PRIMARY, Resources.SECONDARY, Resources.WHITE);
        FilledButton cancel = new FilledButton("Cancel", 110, Color.web("#cfd8dc"), Color.web("#b0bec5"), Color.web("#2a4d7b"));
        btns.getChildren().addAll(cancel, primary);

        VBox root = new VBox(16, title, cardInput, idInput, newPwd, confirm, btns);
        root.setPadding(new Insets(18));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10,0,0,2);");

        // 动作绑定
        cancel.setOnAction(stage::close);

        Runnable doAuth = () -> {
            if (authBusy) return;
            String card = cardInput.getText()==null?"":cardInput.getText().trim();
            String id = idInput.getPassword()==null?"":idInput.getPassword().trim();
            if (card.isEmpty() || id.isEmpty()) { Alert al = new Alert(Alert.AlertType.WARNING, "一卡通号和身份证号不能为空"); UIUtil.applyLogoToAlert(al); al.showAndWait(); return; }
            setAuthBusy(cardInput, idInput, primary, cancel, true);
            new Thread(() -> {
                try {
                    String resp = RequestSender.forgetPwd(card, id);
                    Platform.runLater(() -> handleAuthResponse(resp, card, title, cardInput, idInput, newPwd, confirm, primary));
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setAuthBusy(cardInput, idInput, primary, cancel, false);
                        Alert al = new Alert(Alert.AlertType.ERROR, "网络错误，无法连接服务器"); UIUtil.applyLogoToAlert(al); al.showAndWait();
                    });
                }
            }).start();
        };
        primary.setOnAction(doAuth);
        idInput.setOnAction(doAuth);

        // 保存新密码
        Runnable doReset = () -> {
            if (resetBusy) return;
            String np = newPwd.getPassword();
            String cp = confirm.getPassword();
            if (np == null || np.isEmpty()) { Alert al = new Alert(Alert.AlertType.WARNING, "请输入新密码"); UIUtil.applyLogoToAlert(al); al.showAndWait(); return; }
            if (!np.equals(cp)) { Alert al = new Alert(Alert.AlertType.WARNING, "两次输入的新密码不一致"); UIUtil.applyLogoToAlert(al); al.showAndWait(); return; }
            setResetBusy(newPwd, confirm, primary, cancel, true);
            new Thread(() -> {
                try {
                    String resp = RequestSender.resetPwd(currentCardNumber, np);
                    Platform.runLater(() -> handleResetResponse(resp, stage));
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setResetBusy(newPwd, confirm, primary, cancel, false);
                        Alert al = new Alert(Alert.AlertType.ERROR, "网络错误，无法连接服务器"); UIUtil.applyLogoToAlert(al); al.showAndWait();
                    });
                }
            }).start();
        };

        // 场景
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setWidth(420);
        stage.setHeight(320);
        stage.setResizable(false);
        stage.show();

        // 将回车绑定为 primary
        cardInput.setOnAction(doAuth);
        confirm.setOnAction(doReset);

        // 内部方法：处理认证响应
        // 进入新密码模式
    }

    private void handleAuthResponse(String response, String card,
                                    Label title,
                                    UsernameInput cardInput,
                                    PasswordInput idInput,
                                    PasswordInput newPwd,
                                    PasswordInput confirm,
                                    FilledButton primary){
        setAuthBusy(cardInput, idInput, primary, null, false);
        int code = parseCode(response);
        if (code == 200) {
            this.currentCardNumber = card;
            title.setText("Set New Password");
            // 隐藏认证输入
            cardInput.setVisible(false); cardInput.setManaged(false);
            idInput.setVisible(false); idInput.setManaged(false);
            // 显示新密码输入
            newPwd.setVisible(true); newPwd.setManaged(true);
            confirm.setVisible(true); confirm.setManaged(true);
            primary.setText("Save");
            // 将 primary 行为改为 doReset（通过 onAction 替换）
            primary.setOnAction(this::noop);
            primary.setOnAction(() -> {}); // 清一次以覆盖旧绑定
            primary.setOnAction(() -> doResetFromPrimary(newPwd, confirm, primary));
        } else if (code == 400) {
            Alert al = new Alert(Alert.AlertType.WARNING, "身份验证失败！一卡通号或身份证号不正确"); UIUtil.applyLogoToAlert(al); al.showAndWait();
        } else if (code == 500) {
            Alert al = new Alert(Alert.AlertType.ERROR, "服务器内部错误，请稍后再试"); UIUtil.applyLogoToAlert(al); al.showAndWait();
        } else {
            Alert al = new Alert(Alert.AlertType.ERROR, "验证失败，请检查输入信息"); UIUtil.applyLogoToAlert(al); al.showAndWait();
        }
    }

    private void doResetFromPrimary(PasswordInput newPwd, PasswordInput confirm, FilledButton primary){
        if (resetBusy) return;
        String np = newPwd.getPassword();
        String cp = confirm.getPassword();
        if (np == null || np.isEmpty()) { Alert al = new Alert(Alert.AlertType.WARNING, "请输入新密码"); UIUtil.applyLogoToAlert(al); al.showAndWait(); return; }
        if (!np.equals(cp)) { Alert al = new Alert(Alert.AlertType.WARNING, "两次输入的新密码不一致"); UIUtil.applyLogoToAlert(al); al.showAndWait(); return; }
        setResetBusy(newPwd, confirm, primary, null, true);
        new Thread(() -> {
            try {
                String resp = RequestSender.resetPwd(currentCardNumber, np);
                Platform.runLater(() -> handleResetResponse(resp, null));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setResetBusy(newPwd, confirm, primary, null, false);
                    Alert al = new Alert(Alert.AlertType.ERROR, "网络错误，无法连接服务器"); UIUtil.applyLogoToAlert(al); al.showAndWait();
                });
            }
        }).start();
    }

    private void handleResetResponse(String response, Stage stage){
        int code = parseCode(response);
        if (code == 200) {
            Alert al = new Alert(Alert.AlertType.INFORMATION, "密码已更新"); UIUtil.applyLogoToAlert(al); al.showAndWait();
            if (stage != null) stage.close();
        } else {
            Alert al = new Alert(Alert.AlertType.ERROR, "重置失败，请稍后重试"); UIUtil.applyLogoToAlert(al); al.showAndWait();
        }
        resetBusy = false;
    }

    private int parseCode(String response){
        try {
            Map<?,?> map = GSON.fromJson(response, Map.class);
            Object c = map==null? null : map.get("code");
            if (c instanceof Number) { return ((Number)c).intValue(); }
        } catch (Exception ignored) {}
        return -1;
    }

    private void setAuthBusy(UsernameInput card, PasswordInput id, FilledButton primary, FilledButton cancel, boolean busy){
        this.authBusy = busy;
        card.setDisable(busy);
        id.setDisable(busy);
        if (primary != null) primary.setBusy(busy);
        if (cancel != null) cancel.setBusy(busy);
    }

    private void setResetBusy(PasswordInput newPwd, PasswordInput confirm, FilledButton primary, FilledButton cancel, boolean busy){
        this.resetBusy = busy;
        newPwd.setDisable(busy);
        confirm.setDisable(busy);
        if (primary != null) primary.setBusy(busy);
        if (cancel != null) cancel.setBusy(busy);
    }

    // no-op 以便覆盖旧 handler
    private void noop() {}
}
