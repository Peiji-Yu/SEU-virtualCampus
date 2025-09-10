package Client.finance;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal; // 新增
import java.math.RoundingMode; // 新增
import java.text.SimpleDateFormat;
import java.time.LocalDateTime; // 新增
import java.time.format.DateTimeFormatter; // 新增
import java.util.*;

/**
 * 一卡通交易管理面板：
 * 学生/教师：查看自身余额、充值、查询自身交易（可按类别过滤）。
 * 管理员：可输入任意一卡通号查询余额、按类别查询其交易记录。（充值仍针对该卡号）
 * 请求/响应约定：
 *  - getFinanceCard => {code,message,data:{cardNumber,balance,status}}
 *  - rechargeFinanceCard => {code,message,data:null}
 *  - getTransactions => {code,message,data:[{transactionId,cardNumber,type,amount,description,timestamp}]}
 */
public class FinancePanel extends BorderPane {
    private static final Gson GSON = new Gson();
    private static final String BG = "#f8fbff";
    private static final String CARD = "#ffffff";
    private static final String PRIMARY = "#4e8cff";
    private static final String DANGER = "#ff6b6b";
    private static final String TEXT = "#2a4d7b";
    private static final String SUB = "#555b66";
    private static final String BORDER = "#c0c9da";

    private final String selfCardNumber; // 当前登录用户卡号
    private final boolean admin;

    private TextField cardField;
    private Label balancePrefixLabel; // 新增
    private Label balanceValueLabel; // 新增
    private ComboBox<String> typeFilter;
    private TableView<Transaction> table;
    private TextField rechargeAmtField;
    private TextField rechargeDescField;
    private Button queryBalanceBtn;
    private Button queryTxBtn;
    private Button rechargeBtn;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter isoOutFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // 新增

    public FinancePanel(String cardNumber, String userType) {
        this.selfCardNumber = cardNumber;
        this.admin = "admin".equals(userType);
        setStyle("-fx-background-color: " + BG + ";");
        setPadding(new Insets(12));
        buildUI();
        fetchCardInfo();
        fetchTransactions(null);
    }

    // ---------------- UI -----------------
    private void buildUI() {
        VBox container = new VBox(14);
        container.getChildren().add(buildHeader());
        container.getChildren().add(buildControlBar()); // 合并后的控制栏
        container.getChildren().add(buildTableCard());

        ScrollPane sp = new ScrollPane(container);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        setCenter(sp);
    }

    private Region buildHeader() {
        VBox box = new VBox(4);
        Label title = new Label("交易管理");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        Label sub = new Label(admin ? "管理员可按一卡通号查询用户交易" : "可查询余额、充值、查看交易记录");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: " + SUB + ";");
        box.getChildren().addAll(title, sub);
        return box;
    }

