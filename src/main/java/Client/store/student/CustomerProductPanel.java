package Client.store.student;

import Client.ClientNetworkHelper;
import Client.store.util.StoreUtils;
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



    public CustomerProductPanel(String cardNumber) {
        this.cardNumber = cardNumber;

        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadCategories();
        loadAllItems();
    }

    private void initializeUI() {
        setPadding(new Insets(15));

        // 顶部搜索区域
        VBox topBox = new VBox(10);

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("选择类别");
        categoryCombo.setPrefWidth(150);
        categoryCombo.setOnAction(e -> performSearch());

        searchField = new TextField();
        searchField.setPromptText("搜索商品...");
        searchField.setPrefWidth(250);
        searchField.setOnAction(e -> performSearch());

        Button searchBtn = new Button("搜索");
        searchBtn.setOnAction(e -> performSearch());

        searchBar.getChildren().addAll(new Label("类别:"), categoryCombo, new Label("关键词:"), searchField, searchBtn);
        topBox.getChildren().add(searchBar);

        setTop(topBox);

        // 中心商品展示区域
        productsContainer = new VBox(15);
        productsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(productsContainer);
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);

        // 底部购物车区域
        initializeCart();
    }

    private void initializeCart() {
        VBox cartBox = new VBox(10);
        cartBox.setPadding(new Insets(10));
        cartBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        HBox cartHeader = new HBox();
        cartHeader.setAlignment(Pos.CENTER_LEFT);

        Label cartTitle = new Label("购物车");
        cartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button toggleCartBtn = new Button("展开");
        toggleCartBtn.setOnAction(e -> toggleCart());

        cartHeader.getChildren().addAll(cartTitle, spacer, toggleCartBtn);

        cartContainer = new VBox(10);
        cartContainer.setVisible(false);
        cartContainer.setManaged(false);

        HBox cartFooter = new HBox(10);
        cartFooter.setAlignment(Pos.CENTER_RIGHT);

        cartTotalLabel = new Label("合计: 0.00 元");
        cartTotalLabel.setStyle("-fx-font-weight: bold;");

        Button checkoutBtn = new Button("提交订单");
        checkoutBtn.setOnAction(e -> createOrder());

        cartFooter.getChildren().addAll(cartTotalLabel, checkoutBtn);

        cartBox.getChildren().addAll(cartHeader, cartContainer, cartFooter);
        setBottom(cartBox);
    }

    private void toggleCart() {
        cartExpanded = !cartExpanded;
        cartContainer.setVisible(cartExpanded);
        cartContainer.setManaged(cartExpanded);

        Button toggleBtn = (Button) ((HBox) ((VBox) getBottom()).getChildren().get(0)).getChildren().get(2);
        toggleBtn.setText(cartExpanded ? "收起" : "展开");

        if (cartExpanded) {
            refreshCart();
        }
    }

    private void refreshCart() {
        cartContainer.getChildren().clear();

        if (cartItems.isEmpty()) {
            Label emptyLabel = new Label("购物车为空");
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
        HBox card = new HBox(10);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                "-fx-border-color: #ddd; -fx-border-radius: 5; -fx-border-width: 1;");
        card.setAlignment(Pos.CENTER_LEFT);

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setStyle("-fx-background-color: #f0f0f0;");

        if (item.getPictureLink() != null && !item.getPictureLink().isEmpty()) {
            try {
                Image image = new Image(item.getPictureLink(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                // 使用默认图片
            }
        }

        // 商品信息
        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(item.getItemName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label priceLabel = new Label("单价: " + item.getPriceYuan() + "元");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        // 数量控制
        HBox quantityBox = new HBox(5);
        quantityBox.setAlignment(Pos.CENTER);

        Button decreaseBtn = new Button("-");
        decreaseBtn.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                refreshCart();
            } else {
                cartItems.remove(item);
                refreshCart();
            }
        });

        Label quantityLabel = new Label(String.valueOf(item.getQuantity()));

        Button increaseBtn = new Button("+");
        increaseBtn.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            refreshCart();
        });

        quantityBox.getChildren().addAll(decreaseBtn, quantityLabel, increaseBtn);

        // 小计
        Label subtotalLabel = new Label("小计: " + item.getSubtotalYuan() + "元");
        subtotalLabel.setStyle("-fx-font-weight: bold;");

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

    private void loadCategories() {
        new Thread(() -> {
            try {
                Request request = new Request("getAllCategories", new HashMap<>());
                String response = ClientNetworkHelper.send(request);

                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    List<String> categories = (List<String>) responseMap.get("data");
                    Platform.runLater(() -> {
                        categoryCombo.getItems().clear();
                        categoryCombo.getItems().addAll(categories);
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "获取类别失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "获取类别时发生异常: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void loadAllItems() {
        new Thread(() -> {
            try {
                Request request = new Request("getAllItems", new HashMap<>());
                String response = ClientNetworkHelper.send(request);

                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Type itemListType = new TypeToken<List<Item>>(){}.getType();
                    List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);

                    Platform.runLater(() -> {
                        displayItems(items);
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "获取商品失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "获取商品时发生异常: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void displayItems(List<Item> items) {
        productsContainer.getChildren().clear();

        if (items.isEmpty()) {
            Label noItemsLabel = new Label("暂无商品");
            noItemsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
            productsContainer.getChildren().add(noItemsLabel);
            return;
        }

        for (Item item : items) {
            HBox productCard = createProductCard(item);
            productsContainer.getChildren().add(productCard);
        }
    }

    private HBox createProductCard(Item item) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-border-width: 1;");
        card.setAlignment(Pos.CENTER_LEFT);

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);
        imageView.setStyle("-fx-background-color: #f8f8f8;");

        if (item.getPictureLink() != null && !item.getPictureLink().isEmpty()) {
            try {
                Image image = new Image(item.getPictureLink(), true);
                imageView.setImage(image);
            } catch (Exception e) {
                // 使用默认图片
            }
        }

        // 商品信息
        VBox infoBox = new VBox(8);

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label priceLabel = new Label("价格: " + item.getPriceYuan() + " 元");
        priceLabel.setStyle("-fx-text-fill: #e53935; -fx-font-weight: bold;");

        Label stockLabel = new Label("库存: " + item.getStock() + " 件");
        stockLabel.setStyle("-fx-text-fill: #666;");

        Label salesLabel = new Label("销量: " + item.getSalesVolume() + " 件");
        salesLabel.setStyle("-fx-text-fill: #666;");

        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            Label descLabel = new Label(item.getDescription());
            descLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
            descLabel.setWrapText(true);
            infoBox.getChildren().add(descLabel);
        }

        infoBox.getChildren().addAll(nameLabel, priceLabel, stockLabel, salesLabel);

        // 添加到购物车按钮
        Button addToCartBtn = new Button("加入购物车");
        addToCartBtn.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-font-weight: bold;");
        addToCartBtn.setOnAction(e -> addToCart(item));

        VBox rightBox = new VBox();
        rightBox.setAlignment(Pos.CENTER);
        rightBox.getChildren().add(addToCartBtn);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        card.getChildren().addAll(imageView, infoBox, spacer, rightBox);

        return card;
    }

    private void addToCart(Item item) {
        // 检查购物车中是否已有该商品
        for (CartItem cartItem : cartItems) {
            if (cartItem.getUuid().equals(item.getUuid())) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                if (cartExpanded) {
                    refreshCart();
                }
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
        showAlert("提示", "已添加到购物车: " + item.getItemName());
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        String category = categoryCombo.getValue();

        new Thread(() -> {
            try {
                Map<String, Object> data = new HashMap<>();
                Request request;

                if ((category == null || category.isEmpty()) && (keyword == null || keyword.isEmpty())) {
                    // 如果类别和关键词都为空，加载所有商品
                    loadAllItems();
                    return;
                } else if (category == null || category.isEmpty()) {
                    // 只按关键词搜索
                    data.put("keyword", keyword);
                    request = new Request("searchItems", data);
                } else if (keyword == null || keyword.isEmpty()) {
                    // 只按类别搜索
                    data.put("category", category);
                    request = new Request("getItemsByCategory", data);
                } else {
                    // 按类别和关键词搜索
                    data.put("category", category);
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
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "搜索失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "搜索时发生异常: " + e.getMessage());
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
                        showAlert("成功", "订单创建成功!");
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("错误", "创建订单失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "创建订单时发生异常: " + e.getMessage());
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

    // 内部类：购物车项
    public static class CartItem {
        private String uuid;
        private String itemName;
        private int priceFen;
        private String pictureLink;
        private int quantity;

        public CartItem() {}

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public int getPriceFen() { return priceFen; }
        public void setPriceFen(int priceFen) { this.priceFen = priceFen; }

        public String getPictureLink() { return pictureLink; }
        public void setPictureLink(String pictureLink) { this.pictureLink = pictureLink; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public String getSubtotalYuan() {
            return StoreUtils.fenToYuan((long) priceFen * quantity);
        }

        public String getPriceYuan() {
            return StoreUtils.fenToYuan(priceFen);
        }
    }
}