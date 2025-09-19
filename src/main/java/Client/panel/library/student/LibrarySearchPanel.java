package Client.panel.library.student;

import Client.ClientNetworkHelper;
import Client.model.library.Book;
import Client.model.library.BookItem;
import Client.model.library.BookClass;
import Client.model.library.Category;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Client.model.Request;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LibrarySearchPanel extends BorderPane {
    // UI组件
    private TextField searchField;
    private HBox categoryCheckBoxContainer;
    private Button searchButton;
    private Label resultsLabel;
    private VBox booksContainer;

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
        performSearch();
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("搜索图书");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查找图书馆藏书");
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

        searchField = createStyledTextField("输入书名、作者或ISBN进行搜索");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> performSearch());

        searchRow.getChildren().addAll(searchField, searchButton);

        // 类别筛选 - 水平排列
        categoryCheckBoxContainer = new HBox(10);
        categoryCheckBoxContainer.setAlignment(Pos.CENTER_LEFT);
        categoryCheckBoxContainer.setPadding(new Insets(0, 0, 5, 0));

        // 为每个类别创建复选框
        for (Category category : Category.values()) {
            CheckBox checkBox = new CheckBox(category.getDescription());
            checkBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #3b3e45;" +
                    "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent;");
            categoryCheckBoxContainer.getChildren().add(checkBox);
        }

        // 结果标签
        resultsLabel = new Label("找到 0 本图书");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(headtitleBox, searchRow, categoryCheckBoxContainer, resultsLabel);
        topContainer.getChildren().addAll(searchBox);
        setTop(topContainer);

        // 中心图书展示区域
        booksContainer = new VBox(15);
        booksContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(booksContainer);
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

    private void performSearch() {
        String searchText = searchField.getText().trim();

        // 获取选中的类别
        List<Category> selectedCategories = categoryCheckBoxContainer.getChildren().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(CheckBox::isSelected)
                .map(checkBox -> {
                    for (Category category : Category.values()) {
                        if (category.getDescription().equals(checkBox.getText())) {
                            return category;
                        }
                    }
                    return null;
                })
                .filter(category -> category != null)
                .collect(Collectors.toList());

        new Thread(() -> {
            try {
                List<BookClass> allBookClasses = new ArrayList<>();

                // 如果没有选中任何类别，则发送一个空类别请求
                if (selectedCategories.isEmpty()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("searchText", searchText.isEmpty() ? "" : searchText);
                    data.put("category", null);

                    Request request = new Request("searchBooks", data);
                    String response = ClientNetworkHelper.send(request);
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Type bookClassListType = new TypeToken<List<BookClass>>(){}.getType();
                        List<BookClass> bookClasses = gson.fromJson(gson.toJson(responseMap.get("data")), bookClassListType);
                        allBookClasses.addAll(bookClasses);
                    }
                } else {
                    // 对每个选中的类别发送请求
                    for (Category category : selectedCategories) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("searchText", searchText.isEmpty() ? "" : searchText);
                        data.put("category", category.name());

                        Request request = new Request("searchBooks", data);
                        String response = ClientNetworkHelper.send(request);
                        Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                        int code = ((Double) responseMap.get("code")).intValue();

                        if (code == 200) {
                            Type bookClassListType = new TypeToken<List<BookClass>>(){}.getType();
                            List<BookClass> bookClasses = gson.fromJson(gson.toJson(responseMap.get("data")), bookClassListType);
                            allBookClasses.addAll(bookClasses);
                        }
                    }
                }

                // 为每个图书类别获取对应的图书副本
                List<CompletableFuture<Book>> bookFutures = allBookClasses.stream()
                        .map(bookClass -> CompletableFuture.supplyAsync(() -> {
                            try {
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

                                    Book book = new Book();
                                    book.setBookClass(bookClass);
                                    book.setItems(items);
                                    return book;
                                } else {
                                    Book book = new Book();
                                    book.setBookClass(bookClass);
                                    book.setItems(List.of());
                                    return book;
                                }
                            } catch (Exception e) {
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
                });

            } catch (Exception e) {
                System.err.println("通信错误");
                e.printStackTrace();
            }
        }).start();
    }

    private void displayBooks(List<Book> books) {
        booksContainer.getChildren().clear();
        currentExpandedCard = null;

        if (books.isEmpty()) {
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
            setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
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

            // 添加基本信息与详细信息之间的分割线
            Region separator1 = createSeparator();
            detailBox.getChildren().add(separator1);

            // 创建详细信息容器
            VBox detailsContainer = new VBox(10);
            detailsContainer.setFillWidth(true);
            detailsContainer.setPadding(new Insets(10, 0, 0, 0));

            // 使用表格形式展示出版社、出版日期和类别
            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(10);
            infoGrid.setVgap(5);
            infoGrid.setPadding(new Insets(0, 0, 10, 0));

            // 设置列约束，使三列等宽
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(33);
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(33);
            ColumnConstraints col3 = new ColumnConstraints();
            col3.setPercentWidth(34);
            infoGrid.getColumnConstraints().addAll(col1, col2, col3);

            // 表头
            Label publisherHeader = new Label("出版社");
            publisherHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            Label publishDateHeader = new Label("出版日期");
            publishDateHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            Label categoryHeader = new Label("类别");
            categoryHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            infoGrid.add(publisherHeader, 0, 0);
            infoGrid.add(publishDateHeader, 1, 0);
            infoGrid.add(categoryHeader, 2, 0);

            // 表头与内容之间的分割线
            Region headerSeparator = createSeparator();
            infoGrid.add(headerSeparator, 0, 1, 3, 1); // 跨3列

            // 内容
            Label publisherValue = new Label(bookClass.getPublisher() != null ? bookClass.getPublisher() : "未知");
            publisherValue.setStyle("-fx-font-size: 14px;");

            String publishDate = bookClass.getPublishDate() != null ?
                    bookClass.getPublishDate().toString() : "未知";
            Label publishDateValue = new Label(publishDate);
            publishDateValue.setStyle("-fx-font-size: 14px;");

            String categoryText = bookClass.getCategory() != null ?
                    bookClass.getCategory().getDescription() : "未分类";
            Label categoryValue = new Label(categoryText);
            categoryValue.setStyle("-fx-font-size: 14px;");

            infoGrid.add(publisherValue, 0, 2);
            infoGrid.add(publishDateValue, 1, 2);
            infoGrid.add(categoryValue, 2, 2);

            detailsContainer.getChildren().add(infoGrid);

            // 添加基本信息表格与简介之间的分割线
            Region separator2 = createSeparator();
            detailsContainer.getChildren().add(separator2);

            // 图书描述
            if (bookClass.getDescription() != null && !bookClass.getDescription().isEmpty()) {
                HBox descriptionBox = new HBox(10);
                descriptionBox.setAlignment(Pos.TOP_LEFT);

                Label descLabel = new Label(bookClass.getDescription());
                descLabel.setStyle("-fx-wrap-text: true; -fx-font-size: 14px;");

                // 创建加粗的标签
                Label descTitle = new Label("简介:");
                descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                descTitle.setMinWidth(60);

                descriptionBox.getChildren().addAll(descTitle, descLabel);
                detailsContainer.getChildren().add(descriptionBox);
            }

            detailBox.getChildren().add(detailsContainer);

            // 添加详细信息与副本信息之间的分割线
            Region separator3 = createSeparator();
            detailBox.getChildren().add(separator3);

            // 添加副本信息表格
            VBox itemsContainer = new VBox(5);
            itemsContainer.setPadding(new Insets(10, 0, 0, 0));

            // 表头
            GridPane headerGrid = new GridPane();
            headerGrid.setHgap(10);
            headerGrid.setPadding(new Insets(0, 0, 5, 0));

            // 设置列约束，使三列等宽
            ColumnConstraints itemCol1 = new ColumnConstraints();
            itemCol1.setPercentWidth(33);
            ColumnConstraints itemCol2 = new ColumnConstraints();
            itemCol2.setPercentWidth(33);
            ColumnConstraints itemCol3 = new ColumnConstraints();
            itemCol3.setPercentWidth(34);
            headerGrid.getColumnConstraints().addAll(itemCol1, itemCol2, itemCol3);

            Label headerId = new Label("副本ID");
            headerId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            Label headerPlace = new Label("馆藏位置");
            headerPlace.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            Label headerStatus = new Label("副本状态");
            headerStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            headerGrid.add(headerId, 0, 0);
            headerGrid.add(headerPlace, 1, 0);
            headerGrid.add(headerStatus, 2, 0);

            itemsContainer.getChildren().add(headerGrid);

            // 添加表头与内容之间的分割线
            Region itemHeaderSeparator = createSeparator();
            itemsContainer.getChildren().add(itemHeaderSeparator);

            // 表格内容
            if (!book.getItems().isEmpty()) {
                for (BookItem item : book.getItems()) {
                    GridPane itemGrid = new GridPane();
                    itemGrid.setHgap(10);
                    itemGrid.setPadding(new Insets(5, 0, 5, 0));

                    // 使用相同的列约束
                    itemGrid.getColumnConstraints().addAll(itemCol1, itemCol2, itemCol3);

                    Label uuidLabel = new Label(item.getUuid());
                    uuidLabel.setStyle("-fx-font-size: 14px;");

                    Label placeLabel = new Label(item.getPlace());
                    placeLabel.setStyle("-fx-font-size: 14px;");

                    Label statusLabel = new Label(item.getBookStatus().getDescription());
                    statusLabel.setStyle("-fx-font-size: 14px;");

                    // 根据状态设置颜色
                    switch (item.getBookStatus()) {
                        case INLIBRARY:
                            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: green;");
                            break;
                        case LEND:
                            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: orange;");
                            break;
                        case LOST:
                            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: red;");
                            break;
                        case REPAIR:
                            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: blue;");
                            break;
                    }

                    itemGrid.add(uuidLabel, 0, 0);
                    itemGrid.add(placeLabel, 1, 0);
                    itemGrid.add(statusLabel, 2, 0);

                    itemsContainer.getChildren().add(itemGrid);

                    // 为每个项目添加分割线
                    if (book.getItems().indexOf(item) < book.getItems().size()) {
                        Region itemSeparator = createSeparator();
                        itemsContainer.getChildren().add(itemSeparator);
                    }
                }
            } else {
                // 没有副本时显示提示信息
                Label noItemsLabel = new Label("暂无副本信息");
                noItemsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
                noItemsLabel.setPadding(new Insets(10, 0, 10, 0));
                itemsContainer.getChildren().add(noItemsLabel);

                Region itemSeparator = createSeparator();
                itemsContainer.getChildren().add(itemSeparator);
            }

            detailBox.getChildren().add(itemsContainer);
        }

        // 创建分割线辅助方法
        private Region createSeparator() {
            Region separator = new Region();
            separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator.setMaxWidth(Double.MAX_VALUE);
            return separator;
        }
    }
}