    private Region buildControlBar() {
        // 使用 FlowPane 允许换行，自适应窗口宽度
        FlowPane pane = new FlowPane();
        pane.setHgap(10);
        pane.setVgap(8);
        pane.setPadding(new Insets(10));
        pane.setStyle(cardStyle());

        // 一卡通号 + 查询余额
        Label cardLb = new Label("一卡通号:");
        cardField = new TextField(selfCardNumber);
        cardField.setPromptText("输入一卡通号");
        cardField.setDisable(!admin);

        queryBalanceBtn = new Button("查询余额");
        stylePrimary(queryBalanceBtn);
        queryBalanceBtn.setOnAction(e -> fetchCardInfo());

        // 余额显示：前缀 + 金额（金额蓝色）
        balancePrefixLabel = new Label("一卡通号："); // 修改前缀文字
        balancePrefixLabel.setTextFill(Color.web(TEXT));
        balanceValueLabel = new Label("--");
        balanceValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-weight: bold;");
        HBox balanceBox = new HBox(4, balancePrefixLabel, balanceValueLabel);
        balanceBox.setAlignment(Pos.CENTER_LEFT);

        // 充值区域（单位改为元）
        Label amtLb = new Label("充值金额(元):");
        rechargeAmtField = new TextField();
        rechargeAmtField.setPromptText("金额(元)");
        rechargeAmtField.setPrefWidth(90);

        Label descLb = new Label("备注:");
        rechargeDescField = new TextField();
        // 修改占位文本
        rechargeDescField.setPromptText("备注(可选)");
        rechargeDescField.setPrefWidth(140);

        rechargeBtn = new Button("充值");
        stylePrimary(rechargeBtn);
        rechargeBtn.setOnAction(e -> doRecharge());

        // 交易过滤
        Label typeLb = new Label("交易类型:");
        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("全部", "充值", "消费", "退款");
        typeFilter.getSelectionModel().selectFirst();
        // 切换类别时自动查询
        typeFilter.valueProperty().addListener((obs, oldV, newV) -> {
            String sel = newV;
            if (sel == null || "全部".equals(sel)) sel = null;
            fetchTransactions(sel);
        });

        queryTxBtn = new Button("查询交易");
        stylePrimary(queryTxBtn);
        queryTxBtn.setOnAction(e -> {
            String sel = typeFilter.getValue();
            if (sel == null || "全部".equals(sel)) sel = null;
            fetchTransactions(sel);
        });

        // 去除原先的三个分隔符，不再添加 Separator()
        pane.getChildren().addAll(
                cardLb, cardField, queryBalanceBtn, balanceBox,
                amtLb, rechargeAmtField, descLb, rechargeDescField, rechargeBtn,
                typeLb, typeFilter, queryTxBtn
        );
        return pane;
    }

    private Region buildTableCard() {
        VBox box = new VBox();
        box.setStyle(cardStyle());
        box.setPadding(new Insets(4, 4, 10, 4));

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, String> idCol = new TableColumn<>("交易ID");
        idCol.setCellValueFactory(c -> c.getValue().transactionIdProperty());
        idCol.setPrefWidth(180);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(c -> c.getValue().typeProperty());
        typeCol.setPrefWidth(60);

        TableColumn<Transaction, String> amountCol = new TableColumn<>("金额(元)");
        amountCol.setCellValueFactory(c -> c.getValue().amountProperty());
        amountCol.setPrefWidth(90);

        TableColumn<Transaction, String> descCol = new TableColumn<>("描述");
        descCol.setCellValueFactory(c -> c.getValue().descriptionProperty());

        TableColumn<Transaction, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(c -> c.getValue().timeProperty());
        timeCol.setPrefWidth(150);

        table.getColumns().setAll(idCol, typeCol, amountCol, descCol, timeCol);
        table.skinProperty().addListener((obs, o, n) -> styleTableHeader());
        styleTableHeader();

        box.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return box;
    }

    private String cardStyle() {
        return "-fx-background-color: " + CARD + "; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8,0,0,2);";
    }

    private void stylePrimary(Button b) {
        b.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-background-radius: 6;");
    }

    // --------------- Actions ----------------
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

