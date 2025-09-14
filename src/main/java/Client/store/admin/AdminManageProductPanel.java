package Client.store.admin;

import Client.ClientNetworkHelper;
import Client.store.util.model.Item;
import Client.util.adapter.LocalDateAdapter;
import Client.util.adapter.UUIDAdapter;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public class AdminManageProductPanel extends BorderPane {
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private VBox productsContainer;
    private ComboBox<String> categoryCombo;
    private TextField searchField;
    private Label statusLabel;
    private Gson gson;
    private Map<String, Boolean> expandedProducts = new HashMap<>();

    public AdminManageProductPanel() {
        // 创建配置了LocalDate和UUID适配器的Gson实例
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
        Label titleLabel = new Label("管理商品");
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
        searchField.setPromptText("搜索商品名称或UUID...");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("搜索");
        searchBtn.setPrefSize(100, 40);
        searchBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        searchBtn.setOnAction(e -> performSearch());

        Button refreshBtn = new Button("刷新");
        refreshBtn.setPrefSize(100, 40);
        refreshBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 5;");
        refreshBtn.setOnAction(e -> loadAllItems());

        searchBar.getChildren().addAll(searchField, searchBtn, refreshBtn);

        // 类别下拉框
        HBox categoryBox = new HBox(10);
        categoryBox.setAlignment(Pos.CENTER_LEFT);

        Label categoryLabel = new Label("筛选类别:");
        categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("所有类别");
        categoryCombo.setPrefWidth(200);
        categoryCombo.setPrefHeight(35);
        categoryCombo.setStyle("-fx-font-size: 14px; -fx-background-radius: 5;");

        List<String> categories =  Arrays.asList("书籍", "文具", "食品", "日用品", "电子产品", "其他");
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add("所有类别");
        categoryCombo.getItems().addAll(categories);
        categoryCombo.getSelectionModel().selectFirst();

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
    }

    private void loadAllItems() {
        String keyword = searchField.getText().trim();
        String category = categoryCombo.getValue();

        searchByKeywordAndCategory(keyword, category);
    }

    private void performSearch() {
        String keyword = searchField.getText().trim();
        String category = categoryCombo.getValue();

        // 判断是否是UUID搜索
        boolean isUuidSearch = false;
        try {
            UUID.fromString(keyword);
            isUuidSearch = true;
        } catch (IllegalArgumentException e) {
            // 不是有效的UUID，按普通搜索处理
        }

        if (isUuidSearch) {
            searchById(keyword);
        } else {
            searchByKeywordAndCategory(keyword, category);
        }
    }

    private void searchByKeywordAndCategory(String keyword, String category) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                // 构建搜索请求
                Map<String, Object> data = new HashMap<>();

                if ("所有类别".equals(category) || category == null) {
                    // 搜索所有类别
                    data.put("keyword", keyword);
                    Request request = new Request("searchItems", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Type itemListType = new TypeToken<List<Item>>(){}.getType();
                        List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);

                        Platform.runLater(() -> {
                            displayProducts(items);
                            setStatus("搜索完成，找到 " + items.size() + " 个商品");
                        });
                    } else {
                        Platform.runLater(() ->
                                setStatus("搜索失败: " + responseMap.get("message")));
                    }
                } else {
                    // 按特定类别搜索
                    data.put("category", category);
                    data.put("keyword", keyword);
                    Request request = new Request("searchItemsByCategory", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Type itemListType = new TypeToken<List<Item>>(){}.getType();
                        List<Item> items = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);

                        Platform.runLater(() -> {
                            displayProducts(items);
                            setStatus("搜索完成，找到 " + items.size() + " 个商品");
                        });
                    } else {
                        Platform.runLater(() ->
                                setStatus("搜索失败: " + responseMap.get("message")));
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("通信错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void searchById(String uuid) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("搜索中..."));

                // 构建按UUID搜索请求
                Map<String, Object> data = new HashMap<>();
                data.put("itemId", uuid);
                Request request = new Request("getItemById", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取单个商品
                    Item item = gson.fromJson(gson.toJson(responseMap.get("data")), Item.class);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        List<Item> singleItemList = new ArrayList<>();
                        singleItemList.add(item);
                        displayProducts(singleItemList);
                        setStatus("搜索完成");
                    });
                } else {
                    Platform.runLater(() -> {
                        displayProducts(new ArrayList<>());
                        setStatus("未找到商品: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("通信错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void displayProducts(List<Item> productList) {
        productsContainer.getChildren().clear();
        expandedProducts.clear();

        if (productList.isEmpty()) {
            Label emptyLabel = new Label("没有找到商品");
            emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 16px; -fx-padding: 40;");
            emptyLabel.setAlignment(Pos.CENTER);
            productsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Item product : productList) {
            VBox productCard = createProductCard(product);
            productsContainer.getChildren().add(productCard);
        }
    }

    private VBox createProductCard(Item product) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 10; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        card.setSpacing(10);

        // 存储商品ID和展开状态
        String productId = product.getUuid();
        boolean isExpanded = expandedProducts.getOrDefault(productId, false);

        // 商品基本信息区域（始终显示）
        HBox summaryBox = new HBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setSpacing(15);

        // 商品图片
        ImageView imageView = new ImageView();
        imageView.setFitWidth(60);
        imageView.setFitHeight(60);
        imageView.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");

        if (product.getPictureLink() != null && !product.getPictureLink().isEmpty()) {
            try {
                Image image = new Image(product.getPictureLink(), true);
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
        Label nameLabel = new Label(product.getItemName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label categoryLabel = new Label("类别: " + product.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(nameLabel, categoryLabel);

        // 库存和价格信息
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        Label priceLabel = new Label(product.getPriceYuan() + "元");
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #e74c3c;");

        Label stockLabel = new Label("库存: " + product.getStock());
        stockLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

        statusBox.getChildren().addAll(priceLabel, stockLabel);

        summaryBox.getChildren().addAll(imageView, infoBox, statusBox);

        // 详细信息区域（默认折叠）
        VBox detailBox = new VBox(10);
        detailBox.setVisible(isExpanded);
        detailBox.setManaged(isExpanded);

        if (isExpanded) {
            // 添加详细信息
            addProductDetails(detailBox, product);
        }

        // 操作按钮区域（仅在展开时显示）
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setVisible(isExpanded);
        buttonBox.setManaged(isExpanded);

        if (isExpanded) {
            Button editBtn = new Button("修改");
            editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 8 16; -fx-background-radius: 5;");
            editBtn.setOnAction(e -> editProduct(product));

            Button deleteBtn = new Button("删除");
            deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-padding: 8 16; -fx-background-radius: 5;");
            deleteBtn.setOnAction(e -> deleteProduct(product.getUuid()));

            buttonBox.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(summaryBox, detailBox, buttonBox);

        // 点击卡片切换展开状态
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                boolean newExpandedState = !expandedProducts.getOrDefault(productId, false);
                expandedProducts.put(productId, newExpandedState);

                detailBox.setVisible(newExpandedState);
                detailBox.setManaged(newExpandedState);
                buttonBox.setVisible(newExpandedState);
                buttonBox.setManaged(newExpandedState);

                if (newExpandedState) {
                    addProductDetails(detailBox, product);

                    // 添加操作按钮
                    Button editBtn = new Button("修改");
                    editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px; " +
                            "-fx-padding: 8 16; -fx-background-radius: 5;");
                    editBtn.setOnAction(event -> editProduct(product));

                    Button deleteBtn = new Button("删除");
                    deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; " +
                            "-fx-padding: 8 16; -fx-background-radius: 5;");
                    deleteBtn.setOnAction(event -> deleteProduct(product.getUuid()));

                    buttonBox.getChildren().setAll(editBtn, deleteBtn);
                } else {
                    buttonBox.getChildren().clear();
                }
            }
        });

        return card;
    }

    private void addProductDetails(VBox detailBox, Item product) {
        detailBox.getChildren().clear();

        // 创建详细信息网格
        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(15);
        detailGrid.setVgap(10);
        detailGrid.setPadding(new Insets(10, 0, 0, 0));

        // 添加详细信息
        detailGrid.add(new Label("商品ID:"), 0, 0);
        detailGrid.add(new Label(product.getUuid()), 1, 0);

        detailGrid.add(new Label("条形码:"), 0, 1);
        detailGrid.add(new Label(product.getBarcode()), 1, 1);

        detailGrid.add(new Label("销量:"), 0, 2);
        detailGrid.add(new Label(String.valueOf(product.getSalesVolume())), 1, 2);

        // 商品描述
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            detailGrid.add(new Label("描述:"), 0, 3);
            Label descLabel = new Label(product.getDescription());
            descLabel.setStyle("-fx-wrap-text: true;");
            descLabel.setMaxWidth(300);
            detailGrid.add(descLabel, 1, 3);
        }

        detailBox.getChildren().add(detailGrid);
    }

    private void editProduct(Item product) {
        // 创建编辑商品对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("修改商品");
        dialog.setHeaderText("修改《" + product.getItemName() + "》的信息");

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(product.getItemName());
        nameField.setPromptText("商品名称");

        TextField categoryField = new TextField(product.getCategory());
        categoryField.setPromptText("商品类别");

        TextField priceField = new TextField(String.valueOf(product.getPrice()));
        priceField.setPromptText("价格(分)");

        TextField stockField = new TextField(String.valueOf(product.getStock()));
        stockField.setPromptText("库存数量");

        TextField imageField = new TextField(product.getPictureLink());
        imageField.setPromptText("图片链接");

        TextArea descriptionArea = new TextArea(product.getDescription());
        descriptionArea.setPromptText("商品描述");
        descriptionArea.setPrefRowCount(3);

        TextField barcodeField = new TextField(product.getBarcode());
        barcodeField.setPromptText("条形码");

        grid.add(new Label("名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("类别:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("价格(分):"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("库存:"), 0, 3);
        grid.add(stockField, 1, 3);
        grid.add(new Label("图片链接:"), 0, 4);
        grid.add(imageField, 1, 4);
        grid.add(new Label("描述:"), 0, 5);
        grid.add(descriptionArea, 1, 5);
        grid.add(new Label("条形码:"), 0, 6);
        grid.add(barcodeField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // 更新商品信息
                product.setItemName(nameField.getText());
                product.setCategory(categoryField.getText());
                product.setPrice(Integer.parseInt(priceField.getText()));
                product.setStock(Integer.parseInt(stockField.getText()));
                product.setPictureLink(imageField.getText());
                product.setDescription(descriptionArea.getText());
                product.setBarcode(barcodeField.getText());

                // 发送更新请求
                updateProduct(product);
            }
        });
    }

    private void updateProduct(Item product) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("更新商品中..."));

                // 构建更新商品请求
                Map<String, Object> data = new HashMap<>();
                data.put("item", product);
                Request request = new Request("updateItem", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("更新商品成功");
                        // 刷新商品列表
                        loadAllItems();
                    });
                } else {
                    Platform.runLater(() ->
                            setStatus("更新商品失败: " + responseMap.get("message")));
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        setStatus("更新商品错误: " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteProduct(String productId) {
        // 确认删除对话框
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认删除");
        confirmDialog.setHeaderText("确认删除商品");
        confirmDialog.setContentText("确定要删除这个商品吗？此操作不可恢复。");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        Platform.runLater(() -> setStatus("删除商品中..."));

                        // 构建删除商品请求
                        Map<String, Object> data = new HashMap<>();
                        data.put("itemId", productId);
                        Request request = new Request("deleteItem", data);

                        // 使用ClientNetworkHelper发送请求
                        String netResponse = ClientNetworkHelper.send(request);

                        // 解析响应
                        Map<String, Object> responseMap = gson.fromJson(netResponse, Map.class);
                        int code = ((Double) responseMap.get("code")).intValue();

                        if (code == 200) {
                            Platform.runLater(() -> {
                                setStatus("删除商品成功");
                                // 刷新商品列表
                                loadAllItems();
                            });
                        } else {
                            Platform.runLater(() ->
                                    setStatus("删除商品失败: " + responseMap.get("message")));
                        }
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                setStatus("删除商品错误: " + e.getMessage()));
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private void setStatus(String message) {
        statusLabel.setText("状态: " + message);
    }
}