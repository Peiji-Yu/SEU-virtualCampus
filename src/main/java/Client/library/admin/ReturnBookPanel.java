package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.library.model.BookRecord;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReturnBookPanel extends BorderPane {
    private TextField cardNumberField;
    private Button searchButton;
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
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("办理还书");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查询用户借阅记录并办理还书");
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

        cardNumberField = createStyledTextField("请输入一卡通号");
        HBox.setHgrow(cardNumberField, Priority.ALWAYS);

        // 限制只能输入数字
        cardNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                cardNumberField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        searchButton = new Button("查询借阅记录");
        searchButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 150px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> searchBorrowedBooks());

        searchRow.getChildren().addAll(cardNumberField, searchButton);

        // 结果标签
        resultsLabel = new Label("输入一卡通号查询借阅记录");
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

    private void searchBorrowedBooks() {
        String cardNumber = cardNumberField.getText().trim();
        // 验证输入
        if (cardNumber.isEmpty()) {
            resultsLabel.setText("请输入一卡通号");
            highlightField(cardNumberField);
            return;
        }
        new Thread(() -> {
            try {
                // 构建查询请求
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

                    // 保存所有借阅记录用于筛选
                    allBookRecords = recordList;

                    Platform.runLater(() -> {
                        displayRecords(recordList);
                    });
                } else {
                    System.err.println("查询失败: " + responseMap.get("message"));
                }
            } catch (Exception e) {
                System.err.println("通信错误: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
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

        Label uuidLabel = new Label("副本ID: " + record.getUuid());
        uuidLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(nameLabel, uuidLabel);

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

        // 按钮区域 - 放在右下角
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        // 续借按钮
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

        // 还书按钮
        Button returnBtn = new Button("归还");
        returnBtn.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 80px; -fx-pref-height: 25px; -fx-background-radius: 5;");
        returnBtn.setOnAction(e -> returnBook(record.getUuid()));

        actionBox.getChildren().addAll(renewBtn, returnBtn);

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
                        // 刷新借阅记录列表
                        searchBorrowedBooks();
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

    private void returnBook(String uuid) {
        new Thread(() -> {
            try {
                // 构建归还请求
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
                        showSuccessAlert("还书成功", "书籍归还成功");
                        // 刷新借阅记录列表
                        searchBorrowedBooks();
                    });
                } else {
                    System.err.println("还书失败: " + responseMap.get("message"));
                }
            } catch (Exception e) {
                System.err.println("还书过程中发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void highlightField(TextField field) {
        field.setStyle(field.getStyle() + " -fx-border-color: #dc3545; -fx-border-width: 2px; " +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent; ");
        // 5秒后移除高亮
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    field.setStyle("-fx-font-size: 16px; -fx-pref-height: 45px; " +
                            "-fx-background-radius: 5; -fx-border-radius: 5; " +
                            "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent; " +
                            "-fx-padding: 0 10px;");
                });
            } catch (InterruptedException e) {
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