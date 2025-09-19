package Client.panel.library.admin;

import Client.ClientNetworkHelper;
import Client.model.library.*;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Client.model.Request;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ModifyBookPanel extends BorderPane {
    // UI组件
    private TextField searchField;
    private HBox categoryCheckBoxContainer;
    private Button searchButton;
    private Label resultsLabel;
    private VBox booksContainer;

    // 当前展开的卡片
    private ExpandableBookCard currentExpandedCard;

    // JSON处理
    private Gson gson;

    public ModifyBookPanel() {
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
        Label titleLabel = new Label("管理书籍");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查找和管理图书馆藏书");
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
            ExpandableBookCard card = new ExpandableBookCard(book);

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

    // 内部类：可折叠的图书卡片
    private class ExpandableBookCard extends VBox {
        private final Book book;
        private boolean expanded = false;
        private static boolean isAnyCardEditing = false; // 跟踪当前是否有卡片处于编辑状态
        private VBox detailBox;
        private boolean isEditing = false;
        private HBox actionButtons;
        private HBox confirmCancelButtons;

        // 编辑状态下的组件
        private TextArea descriptionArea;
        private ComboBox<Category> categoryCombo;
        private List<BookItemEditor> itemEditors = new ArrayList<>();
        private List<BookItemEditor> newItemEditors = new ArrayList<>();


        public ExpandableBookCard(Book book) {
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

            // 操作按钮区域（默认隐藏，展开时显示）
            actionButtons = new HBox(10);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            actionButtons.setPadding(new Insets(10, 0, 0, 0));
            actionButtons.setVisible(false);
            actionButtons.setManaged(false);

            // 修改书籍按钮
            Button modifyButton = new Button("修改");
            modifyButton.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-size: 14px;");
            modifyButton.setOnAction(e -> startEditing());

            // 删除书籍按钮
            Button deleteBookButton = new Button("删除");
            deleteBookButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 14px;");
            deleteBookButton.setOnAction(e -> deleteBook());

            actionButtons.getChildren().addAll(modifyButton, deleteBookButton);

            // 编辑时的按钮区域（默认隐藏）
            confirmCancelButtons = new HBox(10);
            confirmCancelButtons.setAlignment(Pos.CENTER_LEFT);
            confirmCancelButtons.setVisible(false);
            confirmCancelButtons.setManaged(false);

            // 添加副本按钮
            Button addItemButton = new Button("添加副本");
            addItemButton.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 100;");
            addItemButton.setOnAction(e -> addNewItem());

            // 占位区域，推动右侧组件到最右边
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // 确认按钮
            Button confirmButton = new Button("确认");
            confirmButton.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 80;");
            confirmButton.setOnAction(e -> confirmChanges());

            // 取消按钮
            Button cancelButton = new Button("取消");
            cancelButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 80;");
            cancelButton.setOnAction(e -> cancelEditing());

            confirmCancelButtons.getChildren().addAll(addItemButton, spacer, confirmButton, cancelButton);

            getChildren().addAll(summaryBox, detailBox, actionButtons, confirmCancelButtons);
        }

        public void toggleExpand() {
            // 如果有任何卡片正在编辑，则不允许展开或折叠
            if (isAnyCardEditing) {
                return;
            }

            expanded = !expanded;
            detailBox.setVisible(expanded);
            detailBox.setManaged(expanded);

            // 控制按钮的显示
            actionButtons.setVisible(expanded && !isEditing);
            actionButtons.setManaged(expanded && !isEditing);

            if (expanded) {
                addBookDetails();
            } else {
                detailBox.getChildren().clear();
            }
        }

        public void collapse() {
            // 如果有任何卡片正在编辑，则不允许展开或折叠
            if (isAnyCardEditing) {
                return;
            }

            if (expanded) {
                expanded = false;
                detailBox.setVisible(false);
                detailBox.setManaged(false);
                detailBox.getChildren().clear();

                // 隐藏按钮
                actionButtons.setVisible(false);
                actionButtons.setManaged(false);
            }
        }

        public boolean isExpanded() {
            return expanded;
        }

        private void addBookDetails() {
            detailBox.getChildren().clear();
            itemEditors.clear();

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

            // 内容 - 只读模式
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

            // 为编辑模式保存组件引用
            descriptionArea = new TextArea(bookClass.getDescription());
            descriptionArea.setWrapText(true);

            descriptionArea.setPrefRowCount(5);
            descriptionArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                    "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                    "-fx-border-radius: 5;");

            categoryCombo = new ComboBox<>();
            categoryCombo.getItems().addAll(Category.values());
            categoryCombo.setValue(bookClass.getCategory());
            categoryCombo.setStyle("-fx-font-size: 14px; -fx-pref-height: 15px; " +
                    "-fx-background-radius: 5; -fx-border-radius: 5; " +
                    "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent;");

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

            // 设置列约束
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
                    BookItemEditor editor = new BookItemEditor(item, false);
                    itemEditors.add(editor);
                    itemsContainer.getChildren().add(editor);

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

        private void startEditing() {
            isEditing = true;
            isAnyCardEditing = true; // 设置全局编辑状态
            updateEditMode();
        }

        private void cancelEditing() {
            isEditing = false;
            isAnyCardEditing = false; // 取消全局编辑状态
            newItemEditors.clear(); // 清空新项目列表
            updateEditMode();
            // 重新加载详情以恢复原始数据
            addBookDetails();
        }

        private void updateEditMode() {
            actionButtons.setVisible(!isEditing && expanded);
            actionButtons.setManaged(!isEditing && expanded);
            confirmCancelButtons.setVisible(isEditing);
            confirmCancelButtons.setManaged(isEditing);

            if (isEditing) {
                // 切换到编辑模式
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

                // 内容 - 编辑模式下，出版社和出版日期不可编辑（标签），类别可编辑（下拉框）
                Label publisherValue = new Label(bookClass.getPublisher() != null ? bookClass.getPublisher() : "未知");
                publisherValue.setStyle("-fx-font-size: 14px;");

                String publishDate = bookClass.getPublishDate() != null ?
                        bookClass.getPublishDate().toString() : "未知";
                Label publishDateValue = new Label(publishDate);
                publishDateValue.setStyle("-fx-font-size: 14px;");

                // 类别使用下拉框 - 显示中文描述
                categoryCombo = new ComboBox<>();
                categoryCombo.getItems().addAll(Category.values());
                categoryCombo.setValue(bookClass.getCategory());
                categoryCombo.setConverter(new StringConverter<Category>() {
                    @Override
                    public String toString(Category category) {
                        return category.getDescription(); // 使用中文描述
                    }

                    @Override
                    public Category fromString(String string) {
                        for (Category category : Category.values()) {
                            if (category.getDescription().equals(string)) {
                                return category;
                            }
                        }
                        return null;
                    }
                });
                categoryCombo.setStyle("-fx-font-size: 12px; " +
                        "-fx-background-radius: 5; -fx-border-radius: 5; " +
                        "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

                infoGrid.add(publisherValue, 0, 2);
                infoGrid.add(publishDateValue, 1, 2);
                infoGrid.add(categoryCombo, 2, 2);

                detailsContainer.getChildren().add(infoGrid);

                // 添加基本信息表格与简介之间的分割线
                Region separator2 = createSeparator();
                detailsContainer.getChildren().add(separator2);

                // 图书描述 - 编辑模式使用文本区域
                HBox descriptionBox = new HBox(10);

                descriptionBox.setAlignment(Pos.TOP_LEFT);

                Label descTitle = new Label("简介:");
                descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                descTitle.setMinWidth(60);

                descriptionArea = new TextArea(bookClass.getDescription());
                descriptionArea.setWrapText(true);
                descriptionArea.setPrefRowCount(5);
                descriptionArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");
                HBox.setHgrow(descriptionArea, Priority.ALWAYS);

                descriptionBox.getChildren().addAll(descTitle, descriptionArea);
                detailsContainer.getChildren().add(descriptionBox);

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

                // 设置列约束
                ColumnConstraints itemCol1 = new ColumnConstraints();
                itemCol1.setPercentWidth(25);
                ColumnConstraints itemCol2 = new ColumnConstraints();
                itemCol2.setPercentWidth(25);
                ColumnConstraints itemCol3 = new ColumnConstraints();
                itemCol3.setPercentWidth(25);
                ColumnConstraints itemCol4 = new ColumnConstraints();
                itemCol4.setPercentWidth(25);
                headerGrid.getColumnConstraints().addAll(itemCol1, itemCol2, itemCol3, itemCol4);

                Label headerId = new Label("副本ID");
                headerId.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                Label headerPlace = new Label("馆藏位置");
                headerPlace.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                Label headerStatus = new Label("副本状态");
                headerStatus.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                Label headerAction = new Label("操作");
                headerAction.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                headerGrid.add(headerId, 0, 0);
                headerGrid.add(headerPlace, 1, 0);
                headerGrid.add(headerStatus, 2, 0);
                headerGrid.add(headerAction, 3, 0);

                itemsContainer.getChildren().add(headerGrid);

                // 添加表头与内容之间的分割线
                Region itemHeaderSeparator = createSeparator();
                itemsContainer.getChildren().add(itemHeaderSeparator);

                // 表格内容
                itemEditors.clear();
                List<BookItem> allItems = new ArrayList<>(book.getItems());

                if (!allItems.isEmpty() || !newItemEditors.isEmpty()) {
                    for (int i = 0; i < allItems.size(); i++) {
                        BookItem item = allItems.get(i);
                        BookItemEditor editor = new BookItemEditor(item, true);
                        itemEditors.add(editor);

                        // 创建包含编辑器和分割线的容器
                        VBox itemContainer = new VBox();
                        itemContainer.getChildren().add(editor);

                        // 为每个项目添加分割线
                        if (i < allItems.size() || !newItemEditors.isEmpty()) {
                            Region itemSeparator = createSeparator();
                            itemContainer.getChildren().add(itemSeparator);
                        }

                        itemsContainer.getChildren().add(itemContainer);
                        editor.setParentContainer(itemContainer); // 设置父容器引用
                    }
                }

                // 添加新项目
                if (!newItemEditors.isEmpty()) {
                    for (int i = 0; i < newItemEditors.size(); i++) {
                        BookItemEditor editor = newItemEditors.get(i);
                        VBox itemContainer = new VBox();
                        itemContainer.getChildren().add(editor);

                        // 为每个项目添加分割线
                        if (i < newItemEditors.size()) {
                            Region itemSeparator = createSeparator();
                            itemContainer.getChildren().add(itemSeparator);
                        }

                        itemsContainer.getChildren().add(itemContainer);
                        editor.setParentContainer(itemContainer); // 设置父容器引用
                    }
                }

                detailBox.getChildren().add(itemsContainer);
            }
        }

        private void addNewItem() {
            if (!isEditing) {
                startEditing();
            }

            BookItem newItem = new BookItem();
            newItem.setUuid(UUID.randomUUID().toString());
            newItem.setIsbn(book.getBookClass().getIsbn());
            newItem.setPlace("");
            newItem.setBookStatus(BookStatus.INLIBRARY);

            BookItemEditor editor = new BookItemEditor(newItem, true);
            newItemEditors.add(editor);

            // 刷新编辑视图
            updateEditMode();
        }

        private void confirmChanges() {
            // 更新书籍信息 - 只更新类别和简介
            BookClass bookClass = book.getBookClass();
            bookClass.setCategory(categoryCombo.getValue());
            bookClass.setDescription(descriptionArea.getText());

            // 更新书籍信息
            updateBook(bookClass);

            // 更新副本信息
            for (BookItemEditor editor : itemEditors) {
                BookItem item = editor.getItem();
                if (editor.isDeleted()) {
                    deleteBookItem(item);
                } else if (editor.isModified()) {
                    updateBookItem(item);
                }
            }

            // 添加所有新副本
            for (BookItemEditor editor : newItemEditors) {
                if (!editor.isDeleted()) {
                    addBookItem(editor.getItem());
                }
            }

            isEditing = false;
            isAnyCardEditing = false; // 取消全局编辑状态
            newItemEditors.clear(); // 清空新项目列表

            try {
                Thread.sleep(100); //等待数据库更新
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 刷新显示
            performSearch();
        }

        private void deleteBook() {
            // 确认删除对话框
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("确认删除");
            confirmDialog.setHeaderText("确认删除书籍");
            confirmDialog.setContentText("确定要删除《" + book.getBookClass().getName() + "》及其所有副本吗？此操作不可撤销。");

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deleteBookAll(book.getBookClass());
                }
            });
        }

        // 创建分割线辅助方法
        private Region createSeparator() {
            Region separator = new Region();
            separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
            separator.setMaxWidth(Double.MAX_VALUE);
            return separator;
        }
    }

    // 内部类：图书副本编辑器
    private class BookItemEditor extends GridPane {
        private BookItem item;
        private boolean isEditing;
        private boolean isDeleted = false;
        private boolean isModified = false;
        private Node parentContainer; // 引用父容器

        private TextField placeField;
        private ComboBox<BookStatus> statusCombo;
        private Button deleteButton;

        public BookItemEditor(BookItem item, boolean isEditing) {
            this.item = item;
            this.isEditing = isEditing;
            initializeUI();
        }

        // 设置父容器的方法
        public void setParentContainer(Node parentContainer) {
            this.parentContainer = parentContainer;
        }

        private void initializeUI() {
            setHgap(10);
            setPadding(new Insets(5, 0, 5, 0));

            if (isEditing) {
                // 编辑模式 - 四列均分
                ColumnConstraints col1 = new ColumnConstraints();
                col1.setPercentWidth(25);
                ColumnConstraints col2 = new ColumnConstraints();
                col2.setPercentWidth(25);
                ColumnConstraints col3 = new ColumnConstraints();
                col3.setPercentWidth(25);
                ColumnConstraints col4 = new ColumnConstraints();
                col4.setPercentWidth(25);
                getColumnConstraints().addAll(col1, col2, col3, col4);

                // 副本ID（只读）
                Label uuidLabel = new Label(item.getUuid());
                uuidLabel.setStyle("-fx-font-size: 14px;");
                add(uuidLabel, 0, 0);

                // 馆藏位置（可编辑）
                placeField = new TextField(item.getPlace());
                placeField.textProperty().addListener((obs, oldVal, newVal) -> {
                    isModified = true;
                    item.setPlace(newVal);
                });
                placeField.setStyle("-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent;");
                add(placeField, 1, 0);

                // 状态（可编辑）
                statusCombo = new ComboBox<>();
                statusCombo.getItems().addAll(BookStatus.values());
                statusCombo.setValue(item.getBookStatus());
                statusCombo.setConverter(new StringConverter<BookStatus>() {
                    @Override
                    public String toString(BookStatus status) {
                        return status.getDescription();
                    }

                    @Override
                    public BookStatus fromString(String string) {
                        return BookStatus.valueOf(string);
                    }
                });
                statusCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                    isModified = true;
                    item.setBookStatus(newVal);
                });
                statusCombo.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
                add(statusCombo, 2, 0);

                // 删除按钮
                deleteButton = new Button("删除");
                deleteButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 12px;");
                deleteButton.setOnAction(e -> markAsDeleted());
                add(deleteButton, 3, 0);
            } else {
                // 只读模式 - 三列均分
                ColumnConstraints col1 = new ColumnConstraints();
                col1.setPercentWidth(33);
                ColumnConstraints col2 = new ColumnConstraints();
                col2.setPercentWidth(33);
                ColumnConstraints col3 = new ColumnConstraints();
                col3.setPercentWidth(34);
                getColumnConstraints().addAll(col1, col2, col3);

                // 副本ID
                Label uuidLabel = new Label(item.getUuid());
                uuidLabel.setStyle("-fx-font-size: 14px;");
                add(uuidLabel, 0, 0);

                // 馆藏位置
                Label placeLabel = new Label(item.getPlace());
                placeLabel.setStyle("-fx-font-size: 14px;");
                add(placeLabel, 1, 0);

                // 状态
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

                add(statusLabel, 2, 0);
            }
        }

        private void markAsDeleted() {
            isDeleted = true;
            // 同时移除父容器（包含本编辑器和分割线）
            if (parentContainer != null) {
                Pane parent = (Pane) parentContainer.getParent();
                if (parent != null) {
                    parent.getChildren().remove(parentContainer);
                }
            }
            setVisible(false);
            setManaged(false);
        }

        public BookItem getItem() {
            return item;
        }

        public boolean isDeleted() {
            return isDeleted;
        }

        public boolean isModified() {
            return isModified;
        }
    }

    private void updateBook(BookClass bookClass) {
        new Thread(() -> {
            try {
                // 构建更新请求
                Map<String, Object> data = new HashMap<>();
                data.put("book", bookClass);
                Request request = new Request("updateBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code != 200) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setHeaderText("更新失败");
                        alert.setContentText("更新书籍信息失败: " + responseMap.get("message"));
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("更新错误");
                    alert.setContentText("更新书籍信息时发生错误: " + e.getMessage());
                    alert.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void addBookItem(BookItem bookItem) {
        new Thread(() -> {
            try {
                // 构建添加副本请求
                Map<String, Object> data = new HashMap<>();
                data.put("bookItem", bookItem);
                Request request = new Request("addBookItem", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code != 200) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setHeaderText("添加失败");
                        alert.setContentText("添加副本失败: " + responseMap.get("message"));
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("添加错误");
                    alert.setContentText("添加副本时发生错误: " + e.getMessage());
                    alert.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void updateBookItem(BookItem bookItem) {
        new Thread(() -> {
            try {
                // 构建更新副本请求
                Map<String, Object> data = new HashMap<>();
                data.put("bookItem", bookItem);
                Request request = new Request("updateBookItem", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code != 200) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setHeaderText("更新失败");
                        alert.setContentText("更新副本失败: " + responseMap.get("message"));
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("更新错误");
                    alert.setContentText("更新副本时发生错误: " + e.getMessage());
                    alert.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteBookItem(BookItem bookItem) {
        new Thread(() -> {
            try {
                // 构建删除副本请求
                Map<String, Object> data = new HashMap<>();
                data.put("uuid", bookItem.getUuid());
                Request request = new Request("deleteBookItem", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code != 200) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setHeaderText("删除失败");
                        alert.setContentText("删除副本失败: " + responseMap.get("message"));
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("删除错误");
                    alert.setContentText("删除副本时发生错误: " + e.getMessage());
                    alert.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteBookAll(BookClass bookClass) {
        new Thread(() -> {
            try {
                // 构建删除书籍请求
                Map<String, Object> data = new HashMap<>();
                data.put("isbn", bookClass.getIsbn());
                Request request = new Request("deleteBook", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        // 刷新搜索结果
                        performSearch();
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("错误");
                        alert.setHeaderText("删除失败");
                        alert.setContentText("删除书籍失败: " + responseMap.get("message"));
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("删除错误");
                    alert.setContentText("删除书籍时发生错误: " + e.getMessage());
                    alert.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }
}