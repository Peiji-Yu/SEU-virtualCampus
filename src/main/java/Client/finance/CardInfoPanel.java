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
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 一卡通信息面板：显示余额和充值功能
 */
public class CardInfoPanel extends BorderPane {
    private static final Gson GSON = new Gson();
    private static final String PRIMARY = "#4e8cff";
    private static final String SUCCESS = "#00b894";
    private static final String TEXT = "#2d3436";
    private static final String SUB = "#636e72";

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
        // 设置主面板背景
        this.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa, #e4e8f0);");

        VBox container = new VBox(24);
        container.setPadding(new Insets(32));
        container.setStyle("-fx-background-color: #F6F8FA; " +
                "-fx-border-color: #e0e0e0; " +
                "-fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 4);");

        // 标题
        Label title = new Label("💳 一卡通信息");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));

        // 分隔线
        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 16, 0));

        // 卡号查询和余额显示
        VBox firstSection = new VBox(16);
        firstSection.setPadding(new Insets(0, 0, 16, 0));

        HBox cardRow = new HBox(12);
        cardRow.setAlignment(Pos.CENTER_LEFT);

        Label cardLb = new Label("一卡通号:");
        cardLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold; -fx-font-size: 18px;");

        cardField = new TextField(selfCardNumber);
        cardField.setPromptText("输入一卡通号");
        cardField.setDisable(!admin);
        cardField.setStyle("-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-padding: 10; " +
                "-fx-font-size: 14px; " +
                "-fx-border-color: #ddd; " +
                "-fx-background-color: #fafafa;");
        cardField.setPrefWidth(200);

        queryBalanceBtn = new Button("🔍 查询余额");
        stylePrimary(queryBalanceBtn);
        queryBalanceBtn.setOnAction(e -> fetchCardInfo());

        cardRow.getChildren().addAll(cardLb, cardField, queryBalanceBtn);

        // 余额显示区域
        HBox balanceBox = new HBox(8);
        balanceBox.setAlignment(Pos.CENTER_LEFT);
        balanceBox.setStyle("-fx-background-color: linear-gradient(to right, #f0f7ff, #e3f2fd); " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 16; " +
                "-fx-border-color: #bbdefb; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 1;");

        Label balancePrefixLabel = new Label("当前余额:");
        balancePrefixLabel.setStyle("-fx-text-fill: " + SUB + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        balanceValueLabel = new Label("--");
        balanceValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 24px;");

        balanceBox.getChildren().addAll(balancePrefixLabel, balanceValueLabel);

        // 将余额外框宽度绑定为整个 cardRow 的宽度（包括查询按钮和所有间距），以便与查询按钮对齐
        balanceBox.prefWidthProperty().bind(
                cardLb.widthProperty()
                        .add(cardField.widthProperty())
                        .add(queryBalanceBtn.widthProperty())
                        .add(cardRow.spacingProperty().multiply(2))
        );
        balanceBox.setMaxWidth(Region.USE_PREF_SIZE);

        // 新增：一卡通状态显示（和余额外观一致）
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setStyle("-fx-background-color: linear-gradient(to right, #f0f7ff, #e3f2fd); " +
                "-fx-background-radius: 12; -fx-padding: 16; -fx-border-color: #bbdefb; -fx-border-radius: 12; -fx-border-width: 1;");
        Label statusPrefixLabel = new Label("一卡通状态:");
        statusPrefixLabel.setStyle("-fx-text-fill: " + SUB + "; -fx-font-size: 18px; -fx-font-weight: bold;");
        statusValueLabel = new Label("--");
        statusValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-weight: bold; -fx-font-size: 24px;");
        statusBox.getChildren().addAll(statusPrefixLabel, statusValueLabel);

        // 同样与整个 cardRow 对齐（包含查询按钮与间距）
        statusBox.prefWidthProperty().bind(
                cardLb.widthProperty()
                        .add(cardField.widthProperty())
                        .add(queryBalanceBtn.widthProperty())
                        .add(cardRow.spacingProperty().multiply(2))
        );
        statusBox.setMaxWidth(Region.USE_PREF_SIZE);

        firstSection.getChildren().addAll(cardRow, balanceBox);
        firstSection.getChildren().add(statusBox);

        // 充值操作区域
        VBox rechargeSection = new VBox(16);
        rechargeSection.setPadding(new Insets(16, 0, 0, 0));
        rechargeSection.setStyle("-fx-border-color: #eee; -fx-border-width: 1 0 0 0; -fx-padding: 16 0 0 0;");

        Label rechargeTitle = new Label("💰 充值操作");
        rechargeTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold; -fx-font-size: 28px;");

        GridPane rechargeGrid = new GridPane();
        rechargeGrid.setHgap(12);
        rechargeGrid.setVgap(16);
        rechargeGrid.setPadding(new Insets(8, 0, 0, 0));

        // 金额输入
        Label amtLb = new Label("金额(元):");
        amtLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold;-fx-font-size: 18px;");
        GridPane.setConstraints(amtLb, 0, 0);

        rechargeAmtField = new TextField();
        rechargeAmtField.setPromptText("输入金额");
        rechargeAmtField.setPrefWidth(150);
        rechargeAmtField.setStyle("-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-padding: 10; " +
                "-fx-font-size: 14px; " +
                "-fx-border-color: #ddd; " +
                "-fx-background-color: #fafafa;");
        GridPane.setConstraints(rechargeAmtField, 1, 0);

        // 备注输入
        Label descLb = new Label("备注:");
        descLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold;-fx-font-size: 18px");
        GridPane.setConstraints(descLb, 0, 1);

        rechargeDescField = new TextField();
        rechargeDescField.setPromptText("备注信息(可选)");
        rechargeDescField.setPrefWidth(200);
        rechargeDescField.setStyle("-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-padding: 10; " +
                "-fx-font-size: 14px; " +
                "-fx-border-color: #ddd; " +
                "-fx-background-color: #fafafa;");
        GridPane.setConstraints(rechargeDescField, 1, 1);

        // 充值按钮
        rechargeBtn = new Button("⚡ 立即充值");
        styleSuccess(rechargeBtn);
        rechargeBtn.setOnAction(e -> doRecharge());
        GridPane.setConstraints(rechargeBtn, 2, 0, 1, 2);
        GridPane.setValignment(rechargeBtn, javafx.geometry.VPos.CENTER);

        rechargeGrid.getChildren().addAll(amtLb, rechargeAmtField, descLb, rechargeDescField, rechargeBtn);

        rechargeSection.getChildren().addAll(rechargeTitle, rechargeGrid);
        container.getChildren().addAll(title, separator, firstSection, rechargeSection);

        setCenter(container);
    }

    private void stylePrimary(Button b) {
        b.setStyle("-fx-background-color: " + PRIMARY + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 20; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #3a7be0; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 20; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + PRIMARY + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 20; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;"));
    }

    private void styleSuccess(Button b) {
        b.setStyle("-fx-background-color: " + SUCCESS + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 24; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #00a382; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 24; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + SUCCESS + "; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 12 24; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;"));
    }

    private Integer parseTargetCard() {
        String v = cardField.getText().trim();
        if (v.isEmpty()) {
            alertInfo("请输入一卡通号");
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            alertInfo("一卡通号需为数字");
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
                alertInfo(obj.get("message").getAsString());
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
                // 处理 status 字段并更新显示
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
        if (amtStr.isEmpty()) { alertInfo("请输入充值金额(元)"); return; }
        BigDecimal yuan;
        try { yuan = new BigDecimal(amtStr); } catch (NumberFormatException e) { alertInfo("金额格式不正确"); return; }
        if (yuan.compareTo(BigDecimal.ZERO) <= 0) { alertInfo("金额需>0"); return; }
        BigDecimal centsBD = yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        if (centsBD.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) { alertInfo("金额过大"); return; }
        int amount = centsBD.intValueExact();
        String desc = rechargeDescField.getText().trim();
        runAsync(() -> FinanceRequestSender.rechargeFinanceCard(card, amount, desc), json -> {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.get("code").getAsInt() != 200) {
                alertInfo(obj.get("message").getAsString());
                return;
            }
            alertInfo("充值成功");
            rechargeAmtField.clear();
            rechargeDescField.clear();
            fetchCardInfo();
        });
    }

    private void alertInfo(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setHeaderText(null);
            a.setTitle("提示");
            a.showAndWait();
        });
    }

    private void updateBalanceDisplay(int cents) {
        Platform.runLater(() -> {
            BigDecimal yuan = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN).stripTrailingZeros();
            balanceValueLabel.setText(yuan.toPlainString() + "元");
            balanceValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 24px;");
        });
    }

    private void updateStatusDisplay(String status) {
        Platform.runLater(() -> {
            statusValueLabel.setText(status);
            statusValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-weight: bold; -fx-font-size: 24px;");
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
            alertInfo("网络或服务器错误" + Optional.ofNullable(task.getException()).map(ex -> ": " + ex.getMessage()).orElse(""));
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
                queryBalanceBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; -fx-background-radius: 10; -fx-padding: 12 20;");
                rechargeBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; -fx-background-radius: 10; -fx-padding: 12 24;");
            } else {
                stylePrimary(queryBalanceBtn);
                styleSuccess(rechargeBtn);
            }
        });
    }

    @FunctionalInterface
    private interface SupplierWithException<T> { T get() throws Exception; }
}