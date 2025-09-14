package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.library.util.model.BookRecord;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReturnBookPanel extends BorderPane {
    private TextField cardNumberField;
    private Button searchButton;
    private Button refreshButton;
    private Label statusLabel;
    private Label resultsLabel;
    private VBox recordsContainer;

    private Gson gson;
    private List<BookRecord> allBookRecords;

    public ReturnBookPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(20);
        topContainer.setPadding(new Insets(0, 0, 20, 0));

        // 标题
        Label titleLabel = new Label("还书办理");
        titleLabel.setFont(Font.font(24));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(20));
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        // 搜索框和按钮
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        cardNumberField = new TextField();
        cardNumberField.setPromptText("请输入一卡通号");
        cardNumberField.setPrefHeight(40);
        cardNumberField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");
        HBox.setHgrow(cardNumberField, Priority.ALWAYS);

        // 限制只能输入数字
        cardNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                cardNumberField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        searchButton = new Button("查询借阅记录");
        searchButton.setPrefSize(120, 40);
        searchButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> searchBorrowedBooks());

        refreshButton = new Button("刷新");
        refreshButton.setPrefSize(100, 40);
        refreshButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        refreshButton.setOnAction(e -> searchBorrowedBooks());

        searchBar.getChildren().addAll(cardNumberField, searchButton, refreshButton);

        // 结果标签
        resultsLabel = new Label("请输入一卡通号查询借阅记录");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(searchBar, resultsLabel);
        topContainer.getChildren().addAll(titleLabel, searchBox);
        setTop(topContainer);

        // 中心借阅记录展示区域
        recordsContainer = new VBox(15);
        recordsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(recordsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void searchBorrowedBooks() {
        String cardNumber = cardNumberField.getText().trim();

        // 验证输入
        if (cardNumber.isEmpty()) {
            setStatus("请输入一卡通号", "error");
            return;
        }

        setStatus("查询中...", "info");
        searchButton.setDisable(true);
        refreshButton.setDisable(true);

        new Thread(() -> {
            try {
                // 构建查询请求 - 按照要求格式
                Map<String, Object> data = new HashMap<>();
                data.put("userId", Integer.parseInt(cardNumber));

                Request request = new Request("getOwnRecords", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取借阅记录列表 - 使用BookRecord类
                    Type recordListType = new TypeToken<List<BookRecord>>(){}.getType();
                    List<BookRecord> recordList = gson.fromJson(gson.toJson(responseMap.get("data")), recordListType);

                    // 保存所有借阅记录用于筛选
                    allBookRecords = recordList;

                    Platform.runLater(() -> {
                        displayRecords(recordList);
                        setStatus("查询完成", "success");
                        searchButton.setDisable(false);
                        refreshButton.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("查询失败: " + responseMap.get("message"), "error");
                        searchButton.setDisable(false);
                        refreshButton.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage(), "error");
                    searchButton.setDisable(false);
                    refreshButton.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void displayRecords(List<BookRecord> recordList) {
        Platform.runLater(() -> {
            recordsContainer.getChildren().clear();

            if (recordList.isEmpty()) {
                Label emptyLabel = new Label("没有找到借阅记录");
                emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 16px; -fx-padding: 40;");
                emptyLabel.setAlignment(Pos.CENTER);
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

        // 右侧还书按钮
        Button returnBtn = new Button("归还");
        returnBtn.setPrefSize(80, 40);
        returnBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
        returnBtn.setOnAction(e -> returnBook(record.getUuid()));

        card.getChildren().addAll(infoBox, returnBtn);

        return card;
    }

    private void returnBook(String uuid) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("还书中...", "info"));

                // 构建归还请求 - 按照要求格式
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", uuid);

                Request request = new Request("returnBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("还书成功", "success");
                        showAlert("成功", "书籍归还成功");
                        // 刷新借阅记录列表
                        searchBorrowedBooks();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("还书失败: " + responseMap.get("message"), "error");
                        showAlert("错误", "还书失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("还书错误: " + e.getMessage(), "error");
                    showAlert("错误", "还书过程中发生错误: " + e.getMessage());
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
            statusLabel.setText("状态: " + message);
            switch (type) {
                case "error":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                    break;
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
                    break;
            }
        });
    }
}