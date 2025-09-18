package Client.finance;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 一卡通信息面板：现代化卡片式设计
 * 采用与学籍管理界面一致的风格和主题色
 */
public class CardInfoPanel extends BorderPane {
    private static final Gson GSON = new Gson();
    private static final String PRIMARY_COLOR = "#176b3a";
    private static final String PRIMARY_HOVER_COLOR = "#1e7d46";
    private static final String TEXT_COLOR = "#2c3e50";
    private static final String SUBTITLE_COLOR = "#6c757d";

    private final String selfCardNumber;
    private final boolean admin;

    private TextField cardField;
    private Label balanceValueLabel;
    private Label statusValueLabel;
    private TextField rechargeAmtField;
    private TextField rechargeDescField;
    private Button queryBalanceBtn;
    private Button rechargeBtn;

    public CardInfoPanel(String cardNumber, boolean admin) {
        this.selfCardNumber = cardNumber;
        this.admin = admin;
        initializeUI();
        fetchCardInfo();
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 主容器
        VBox container = new VBox(20);
        container.setPadding(new Insets(30, 30, 30, 30));

        // 标题区域
        VBox titleBox = new VBox(5);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        Label titleLabel = new Label("一卡通管理");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查询和管理一卡通信息");
        subtitleLabel.setStyle("-fx-text-fill: " + SUBTITLE_COLOR + "; -fx-font-size: 14px;");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        // 卡片查询区域
        VBox cardQueryBox = new VBox(15);
        cardQueryBox.setPadding(new Insets(20));
        cardQueryBox.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");

        // 卡号输入行
        HBox cardInputRow = new HBox(10);
        cardInputRow.setAlignment(Pos.CENTER_LEFT);

        Label cardLabel = new Label("一卡通号:");
        cardLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold; -fx-font-size: 18px;");

        cardField = createStyledTextField("输入一卡通号");
        cardField.setText(selfCardNumber);
        cardField.setDisable(!admin);
        HBox.setHgrow(cardField, Priority.ALWAYS);
        queryBalanceBtn = createPrimaryButton("查询余额");
        queryBalanceBtn.setOnAction(e -> fetchCardInfo());

        cardInputRow.getChildren().addAll(cardLabel, cardField, queryBalanceBtn);
        cardInputRow.setSpacing(30);
        // 余额和状态显示区域
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(15);
        infoGrid.setPadding(new Insets(15, 0, 0, 0));

        // 余额显示
        Label balanceLabel = new Label("当前余额:");
        balanceLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        GridPane.setConstraints(balanceLabel, 0, 0);

        balanceValueLabel = new Label("--");
        balanceValueLabel.setStyle("-fx-text-fill: " + PRIMARY_COLOR + "; -fx-font-weight: bold; -fx-font-size: 20px;");
        GridPane.setConstraints(balanceValueLabel, 1, 0);

        // 状态显示
        Label statusLabel = new Label("一卡通状态:");
        statusLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        GridPane.setConstraints(statusLabel, 0, 1);

        statusValueLabel = new Label("--");
        statusValueLabel.setStyle("-fx-text-fill: " + PRIMARY_COLOR + "; -fx-font-weight: bold; -fx-font-size: 20px;");
        GridPane.setConstraints(statusValueLabel, 1, 1);

        infoGrid.getChildren().addAll(balanceLabel, balanceValueLabel, statusLabel, statusValueLabel);

        cardQueryBox.getChildren().addAll(cardInputRow, infoGrid);

        // 充值操作区域
        VBox rechargeBox = new VBox(15);
        rechargeBox.setPadding(new Insets(20));
        rechargeBox.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");

        Label rechargeTitle = new Label("充值操作");
        rechargeTitle.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold; -fx-font-size: 22px;");

        // 充值表单
        GridPane rechargeForm = new GridPane();
        rechargeForm.setHgap(15);
        rechargeForm.setVgap(15);
        rechargeForm.setPadding(new Insets(10, 0, 0, 0));

        // 金额输入
        Label amtLabel = new Label("充值金额(元):");
        amtLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        GridPane.setConstraints(amtLabel, 0, 0);

        rechargeAmtField = createStyledTextField("输入金额");
        rechargeAmtField.setPrefWidth(200);
        GridPane.setConstraints(rechargeAmtField, 1, 0);

        // 备注输入
        Label descLabel = new Label("备注:");
        descLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        GridPane.setConstraints(descLabel, 0, 1);

        rechargeDescField = createStyledTextField("备注信息(可选)");
        rechargeDescField.setPrefWidth(200);
        GridPane.setConstraints(rechargeDescField, 1, 1);

        // 充值按钮
        rechargeBtn = createPrimaryButton("立即充值");
        rechargeBtn.setOnAction(e -> doRecharge());
        GridPane.setConstraints(rechargeBtn, 2,0);
        GridPane.setValignment(rechargeBtn, javafx.geometry.VPos.CENTER);

        rechargeForm.getChildren().addAll(amtLabel, rechargeAmtField, descLabel, rechargeDescField, rechargeBtn);
        rechargeBox.getChildren().addAll(rechargeForm);

        container.getChildren().addAll(titleBox, cardQueryBox, rechargeBox);
        setCenter(container);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 14px; -fx-pref-height: 40px; " +
                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-focus-color: " + PRIMARY_COLOR + "; -fx-faint-focus-color: transparent;" +
                "-fx-padding: 0 10px;");
        return field;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-height: 40px; -fx-min-width: 100px; -fx-background-radius: 5;");

        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: " + PRIMARY_HOVER_COLOR + "; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-pref-height: 40px; -fx-min-width: 100px; -fx-background-radius: 5;")
        );

        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-pref-height: 40px; -fx-min-width: 100px; -fx-background-radius: 5;")
        );

        return button;
    }

    private Integer parseTargetCard() {
        String v = cardField.getText().trim();
        if (v.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "输入提示", "请输入一卡通号");
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "输入提示", "一卡通号需为数字");
            return null;
        }
    }

    public void refreshData() {
        fetchCardInfo();
    }

    private void fetchCardInfo() {
        Integer card = parseTargetCard();
        if (card == null) {
            return;
        }
        runAsync(() -> FinanceRequestSender.getFinanceCard(card), json -> {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.get("code").getAsInt() != 200) {
                showAlert(Alert.AlertType.ERROR, "查询失败", obj.get("message").getAsString());
                return;
            }
            JsonElement dataEl = obj.get("data");
            if (dataEl != null && dataEl.isJsonObject()) {
                JsonObject data = dataEl.getAsJsonObject();
                if (data.has("balance") && !data.get("balance").isJsonNull()) {
                    int balance = data.get("balance").getAsInt();
                    updateBalanceDisplay(balance);
                } else {
                    balanceValueLabel.setText("--");
                }
                if (data.has("status") && !data.get("status").isJsonNull()) {
                    String status = data.get("status").getAsString();
                    updateStatusDisplay(status);
                } else {
                    Platform.runLater(() -> statusValueLabel.setText("--"));
                }
            } else {
                balanceValueLabel.setText("--");
                statusValueLabel.setText("--");
            }
        });
    }

    private void doRecharge() {
        Integer card = parseTargetCard();
        if (card == null) {
            return;
        }
        String amtStr = rechargeAmtField.getText().trim();
        if (amtStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "输入提示", "请输入充值金额(元)");
            return;
        }
        BigDecimal yuan;
        try {
            yuan = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "输入提示", "金额格式不正确");
            return;
        }
        if (yuan.compareTo(BigDecimal.ZERO) <= 0) {
            showAlert(Alert.AlertType.WARNING, "输入提示", "金额需大于0");
            return;
        }
        BigDecimal centsBD = yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        if (centsBD.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            showAlert(Alert.AlertType.WARNING, "输入提示", "金额过大");
            return;
        }
        int amount = centsBD.intValueExact();
        String desc = rechargeDescField.getText().trim();
        runAsync(() -> FinanceRequestSender.rechargeFinanceCard(card, amount, desc), json -> {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.get("code").getAsInt() != 200) {
                showAlert(Alert.AlertType.ERROR, "充值失败", obj.get("message").getAsString());
                return;
            }
            showAlert(Alert.AlertType.INFORMATION, "操作成功", "充值成功");
            rechargeAmtField.clear();
            rechargeDescField.clear();
            fetchCardInfo();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    private void updateBalanceDisplay(int cents) {
        Platform.runLater(() -> {
            BigDecimal yuan = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN).stripTrailingZeros();
            balanceValueLabel.setText(yuan.toPlainString() + "元");
        });
    }

    private void updateStatusDisplay(String status) {
        Platform.runLater(() -> {
            statusValueLabel.setText(status);
        });
    }

    private void runAsync(SupplierWithException<String> supplier, java.util.function.Consumer<String> onSuccess) {
        disableOps(true);
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> {
            disableOps(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            disableOps(false);
            showAlert(Alert.AlertType.ERROR, "网络错误",
                    "网络或服务器错误" + Optional.ofNullable(task.getException())
                            .map(ex -> ": " + ex.getMessage()).orElse(""));
        });
        Thread th = new Thread(task, "finance-req");
        th.setDaemon(true);
        th.start();
    }

    private void disableOps(boolean disable) {
        Platform.runLater(() -> {
            queryBalanceBtn.setDisable(disable);
            rechargeBtn.setDisable(disable);
            if (disable) {
                queryBalanceBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; -fx-pref-height: 40px; " +
                        "-fx-min-width: 100px; -fx-background-radius: 5;");
                rechargeBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; -fx-pref-height: 40px; " +
                        "-fx-min-width: 100px; -fx-background-radius: 5;");
            } else {
                queryBalanceBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-pref-height: 40px; -fx-min-width: 100px; -fx-background-radius: 5;");
                rechargeBtn.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-pref-height: 40px; -fx-min-width: 100px; -fx-background-radius: 5;");
            }
        });
    }

    @FunctionalInterface
    private interface SupplierWithException<T> { T get() throws Exception; }
}