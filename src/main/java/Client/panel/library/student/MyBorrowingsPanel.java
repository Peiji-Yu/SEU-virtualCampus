package Client.panel.library.student;

import Client.ClientNetworkHelper;
import Client.model.library.BookRecord;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Client.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MyBorrowingsPanel extends BorderPane {
    private final String cardNumber;
    private final ObservableList<BookRecord> bookRecords = FXCollections.observableArrayList();
    private VBox recordsContainer;
    private TextField searchField;
    private Label resultsLabel;
    private Gson gson;

    public MyBorrowingsPanel(String cardNumber) {
        this.cardNumber = cardNumber;

        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadBorrowedBooks();
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("我的借阅");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查看和管理当前借阅的图书");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // 搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(0, 0, 5, 0));
        searchBox.setStyle("-fx-background-color: white;");

        // 搜索框和按钮在同一行
        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        searchField = createStyledTextField("输入书名搜索借阅记录");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> searchBooks());

        searchRow.getChildren().addAll(searchField, searchButton);

        // 结果标签
        resultsLabel = new Label("找到 0 条借阅记录");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(headtitleBox, searchRow, resultsLabel);
        topContainer.getChildren().addAll(searchBox);
        setTop(topContainer);

        // 中心借阅记录展示区域
        recordsContainer = new VBox(15);
        recordsContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(recordsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: white;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scrollPane);
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-font-size: 16px; -fx-pref-height: 45px; " +
                "-fx-background-radius: 5; -fx-border-radius: 5; " +
                "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent;" +
                "-fx-padding: 0 10px;");
        return field;
    }

    public void loadBorrowedBooks() {
        new Thread(() -> {
            try {
                // 构建获取借阅记录请求
                Map<String, Object> data = new HashMap<>();
                data.put("userId", Integer.parseInt(cardNumber));
                Request request = new Request("getOwnRecords", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取借阅记录列表
                    Type recordListType = new TypeToken<List<BookRecord>>(){}.getType();
                    List<BookRecord> recordList = gson.fromJson(gson.toJson(responseMap.get("data")), recordListType);

                    bookRecords.setAll(recordList);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        displayRecords(bookRecords);
                    });
                } else {
                    System.err.println("加载失败: " + responseMap.get("message"));
                }
            } catch (Exception e) {
                System.err.println("通信错误: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void searchBooks() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            displayRecords(bookRecords);
            return;
        }

        List<BookRecord> filteredRecords = new ArrayList<>();
        for (BookRecord record : bookRecords) {
            if (record.getName() != null && record.getName().toLowerCase().contains(keyword)) {
                filteredRecords.add(record);
            }
        }

        displayRecords(filteredRecords);
    }

    private void displayRecords(List<BookRecord> recordList) {
        Platform.runLater(() -> {
            recordsContainer.getChildren().clear();

            if (recordList.isEmpty()) {
                resultsLabel.setText("找到 0 条借阅记录");
                return;
            }

            for (BookRecord record : recordList) {
                VBox recordCard = createRecordCard(record);
                recordsContainer.getChildren().add(recordCard);
            }

            resultsLabel.setText("找到 " + recordList.size() + " 条借阅记录");
        });
    }

    private VBox createRecordCard(BookRecord record) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");

        // 图书基本信息区域
        HBox summaryBox = new HBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setSpacing(15);

        // 图书基本信息
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(record.getName() != null ? record.getName() : "未知书籍");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label authorLabel = new Label("副本ID: " + record.getUuid());
        authorLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(nameLabel, authorLabel);

        // 借阅状态信息
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        Label borrowLabel = new Label("借阅时间: " +
                (record.getBorrowTime() != null ? record.getBorrowTime().toString() : "未知"));
        borrowLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

        Label dueLabel = new Label("应还时间: " +
                (record.getDueTime() != null ? record.getDueTime().toString() : "未知"));
        dueLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

        statusBox.getChildren().addAll(borrowLabel, dueLabel);

        summaryBox.getChildren().addAll(infoBox, statusBox);

        // 续借按钮区域 - 放在右下角
        HBox actionBox = new HBox();
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button renewBtn = new Button("续借");
        renewBtn.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 80px; -fx-pref-height: 25px; -fx-background-radius: 5;");
        renewBtn.setOnAction(e -> renewBook(record.getUuid()));

        // 检查是否可以续借
        boolean canRenew = canRenewBook(record);
        renewBtn.setDisable(!canRenew);
        if (!canRenew) {
            renewBtn.setStyle("-fx-background-color: #cccccc; " +
                    "-fx-text-fill: #666666; -fx-font-weight: bold; -fx-font-size: 14px; " +
                    "-fx-pref-width: 80px; -fx-pref-height: 25px; -fx-background-radius: 5;");
        }

        actionBox.getChildren().add(renewBtn);

        card.getChildren().addAll(summaryBox, actionBox);
        return card;
    }

    // 检查是否可以续借
    private boolean canRenewBook(BookRecord record) {
        if (record.getDueTime() == null) {
            return false;
        }

        // 检查是否距还书时间小于3天
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), record.getDueTime());
        if (daysUntilDue < 3) {
            return false;
        }

        // 检查是否已经续借过
        if (record.getBorrowTime() != null) {
            long borrowPeriod = ChronoUnit.DAYS.between(record.getBorrowTime(), record.getDueTime());
            // 如果借阅周期超过35天（30天+缓冲），则认为已经续借过
            if (borrowPeriod > 35) {
                return false;
            }
        }

        return true;
    }

    private void renewBook(String uuid) {
        new Thread(() -> {
            try {
                // 构建续借请求
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", uuid);
                Request request = new Request("renewBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        showSuccessAlert("续借成功", "书籍续借成功，借期延长30天");
                        // 自动刷新借阅记录列表
                        loadBorrowedBooks();
                    });
                } else {
                    System.err.println("续借失败: " + responseMap.get("message"));
                }
            } catch (Exception e) {
                System.err.println("续借过程中发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 设置对话框样式 - 纯白背景，无图标，主题色按钮
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-font-size: 14px;");

        // 获取按钮并设置样式
        ButtonType okButton = alert.getButtonTypes().get(0);
        Button okButtonNode = (Button) dialogPane.lookupButton(okButton);
        okButtonNode.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold;");

        alert.showAndWait();
    }
}