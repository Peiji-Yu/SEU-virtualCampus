package Client.store.student;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import Client.ClientNetworkHelper;
import Client.store.util.StoreUtils;
import Client.store.model.CartItem;
import Client.store.model.Item;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
import Server.model.Request;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

public class ProductSearchPanel extends BorderPane {
    private final String cardNumber;
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private FlowPane productsContainer;
    private HBox categoryCheckBoxContainer;
    private TextField searchField;
    private Gson gson;
    private Label resultsLabel;

    // 添加对购物卡片车的引用
    private ExpandableCartCard cartCard;

    public ProductSearchPanel(String cardNumber) {
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
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("商品浏览");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查找心仪的商品");
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

        searchField = createStyledTextField("输入商品名称或条码编号进行搜索");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> performSearch());

        searchRow.getChildren().addAll(searchField, searchButton);

        // 类别筛选
        categoryCheckBoxContainer = new HBox(10);
        categoryCheckBoxContainer.setAlignment(Pos.CENTER_LEFT);
        categoryCheckBoxContainer.setPadding(new Insets(0, 0, 5, 0));

        // 为每个类别创建复选框
        List<String> categories = Arrays.asList("书籍", "文具", "食品", "日用品", "电子产品", "其他");
        for (String category : categories) {
            CheckBox checkBox = new CheckBox(category);
            checkBox.setStyle("-fx-font-size: 13px; -fx-text-fill: #3b3e45;" +
                    "-fx-focus-color: #176B3A; -fx-faint-focus-color: transparent;");
            categoryCheckBoxContainer.getChildren().add(checkBox);
        }

        // 结果标签
        resultsLabel = new Label("找到 0 个商品");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(headtitleBox, searchRow, categoryCheckBoxContainer, resultsLabel);
        topContainer.getChildren().addAll(searchBox);
        setTop(topContainer);

        // 中心商品展示区域 - 使用FlowPane实现网格布局
        productsContainer = new FlowPane();
        productsContainer.setPadding(new Insets(28));
        productsContainer.setHgap(20);
        productsContainer.setVgap(20);
        productsContainer.setStyle("-fx-background-color: white;");

        ScrollPane scrollPane = new ScrollPane(productsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white; -fx-border-color: white;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scrollPane);

        // 初始化购物车
        cartCard = new ExpandableCartCard();
        setBottom(cartCard);
        // 为购物车设置外边距
        BorderPane.setMargin(cartCard, new Insets(0, 15, 0, 30));
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

    private void refreshCart() {
        cartCard.refreshCartDetails();
    }

    private void updateCartTotal() {
        cartCard.updateCartTotal();
    }

    private void loadAllItems() {
        performSearch(); // 使用统一的搜索方法加载所有商品
    }

    private void displayItems(List<Item> items) {
        productsContainer.getChildren().clear();

        for (Item item : items) {
            VBox productCard = createProductCard(item);
            productsContainer.getChildren().add(productCard);
        }

        resultsLabel.setText("找到 " + items.size() + " 个商品");
    }

