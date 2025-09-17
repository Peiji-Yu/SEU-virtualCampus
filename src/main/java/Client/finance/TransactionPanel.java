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
    private static final String PRIMARY = "#176B3A";
    private static final String PRIMARY_LIGHT = "#D4EDDA";
    private static final String SUCCESS = "#28a745";
    private static final String DANGER = "#dc3545";
    private static final String TEXT = "#2a4d7b";
    private static final String SUB = "#6c757d";
    private static final String BORDER = "#e9ecef";
    private static final String BACKGROUND = "#f8f9fa";

    private final String selfCardNumber;
    private final boolean admin;

    private TextField cardField;
    private ComboBox<String> typeFilter;
    private VBox recordsContainer;
    private Button queryTxBtn;
    private ProgressIndicator loadingIndicator;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter isoOutFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionPanel(String cardNumber, boolean admin) {
        this.selfCardNumber = cardNumber;
        this.admin = admin;
        initializeUI();
        fetchTransactions(null);
    }

    private void initializeUI() {
        // 主容器
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setStyle("-fx-background-color: #F8F9FA;");

        // 标题区域
        HBox titleBox = new HBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 10, 0));

        Label title = new Label("交易记录");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #2c3e50;");

        // 不显示标题左侧圆点，只添加标题文字
        titleBox.getChildren().add(title);

        // 查询控制栏
        HBox controlBar = new HBox(16);
        controlBar.setAlignment(Pos.CENTER_LEFT);
        controlBar.setPadding(new Insets(16));
        controlBar.setStyle("-fx-background-color: " + PRIMARY_LIGHT + "; -fx-background-radius: 12;");

        Label cardLb = new Label("一卡通号:");
        cardLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: 600; -fx-font-size: 14px;");

        cardField = new TextField(selfCardNumber);
        cardField.setPromptText("输入一卡通号");
        cardField.setDisable(!admin);
        cardField.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: " + BORDER + "; -fx-padding: 10 14; -fx-font-size: 14px;");
        cardField.setPrefWidth(120);

        Label typeLb = new Label("交易类型:");
        typeLb.setStyle("-fx-text-fill: " + SUB + "; -fx-font-size: 14px;");

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("全部", "充值", "消费", "退款");
        typeFilter.getSelectionModel().selectFirst();
        typeFilter.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: " + BORDER + "; -fx-pref-width: 100;");
        typeFilter.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setStyle("-fx-padding: 8 12; -fx-font-size: 14px;");
            }
        });
        typeFilter.valueProperty().addListener((obs, oldV, newV) -> {
            String sel = newV;
            if (sel == null || "全部".equals(sel)) {
                sel = null;
            }
            fetchTransactions(sel);
        });

        queryTxBtn = new Button("查询交易");
        queryTxBtn.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20; -fx-font-weight: 600; -fx-font-size: 14px;");
        queryTxBtn.setOnAction(e -> {
            String sel = typeFilter.getValue();
            if (sel == null || "全部".equals(sel)) {
                sel = null;
            }
            fetchTransactions(sel);
        });

        // 加载指示器
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(20, 20);
        loadingIndicator.setStyle("-fx-progress-color: " + PRIMARY + ";");

        controlBar.getChildren().addAll(cardLb, cardField, typeLb, typeFilter, queryTxBtn, loadingIndicator);

        // 交易记录容器
        recordsContainer = new VBox(15);
        recordsContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(recordsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: white;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // 空状态提示
        Label emptyLabel = new Label("暂无交易记录");
        emptyLabel.setStyle("-fx-text-fill: " + SUB + "; -fx-font-size: 16px; -fx-padding: 40;");
        recordsContainer.getChildren().add(emptyLabel);

        container.getChildren().addAll(titleBox, controlBar, scrollPane);
        setCenter(container);

        // 设置整体背景
        setStyle("-fx-background-color: " + BACKGROUND + "; -fx-padding: 20;");
    }

    private Integer parseTargetCard() {
        String v = cardField.getText().trim();
        if (v.isEmpty()) {
            showAlert("提示", "请输入一卡通号", Alert.AlertType.INFORMATION);
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            showAlert("错误", "一卡通号需为数字", Alert.AlertType.WARNING);
            return null;
        }
    }

    public void refreshData() {
        fetchTransactions(null);
    }

    private void fetchTransactions(String type) {
        Integer card = parseTargetCard();
        if (card == null) {
            return;
        }

        runAsync(() -> FinanceRequestSender.getTransactions(card, type), json -> {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj.get("code").getAsInt() != 200) {
                showAlert("错误", obj.get("message").getAsString(), Alert.AlertType.ERROR);
                return;
            }

            Platform.runLater(() -> {
                recordsContainer.getChildren().clear();

                JsonElement dataEl = obj.get("data");
                if (dataEl != null && dataEl.isJsonArray()) {
                    JsonArray arr = dataEl.getAsJsonArray();
                    if (arr.size() == 0) {
                        Label emptyLabel = new Label("暂无交易记录");
                        emptyLabel.setStyle("-fx-text-fill: " + SUB + "; -fx-font-size: 16px; -fx-padding: 40;");
                        recordsContainer.getChildren().add(emptyLabel);
                        return;
                    }

                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) {
                            continue;
                        }
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

                        // 创建交易记录卡片
                        VBox transactionCard = createTransactionCard(id, tp, amountStr, desc, timeDisplay);
                        recordsContainer.getChildren().add(transactionCard);
                    }
                }
            });
        });
    }

    private VBox createTransactionCard(String id, String type, String amount, String description, String time) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: " + BORDER + "; -fx-border-radius: 8; -fx-border-width: 1;");

        // 交易基本信息区域
        HBox summaryBox = new HBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setSpacing(15);

        // 交易类型图标
        Label typeIcon = new Label();
        typeIcon.setStyle("-fx-font-size: 20px; -fx-min-width: 40px; -fx-alignment: CENTER;");

        // 根据交易类型设置不同的样式
        String typeColor = TEXT;
        String amountColor = TEXT;
        String iconText = "💰";

        if ("充值".equals(type) || "退款".equals(type)) {
            typeColor = SUCCESS;
            amountColor = SUCCESS;
            iconText = "⬆️";
        } else if ("消费".equals(type)) {
            typeColor = DANGER;
            amountColor = DANGER;
            iconText = "⬇️";
        }

        typeIcon.setText(iconText);

        // 交易详细信息
        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label idLabel = new Label("交易ID: " + id);
        idLabel.setStyle("-fx-text-fill: " + SUB + "; -fx-font-size: 12px;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-weight: 500; -fx-font-size: 14px; -fx-text-fill: " + TEXT + ";");

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: " + SUB + "; -fx-font-size: 12px;");

        infoBox.getChildren().addAll(idLabel, descLabel, timeLabel);

        // 金额和类型区域
        VBox amountBox = new VBox(4);
        amountBox.setAlignment(Pos.CENTER_RIGHT);

        Label amountLabel = new Label(amount);
        amountLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 16px; -fx-text-fill: " + amountColor + ";");

        Label typeLabel = new Label(type);
        typeLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 12px; -fx-text-fill: " + typeColor + "; " +
                "-fx-padding: 4 8; -fx-background-color: " + (typeColor.equals(SUCCESS) ? "#d4edda" :
                typeColor.equals(DANGER) ? "#f8d7da" : "#e2e3e5") + "; " +
                "-fx-background-radius: 12;");

        amountBox.getChildren().addAll(amountLabel, typeLabel);

        summaryBox.getChildren().addAll(typeIcon, infoBox, amountBox);
        card.getChildren().add(summaryBox);

        return card;
    }

    private String valStr(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type, message, ButtonType.OK);
            alert.setHeaderText(title);
            alert.setTitle(title);
            alert.showAndWait();
        });
    }

    private void runAsync(SupplierWithException<String> supplier, java.util.function.Consumer<String> onSuccess) {
        queryTxBtn.setDisable(true);
        queryTxBtn.setStyle("-fx-background-color: #a0aec0; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");
        loadingIndicator.setVisible(true);

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> {
            queryTxBtn.setDisable(false);
            queryTxBtn.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");
            loadingIndicator.setVisible(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            queryTxBtn.setDisable(false);
            queryTxBtn.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10 20;");
            loadingIndicator.setVisible(false);
            showAlert("错误", "网络或服务器错误" + Optional.ofNullable(task.getException()).map(ex -> ": " + ex.getMessage()).orElse(""), Alert.AlertType.ERROR);
        });
        Thread th = new Thread(task, "finance-tx-req");
        th.setDaemon(true);
        th.start();
    }

    @FunctionalInterface
    private interface SupplierWithException<T> { T get() throws Exception; }
}