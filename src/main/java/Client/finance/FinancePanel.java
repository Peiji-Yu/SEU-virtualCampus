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
    private static final String PRIMARY = "#4e8cff";
    private static final String TEXT = "#2a4d7b";
    private static final String SUB = "#555b66";

    private final String selfCardNumber; // 当前登录用户卡号
    private final boolean admin;

    private TextField cardField;
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
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0; -fx-border-width: 0;");
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(sp);
    }

    private Region buildHeader() {
        VBox box = new VBox(6);
        box.setPadding(new Insets(0, 0, 16, 0));
        Label title = new Label("交易管理");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        Label sub = new Label(admin ? "管理员可按一卡通号查询用户交易" : "可查询余额、充值、查看交易记录");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: " + SUB + ";");
        box.getChildren().addAll(title, sub);
        return box;
    }

    private Region buildControlBar() {
        VBox container = new VBox(16);
        container.setPadding(new Insets(16));
        container.setStyle(cardStyle() + " -fx-background-color: #ffffff;");

        // 第一行：卡号查询和余额显示
        HBox firstRow = new HBox(12);
        firstRow.setAlignment(Pos.CENTER_LEFT);

        Label cardLb = new Label("一卡通号:");
        cardLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold;");
        cardField = new TextField(selfCardNumber);
        cardField.setPromptText("输入一卡通号");
        cardField.setDisable(!admin);
        cardField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6;");

        queryBalanceBtn = new Button("查询余额");
        stylePrimary(queryBalanceBtn);
        queryBalanceBtn.setOnAction(e -> fetchCardInfo());

        // 余额显示区域
        HBox balanceBox = new HBox(6);
        balanceBox.setAlignment(Pos.CENTER_LEFT);
        balanceBox.setStyle("-fx-background-color: #f0f7ff; -fx-background-radius: 8; -fx-padding: 8 12;");
        Label balancePrefixLabel = new Label("当前余额:");
        balancePrefixLabel.setStyle("-fx-text-fill: " + SUB + ";");
        balanceValueLabel = new Label("--");
        balanceValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-weight: bold; -fx-font-size: 16px;");
        balanceBox.getChildren().addAll(balancePrefixLabel, balanceValueLabel);

        firstRow.getChildren().addAll(cardLb, cardField, queryBalanceBtn, balanceBox);
        HBox.setHgrow(balanceBox, Priority.ALWAYS);

        // 第二行：充值操作
        HBox secondRow = new HBox(12);
        secondRow.setAlignment(Pos.CENTER_LEFT);

        Label rechargeTitle = new Label("充值操作");
        rechargeTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label amtLb = new Label("金额(元):");
        amtLb.setStyle("-fx-text-fill: " + SUB + ";");
        rechargeAmtField = new TextField();
        rechargeAmtField.setPromptText("输入金额");
        rechargeAmtField.setPrefWidth(100);
        rechargeAmtField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6;");

        Label descLb = new Label("备注:");
        descLb.setStyle("-fx-text-fill: " + SUB + ";");
        rechargeDescField = new TextField();
        rechargeDescField.setPromptText("备注信息(可选)");
        rechargeDescField.setPrefWidth(160);
        rechargeDescField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6;");

        rechargeBtn = new Button("立即充值");
        stylePrimary(rechargeBtn);
        rechargeBtn.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 16;");
        rechargeBtn.setOnAction(e -> doRecharge());

        secondRow.getChildren().addAll(rechargeTitle, amtLb, rechargeAmtField, descLb, rechargeDescField, rechargeBtn);

        // 第三行：交易查询
        HBox thirdRow = new HBox(12);
        thirdRow.setAlignment(Pos.CENTER_LEFT);

        Label queryTitle = new Label("交易查询");
        queryTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label typeLb = new Label("类型:");
        typeLb.setStyle("-fx-text-fill: " + SUB + ";");
        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("全部", "充值", "消费", "退款");
        typeFilter.getSelectionModel().selectFirst();
        typeFilter.setStyle("-fx-background-radius: 6;");
        typeFilter.valueProperty().addListener((obs, oldV, newV) -> {
            String sel = newV;
            if (sel == null || "全部".equals(sel)) sel = null;
            fetchTransactions(sel);
        });

        queryTxBtn = new Button("查询交易");
        stylePrimary(queryTxBtn);
        queryTxBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16;");
        queryTxBtn.setOnAction(e -> {
            String sel = typeFilter.getValue();
            if (sel == null || "全部".equals(sel)) sel = null;
            fetchTransactions(sel);
        });

        thirdRow.getChildren().addAll(queryTitle, typeLb, typeFilter, queryTxBtn);

        //container.getChildren().addAll(firstRow, createSeparator(), secondRow, createSeparator(), thirdRow);
        // 已移除分割线，直接按顺序添加行
        container.getChildren().addAll(firstRow, secondRow, thirdRow);
        return container;
    }

    private Region buildTableCard() {
        VBox box = new VBox(8);
        box.setStyle(cardStyle() + " -fx-background-color: #ffffff;");
        box.setPadding(new Insets(16));

        // 表格标题
        Label tableTitle = new Label("交易记录");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + "; -fx-padding: 0 0 8 0;");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e9ecef; -fx-border-width: 1;");

        TableColumn<Transaction, String> idCol = new TableColumn<>("交易ID");
        idCol.setCellValueFactory(c -> c.getValue().transactionIdProperty());
        idCol.setPrefWidth(200);
        idCol.setStyle("-fx-alignment: CENTER_LEFT;");

        TableColumn<Transaction, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(c -> c.getValue().typeProperty());
        typeCol.setPrefWidth(80);
        typeCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Transaction, String> amountCol = new TableColumn<>("金额(元)");
        amountCol.setCellValueFactory(c -> c.getValue().amountProperty());
        amountCol.setPrefWidth(100);
        amountCol.setStyle("-fx-alignment: CENTER_RIGHT;");

        TableColumn<Transaction, String> descCol = new TableColumn<>("描述");
        descCol.setCellValueFactory(c -> c.getValue().descriptionProperty());
        descCol.setStyle("-fx-alignment: CENTER_LEFT;");

        TableColumn<Transaction, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(c -> c.getValue().timeProperty());
        timeCol.setPrefWidth(180);
        timeCol.setStyle("-fx-alignment: CENTER;");

        table.getColumns().setAll(idCol, typeCol, amountCol, descCol, timeCol);
        table.skinProperty().addListener((obs, o, n) -> styleTableHeader());
        styleTableHeader();

        // 设置表格行样式（仅设置交替背景，不访问子节点）
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (getIndex() % 2 == 0) setStyle("-fx-background-color: #f8f9fa;");
                    else setStyle("-fx-background-color: #ffffff;");
                }
            }
        });

        // 为各列设置单元格工厂，金额列根据交易类型着色
        // （避免直接访问 TableRow.getChildren()，使用 getTableRow().getItem() 来获取当前行的数据）
        idCol.setCellFactory(new javafx.util.Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>>() {
            @Override public TableCell<Transaction, String> call(TableColumn<Transaction, String> col) {
                return new TableCell<Transaction, String>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); }
                        else { setText(item); setStyle("-fx-padding: 8 12; -fx-font-size: 13px; -fx-text-fill: #495057; -fx-alignment: CENTER-LEFT;"); }
                    }
                };
            }
        });

        typeCol.setCellFactory(new javafx.util.Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>>() {
            @Override public TableCell<Transaction, String> call(TableColumn<Transaction, String> col) {
                return new TableCell<Transaction, String>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); }
                        else { setText(item); setStyle("-fx-padding: 8 12; -fx-font-size: 13px; -fx-text-fill: #495057; -fx-alignment: CENTER;"); }
                    }
                };
            }
        });

        amountCol.setCellFactory(new javafx.util.Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>>() {
            @Override public TableCell<Transaction, String> call(TableColumn<Transaction, String> col) {
                return new TableCell<Transaction, String>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); }
                        else {
                            setText(item);
                            Transaction t = getTableRow() == null ? null : getTableRow().getItem();
                            String baseStyle = "-fx-padding: 8 12; -fx-font-size: 13px; -fx-alignment: CENTER-RIGHT;";
                            if (t != null && t.typeProperty().get() != null) {
                                String tp = t.typeProperty().get();
                                if ("充值".equals(tp) || "退款".equals(tp)) {
                                    setStyle(baseStyle + " -fx-text-fill: #28a745; -fx-font-weight: bold;");
                                } else if ("消费".equals(tp)) {
                                    setStyle(baseStyle + " -fx-text-fill: #dc3545; -fx-font-weight: bold;");
                                } else {
                                    setStyle(baseStyle + " -fx-text-fill: #495057;");
                                }
                            } else {
                                setStyle(baseStyle + " -fx-text-fill: #495057;");
                            }
                        }
                    }
                };
            }
        });

        descCol.setCellFactory(new javafx.util.Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>>() {
            @Override public TableCell<Transaction, String> call(TableColumn<Transaction, String> col) {
                return new TableCell<Transaction, String>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); }
                        else { setText(item); setStyle("-fx-padding: 8 12; -fx-font-size: 13px; -fx-text-fill: #495057; -fx-alignment: CENTER-LEFT;"); }
                    }
                };
            }
        });

        timeCol.setCellFactory(new javafx.util.Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>>() {
            @Override public TableCell<Transaction, String> call(TableColumn<Transaction, String> col) {
                return new TableCell<Transaction, String>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) { setText(null); setStyle(""); }
                        else { setText(item); setStyle("-fx-padding: 8 12; -fx-font-size: 13px; -fx-text-fill: #495057; -fx-alignment: CENTER;"); }
                    }
                };
            }
        });

        // 设置表格边框圆角
        Region tableRegion = (Region) table.lookup(".table-view");
        if (tableRegion != null) {
            tableRegion.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e9ecef; -fx-border-width: 1;");
        }

        // 设置表头分隔线样式
        table.lookupAll(".column-header").forEach(n -> {
            if (n instanceof Region) {
                ((Region) n).setStyle("-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 0 1 0 0;");
            }
        });

        // 表格容器
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPrefHeight(500);

        box.getChildren().addAll(tableTitle, table);
        return box;
    }

    private String cardStyle() {
        return "-fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 3);";
    }

    private void stylePrimary(Button b) {
        b.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 16; -fx-cursor: hand;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #3a7be0; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 16; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 8 16; -fx-cursor: hand;"));
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

    private void updateBalanceDisplay(int cents) {
        BigDecimal yuan = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN).stripTrailingZeros();
        balanceValueLabel.setText(yuan.toPlainString() + "元");
        // 添加余额更新动画效果
        balanceValueLabel.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-weight: bold; -fx-font-size: 16px;");
    }

    private void styleTableHeader() {
        if (table == null) return;
        Platform.runLater(() -> {
            Region headerBg = (Region) table.lookup(".column-header-background");
            if (headerBg != null) {
                headerBg.setStyle("-fx-background-color: " + PRIMARY + "; -fx-background-radius: 8 8 0 0;");
            }
            // 设置每个列标题文本颜色
            table.lookupAll(".column-header .label").forEach(n -> {
                if (n instanceof Label) {
                    ((Label) n).setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 12;");
                }
            });
            // 设置表格边框圆角
            Region tableRegion = (Region) table.lookup(".table-view");
            if (tableRegion != null) {
                tableRegion.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e9ecef; -fx-border-width: 1;");
            }
            // 设置表头分隔线样式
            table.lookupAll(".column-header").forEach(n -> {
                if (n instanceof Region) {
                    ((Region) n).setStyle("-fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 0 1 0 0;");
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
            queryTxBtn.setDisable(disable);
            rechargeBtn.setDisable(disable);
            if (disable) {
                queryBalanceBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; -fx-background-radius: 8; -fx-padding: 8 16;");
                queryTxBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; -fx-background-radius: 8; -fx-padding: 8 16;");
                rechargeBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; -fx-background-radius: 8; -fx-padding: 8 16;");
            } else {
                stylePrimary(queryBalanceBtn);
                queryTxBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16;");
                stylePrimary(rechargeBtn);
            }
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

