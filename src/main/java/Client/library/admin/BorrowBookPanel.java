package Client.library.admin;

import Client.ClientNetworkHelper;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Client.library.util.model.Book;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class BorrowBookPanel extends BorderPane {
    private TextField cardNumberField;
    private TextField bookIdField;
    private Button searchButton;
    private Button borrowButton;
    private Label statusLabel;
    private TableView<Book.BookCopy> copiesTable;
    private ObservableList<Book.BookCopy> copiesData;
    private Book selectedBook;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern ISBN_PATTERN =
            Pattern.compile("^[0-9]{10,13}$");

    private Gson gson;

    public BorrowBookPanel() {
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
        Label titleLabel = new Label("借书办理");
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
        bookIdField.setPromptText("请输入书籍UUID或ISBN");

        searchButton = new Button("查询副本");
        searchButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold;");
        searchButton.setOnAction(e -> searchCopies());

        inputGrid.add(new Label("一卡通号:"), 0, 0);
        inputGrid.add(cardNumberField, 1, 0);
        inputGrid.add(new Label("书籍UUID/ISBN:"), 0, 1);
        inputGrid.add(bookIdField, 1, 1);
        inputGrid.add(searchButton, 2, 1);

        // 副本表格
        Label copiesLabel = new Label("可用副本列表");
        copiesLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        copiesTable = new TableView<>();
        copiesData = FXCollections.observableArrayList();
        copiesTable.setItems(copiesData);
        copiesTable.setPrefHeight(200);

        TableColumn<Book.BookCopy, String> idColumn = new TableColumn<>("副本ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("copyId"));
        idColumn.setPrefWidth(200);

        TableColumn<Book.BookCopy, String> locationColumn = new TableColumn<>("馆藏位置");
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationColumn.setPrefWidth(150);

        TableColumn<Book.BookCopy, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);

        copiesTable.getColumns().addAll(idColumn, locationColumn, statusColumn);

        // 借书按钮
        borrowButton = new Button("办理借阅");
        borrowButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        borrowButton.setPrefWidth(120);
        borrowButton.setPrefHeight(40);
        borrowButton.setOnAction(e -> processBorrow());
        borrowButton.setDisable(true);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        buttonBox.getChildren().add(borrowButton);

        // 状态标签
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        // 主布局
        VBox mainBox = new VBox(15);
        mainBox.getChildren().addAll(titleLabel, inputGrid, copiesLabel, copiesTable, buttonBox, statusLabel);
        setCenter(mainBox);
    }

    private void searchCopies() {
        String cardNumber = cardNumberField.getText().trim();
        String bookId = bookIdField.getText().trim();

        // 验证输入
        if (cardNumber.isEmpty()) {
            setStatus("请输入一卡通号", "error");
            return;
        }

        if (bookId.isEmpty()) {
            setStatus("请输入书籍UUID或ISBN", "error");
            return;
        }

        // 判断输入的是UUID还是ISBN
        boolean isUuid = UUID_PATTERN.matcher(bookId).matches();
        boolean isIsbn = ISBN_PATTERN.matcher(bookId.replaceAll("-", "")).matches();

        if (!isUuid && !isIsbn) {
            setStatus("请输入有效的UUID或ISBN", "error");
            return;
        }

        setStatus("查询中...", "info");

        new Thread(() -> {
            try {
                // 构建查询请求
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", cardNumber);

                if (isUuid) {
                    data.put("copyId", bookId);
                } else {
                    data.put("isbn", bookId);
                }

                Request request = new Request("getBook", data);

//                // 使用ClientNetworkHelper发送请求
//                String response = ClientNetworkHelper.send(request);
//                System.out.println("查询响应: " + response);

                // 模拟服务器响应
                String response =  "{\n" +
                        "  \"code\": 200,\n" +
                        "  \"message\": \"查询成功\",\n" +
                        "  \"data\": " +
                        "    {\n" +
                        "      \"title\": \"深入理解计算机系统\",\n" +
                        "      \"author\": \"Randal E. Bryant, David R. O'Hallaron\",\n" +
                        "      \"publisher\": \"机械工业出版社\",\n" +
                        "      \"isbn\": \"9787111544937\",\n" +
                        "      \"description\": \"计算机系统经典教材，从程序员的视角深入理解计算机系统的工作原理\",\n" +
                        "      \"totalCopies\": 3,\n" +
                        "      \"availableCopies\": 1,\n" +
                        "      \"copies\": [\n" +
                        "        {\n" +
                        "          \"copyId\": \"711bfcc6-99cd-42b5-89e6-930ede35dbc3\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"2ad1aee1-25ea-4777-a5de-8e7b144a99c0\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "}";

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取图书信息和副本列表
                    Type bookType = new TypeToken<Book>(){}.getType();
                    Book book = gson.fromJson(gson.toJson(responseMap.get("data")), bookType);
                    selectedBook = book;

                    Platform.runLater(() -> {
                        copiesData.clear();
                        copiesData.addAll(book.getCopies());

                        // 启用借书按钮
                        borrowButton.setDisable(false);

                        setStatus("找到 " + book.getCopies().size() + " 个副本", "success");
                    });
                } else {
                    Platform.runLater(() -> {
                        copiesData.clear();
                        borrowButton.setDisable(true);
                        setStatus("查询失败: " + responseMap.get("message"), "error");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    copiesData.clear();
                    borrowButton.setDisable(true);
                    setStatus("通信错误: " + e.getMessage(), "error");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void processBorrow() {
        String cardNumber = cardNumberField.getText().trim();
        Book.BookCopy selectedCopy = copiesTable.getSelectionModel().getSelectedItem();

        if (selectedCopy == null) {
            setStatus("请选择一个副本", "error");
            return;
        }

        if (!"可借".equals(selectedCopy.getStatus())) {
            setStatus("该副本当前不可借", "error");
            return;
        }

        setStatus("办理中...", "info");

        new Thread(() -> {
            try {
                // 构建借阅请求
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", cardNumber);
                data.put("copyId", selectedCopy.getCopyId().toString());

                Request request = new Request("borrowBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("借阅响应: " + response);

                // 解析响应
                Gson gson = new Gson();
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("借阅成功", "success");
                        // 刷新副本列表
                        searchCopies();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("借阅失败: " + responseMap.get("message"), "error");
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