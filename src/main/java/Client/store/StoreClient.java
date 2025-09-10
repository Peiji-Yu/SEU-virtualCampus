package Client.store;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * 改进的商店前端界面（美观优化）
 */
public class StoreClient extends Application {

    // 模拟商品数据
    private final ObservableList<StoreItem> items = FXCollections.observableArrayList(
            new StoreItem("1", "Java编程思想", 89.00, "/images/java_book.jpg", "经典的Java编程教材，深入讲解Java语言特性和编程思想", "编程语言"),
            new StoreItem("2", "Python编程", 75.50, "/images/python_book.jpg", "Python入门到精通，适合初学者和进阶开发者", "编程语言"),
            new StoreItem("3", "算法导论", 120.00, "/images/algorithms_book.jpg", "计算机算法经典教材，涵盖各种算法和数据结构", "算法与数据结构"),
            new StoreItem("4", "设计模式", 68.00, "/images/design_patterns.jpg", "软件开发设计模式，提高代码质量和可维护性", "软件工程"),
            new StoreItem("5", "计算机网络", 85.00, "/images/network_book.jpg", "计算机网络原理，讲解网络协议和架构", "网络技术"),
            new StoreItem("6", "数据库系统", 79.00, "/images/database_book.jpg", "数据库系统概念，SQL和NoSQL数据库设计", "数据库"),
            new StoreItem("7", "操作系统", 92.00, "/images/os_book.jpg", "操作系统原理，进程管理、内存管理和文件系统", "系统软件"),
            new StoreItem("8", "编译原理", 88.50, "/images/compiler_book.jpg", "编译原理与技术，词法分析、语法分析和代码生成", "编译技术")
    );

    // 购物车
    private final Map<String, CartItem> cart = new HashMap<>();

    // 布局和组件
    private BorderPane root;
    private VBox menuBar;
    private StackPane contentArea;
    private ScrollPane itemsContainer;
    private VBox cartContainer;
    private VBox itemDetailContainer;
    private Button viewCartButton;
    private Label cartBadge;

    // 当前视图与前一个视图
    private String currentView = "store";
    private String previousView = "store";

    // 新增：把菜单按钮提升为字段以便控制选中样式
    private Button storeButton;
    private Button cartButton;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("智慧校园商店");

        // 主布局
        root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setStyle("-fx-font-family: 'Microsoft YaHei', 'Segoe UI', Tahoma, Arial; -fx-background-color: #f0f4f8;");

        // 左侧菜单和内容区
        createMenuBar();
        createContentArea();

        // 默认显示
        showStoreView();

