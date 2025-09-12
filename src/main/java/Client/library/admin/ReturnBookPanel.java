package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.library.util.model.BorrowedBook;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private TextField bookIdField;
    private Button searchButton;
    private Label statusLabel;
    private Label resultsLabel;
    private VBox booksContainer;
    private ScrollPane scrollPane;

    private Gson gson;
    private List<BorrowedBook> allBorrowedBooks;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public ReturnBookPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(15));

        // 顶部标题
        Label titleLabel = new Label("还书办理");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        // 输入区域
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setPadding(new Insets(20, 0, 20, 0));

        cardNumberField = new TextField();
        cardNumberField.setPromptText("请输入一卡通号");
        cardNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                cardNumberField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        bookIdField = new TextField();
        bookIdField.setPromptText("请输入书籍UUID (可选)");

        searchButton = new Button("查询借阅记录");
        searchButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
        searchButton.setOnAction(e -> searchBorrowedBooks());

        inputGrid.add(new Label("一卡通号:"), 0, 0);
        inputGrid.add(cardNumberField, 1, 0);
        inputGrid.add(new Label("书籍UUID:"), 0, 1);
        inputGrid.add(bookIdField, 1, 1);
        inputGrid.add(searchButton, 2, 1);

        // 结果数量标签
        resultsLabel = new Label("");
        resultsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 0 0 10 0;");

        // 借阅记录容器
        booksContainer = new VBox(15);
        booksContainer.setPadding(new Insets(10, 0, 10, 0));

        scrollPane = new ScrollPane(booksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(resultsLabel, scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        // 主布局
        VBox mainBox = new VBox(15);
        mainBox.getChildren().addAll(titleLabel, inputGrid, centerBox, statusLabel);
        setCenter(mainBox);
    }

    private void searchBorrowedBooks() {
        String cardNumber = cardNumberField.getText().trim();
        String uuidInput = bookIdField.getText().trim();

        // 验证输入
        if (cardNumber.isEmpty()) {
            setStatus("请输入一卡通号", "error");
            return;
        }

        if (!uuidInput.isEmpty() && !UUID_PATTERN.matcher(uuidInput).matches()) {
            setStatus("请输入有效的UUID", "error");
            return;
        }

        setStatus("查询中...", "info");

        new Thread(() -> {
            try {
                // 构建查询请求 - 使用固定格式
                Map<String, Object> data = new HashMap<>();
                data.put("user_id", Integer.parseInt(cardNumber));

                Request request = new Request("getBorrowedBooks", data);

//                // 使用ClientNetworkHelper发送请求
//                String response = ClientNetworkHelper.send(request);
//                System.out.println("查询响应: " + response);

                // 模拟服务器响应
                String response = "{\"code\":200,\"message\":\"success\",\"data\":[" +
                        "{\"recordId\":1001,\"bookTitle\":\"Java编程思想\",\"author\":\"Bruce Eckel\",\"copyId\":\"123e4567-e89b-12d3-a456-426614174000\",\"location\":\"A区-3排-2架\",\"borrowDate\":\"2024-01-15\",\"dueDate\":\"2024-02-15\",\"canRenew\":true,\"totalBorrowDays\":30}," +
                        "{\"recordId\":1002,\"bookTitle\":\"深入理解计算机系统\",\"author\":\"Randal E. Bryant\",\"copyId\":\"223e4567-e89b-12d3-a456-426614174001\",\"location\":\"B区-1排-4架\",\"borrowDate\":\"2024-01-10\",\"dueDate\":\"2024-03-10\",\"canRenew\":false,\"totalBorrowDays\":60}," +
                        "{\"recordId\":1003,\"bookTitle\":\"设计模式：可复用面向对象软件的基础\",\"author\":\"Erich Gamma\",\"copyId\":\"323e4567-e89b-12d3-a456-426614174002\",\"location\":\"C区-2排-1架\",\"borrowDate\":\"2024-01-20\",\"dueDate\":\"2024-02-20\",\"canRenew\":true,\"totalBorrowDays\":30}" +
                        "]}";

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取借阅记录列表
                    Type bookListType = new TypeToken<List<BorrowedBook>>(){}.getType();
                    List<BorrowedBook> borrowedBooks = gson.fromJson(gson.toJson(responseMap.get("data")), bookListType);

                    // 保存所有借阅记录用于筛选
                    allBorrowedBooks = borrowedBooks;

                    // 根据UUID筛选记录
                    if (!uuidInput.isEmpty()) {
                        UUID filterUuid = UUID.fromString(uuidInput);
                        borrowedBooks = borrowedBooks.stream()
                                .filter(book -> book.getCopyId().equals(filterUuid))
                                .collect(Collectors.toList());
                    }

                    List<BorrowedBook> finalBorrowedBooks = borrowedBooks;
                    Platform.runLater(() -> {
                        updateBorrowedBooksUI(finalBorrowedBooks);
                        setStatus("查询完成", "success");
                    });
                } else {
                    Platform.runLater(() -> {
                        booksContainer.getChildren().clear();
                        resultsLabel.setText("查询失败");
                        setStatus("查询失败: " + responseMap.get("message"), "error");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    booksContainer.getChildren().clear();
                    resultsLabel.setText("通信错误");
                    setStatus("通信错误: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void updateBorrowedBooksUI(List<BorrowedBook> borrowedBooks) {
        booksContainer.getChildren().clear();

        if (borrowedBooks.isEmpty()) {
            Label noResults = new Label("未找到借阅记录");
            noResults.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 20;");
            booksContainer.getChildren().add(noResults);
            resultsLabel.setText("找到 0 条借阅记录");
        } else {
            for (BorrowedBook book : borrowedBooks) {
                // 创建借阅记录卡片
                HBox card = createBorrowedBookCard(book);
                booksContainer.getChildren().add(card);
            }
            resultsLabel.setText("找到 " + borrowedBooks.size() + " 条借阅记录");
        }
    }

    private HBox createBorrowedBookCard(BorrowedBook book) {
        // 创建卡片容器
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 15;");
        card.setPrefWidth(700);

        // 左侧 - 图书信息
        VBox infoBox = new VBox(5);
        infoBox.setPrefWidth(500);

        Label titleLabel = new Label(book.getBookTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label authorLabel = new Label("作者: " + book.getAuthor());
        authorLabel.setStyle("-fx-text-fill: #666;");

        Label copyIdLabel = new Label("副本ID: " + book.getCopyId().toString());
        copyIdLabel.setStyle("-fx-text-fill: #666;");

        Label locationLabel = new Label("位置: " + book.getLocation());
        locationLabel.setStyle("-fx-text-fill: #666;");

        Label dueDateLabel = new Label("到期日: " + book.getDueDate().toString());
        dueDateLabel.setStyle("-fx-text-fill: #666;");

        infoBox.getChildren().addAll(titleLabel, authorLabel, copyIdLabel, locationLabel, dueDateLabel);

        // 右侧 - 还书按钮
        Button returnButton = new Button("归还");
        returnButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        returnButton.setPrefWidth(80);
        returnButton.setPrefHeight(35);
        returnButton.setOnAction(e -> processReturn(book.getRecordId()));

        VBox buttonBox = new VBox();
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().add(returnButton);

        // 组合卡片
        card.getChildren().addAll(infoBox, buttonBox);

        return card;
    }

    private void processReturn(int recordId) {
        String cardNumber = cardNumberField.getText().trim();

        setStatus("办理中...", "info");

        new Thread(() -> {
            try {
                // 构建归还请求 - 新增借阅记录id
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", cardNumber);
                data.put("record_id", recordId); // 新增借阅记录id

                Request request = new Request("returnBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("归还响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("归还成功", "success");
                        // 刷新借阅记录
                        searchBorrowedBooks();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("归还失败: " + responseMap.get("message"), "error");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void setStatus(String message, String type) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            switch (type) {
                case "error":
                    statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 14px;");
                    break;
                case "success":
                    statusLabel.setStyle("-fx-text-fill: #388e3c; -fx-font-size: 14px;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
                    break;
            }
        });
    }
}