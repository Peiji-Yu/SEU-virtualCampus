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
import javafx.scene.text.Font;
/**
 * 交易记录面板：显示交易历史记录
 */
class TransactionPanel extends BorderPane {
    private static final Gson GSON = new Gson();
    private static final String PRIMARY = "#176B3A";
    private static final String PRIMARY_LIGHT = "#e8f5ee";
    private static final String SUCCESS = "#28a745";
    private static final String DANGER = "#dc3545";
    private static final String TEXT = "#2c3e50";
    private static final String SUB = "#6c757d";
    private static final String BORDER = "#e9ecef";
    private static final String BACKGROUND = "white";

    private final String selfCardNumber;
    private final boolean admin;

    private TextField cardField;
    private ComboBox<String> typeFilter;
    private VBox recordsContainer;
    private Button queryTxBtn;
    private ProgressIndicator loadingIndicator;
    private Label resultsLabel;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter isoOutFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionPanel(String cardNumber, boolean admin) {
        this.selfCardNumber = cardNumber;
        this.admin = admin;
        initializeUI();
        fetchTransactions(null);
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: " + BACKGROUND + ";");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("交易记录");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查看您的交易历史");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // 查询区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(0, 0, 5, 0));
        searchBox.setStyle("-fx-background-color: white;");

        // 查询控制栏
        HBox controlBar = new HBox(20);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        Label cardLb = new Label("一卡通号:");
        cardLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: 600; -fx-font-size: 14px;");

        cardField = createStyledTextField("输入一卡通号");
        cardField.setText(selfCardNumber);
        cardField.setDisable(!admin);
        cardField.setPrefWidth(180);

        Label typeLb = new Label("交易类型:");
        typeLb.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: 600; -fx-font-size: 14px;");

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("全部", "充值", "消费", "退款");
        typeFilter.getSelectionModel().selectFirst();
        typeFilter.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-border-color: " + BORDER + "; -fx-pref-width: 120; -fx-pref-height: 40px; " +
                "-fx-padding: 0 10px; -fx-font-size: 14px;");

        typeFilter.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
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

        queryTxBtn = new Button("查询");
        queryTxBtn.setStyle("-fx-background-color: " + PRIMARY + "; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-min-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5;");
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
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        controlBar.getChildren().addAll(cardLb, cardField, typeLb, typeFilter, spacer, queryTxBtn, loadingIndicator);

        // 结果标签
        resultsLabel = new Label("找到 0 条交易记录");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(headtitleBox, controlBar, resultsLabel);
        topContainer.getChildren().addAll(searchBox);
        setTop(topContainer);

        // 交易记录容器
        recordsContainer = new VBox(15);
        recordsContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(recordsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: white;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scrollPane);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 16px; -fx-pref-height: 40px; " +
                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-focus-color: " + PRIMARY + "; -fx-faint-focus-color: transparent;" +
                "-fx-padding: 0 10px; -fx-border-color: " + BORDER + ";");
        return field;
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
                        resultsLabel.setText("找到 0 条交易记录");
                        return;
                    }

                    resultsLabel.setText("找到 " + arr.size() + " 条交易记录");

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
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");

        // 交易基本信息区域
        HBox summaryBox = new HBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setSpacing(15);

        // 交易类型图标
        Label typeIcon = new Label();
        typeIcon.setStyle("-fx-font-size: 30px; -fx-min-width: 40px; -fx-alignment: CENTER;");

        // 根据交易类型设置不同的样式
        String typeColor = TEXT;
        String amountColor = TEXT;
        String iconText = "💰";

        if ("充值".equals(type) || "退款".equals(type)) {
            typeColor = SUCCESS;
            amountColor = SUCCESS;
            iconText = "+";
        } else if ("消费".equals(type)) {
            typeColor = DANGER;
            amountColor = DANGER;
            iconText = "-";
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
        queryTxBtn.setStyle("-fx-background-color: " + PRIMARY + "; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-min-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5;");
        loadingIndicator.setVisible(true);

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception { return supplier.get(); }
        };
        task.setOnSucceeded(e -> {
            queryTxBtn.setDisable(false);
            queryTxBtn.setStyle("-fx-background-color: " + PRIMARY + "; " +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                    "-fx-min-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5;");
            loadingIndicator.setVisible(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            queryTxBtn.setDisable(false);
            queryTxBtn.setStyle("-fx-background-color: " + PRIMARY + "; " +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                    "-fx-min-width: 120px; -fx-pref-height: 40px; -fx-background-radius: 5;");
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