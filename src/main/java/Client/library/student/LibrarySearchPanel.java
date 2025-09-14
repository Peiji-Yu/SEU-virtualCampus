package Client.library.student;

import Client.ClientNetworkHelper;
import Client.library.util.model.Book;
import Client.library.util.model.BookItem;
import Client.library.util.model.BookClass;
import Client.library.util.model.Category;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LibrarySearchPanel extends BorderPane {
    // UI组件
    private TextField searchField;
    private ComboBox<Category> categoryComboBox;
    private Button searchButton;
    private Button refreshButton;
    private Label resultsLabel;
    private VBox booksContainer;
    private Label statusLabel;

    // 当前展开的卡片
    private BookCard currentExpandedCard;

    // JSON处理
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
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(20);
        topContainer.setPadding(new Insets(0, 0, 20, 0));

        // 标题
        Label titleLabel = new Label("图书查询");
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

        searchField = new TextField();
        searchField.setPromptText("输入书名、作者或ISBN进行搜索");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // 类别下拉框
        categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll(Category.values());
        categoryComboBox.setPromptText("选择类别");
        categoryComboBox.setPrefWidth(150);
        categoryComboBox.setStyle("-fx-font-size: 14px;");

        searchButton = new Button("搜索");
        searchButton.setPrefSize(100, 40);
        searchButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> performSearch());

        refreshButton = new Button("刷新");
        refreshButton.setPrefSize(100, 40);
        refreshButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        refreshButton.setOnAction(e -> performSearch());

        searchBar.getChildren().addAll(searchField, categoryComboBox, searchButton, refreshButton);

        // 结果标签
        resultsLabel = new Label("找到 0 本图书");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(searchBar, resultsLabel);
        topContainer.getChildren().addAll(titleLabel, searchBox);
        setTop(topContainer);

        // 中心图书展示区域
        booksContainer = new VBox(15);
        booksContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(booksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();
        Category selectedCategory = categoryComboBox.getValue();

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                // 构建搜索请求
                Map<String, Object> data = new HashMap<>();
                data.put("searchText", searchText.isEmpty() ? "" : searchText);
                data.put("category", selectedCategory != null ? selectedCategory.name() : null);

                Request request = new Request("searchBooks", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取图书类别列表
                    Type bookClassListType = new TypeToken<List<BookClass>>(){}.getType();
                    List<BookClass> bookClasses = gson.fromJson(gson.toJson(responseMap.get("data")), bookClassListType);

                    // 为每个图书类别获取对应的图书副本
                    List<CompletableFuture<Book>> bookFutures = bookClasses.stream()
                            .map(bookClass -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    // 请求获取该ISBN对应的所有图书副本
                                    Map<String, Object> itemData = new HashMap<>();
                                    itemData.put("isbn", bookClass.getIsbn());
                                    Request itemRequest = new Request("searchBookItems", itemData);

                                    String itemResponse = ClientNetworkHelper.send(itemRequest);
                                    Map<String, Object> itemResponseMap = gson.fromJson(itemResponse, Map.class);
                                    int itemCode = ((Double) itemResponseMap.get("code")).intValue();

                                    if (itemCode == 200) {
                                        Type itemListType = new TypeToken<List<BookItem>>(){}.getType();
                                        List<BookItem> items = gson.fromJson(
                                                gson.toJson(itemResponseMap.get("data")), itemListType);

                                        // 创建Book对象并设置BookClass和BookItems
                                        Book book = new Book();
                                        book.setBookClass(bookClass);
                                        book.setItems(items);
                                        return book;
                                    } else {
                                        // 如果获取副本失败，创建一个只有BookClass的Book对象
                                        Book book = new Book();
                                        book.setBookClass(bookClass);
                                        book.setItems(List.of());
                                        return book;
                                    }
                                } catch (Exception e) {
                                    // 发生异常时创建一个只有BookClass的Book对象
                                    Book book = new Book();
                                    book.setBookClass(bookClass);
                                    book.setItems(List.of());
                                    return book;
                                }
                            }))
                            .collect(Collectors.toList());

                    // 等待所有异步任务完成
                    CompletableFuture.allOf(bookFutures.toArray(new CompletableFuture[0])).join();

                    // 获取所有Book对象
                    List<Book> books = bookFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        displayBooks(books);
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

    private void displayBooks(List<Book> books) {
        booksContainer.getChildren().clear();
        currentExpandedCard = null;

        if (books.isEmpty()) {
            Label emptyLabel = new Label("没有找到图书");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 16px; -fx-padding: 40;");
            emptyLabel.setAlignment(Pos.CENTER);
            booksContainer.getChildren().add(emptyLabel);
            resultsLabel.setText("找到 0 本图书");
            return;
        }

        for (Book book : books) {
            BookCard card = new BookCard(book);

            // 设置卡片点击事件，实现每次只展开一个
            card.setOnMouseClicked(event -> {
                // 如果已经有展开的卡片且不是当前卡片，则先关闭它
                if (currentExpandedCard != null && currentExpandedCard != card && currentExpandedCard.isExpanded()) {
                    currentExpandedCard.collapse();
                }

                // 切换当前卡片的展开状态
                card.toggleExpand();

                // 更新当前展开的卡片引用
                if (card.isExpanded()) {
                    currentExpandedCard = card;
                } else {
                    // 如果当前卡片被折叠了，且它是之前展开的卡片，则清空引用
                    if (currentExpandedCard == card) {
                        currentExpandedCard = null;
                    }
                }
            });

            booksContainer.getChildren().add(card);
        }

        resultsLabel.setText("找到 " + books.size() + " 本图书");
    }

    private void setStatus(String message) {
        statusLabel.setText("状态: " + message);
    }

    // 内部类：图书卡片
    private static class BookCard extends VBox {
        private final Book book;
        private boolean expanded = false;
        private VBox detailBox;

        public BookCard(Book book) {
            this.book = book;
            initializeUI();
        }

        private void initializeUI() {
            setPadding(new Insets(15));
            setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
            setSpacing(10);

            // 图书基本信息区域（始终显示）
            HBox summaryBox = new HBox();
            summaryBox.setAlignment(Pos.CENTER_LEFT);
            summaryBox.setSpacing(15);

            // 图书基本信息
            VBox infoBox = new VBox(5);
            infoBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            BookClass bookClass = book.getBookClass();
            Label nameLabel = new Label(bookClass.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label authorLabel = new Label("作者: " + bookClass.getAuthor());
            authorLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

            infoBox.getChildren().addAll(nameLabel, authorLabel);

            // 图书状态信息
            VBox statusBox = new VBox(5);
            statusBox.setAlignment(Pos.CENTER_RIGHT);

            Label isbnLabel = new Label("ISBN: " + bookClass.getIsbn());
            isbnLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

            // 计算可借数量（状态为INLIBRARY的图书副本数量）
            long availableCount = book.getItems().stream()
                    .filter(item -> item.getBookStatus().name().equals("INLIBRARY"))
                    .count();

            Label inventoryLabel = new Label("库存: " + bookClass.getInventory() + " | 可借: " + availableCount);
            inventoryLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

            statusBox.getChildren().addAll(isbnLabel, inventoryLabel);

            summaryBox.getChildren().addAll(infoBox, statusBox);

            // 详细信息区域（默认折叠）
            detailBox = new VBox(10);
            detailBox.setVisible(false);
            detailBox.setManaged(false);

            getChildren().addAll(summaryBox, detailBox);
        }

        public void toggleExpand() {
            expanded = !expanded;
            detailBox.setVisible(expanded);
            detailBox.setManaged(expanded);

            if (expanded) {
                addBookDetails();
            } else {
                detailBox.getChildren().clear();
            }
        }

        public void collapse() {
            if (expanded) {
                expanded = false;
                detailBox.setVisible(false);
                detailBox.setManaged(false);
                detailBox.getChildren().clear();
            }
        }

        public boolean isExpanded() {
            return expanded;
        }

        private void addBookDetails() {
            detailBox.getChildren().clear();

            BookClass bookClass = book.getBookClass();

            // 创建详细信息网格
            GridPane detailGrid = new GridPane();
            detailGrid.setHgap(15);
            detailGrid.setVgap(10);
            detailGrid.setPadding(new Insets(10, 0, 0, 0));

            // 添加详细信息
            detailGrid.add(new Label("出版社:"), 0, 0);
            detailGrid.add(new Label(bookClass.getPublisher()), 1, 0);

            detailGrid.add(new Label("出版日期:"), 0, 1);
            detailGrid.add(new Label(bookClass.getPublishDate() != null ? bookClass.getPublishDate().toString() : "未知"), 1, 1);

            detailGrid.add(new Label("类别:"), 0, 2);
            detailGrid.add(new Label(bookClass.getCategory() != null ? bookClass.getCategory().toString() : "未分类"), 1, 2);

            // 图书描述
            if (bookClass.getDescription() != null && !bookClass.getDescription().isEmpty()) {
                detailGrid.add(new Label("简介:"), 0, 3);
                Label descLabel = new Label(bookClass.getDescription());
                descLabel.setStyle("-fx-wrap-text: true;");
                descLabel.setMaxWidth(400);
                detailGrid.add(descLabel, 1, 3);
            }

            // 添加图书副本信息
            if (!book.getItems().isEmpty()) {
                detailGrid.add(new Label("副本信息:"), 0, 4);

                VBox itemsBox = new VBox(5);
                for (BookItem item : book.getItems()) {
                    HBox itemBox = new HBox(10);
                    itemBox.setAlignment(Pos.CENTER_LEFT);

                    Label uuidLabel = new Label("条码: " + item.getUuid());
                    uuidLabel.setStyle("-fx-font-size: 12px;");

                    Label placeLabel = new Label("位置: " + item.getPlace());
                    placeLabel.setStyle("-fx-font-size: 12px;");

                    Label statusLabel = new Label("状态: " + item.getBookStatus().getDescription());
                    statusLabel.setStyle("-fx-font-size: 12px;");

                    // 根据状态设置颜色
                    switch (item.getBookStatus()) {
                        case INLIBRARY:
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: green;");
                            break;
                        case LEND:
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: orange;");
                            break;
                        case LOST:
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");
                            break;
                        case REPAIR:
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: blue;");
                            break;
                    }

                    itemBox.getChildren().addAll(uuidLabel, placeLabel, statusLabel);
                    itemsBox.getChildren().add(itemBox);
                }

                detailGrid.add(itemsBox, 1, 4);
            }

            detailBox.getChildren().add(detailGrid);
        }
    }
}