        // 将窗口宽度设置为 1300，考虑侧边栏与内边距后，中心区域可视宽度刚好能容纳一行 4 个 220px 卡片
        Scene scene = new Scene(root, 1215, 700);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // 固定窗口大小
        primaryStage.show();
    }

    /**
     * 创建左侧菜单栏
     */
    private void createMenuBar() {
        menuBar = new VBox();
        menuBar.setPadding(new Insets(12));
        menuBar.setSpacing(12);
        menuBar.setPrefWidth(200);
        menuBar.setStyle("-fx-background-color: linear-gradient(#1a237e, #0d1b54); -fx-background-radius: 8;");
        menuBar.setMaxHeight(Double.MAX_VALUE);

        Label logo = new Label("智慧商店");
        logo.setTextFill(Color.WHITE);
        logo.setFont(Font.font("Microsoft YaHei", 23)); // 增大字体为23px
        logo.setStyle("-fx-font-weight: bold;"); // 加粗
        logo.setAlignment(Pos.CENTER);
        logo.setMaxWidth(Double.MAX_VALUE); // 居中
        VBox.setMargin(logo, new Insets(18, 0, 18, 0)); // 上下留白

        storeButton = createMenuButton("商店");
        storeButton.setOnAction(e -> showStoreView());

        cartButton = createMenuButton("购物车");
        cartButton.setOnAction(e -> showCartView());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        menuBar.getChildren().addAll(logo, storeButton, cartButton, spacer);
        root.setLeft(menuBar);
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(48);
        // 增大对比度：加深背景色、加粗字体、增大字体
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 17px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';";
        String hoverStyle = "-fx-background-color: #3949ab; -fx-text-fill: #fff; -fx-font-size: 17px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';";
        String activeStyle = "-fx-background-color: #283593; -fx-text-fill: #fff; -fx-font-size: 17px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';";

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> {
            if (button == storeButton && "store".equals(currentView)) {
                button.setStyle(activeStyle);
            } else if (button == cartButton && "cart".equals(currentView)) {
                button.setStyle(activeStyle);
            } else {
                button.setStyle(baseStyle);
            }
        });
        return button;
    }

    private void updateMenuSelection(String view) {
        currentView = view;
        String activeStyle = "-fx-background-color: #283593; -fx-text-fill: #fff; -fx-font-size: 17px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';";
        String baseStyle = "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 17px; -fx-font-weight: bold; -fx-font-family: 'Microsoft YaHei';";
        if (storeButton != null) storeButton.setStyle("store".equals(view) ? activeStyle : baseStyle);
        if (cartButton != null) cartButton.setStyle("cart".equals(view) ? activeStyle : baseStyle);
    }

    /**
     * 创建内容区域（商品、购物车、详情）
     */
    private void createContentArea() {
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(10));

        createStoreView();
        createCartView();
        createItemDetailView();

        root.setCenter(contentArea);
    }

    /**
     * 创建商品页面（使用响应式 FlowPane）
     */
    private void createStoreView() {
        HBox toolbar = new HBox();
        toolbar.setPadding(new Insets(12));
        toolbar.setSpacing(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: transparent;");
        toolbar.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.08)));

        Label title = new Label("商品列表");
        title.setFont(Font.font(20));
        title.setStyle("-fx-font-weight: bold;");

        TextField searchField = new TextField();
        searchField.setPromptText("搜索书名或描述...");
        searchField.setPrefWidth(360);

        // 实时搜索：输入框内容变化时自动过滤
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterItems(newVal));

        Button searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: #2d9cdb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold;");
        searchButton.setOnAction(e -> filterItems(searchField.getText()));

        // 移除 cartBadge
        // cartBadge = new Label("0");
        // cartBadge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 4 8 4 8; -fx-background-radius: 12;");
        // updateCartBadge();

        HBox searchBox = new HBox(8, searchField, searchButton);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(title, spacer, searchBox);

        // 商品展示区：ScrollPane + FlowPane
        itemsContainer = new ScrollPane();
        itemsContainer.setFitToWidth(true);
        itemsContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        itemsContainer.setStyle("-fx-background: transparent; -fx-border-color: transparent;");

        FlowPane flow = new FlowPane();
        flow.setPadding(new Insets(12));
        flow.setHgap(16);
        flow.setVgap(16);
        // 将 wrap 长度绑定到 ScrollPane 的 viewport 宽度（可视内容宽度），更精确地反映可用空间
        // 减去边距/间距以确保 4 个 220px 卡片（220*4 + 16*3 = 928）能并排显示
        // 绑定到 viewport 宽度并减去较小的边距（16），充分利用可视空间以容纳 4 列卡片
        flow.prefWrapLengthProperty().bind(Bindings.createDoubleBinding(
                () -> itemsContainer.getViewportBounds() == null ? 0.0 :
                        Math.max(0, itemsContainer.getViewportBounds().getWidth() - 16),
                itemsContainer.viewportBoundsProperty()
        ));

        flow.setStyle("-fx-background-color: transparent;");

        updateItemsFlow(flow, items);
        itemsContainer.setContent(flow);

        VBox storeContainer = new VBox(12, toolbar, itemsContainer);
        storeContainer.setId("store-container");
        storeContainer.setVisible(false);
        storeContainer.setPadding(new Insets(6));

        contentArea.getChildren().add(storeContainer);
    }

    /**
     * 创建购物车页面
     */
    private void createCartView() {
        cartContainer = new VBox();
        cartContainer.setPadding(new Insets(12));
        cartContainer.setSpacing(12);
        cartContainer.setId("cart-container");
        cartContainer.setVisible(false);
        cartContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        cartContainer.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.06)));

        contentArea.getChildren().add(cartContainer);
    }

    /**
     * 创建商品详情页面
     */
    private void createItemDetailView() {
        itemDetailContainer = new VBox();
        itemDetailContainer.setPadding(new Insets(12));
        itemDetailContainer.setSpacing(12);
        itemDetailContainer.setId("item-detail-container");
        itemDetailContainer.setVisible(false);
        itemDetailContainer.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        itemDetailContainer.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.06)));

        contentArea.getChildren().add(itemDetailContainer);
    }

    /**
     * 显示商品页面
     */
    private void showStoreView() {
        hideAllViews();
        VBox storeContainer = (VBox) contentArea.lookup("#store-container");
        if (storeContainer != null) {
            storeContainer.setVisible(true);
            // 更新左侧菜单选中样式
            updateMenuSelection("store");
            currentView = "store";
        }
    }

    /**
     * 显示购物车页面
     */
    private void showCartView() {
        hideAllViews();
        updateCartView();
        cartContainer.setVisible(true);
        // 更新左侧菜单选中样式
        updateMenuSelection("cart");
        currentView = "cart";
    }

    /**
     * 显示商品详情页面（记录前一个视图）
     */
    private void showItemDetailView(StoreItem item) {
        // 记录前一个视图，便于返回
        previousView = currentView;

        hideAllViews();
        itemDetailContainer.getChildren().clear();

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Button backButton = new Button("← 返回");
        backButton.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #495057; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-font-weight: bold;");
        backButton.setOnAction(e -> {
            if ("store".equals(previousView)) showStoreView();
            else showCartView();
        });
        header.getChildren().add(backButton);

        HBox content = new HBox(24);
        content.setAlignment(Pos.TOP_LEFT);

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream(item.getImageUrl()));
            imageView.setImage(image);
        } catch (Exception e) {
            Image image = new Image(getClass().getResourceAsStream("/Image/default.jpg"));
            imageView.setImage(image);
        }
        imageView.setFitWidth(320);
        imageView.setFitHeight(320);
        imageView.setPreserveRatio(true);

        VBox infoBox = new VBox(14);
        infoBox.setAlignment(Pos.TOP_LEFT);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        // 添加类别标签
        Label categoryLabel = new Label("类别: " + item.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 16px; -fx-font-weight: bold;");

        // 详情页价格也改为黑色
        Label priceLabel = new Label(String.format("¥%.2f", item.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #111111; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label descLabel = new Label(item.getDescription());
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(520);
        descLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #444;");

        // 详情页的"加入购物车"改为红色
        Button addToCartButton = new Button("加入购物车");
        addToCartButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 14px;");

        addToCartButton.setOnAction(e -> {
            addToCart(item);
            showStoreView();
        });

        infoBox.getChildren().addAll(nameLabel, categoryLabel, priceLabel, descLabel, addToCartButton);
        content.getChildren().addAll(imageView, infoBox);

        itemDetailContainer.getChildren().addAll(header, content);
        itemDetailContainer.setVisible(true);
        currentView = "detail";
    }

    /**
     * 隐藏所有视图
     */
    private void hideAllViews() {
        VBox storeContainer = (VBox) contentArea.lookup("#store-container");
        if (storeContainer != null) storeContainer.setVisible(false);
        if (cartContainer != null) cartContainer.setVisible(false);
        if (itemDetailContainer != null) itemDetailContainer.setVisible(false);
    }

    /**
     * 更新购物车视图
     */
    private void updateCartView() {
        cartContainer.getChildren().clear();

        Label title = new Label("购物车");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        cartContainer.getChildren().add(title);

        if (cart.isEmpty()) {
            Label emptyLabel = new Label("购物车为空");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
            cartContainer.getChildren().add(emptyLabel);
            return;
        }

        VBox cartItems = new VBox(10);
        cartItems.setStyle("-fx-padding: 8;");

        double total = 0;
        for (CartItem cartItem : cart.values()) {
            HBox itemRow = createCartItemRow(cartItem);
            cartItems.getChildren().add(itemRow);
            total += cartItem.getItem().getPrice() * cartItem.getQuantity();
        }

        HBox summary = new HBox();
        summary.setPadding(new Insets(10, 0, 0, 0));
        summary.setAlignment(Pos.CENTER_RIGHT);

        Label totalLabel = new Label(String.format("总计: ¥%.2f", total));
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111111;");

        Button checkoutButton = new Button("结算");
        checkoutButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 6;");
        checkoutButton.setOnAction(e -> checkout());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        summary.getChildren().addAll(totalLabel, spacer, checkoutButton);

        cartContainer.getChildren().addAll(cartItems, summary);
    }

    /**
     * 使用 FlowPane 更新商品卡片布局（响应式）
     */
    private void updateItemsFlow(FlowPane flow, ObservableList<StoreItem> itemsToShow) {
        flow.getChildren().clear();
        for (StoreItem item : itemsToShow) {
            VBox itemCard = createItemCard(item);
            flow.getChildren().add(itemCard);
        }
    }

    /**
     * 创建单个商品卡片（美化）
     */
    private VBox createItemCard(StoreItem item) {
        VBox card = new VBox();
        card.setPadding(new Insets(12));
        card.setSpacing(10);
        card.setPrefWidth(220);
        // 减小圆角弧度为8
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        card.setEffect(new DropShadow(6, Color.rgb(0,0,0,0.06)));

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream(item.getImageUrl()));
            imageView.setImage(image);
        } catch (Exception e) {
            Image image = new Image(getClass().getResourceAsStream("/Image/default.jpg"));
            imageView.setImage(image);
        }
        imageView.setFitWidth(180);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(true);
        imageView.setOnMouseClicked(e -> showItemDetailView(item));
        imageView.setStyle("-fx-cursor: hand;");

        Label nameLabel = new Label(item.getName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d9cdb; -fx-cursor: hand;");
        nameLabel.setOnMouseClicked(e -> showItemDetailView(item));

        // 价格显示改为黑色，更醒目
        Label priceLabel = new Label(String.format("¥%.2f", item.getPrice()));
        priceLabel.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 15px;");

        // “+”按钮缩小为32x32，字体保持22px加粗，使用 -fx-font 属性确保兼容性
        Button addButton = new Button("+");
        addButton.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-background-radius: 8; " +
            "-fx-font: bold 22px 'Microsoft YaHei', 'Arial';"
        );
        addButton.setPrefSize(32, 32);
        addButton.setMinSize(32, 32);
        addButton.setMaxSize(32, 32);
        addButton.setOnAction(e -> addToCart(item));

        HBox bottomBox = new HBox();
        bottomBox.setSpacing(10);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottomBox.getChildren().addAll(priceLabel, spacer, addButton);

        card.getChildren().addAll(imageView, nameLabel, bottomBox);
        return card;
    }

    /**
     * 创建购物车商品行
     */
    private HBox createCartItemRow(CartItem cartItem) {
        HBox row = new HBox();
        row.setSpacing(12);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 6;");
        row.setAlignment(Pos.CENTER_LEFT);

        StoreItem item = cartItem.getItem();

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream(item.getImageUrl()));
            imageView.setImage(image);
        } catch (Exception e) {
            Image image = new Image(getClass().getResourceAsStream("/Image/default.jpg"));
            imageView.setImage(image);
        }
        imageView.setFitWidth(56);
        imageView.setFitHeight(56);
        imageView.setPreserveRatio(true);
        imageView.setOnMouseClicked(e -> showItemDetailView(item));
        imageView.setStyle("-fx-cursor: hand;");

        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d9cdb;");
        nameLabel.setOnMouseClicked(e -> showItemDetailView(item));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d9cdb; -fx-cursor: hand;");

        // 购物车内单价显示也使用黑色
        Label priceLabel = new Label(String.format("¥%.2f × %d", item.getPrice(), cartItem.getQuantity()));
        priceLabel.setStyle("-fx-text-fill: #111111;");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        HBox quantityBox = new HBox(6);
        quantityBox.setAlignment(Pos.CENTER);

        Button decreaseButton = new Button("-");
        decreaseButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        decreaseButton.setOnAction(e -> adjustQuantity(cartItem, -1));

        Label quantityLabel = new Label(String.valueOf(cartItem.getQuantity()));
        quantityLabel.setPrefWidth(34);
        quantityLabel.setAlignment(Pos.CENTER);

        Button increaseButton = new Button("+");
        increaseButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        increaseButton.setOnAction(e -> adjustQuantity(cartItem, 1));

        quantityBox.getChildren().addAll(decreaseButton, quantityLabel, increaseButton);

        Button removeButton = new Button("删除");
        removeButton.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white;");
        removeButton.setOnAction(e -> removeFromCart(cartItem));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(imageView, infoBox, spacer, quantityBox, removeButton);
        return row;
    }

    /**
     * 添加商品到购物车
     */
    private void addToCart(StoreItem item) {
        cart.merge(item.getId(), new CartItem(item, 1), (oldVal, newVal) -> {
            oldVal.setQuantity(oldVal.getQuantity() + 1);
            return oldVal;
        });

        // 移除 updateCartBadge();
        // updateCartBadge();
        showAlert(Alert.AlertType.INFORMATION, "添加成功", "商品已添加到购物车！");
    }

    private void adjustQuantity(CartItem cartItem, int delta) {
        int newQuantity = cartItem.getQuantity() + delta;
        if (newQuantity <= 0) {
            removeFromCart(cartItem);
        } else {
            cartItem.setQuantity(newQuantity);
            updateCartView();
        }
        // 移除 updateCartBadge();
        // updateCartBadge();
    }

    private void removeFromCart(CartItem cartItem) {
        cart.remove(cartItem.getItem().getId());
        // 移除 updateCartBadge();
        // updateCartBadge();
        updateCartView();
    }

    private void checkout() {
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "结算失败", "购物车为空，无法结算！");
            return;
        }

        double total = cart.values().stream().mapToDouble(i -> i.getItem().getPrice() * i.getQuantity()).sum();

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认结算");
        confirmDialog.setHeaderText("结算确认");
        confirmDialog.setContentText(String.format("总计金额: ¥%.2f\n确认结算吗？", total));

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                cart.clear();
                // 移除 updateCartBadge();
                // updateCartBadge();
                showAlert(Alert.AlertType.INFORMATION, "结算成功", "订单已提交，等待处理！");
                updateCartView();
            }
        });
    }

    /**
     * 过滤商品（适配 FlowPane）
     */
    private void filterItems(String keyword) {
        VBox storeContainer = (VBox) contentArea.lookup("#store-container");
        if (storeContainer == null) return;

        ScrollPane scrollPane = (ScrollPane) storeContainer.getChildren().get(1);
        FlowPane flow = (FlowPane) scrollPane.getContent();

        if (keyword == null || keyword.trim().isEmpty()) {
            updateItemsFlow(flow, items);
        } else {
            ObservableList<StoreItem> filteredItems = FXCollections.observableArrayList();
            String lowerKeyword = keyword.toLowerCase();
            for (StoreItem item : items) {
                if (item.getName().toLowerCase().contains(lowerKeyword) ||
                        item.getDescription().toLowerCase().contains(lowerKeyword)) {
                    filteredItems.add(item);
                }
            }
            updateItemsFlow(flow, filteredItems);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class StoreItem {
        private String id;
        private String name;
        private double price;
        private String imageUrl;
        private String description;
        private String category;

        public StoreItem(String id, String name, double price, String imageUrl, String description, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
            this.description = description;
            this.category = category;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getImageUrl() { return imageUrl; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
    }

    public static class CartItem {
        private StoreItem item;
        private int quantity;

        public CartItem(StoreItem item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }

        public StoreItem getItem() { return item; }
        public void setItem(StoreItem item) { this.item = item; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
