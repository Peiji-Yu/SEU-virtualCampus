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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

/**
 * 交易记录面板：显示交易历史记录
 */
class TransactionPanel extends BorderPane {
    private static final Gson GSON = new Gson();
    private static final String PRIMARY = "#4e8cff";
    private static final String TEXT = "#2a4d7b";
    private static final String SUB = "#555b66";

    private final String selfCardNumber;
    private final boolean admin;

    private TextField cardField;
    private ComboBox<String> typeFilter;
    private TableView<Transaction> table;
    private Button queryTxBtn;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter isoOutFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionPanel(String cardNumber, boolean admin) {
        this.selfCardNumber = cardNumber;
        this.admin = admin;
        initializeUI();
        fetchTransactions(null);
    }

    private void initializeUI() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12;");

        // 标题
        Label title = new Label("交易记录");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");

        // 查询控制栏
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Label cardLb = new Label("一卡通号:");
        cardLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold;");
        cardField = new TextField(selfCardNumber);
        cardField.setPromptText("输入一卡通号");
        cardField.setDisable(!admin);
        cardField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6;");

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
        queryTxBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16;");
        queryTxBtn.setOnAction(e -> {
            String sel = typeFilter.getValue();
            if (sel == null || "全部".equals(sel)) sel = null;
            fetchTransactions(sel);
        });

        controlBar.getChildren().addAll(cardLb, cardField, typeLb, typeFilter, queryTxBtn);

        // 表格
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

        // 设置单元格工厂
        amountCol.setCellFactory(col -> new TableCell<Transaction, String>() {
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
        });

        // 设置表格高度
        table.setPrefHeight(500);
        VBox.setVgrow(table, Priority.ALWAYS);

        container.getChildren().addAll(title, controlBar, table);
        setCenter(container);
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
        fetchTransactions(null);
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
                                if (raw.matches("\\d+")) {
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
                    BigDecimal yuan = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN).stripTrailingZeros();
                    String amountStr = yuan.toPlainString() + "元";
                    table.getItems().add(new Transaction(id, tp, amountStr, desc, timeDisplay));
                }
            }
        });
    }

    private String valStr(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void styleTableHeader() {
        if (table == null) return;
        Platform.runLater(() -> {
            Region headerBg = (Region) table.lookup(".column-header-background");
            if (headerBg != null) {
                headerBg.setStyle("-fx-background-color: " + PRIMARY + "; -fx-background-radius: 8 8 0 0;");
            }
            table.lookupAll(".column-header .label").forEach(n -> {
                if (n instanceof Label) {
                    ((Label) n).setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 12;");
                }
            });
        });
    }

    private void runAsync(SupplierWithException<String> supplier, java.util.function.Consumer<String> onSuccess) {
        queryTxBtn.setDisable(true);
        queryTxBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666; -fx-background-radius: 8; -fx-padding: 8 16;");

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> {
            queryTxBtn.setDisable(false);
            queryTxBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16;");
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            queryTxBtn.setDisable(false);
            queryTxBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16;");
            alertInfo("网络或服务器错误" + Optional.ofNullable(task.getException()).map(ex -> ": " + ex.getMessage()).orElse(""));
        });
        Thread th = new Thread(task, "finance-tx-req");
        th.setDaemon(true);
        th.start();
    }

    @FunctionalInterface
    private interface SupplierWithException<T> { T get() throws Exception; }
}