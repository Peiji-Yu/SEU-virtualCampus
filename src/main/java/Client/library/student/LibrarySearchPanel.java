package Client.library.student;

import Client.library.util.component.BookCard;
import Client.library.util.model.Book;
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
import javafx.scene.control.TextField;
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

public class LibrarySearchPanel extends BorderPane {
    // UI组件
    private TextField searchTextField;
    private Button searchButton;
    private Label resultsLabel;
    private VBox booksContainer;
    private ScrollPane scrollPane;
    private Label statusLabel;

    // 当前展开的卡片
    private BookCard expandedCard;

    // JSON处理 - 使用配置了适配器的Gson实例
    private Gson gson;

    public LibrarySearchPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(15));

        // 顶部标题和搜索区域
        VBox topBox = new VBox(15);

        Label titleLabel = new Label("查询图书");
        titleLabel.setFont(Font.font(20));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a4d7b;");

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        searchTextField = new TextField();
        searchTextField.setPromptText("输入书名、作者或ISBN进行搜索");
        searchTextField.setPrefWidth(400);
        searchTextField.setPrefHeight(40);
        searchTextField.setStyle("-fx-font-size: 14px;");

        searchButton = new Button("搜索");
        searchButton.setPrefWidth(80);
        searchButton.setPrefHeight(40);
        searchButton.setStyle("-fx-background-color: #4e8cff; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        searchButton.setOnAction(event -> performSearch());

        searchBox.getChildren().addAll(searchTextField, searchButton);

        // 结果数量标签
        resultsLabel = new Label("找到 0 本图书");
        resultsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        topBox.getChildren().addAll(titleLabel, searchBox, resultsLabel);
        setTop(topBox);

        // 中心图书列表区域
        booksContainer = new VBox(15);
        booksContainer.setPadding(new Insets(10, 0, 10, 0));

        scrollPane = new ScrollPane(booksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void performSearch() {
        String searchText = searchTextField.getText().trim();

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                // 构建搜索请求
                Map<String, Object> data = new HashMap<>();
                data.put("searchText", searchText.isEmpty() ? "" : searchText);
                Request request = new Request("searchBooks", data);

//                // 使用ClientNetworkHelper发送请求
//                String response = ClientNetworkHelper.send(request);
//                System.out.println("服务器响应: " + response);

                // 模拟服务器响应
                String response = "{\n" +
                        "  \"code\": 200,\n" +
                        "  \"message\": \"搜索成功\",\n" +
                        "  \"data\": [\n" +
                        "    {\n" +
                        "      \"title\": \"Java编程思想\",\n" +
                        "      \"author\": \"Bruce Eckel\",\n" +
                        "      \"publisher\": \"机械工业出版社\",\n" +
                        "      \"isbn\": \"9787111213826\",\n" +
                        "      \"description\": \"Java学习经典书籍，全面介绍Java编程语言特性和面向对象程序设计思想\",\n" +
                        "      \"totalCopies\": 5,\n" +
                        "      \"availableCopies\": 3,\n" +
                        "      \"copies\": [\n" +
                        "        {\n" +
                        "          \"copyId\": \"1987bb9a-e5d3-4a14-82d7-879e0e15c83c\",\n" +
                        "          \"location\": \"A区3排2架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"f30216a5-09f2-47e4-ad40-3cfdcdc9c0ea\",\n" +
                        "          \"location\": \"A区3排2架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"10537680-9ae7-4d99-9ced-bcf8aa3e30ea\",\n" +
                        "          \"location\": \"A区3排2架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"691df85d-7895-406e-bc75-20ca9f1f208f\",\n" +
                        "          \"location\": \"B区1排5架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"36cab9d8-021a-474f-890a-d64e7e09880a\",\n" +
                        "          \"location\": \"B区1排5架\",\n" +
                        "          \"status\": \"已预约\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
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
                        "          \"copyId\": \"ff85f69b-ff3a-4b8e-b490-e3956f567e5a\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"711bfcc6-99cd-42b5-89e6-930ede35dbc3\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"2ad1aee1-25ea-4777-a5de-8e7b144a99c0\",\n" +
                        "          \"location\": \"C区2排3架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"title\": \"算法导论\",\n" +
                        "      \"author\": \"Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein\",\n" +
                        "      \"publisher\": \"机械工业出版社\",\n" +
                        "      \"isbn\": \"9787111407010\",\n" +
                        "      \"description\": \"算法领域的经典权威著作，全面深入地介绍了算法设计与分析\",\n" +
                        "      \"totalCopies\": 4,\n" +
                        "      \"availableCopies\": 2,\n" +
                        "      \"copies\": [\n" +
                        "        {\n" +
                        "          \"copyId\": \"f77c0f64-8fd2-4153-9dc7-2bf7bcadb9bd\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"2854bb4f-5321-42eb-ba83-fcc4dbf580db\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"可借\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"8555ebb3-6319-4e75-937c-989f19b5be92\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"copyId\": \"f0ae2ab2-a52c-45bb-aac6-26b9a49ac880\",\n" +
                        "          \"location\": \"D区4排1架\",\n" +
                        "          \"status\": \"已借出\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取图书列表
                    Type bookListType = new TypeToken<List<Book>>(){}.getType();
                    List<Book> books = gson.fromJson(gson.toJson(responseMap.get("data")), bookListType);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        booksContainer.getChildren().clear();

                        if (books.isEmpty()) {
                            Label noResults = new Label("未找到相关图书");
                            noResults.setStyle("-fx-text-fill: #666; -fx-font-size: 16px; -fx-padding: 20;");
                            booksContainer.getChildren().add(noResults);
                            resultsLabel.setText("找到 0 本图书");
                        } else {
                            for (Book book : books) {
                                BookCard card = new BookCard(book);

                                // 设置卡片点击事件，实现每次只展开一个
                                card.setOnMouseClicked(event -> {
                                    if (expandedCard != null && expandedCard != card && expandedCard.isExpanded()) {
                                        expandedCard.collapse();
                                    }
                                    expandedCard = card.isExpanded() ? card : null;
                                });

                                booksContainer.getChildren().add(card);
                            }
                            resultsLabel.setText("找到 " + books.size() + " 本图书");
                        }

                        setStatus("搜索完成");
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("搜索失败: " + responseMap.get("message"));
                        resultsLabel.setText("搜索失败");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage());
                    resultsLabel.setText("通信错误");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
