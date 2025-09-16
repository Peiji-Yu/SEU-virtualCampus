package Client.library.student;

import Client.ClientNetworkHelper;
import Client.library.model.BookRecord;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
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
import java.util.*;

public class MyBorrowingsPanel extends BorderPane {
    private final String cardNumber;
    private final ObservableList<BookRecord> bookRecords = FXCollections.observableArrayList();
    private VBox recordsContainer;
    private TextField searchField;
    private Label statusLabel;
    private Label resultsLabel;
    private Button refreshBtn;

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
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(20);
        topContainer.setPadding(new Insets(0, 0, 20, 0));

        // 标题
        Label titleLabel = new Label("我的借阅");
        titleLabel.setFont(Font.font(24));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 顶部搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(20));
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // 搜索框
        searchField = new TextField();
        searchField.setPromptText("输入书籍名称搜索...");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 16px; -fx-padding: 0 10px;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("搜索");
        searchBtn.setPrefSize(100, 40);
        searchBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #3498db; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> searchBooks());

        refreshBtn = new Button("刷新");
        refreshBtn.setPrefSize(100, 40);
        refreshBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #2ecc71; -fx-text-fill: white;");
        refreshBtn.setOnAction(e -> loadBorrowedBooks());

        searchBar.getChildren().addAll(searchField, searchBtn, refreshBtn);

        searchBox.getChildren().addAll(searchBar);
        topContainer.getChildren().addAll(titleLabel, searchBox);
        setTop(topContainer);

        // 结果数量标签
        resultsLabel = new Label("加载中...");
        resultsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 0 0 10 0;");

        // 中心借阅记录展示区域
        recordsContainer = new VBox(15);
        recordsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(recordsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(resultsLabel, scrollPane);
        setCenter(centerBox);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    public void loadBorrowedBooks() {
        setStatus("加载借阅记录中...", "info");
        refreshBtn.setDisable(true);

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
                        setStatus("成功加载 " + recordList.size() + " 条借阅记录", "success");
                        refreshBtn.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("加载失败: " + responseMap.get("message"), "error");
                        refreshBtn.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage(), "error");
                    refreshBtn.setDisable(false);
                });
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
                Label emptyLabel = new Label("没有找到借阅记录");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 20;");
                recordsContainer.getChildren().add(emptyLabel);
                resultsLabel.setText("找到 0 条借阅记录");
                return;
            }

            for (BookRecord record : recordList) {
                HBox recordCard = createRecordCard(record);
                recordsContainer.getChildren().add(recordCard);
            }

            resultsLabel.setText("找到 " + recordList.size() + " 条借阅记录");
        });
    }

    private HBox createRecordCard(BookRecord record) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        card.setAlignment(Pos.CENTER_LEFT);

        // 左侧书籍信息区域
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label titleLabel = new Label(record.getName() != null ? record.getName() : "未知书籍");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label uuidLabel = new Label("副本ID: " + record.getUuid());
        uuidLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        Label borrowLabel = new Label("借阅时间: " +
                (record.getBorrowTime() != null ? record.getBorrowTime().toString() : "未知"));
        borrowLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        Label dueLabel = new Label("应还时间: " +
                (record.getDueTime() != null ? record.getDueTime().toString() : "未知"));
        dueLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(titleLabel, uuidLabel, borrowLabel, dueLabel);

        // 右侧续借按钮
        Button renewBtn = new Button("续借");
        renewBtn.setPrefSize(80, 40);
        renewBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #3498db; -fx-text-fill: white;");
        renewBtn.setOnAction(e -> renewBook(record.getUuid()));

        card.getChildren().addAll(infoBox, renewBtn);

        return card;
    }

    private void renewBook(String uuid) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("续借中...", "info"));

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
                        setStatus("续借成功", "success");
                        showAlert("成功", "书籍续借成功");
                        // 刷新借阅记录列表
                        loadBorrowedBooks();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("续借失败: " + responseMap.get("message"), "error");
                        showAlert("错误", "续借失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("续借错误: " + e.getMessage(), "error");
                    showAlert("错误", "续借过程中发生错误: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 设置对话框样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 14px;");

        alert.showAndWait();
    }

    private void setStatus(String message, String type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            switch (type) {
                case "error":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
                    break;
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 14px;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
                    break;
            }
        });
    }
}