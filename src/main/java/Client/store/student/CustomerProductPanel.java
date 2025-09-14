package Client.store.student;

import Client.ClientNetworkHelper;
import Client.store.util.StoreUtils;
import Client.store.util.model.CartItem;
import Client.store.util.model.Item;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public class CustomerProductPanel extends BorderPane {
    private final String cardNumber;
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private VBox productsContainer;
    private VBox cartContainer;
    private ComboBox<String> categoryCombo;
    private TextField searchField;
    private Label cartTotalLabel;
    private boolean cartExpanded = false;
    private Gson gson;
    private Label cartHeaderLabel;
    private Map<String, Boolean> expandedCards = new HashMap<>();
    private Label statusLabel;

    public CustomerProductPanel(String cardNumber) {
        this.cardNumber = cardNumber;

        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadAllItems();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #f5f7fa;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(20);
        topContainer.setPadding(new Insets(0, 0, 20, 0));

        // 标题
        Label titleLabel = new Label("商品浏览");
        titleLabel.setFont(Font.font(24));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 搜索区域
        VBox searchBox = new VBox(15);
        searchBox.setPadding(new Insets(20));
        searchBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");

        // 搜索框
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("搜索商品名称...");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");

    // 在搜索框初始化后添加监听器
        searchField.textProperty().addListener((obs, oldVal, newVal) -> performSearch());

        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("搜索");
        searchBtn.setPrefSize(100, 40);
        searchBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        searchBtn.setOnAction(e -> performSearch());

//        Button refreshBtn = new Button("刷新");
//        refreshBtn.setPrefSize(100, 40);
//        refreshBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
//        refreshBtn.setOnAction(e -> loadAllItems());

        // 将“刷新”按钮改为“重置”按钮
        Button resetBtn = new Button("重置");
        resetBtn.setPrefSize(100, 40);
        resetBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            categoryCombo.getSelectionModel().selectFirst();
            loadAllItems();
        });

        searchBar.getChildren().addAll(searchField, searchBtn, resetBtn);

        // 类别下拉框
        HBox categoryBox = new HBox(10);
        categoryBox.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label("筛选类别:");
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        categoryCombo = new ComboBox<>();
        categoryCombo.setPrefWidth(200);
        categoryCombo.setPrefHeight(35);
        categoryCombo.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");

        List<String> categories =  Arrays.asList("书籍", "文具", "食品", "日用品", "电子产品", "其他");
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add("所有类别");
        categoryCombo.getItems().addAll(categories);
        categoryCombo.getSelectionModel().selectFirst();

        // 在类别下拉框初始化后添加监听器
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> performSearch());


        categoryBox.getChildren().addAll(categoryLabel, categoryCombo);

        searchBox.getChildren().addAll(searchBar, categoryBox);
        topContainer.getChildren().addAll(titleLabel, searchBox);
        setTop(topContainer);

        // 中心商品展示区域
        productsContainer = new VBox(15);
        productsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(productsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        setCenter(scrollPane);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        setBottom(statusLabel);

        // 初始化购物车
        initializeCart();
    }

    private void initializeCart() {
        VBox cartBox = new VBox(0);
        cartBox.setPadding(new Insets(10, 0, 0, 0));

        // 购物车头部 - 长条形卡片
        StackPane cartHeader = new StackPane();
        cartHeader.setPadding(new Insets(15));
        cartHeader.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        cartHeader.setOnMouseClicked(e -> toggleCart());

        HBox headerContent = new HBox();
        headerContent.setAlignment(Pos.CENTER_LEFT);

        cartHeaderLabel = new Label("点击展开购物车");
        cartHeaderLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cartTotalLabel = new Label("合计: 0.00 元");
        cartTotalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e53935;");

        headerContent.getChildren().addAll(cartHeaderLabel, spacer, cartTotalLabel);
        HBox.setMargin(cartHeaderLabel, new Insets(0, 0, 0, 10));

        cartHeader.getChildren().add(headerContent);

        // 购物车内容区域
        cartContainer = new VBox(10);
        cartContainer.setPadding(new Insets(15));
        cartContainer.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 10 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 10 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        cartContainer.setVisible(false);
        cartContainer.setManaged(false);

        // 结算按钮区域 - 放在购物车内容区域的右下角
        HBox checkoutBox = new HBox();
        checkoutBox.setAlignment(Pos.CENTER_RIGHT);
        checkoutBox.setPadding(new Insets(15, 0, 0, 0));

        Button checkoutBtn = new Button("提交订单");
        checkoutBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #4caf50; -fx-text-fill: white; " +
                "-fx-background-radius: 20; -fx-padding: 10 20 10 20;");
        checkoutBtn.setOnAction(e -> createOrder());

        checkoutBox.getChildren().add(checkoutBtn);

        VBox cartContent = new VBox(10);
        cartContent.getChildren().addAll(cartContainer, checkoutBox);

        cartBox.getChildren().addAll(cartHeader, cartContent);

        // 将购物车放在底部
        setBottom(new VBox(statusLabel, cartBox));
    }

    private void toggleCart() {
        cartExpanded = !cartExpanded;
        cartContainer.setVisible(cartExpanded);
        cartContainer.setManaged(cartExpanded);

        cartHeaderLabel.setText(cartExpanded ? "点击收起购物车" : "点击展开购物车");

        if (cartExpanded) {
            refreshCart();
        }
    }

    private void refreshCart() {
        cartContainer.getChildren().clear();

        if (cartItems.isEmpty()) {
            Label emptyLabel = new Label("购物车为空");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            emptyLabel.setAlignment(Pos.CENTER);
            cartContainer.getChildren().add(emptyLabel);
            return;
        }

        for (CartItem item : cartItems) {
            HBox cartItemCard = createCartItemCard(item);
            cartContainer.getChildren().add(cartItemCard);
        }

        updateCartTotal();
    }

    private HBox createCartItemCard(CartItem item) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8; " +
                "-fx-border-color: #eeeeee; -fx-border-radius: 8; -fx-border-width: 1;");
        card.setAlignment(Pos.CENTER_LEFT);

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");

        // 图片圆角裁剪
        Rectangle clip = new Rectangle(60, 60);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imageView.setClip(clip);

        if (item.getPictureLink() != null && !item.getPictureLink().isEmpty()) {
            try {
                Image image = new Image(item.getPictureLink(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                // 使用默认图片
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                    imageView.setImage(defaultImage);
                } catch (Exception ex) {
                    imageView.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;");
                }
            }
        } else {
            // 使用默认图片
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                imageView.setImage(defaultImage);
            } catch (Exception e) {
                imageView.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;");
            }
        }

        // 商品信息
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(item.getItemName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label priceLabel = new Label("单价: " + item.getPriceYuan() + "元");
        priceLabel.setStyle("-fx-text-fill: #e53935; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        // 数量控制
        HBox quantityBox = new HBox(5);
        quantityBox.setAlignment(Pos.CENTER);

        Button decreaseBtn = new Button("-");
        decreaseBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15;");
        decreaseBtn.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                refreshCart();
            } else {
                cartItems.remove(item);
                refreshCart();
            }
            updateCartTotal();
        });

        Label quantityLabel = new Label(String.valueOf(item.getQuantity()));
        quantityLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30; -fx-alignment: center;");

        Button increaseBtn = new Button("+");
        increaseBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15;");
        increaseBtn.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            refreshCart();
            updateCartTotal();
        });

        quantityBox.getChildren().addAll(decreaseBtn, quantityLabel, increaseBtn);

        // 小计
        Label subtotalLabel = new Label("小计: " + item.getSubtotalYuan() + "元");
        subtotalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e53935; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imageView, infoBox, spacer, quantityBox, subtotalLabel);

        return card;
    }

    private void updateCartTotal() {
        long total = 0;
        for (CartItem item : cartItems) {
            total += (long) item.getPriceFen() * item.getQuantity();
        }
        cartTotalLabel.setText("合计: " + StoreUtils.fenToYuan(total) + " 元");
    }

    private void loadAllItems() {
        performSearch(); // 使用统一的搜索方法加载所有商品
    }

    private void displayItems(List<Item> items) {
        productsContainer.getChildren().clear();
        expandedCards.clear();

        if (items.isEmpty()) {
            Label emptyLabel = new Label("没有找到商品");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 16px; -fx-padding: 40;");
            emptyLabel.setAlignment(Pos.CENTER);
            productsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Item item : items) {
            VBox productCard = createProductCard(item);
            productsContainer.getChildren().add(productCard);
        }
    }

    private VBox createProductCard(Item item) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        card.setSpacing(10);

        // 存储商品ID和展开状态
        String productId = item.getUuid();
        boolean isExpanded = expandedCards.getOrDefault(productId, false);

        // 商品基本信息区域（始终显示）
        HBox summaryBox = new HBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setSpacing(15);

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");

        if (item.getPictureLink() != null && !item.getPictureLink().isEmpty()) {
            try {
                Image image = new Image(item.getPictureLink(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                // 使用默认图片
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                    imageView.setImage(defaultImage);
                } catch (Exception ex) {
                    // 如果默认图片加载失败，使用纯色背景
                    imageView.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;");
                }
            }
        } else {
            // 使用默认图片
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                imageView.setImage(defaultImage);
            } catch (Exception e) {
                // 如果默认图片加载失败，使用纯色背景
                imageView.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5;");
            }
        }

        // 商品基本信息
        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(item.getItemName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label categoryLabel = new Label("类别: " + item.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(nameLabel, categoryLabel);

        // 库存和价格信息
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        Label priceLabel = new Label(item.getPriceYuan() + "元");
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #e74c3c;");

        Label stockLabel = new Label("库存: " + item.getStock());
        stockLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

        statusBox.getChildren().addAll(priceLabel, stockLabel);

        summaryBox.getChildren().addAll(imageView, infoBox, statusBox);

        // 详细信息区域（默认折叠）
        VBox detailBox = new VBox(10);
        detailBox.setVisible(isExpanded);
        detailBox.setManaged(isExpanded);

        if (isExpanded) {
            // 添加详细信息
            addProductDetails(detailBox, item);
        }

        // 操作按钮区域（仅在展开时显示）
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setVisible(isExpanded);
        buttonBox.setManaged(isExpanded);

        if (isExpanded) {
            Button addToCartBtn = new Button("加入购物车");
            addToCartBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 8 16; -fx-background-radius: 5;");
            addToCartBtn.setOnAction(e -> addToCart(item));

            buttonBox.getChildren().add(addToCartBtn);
        }

        card.getChildren().addAll(summaryBox, detailBox, buttonBox);

        // 点击卡片切换展开状态
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                boolean newExpandedState = !expandedCards.getOrDefault(productId, false);
                expandedCards.put(productId, newExpandedState);

                detailBox.setVisible(newExpandedState);
                detailBox.setManaged(newExpandedState);
                buttonBox.setVisible(newExpandedState);
                buttonBox.setManaged(newExpandedState);

                if (newExpandedState) {
                    addProductDetails(detailBox, item);

                    // 添加操作按钮
                    Button addToCartBtn = new Button("加入购物车");
                    addToCartBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; " +
                            "-fx-padding: 8 16; -fx-background-radius: 5;");
                    addToCartBtn.setOnAction(event -> addToCart(item));

                    buttonBox.getChildren().setAll(addToCartBtn);
                } else {
                    buttonBox.getChildren().clear();
                }
            }
        });

        return card;
    }

    private void addProductDetails(VBox detailBox, Item item) {
        detailBox.getChildren().clear();

        // 创建详细信息网格
        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(15);
        detailGrid.setVgap(10);
        detailGrid.setPadding(new Insets(10, 0, 0, 0));

        // 添加详细信息
        detailGrid.add(new Label("商品ID:"), 0, 0);
        detailGrid.add(new Label(item.getUuid()), 1, 0);

        detailGrid.add(new Label("销量:"), 0, 1);
        detailGrid.add(new Label(String.valueOf(item.getSalesVolume())), 1, 1);

        // 商品描述
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            detailGrid.add(new Label("描述:"), 0, 2);
            Label descLabel = new Label(item.getDescription());
            descLabel.setStyle("-fx-wrap-text: true;");
            descLabel.setMaxWidth(300);
            detailGrid.add(descLabel, 1, 2);
        }

        detailBox.getChildren().add(detailGrid);
    }

    private void addToCart(Item item) {
        // 检查购物车中是否已有该商品
        for (CartItem cartItem : cartItems) {
            if (cartItem.getUuid().equals(item.getUuid())) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                if (cartExpanded) {
                    refreshCart();
                }
                updateCartTotal();
                showAlert("提示", "已增加商品数量: " + item.getItemName());
                return;
            }
        }

        // 如果没有，创建新的购物车项
        CartItem newItem = new CartItem();
        newItem.setUuid(item.getUuid());
        newItem.setItemName(item.getItemName());
        newItem.setPriceFen(item.getPrice());
        newItem.setPictureLink(item.getPictureLink());
        newItem.setQuantity(1);

        cartItems.add(newItem);
        if (cartExpanded) {
            refreshCart();
        }
        updateCartTotal();
        showAlert("提示", "已添加到购物车: " + item.getItemName());
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        String category = categoryCombo.getValue();

        // 如果选择的是"所有类别"，则视为未选择类别
        if ("所有类别".equals(category)) {
            category = null;
        }

        String finalCategory = category;
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                Map<String, Object> data = new HashMap<>();
                Request request;

                if ((finalCategory == null || finalCategory.isEmpty()) && (keyword == null || keyword.isEmpty())) {
                    // 如果类别和关键词都为空，加载所有商品
                    request = new Request("getAllItems", data);
                } else if (finalCategory == null || finalCategory.isEmpty()) {
                    // 只按关键词搜索
                    data.put("keyword", keyword);
                    request = new Request("searchItems", data);
                } else if (keyword == null || keyword.isEmpty()) {
                    // 只按类别搜索
                    data.put("category", finalCategory);
                    request = new Request("getItemsByCategory", data);
                } else {
                    // 按类别和关键词搜索
                    data.put("category", finalCategory);
                    data.put("keyword", keyword);
                    request = new Request("searchItemsByCategory", data);
                }

                String response = ClientNetworkHelper.send(request);

                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Type itemListType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);

                    Platform.runLater(() -> {
                        displayItems(items);
                        setStatus("搜索完成，找到 " + items.size() + " 个商品");
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("搜索失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("搜索时发生异常: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void createOrder() {
        if (cartItems.isEmpty()) {
            showAlert("提示", "购物车为空，无法创建订单");
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("创建订单中..."));

                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", cardNumber);
                data.put("remark", ""); // 可以添加备注输入框

                List<Map<String, Object>> itemsData = new ArrayList<>();
                for (CartItem item : cartItems) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("itemId", item.getUuid());
                    itemData.put("amount", item.getQuantity());
                    itemsData.add(itemData);
                }
                data.put("items", itemsData);

                Request request = new Request("createOrder", data);
                String response = ClientNetworkHelper.send(request);

                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        cartItems.clear();
                        refreshCart();
                        updateCartTotal();
                        setStatus("订单创建成功!");
                        showAlert("成功", "订单创建成功!");
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("创建订单失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("创建订单时发生异常: " + e.getMessage());
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
        alert.showAndWait();
    }

    private void setStatus(String message) {
        statusLabel.setText("状态: " + message);
    }
}