    private VBox createProductCard(Item item) {
        // 主卡片容器 - 扩大尺寸，一行显示4个
        VBox card = new VBox();
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
        card.setSpacing(0);
        card.setAlignment(Pos.TOP_CENTER);

        // 图片容器 - 占据主导位置
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(220, 220);
        imageContainer.setStyle("-fx-background-color: #f8f9fa;");

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true);

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
                    imageView.setStyle("-fx-background-color: #e0e0e0;");
                }
            }
        } else {
            // 使用默认图片
            try {
                Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                imageView.setImage(defaultImage);
            } catch (Exception e) {
                imageView.setStyle("-fx-background-color: #e0e0e0;");
            }
        }

        // 图片居中
        StackPane.setAlignment(imageView, Pos.CENTER);
        imageContainer.getChildren().add(imageView);

        // 信息容器
        VBox infoContainer = new VBox(8);
        infoContainer.setPadding(new Insets(10));
        infoContainer.setStyle("-fx-background-color: white;");

        // 商品名称 - 限制行数并添加省略号
        Label nameLabel = new Label(item.getItemName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; " +
                "-fx-font-weight: bold;  -fx-wrap-text: true;");
        nameLabel.setMaxWidth(200);
        nameLabel.setMaxHeight(40);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

        // 底部信息栏
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        // 价格和销量信息
        VBox priceSalesBox = new VBox(3);
        priceSalesBox.setAlignment(Pos.CENTER_LEFT);

        Label priceLabel = new Label("¥" + item.getPriceYuan());
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label salesLabel = new Label("已售 " + item.getSalesVolume() + " 件");
        salesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");

        priceSalesBox.getChildren().addAll(priceLabel, salesLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 加入购物车按钮
        Button addCartBtn = new Button("加入购物车");
        addCartBtn.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-padding: 5 10; -fx-background-radius: 3;");
        addCartBtn.setOnAction(e -> addToCart(item));

        bottomBox.getChildren().addAll(priceSalesBox, spacer, addCartBtn);

        infoContainer.getChildren().addAll(nameLabel, bottomBox);

        // 添加到卡片
        card.getChildren().addAll(imageContainer, infoContainer);

        // 添加鼠标悬停效果
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #176B3A; -fx-border-radius: 5; -fx-border-width: 1;");
        });

        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
        });

        return card;
    }

    private void addToCart(Item item) {
        // 检查购物车中是否已有该商品
        for (CartItem cartItem : cartItems) {
            if (cartItem.getUuid().equals(item.getUuid())) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                if (cartCard.isExpanded()) {
                    refreshCart();
                }
                updateCartTotal();
                setStatus("已增加商品数量: " + item.getItemName());
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
        if (cartCard.isExpanded()) {
            refreshCart();
        }
        updateCartTotal();
        setStatus("已添加到购物车: " + item.getItemName());
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();

        // 获取选中的类别
        List<String> selectedCategories = categoryCheckBoxContainer.getChildren().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                List<Item> allItems = new ArrayList<>();

                // 如果没有选中任何类别，则发送一个空类别请求
                if (selectedCategories.isEmpty()) {
                    Map<String, Object> data = new HashMap<>();
                    if (keyword != null && !keyword.isEmpty()) {
                        data.put("keyword", keyword);
                    }
                    else {
                        data.put("keyword", "");
                    }
                    // 不传递category参数
                    Request request = new Request("searchItems", data);
                    String response = ClientNetworkHelper.send(request);

                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Type itemListType = new TypeToken<List<Item>>(){}.getType();
                        List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);
                        allItems.addAll(items);
                    }
                } else {
                    // 对每个选中的类别发送请求
                    for (String category : selectedCategories) {
                        Map<String, Object> data = new HashMap<>();
                        if (keyword != null && !keyword.isEmpty()) {
                            data.put("keyword", keyword);
                        }
                        else {
                            data.put("keyword", "");
                        }
                        data.put("category", category);

                        Request request = new Request("searchItemsByCategory", data);
                        String response = ClientNetworkHelper.send(request);

                        Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                        int code = ((Double) responseMap.get("code")).intValue();

                        if (code == 200) {
                            Type itemListType = new TypeToken<List<Item>>(){}.getType();
                            List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);
                            allItems.addAll(items);
                        }
                    }
                }

                Platform.runLater(() -> {
                    displayItems(allItems);
                    setStatus("搜索完成，找到 " + allItems.size() + " 个商品");
                });
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
            setStatus("购物车为空，无法创建订单");
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("创建订单中..."));

                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", cardNumber);
                data.put("remark", ""); // 备注

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

    private void setStatus(String message) {
        resultsLabel.setText("状态: " + message);
    }

    // 购物车内部类
    private class ExpandableCartCard extends VBox {
        private boolean expanded = false;
        private StackPane cartHeader;
        private VBox detailBox;
        private HBox actionButtons;
        private Label cartHeaderLabel;
        private Label cartTotalLabel;

        public ExpandableCartCard() {
            initializeUI();
        }

        private void initializeUI() {
            setPadding(new Insets(0));
            setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
            setSpacing(0);

            // 购物车头部 - 可点击区域
            cartHeader = new StackPane();
            cartHeader.setPadding(new Insets(15));
            cartHeader.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
            cartHeader.setOnMouseClicked(e -> toggleExpand());

            HBox headerContent = new HBox();
            headerContent.setAlignment(Pos.CENTER_LEFT);

            cartHeaderLabel = new Label("购物车 (点击展开)");
            cartHeaderLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            cartTotalLabel = new Label("合计 0.00 元");
            cartTotalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            headerContent.getChildren().addAll(cartHeaderLabel, spacer, cartTotalLabel);
            cartHeader.getChildren().add(headerContent);

            // 详细信息区域（默认折叠）
            detailBox = new VBox(10);
            detailBox.setPadding(new Insets(15, 15, 20, 15));
            detailBox.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 5 5;");
            detailBox.setVisible(false);
            detailBox.setManaged(false);

            // 操作按钮区域（在详情区域显示）
            actionButtons = new HBox(10);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            actionButtons.setPadding(new Insets(15, 0, 0, 0));

            Button checkoutBtn = new Button("提交订单");
            checkoutBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #176B3A; -fx-text-fill: white; " +
                    "-fx-background-radius: 5; -fx-padding: 8 16;");
            checkoutBtn.setOnAction(e -> createOrder());

            actionButtons.getChildren().addAll(checkoutBtn);
            detailBox.getChildren().add(actionButtons);

            getChildren().addAll(cartHeader, detailBox);
        }

        public void toggleExpand() {
            expanded = !expanded;
            detailBox.setVisible(expanded);
            detailBox.setManaged(expanded);

            if (expanded) {
                cartHeader.setStyle("-fx-background-color: white; -fx-background-radius: 5 5 0 0;");
                cartHeaderLabel.setText("购物车 (点击收起)");
                refreshCartDetails();
            } else {
                cartHeader.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
                cartHeaderLabel.setText("购物车 (点击展开)");
            }
        }

        public boolean isExpanded() {
            return expanded;
        }

        private void refreshCartDetails() {
            // 先清除现有内容
            if (detailBox.getChildren().size() > 1) {
                detailBox.getChildren().remove(0, detailBox.getChildren().size() - 1);
            }

            if (cartItems.isEmpty()) {
                return;
            }

            VBox itemsContainer = new VBox(10);
            for (CartItem item : cartItems) {
                HBox cartItemCard = createCartItemCard(item);
                itemsContainer.getChildren().add(cartItemCard);
            }

            detailBox.getChildren().add(0, itemsContainer);
            updateCartTotal();
        }

        private HBox createCartItemCard(CartItem item) {
            HBox card = new HBox(15);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
            card.setAlignment(Pos.CENTER_LEFT);

            // 商品图片
            ImageView imageView = new ImageView();
            imageView.setFitWidth(50);
            imageView.setFitHeight(50);

            // 图片圆角裁剪
            Rectangle clip = new Rectangle(50, 50);
            clip.setArcWidth(5);
            clip.setArcHeight(5);
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
                        // 如果默认图片加载失败，使用纯色背景
                        imageView.setStyle("-fx-background-color: #e0e0e0;");
                    }
                }
            } else {
                // 使用默认图片
                try {
                    Image defaultImage = new Image(getClass().getResourceAsStream("/Image/Logo.png"));
                    imageView.setImage(defaultImage);
                } catch (Exception e) {
                    imageView.setStyle("-fx-background-color: #e0e0e0;");
                }
            }

            // 商品信息
            VBox infoBox = new VBox(5);
            infoBox.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label(item.getItemName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label priceLabel = new Label("单价: " + item.getPriceYuan() + "元");
            priceLabel.setStyle("-fx-font-size: 13px;");

            infoBox.getChildren().addAll(nameLabel, priceLabel);

            // 数量控制
            HBox quantityBox = new HBox(5);
            quantityBox.setAlignment(Pos.CENTER);

            // 加载减少按钮图标
            ImageView minusIcon = new ImageView();
            try {
                Image minusImage = new Image(getClass().getResourceAsStream("/Image/minus.png"));
                minusIcon.setImage(minusImage);
                minusIcon.setFitWidth(12);
                minusIcon.setFitHeight(12);
            } catch (Exception e) {
                // 图标加载失败
                minusIcon = null;
            }

            Button decreaseBtn = new Button();
            decreaseBtn.setGraphic(minusIcon);
            decreaseBtn.setStyle("-fx-background-color: transparent; " +
                    "-fx-min-width: 14; -fx-min-height: 14; " +
                    "-fx-border-color: #bbbbbb; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 7px;");
            decreaseBtn.setOnAction(e -> {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    refreshCartDetails();
                } else {
                    cartItems.remove(item);
                    refreshCartDetails();
                }
                updateCartTotal();
            });

            Label quantityLabel = new Label(String.valueOf(item.getQuantity()));
            quantityLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30; -fx-alignment: center;");

            // 加载增加按钮图标
            ImageView plusIcon = new ImageView();
            try {
                Image plusImage = new Image(getClass().getResourceAsStream("/Image/plus.png"));
                plusIcon.setImage(plusImage);
                plusIcon.setFitWidth(12);
                plusIcon.setFitHeight(12);
            } catch (Exception e) {
                // 图标加载失败
                plusIcon = null;
            }

            Button increaseBtn = new Button();
            increaseBtn.setGraphic(plusIcon);
            increaseBtn.setStyle("-fx-background-color: transparent; " +
                    "-fx-min-width: 14; -fx-min-height: 14; " +
                    "-fx-border-color: #bbbbbb; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 7px;");
            increaseBtn.setOnAction(e -> {
                item.setQuantity(item.getQuantity() + 1);
                refreshCartDetails();
                updateCartTotal();
            });

            quantityBox.getChildren().addAll(decreaseBtn, quantityLabel, increaseBtn);

            // 小计
            Label subtotalLabel = new Label("小计 " + item.getSubtotalYuan() + "元");
            subtotalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

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
            cartTotalLabel.setText("合计 " + StoreUtils.fenToYuan(total) + " 元");
        }
    }
}