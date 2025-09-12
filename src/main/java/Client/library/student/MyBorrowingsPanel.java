package Client.library.student;

import Client.ClientNetworkHelper;
import Client.library.util.component.BorrowedBookCard;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBorrowingsPanel extends BorderPane {
    private String cardNumber;
    private Label statusLabel;
    private Label resultsLabel;
    private VBox booksContainer;
    private ScrollPane scrollPane;
    private Button refreshButton;

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
        setPadding(new Insets(15));

        // 顶部标题和刷新按钮区域
        HBox topBox = new HBox(15);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("我的借阅");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        refreshButton = new Button("刷新");
        refreshButton.setPrefWidth(80);
        refreshButton.setPrefHeight(35);
        refreshButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshButton.setOnAction(event -> loadBorrowedBooks());

        topBox.getChildren().addAll(titleLabel, refreshButton);
        setTop(topBox);

        // 结果数量标签
        resultsLabel = new Label("加载中...");
        resultsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 0 0 10 0;");

        // 中心图书列表区域
        booksContainer = new VBox(15);
        booksContainer.setPadding(new Insets(10, 0, 10, 0));

        scrollPane = new ScrollPane(booksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(resultsLabel, scrollPane);
        setCenter(centerBox);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    public void loadBorrowedBooks() {

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    setStatus("加载中...");
                    refreshButton.setDisable(true);
                });

                // 构建获取借阅记录请求
                Map<String, Object> data = new HashMap<>();
                data.put("user_id", Integer.parseInt(cardNumber));
                Request request = new Request("getBorrowedBooks", data);

//                // 使用ClientNetworkHelper发送请求
//                String response = ClientNetworkHelper.send(request);
//                System.out.println("服务器响应: " + response);

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

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        updateBorrowedBooksUI(borrowedBooks);
                        setStatus("加载完成");
                        refreshButton.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("加载失败: " + responseMap.get("message"));
                        resultsLabel.setText("加载失败");
                        refreshButton.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage());
                    resultsLabel.setText("通信错误");
                    refreshButton.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void updateBorrowedBooksUI(List<BorrowedBook> borrowedBooks) {
        booksContainer.getChildren().clear();

        if (borrowedBooks.isEmpty()) {
            Label noResults = new Label("您当前没有借阅任何图书");
            noResults.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 20;");
            booksContainer.getChildren().add(noResults);
            resultsLabel.setText("找到 0 条借阅记录");
        } else {
            for (BorrowedBook book : borrowedBooks) {
                // 创建final引用以避免lambda表达式中的变量初始化问题
                final BorrowedBook finalBook = book;
                BorrowedBookCard card = new BorrowedBookCard(finalBook, () -> {
                    renewBook(finalBook.getRecordId());
                });
                booksContainer.getChildren().add(card);
            }
            resultsLabel.setText("找到 " + borrowedBooks.size() + " 条借阅记录");
        }
    }

    private void renewBook(int recordId) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("续借中..."));

                // 构建续借请求
                Map<String, Object> data = new HashMap<>();
                data.put("record_id", recordId);
                data.put("user_id", Integer.parseInt(cardNumber));
                Request request = new Request("renewBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("续借响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("续借成功");
                        // 刷新借阅列表以更新UI
                        loadBorrowedBooks();
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("续借失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("续借错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}