    private void fetchCardInfo() {
        Integer card = parseTargetCard();
        if (card == null) return;
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
            } else {
                // data 为空（可能未找到卡），显示 --
                balanceValueLabel.setText("--");
            }
        });
    }

    private void fetchTransactions(String type) {
        Integer card = parseTargetCard();
        if (card == null) return;
        runAsync(() -> FinanceRequestSender.getTransactions(card, type), json -> {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.get("code").getAsInt() != 200) {
                alertInfo(obj.get("message").getAsString());
                return;
            }
            table.getItems().clear();
            JsonElement dataEl = obj.get("data");
            if (dataEl != null && dataEl.isJsonArray()) {
                JsonArray arr = dataEl.getAsJsonArray();
                for (JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject t = el.getAsJsonObject();
                    String id = valStr(t, "transactionId");
                    String tp = valStr(t, "type");
                    long amountCents = t.has("amount") && !t.get("amount").isJsonNull() ? t.get("amount").getAsLong() : 0L;
                    String desc = valStr(t, "description");
                    String timeDisplay = "";
                    if (t.has("timestamp") && !t.get("timestamp").isJsonNull()) {
                        try {
                            JsonElement tsEl = t.get("timestamp");
                            if (tsEl.isJsonPrimitive()) {
                                String raw = tsEl.getAsString();
                                if (raw.matches("\\d+")) { // 毫秒
                                    long ms = Long.parseLong(raw);
                                    timeDisplay = sdf.format(new Date(ms));
                                } else {
                                    LocalDateTime ldt = LocalDateTime.parse(raw);
                                    timeDisplay = ldt.format(isoOutFmt);
                                }
                            }
                        } catch (Exception ignore) {
                            timeDisplay = valStr(t, "timestamp");
                        }
                    }
                    // 金额转换为元（带符号），去除末尾多余0
                    java.math.BigDecimal yuan = java.math.BigDecimal.valueOf(amountCents).divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.DOWN).stripTrailingZeros();
                    String amountStr = yuan.toPlainString() + "元";
                    table.getItems().add(new Transaction(id, tp, amountStr, desc, timeDisplay));
                }
            }
        });
    }

    private void doRecharge() {
        Integer card = parseTargetCard();
        if (card == null) return;
        String amtStr = rechargeAmtField.getText().trim();
        if (amtStr.isEmpty()) { alertInfo("请输入充值金额(元)"); return; }
        BigDecimal yuan;
        try { yuan = new BigDecimal(amtStr); } catch (NumberFormatException e) { alertInfo("金额格式不正确"); return; }
        if (yuan.compareTo(BigDecimal.ZERO) <= 0) { alertInfo("金额需>0"); return; }
        // 转换为分，四舍五入到分
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
            fetchTransactions(null);
        });
    }

    private String valStr(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void updateBalanceDisplay(int cents) { // 新增：分转元
        BigDecimal yuan = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN).stripTrailingZeros();
        balanceValueLabel.setText(yuan.toPlainString() + "元");
    }

    private void styleTableHeader() {
        if (table == null) return;
        Platform.runLater(() -> {
            Region headerBg = (Region) table.lookup(".column-header-background");
            if (headerBg != null) {
                headerBg.setStyle("-fx-background-color: " + PRIMARY + ";");
            }
            // 设置每个列标题文本颜色
            table.lookupAll(".column-header .label").forEach(n -> {
                if (n instanceof Label) {
                    ((Label) n).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                }
            });
        });
    }

    // 统一异步执行（简单封装）
    private void runAsync(SupplierWithException<String> supplier, java.util.function.Consumer<String> onSuccess) {
        disableOps(true);
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> { disableOps(false); onSuccess.accept(task.getValue()); });
        task.setOnFailed(e -> { disableOps(false); alertInfo("网络或服务器错误" + Optional.ofNullable(task.getException()).map(ex -> ": " + ex.getMessage()).orElse("")); });
        Thread th = new Thread(task, "finance-req"); th.setDaemon(true); th.start();
    }

    private void disableOps(boolean disable) {
        Platform.runLater(() -> {
            queryBalanceBtn.setDisable(disable);
            queryTxBtn.setDisable(disable);
            rechargeBtn.setDisable(disable);
        });
    }

    @FunctionalInterface
    private interface SupplierWithException<T> { T get() throws Exception; }

    // ----------------- Transaction Model -----------------
    public static class Transaction {
        private final StringProperty transactionId = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty amount = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();
        private final StringProperty time = new SimpleStringProperty();
        public Transaction(String id, String tp, String amt, String desc, String time) {
            this.transactionId.set(id); this.type.set(tp); this.amount.set(amt); this.description.set(desc); this.time.set(time);
        }
        public StringProperty transactionIdProperty(){ return transactionId; }
        public StringProperty typeProperty(){ return type; }
        public StringProperty amountProperty(){ return amount; }
        public StringProperty descriptionProperty(){ return description; }
        public StringProperty timeProperty(){ return time; }
    }
}
