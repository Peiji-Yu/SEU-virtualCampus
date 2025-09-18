package Client.store.admin;

import Client.ClientNetworkHelper;
import Client.store.model.Item;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class ManageProductPanel extends BorderPane {
    // UI组件
    private TextField searchField;
    private HBox categoryCheckBoxContainer;
    private Button searchButton;
    private Label resultsLabel;
    private VBox productsContainer;

    // 当前展开的卡片
    private ExpandableProductCard currentExpandedCard;

    // JSON处理
    private Gson gson;

    public ManageProductPanel() {
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
        Label titleLabel = new Label("管理商品");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查找和管理商店商品");
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

        searchField = createStyledTextField("输入商品名称或条形码进行搜索");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> performSearch());

        searchRow.getChildren().addAll(searchField, searchButton);

        // 类别筛选
        categoryCheckBoxContainer = new HBox(10);
        categoryCheckBoxContainer.setPadding(new Insets(0, 0, 5, 0));
        categoryCheckBoxContainer.setAlignment(Pos.CENTER_LEFT);

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

        // 中心商品展示区域
        productsContainer = new VBox(15);
        productsContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(productsContainer);
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
        List<String> selectedCategories = categoryCheckBoxContainer.getChildren().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());

        new Thread(() -> {
            try {
                List<Item> items = new ArrayList<>();

                // 构建搜索请求
                Map<String, Object> data = new HashMap<>();

                // 如果没有选中任何类别，则发送一个空类别请求
                if (selectedCategories.isEmpty()) {
                    data.put("keyword", searchText.isEmpty() ? "" : searchText);
                    // 不传递category参数
                    Request request = new Request("searchItems", data);
                    String response = ClientNetworkHelper.send(request);

                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Type itemListType = new TypeToken<List<Item>>(){}.getType();
                        List<Item> foundItems = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);
                        items.addAll(foundItems);
                    }
                } else {
                    // 对每个选中的类别发送请求
                    for (String category : selectedCategories) {
                        Map<String, Object> categoryData = new HashMap<>();
                        categoryData.put("keyword", searchText.isEmpty() ? "" : searchText);
                        categoryData.put("category", category);

                        Request request = new Request("searchItemsByCategory", categoryData);
                        String response = ClientNetworkHelper.send(request);

                        Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                        int code = ((Double) responseMap.get("code")).intValue();

                        if (code == 200) {
                            Type itemListType = new TypeToken<List<Item>>(){}.getType();
                            List<Item> foundItems = gson.fromJson(gson.toJson(responseMap.get("data")), itemListType);
                            items.addAll(foundItems);
                        }
                    }
                }

                // 在UI线程中更新界面
                Platform.runLater(() -> {
                    displayProducts(items);
                });

            } catch (Exception e) {
                System.err.println("通信错误");
                e.printStackTrace();
            }
        }).start();
    }

    private void displayProducts(List<Item> products) {
        productsContainer.getChildren().clear();
        currentExpandedCard = null;

        if (products.isEmpty()) {
            resultsLabel.setText("找到 0 个商品");
            return;
        }

        for (Item product : products) {
            ExpandableProductCard card = new ExpandableProductCard(product);

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

            productsContainer.getChildren().add(card);
        }

        resultsLabel.setText("找到 " + products.size() + " 个商品");
    }

    // 内部类：可折叠的商品卡片
    private class ExpandableProductCard extends VBox {
        private final Item item;
        private boolean expanded = false;
        private static boolean isAnyCardEditing = false; // 跟踪当前是否有卡片处于编辑状态
        private VBox detailBox;
        private boolean isEditing = false;
        private HBox actionButtons;
        private HBox confirmCancelButtons;

        // 编辑状态下的组件
        private TextField nameField;
        private TextField categoryField;
        private TextField priceField;
        private TextField stockField;
        private TextField imageField;
        private TextArea descriptionArea;
        private TextField barcodeField;

        public ExpandableProductCard(Item item) {
            this.item = item;
            initializeUI();
        }

        private void initializeUI() {
            setPadding(new Insets(15));
            setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
            setSpacing(10);

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
            infoBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label nameLabel = new Label(item.getItemName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label categoryLabel = new Label("类别: " + item.getCategory());
            categoryLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

            infoBox.getChildren().addAll(nameLabel, categoryLabel);

            // 商品状态信息
            VBox statusBox = new VBox(5);
            statusBox.setAlignment(Pos.CENTER_RIGHT);

            Label priceLabel = new Label("¥" + item.getPriceYuan());
            priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label stockLabel = new Label("库存: " + item.getStock());
            stockLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");

            statusBox.getChildren().addAll(priceLabel, stockLabel);

            summaryBox.getChildren().addAll(imageView, infoBox, statusBox);

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

            // 修改商品按钮
            Button modifyButton = new Button("修改");
            modifyButton.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-size: 14px;");
            modifyButton.setOnAction(e -> startEditing());

            // 删除商品按钮
            Button deleteItemButton = new Button("删除");
            deleteItemButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-size: 14px;");
            deleteItemButton.setOnAction(e -> deleteItem());

            actionButtons.getChildren().addAll(modifyButton, deleteItemButton);

            // 编辑时的按钮区域（默认隐藏）
            confirmCancelButtons = new HBox(10);
            confirmCancelButtons.setAlignment(Pos.CENTER_RIGHT);
            confirmCancelButtons.setVisible(false);
            confirmCancelButtons.setManaged(false);

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

            confirmCancelButtons.getChildren().addAll(spacer, confirmButton, cancelButton);

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
                addItemDetails();
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

        private void addItemDetails() {
            detailBox.getChildren().clear();

            // 添加基本信息与详细信息之间的分割线
//            Region separator1 = createSeparator();
//            detailBox.getChildren().add(separator1);

            // 创建详细信息容器
            VBox detailsContainer = new VBox(10);
            detailsContainer.setFillWidth(true);
            detailsContainer.setPadding(new Insets(10, 0, 0, 0));

            // 使用表格形式展示商品信息
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
            Label idHeader = new Label("商品ID");
            idHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            Label barcodeHeader = new Label("条形码");
            barcodeHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            Label salesHeader = new Label("销量");
            salesHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");


            infoGrid.add(idHeader, 0, 0);
            infoGrid.add(barcodeHeader, 1, 0);
            infoGrid.add(salesHeader, 2, 0);


            // 表头与内容之间的分割线
            Region headerSeparator = createSeparator();
            infoGrid.add(headerSeparator, 0, 1, 3, 1); // 跨3列

            // 内容 - 只读模式
            Label idValue = new Label(item.getUuid());
            idValue.setStyle("-fx-font-size: 14px;");

            Label barcodeValue = new Label(item.getBarcode() != null ? item.getBarcode() : "无");
            barcodeValue.setStyle("-fx-font-size: 14px;");

            Label salesValue = new Label(String.valueOf(item.getSalesVolume()));
            salesValue.setStyle("-fx-font-size: 14px;");

            infoGrid.add(idValue, 0, 2);
            infoGrid.add(barcodeValue, 1, 2);
            infoGrid.add(salesValue, 2, 2);

            detailsContainer.getChildren().add(infoGrid);

            // 添加基本信息表格与描述之间的分割线
//            Region separator2 = createSeparator();
//            detailsContainer.getChildren().add(separator2);

            // 商品描述
            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                HBox descriptionBox = new HBox(10);
                descriptionBox.setAlignment(Pos.TOP_LEFT);

                Label descLabel = new Label(item.getDescription());
                descLabel.setStyle("-fx-wrap-text: true; -fx-font-size: 14px;");

                // 创建加粗的标签
                Label descTitle = new Label("描述:");
                descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                descTitle.setMinWidth(60);

                descriptionBox.getChildren().addAll(descTitle, descLabel);
                detailsContainer.getChildren().add(descriptionBox);
            }

            detailBox.getChildren().add(detailsContainer);
        }

        private void startEditing() {
            isEditing = true;
            isAnyCardEditing = true; // 设置全局编辑状态
            updateEditMode();
        }

        private void cancelEditing() {
            isEditing = false;
            isAnyCardEditing = false; // 取消全局编辑状态
            updateEditMode();
            // 重新加载详情以恢复原始数据
            addItemDetails();
        }

        private void updateEditMode() {
            actionButtons.setVisible(!isEditing && expanded);
            actionButtons.setManaged(!isEditing && expanded);
            confirmCancelButtons.setVisible(isEditing);
            confirmCancelButtons.setManaged(isEditing);

            if (isEditing) {
                // 切换到编辑模式
                detailBox.getChildren().clear();

                // 添加基本信息与详细信息之间的分割线
                Region separator1 = createSeparator();
                detailBox.getChildren().add(separator1);

                // 创建详细信息容器
                VBox detailsContainer = new VBox(10);
                detailsContainer.setFillWidth(true);
                detailsContainer.setPadding(new Insets(10, 0, 0, 0));

                // 使用表格形式展示商品信息
                GridPane infoGrid = new GridPane();
                infoGrid.setHgap(10);
                infoGrid.setVgap(5);
                infoGrid.setPadding(new Insets(0, 0, 10, 0));

                // 设置列约束，使两列等宽
                ColumnConstraints col1 = new ColumnConstraints();
                col1.setPercentWidth(50);
                ColumnConstraints col2 = new ColumnConstraints();
                col2.setPercentWidth(50);
                infoGrid.getColumnConstraints().addAll(col1, col2);

                // 表头
                Label nameHeader = new Label("商品名称");
                nameHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                Label categoryHeader = new Label("类别");
                categoryHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                infoGrid.add(nameHeader, 0, 0);
                infoGrid.add(categoryHeader, 1, 0);

                // 内容 - 编辑模式
                nameField = new TextField(item.getItemName());
                nameField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");

                categoryField = new TextField(item.getCategory());
                categoryField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");

                infoGrid.add(nameField, 0, 1);
                infoGrid.add(categoryField, 1, 1);

                // 第二行
                Label priceHeader = new Label("价格(元)");
                priceHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                Label stockHeader = new Label("库存");
                stockHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                infoGrid.add(priceHeader, 0, 2);
                infoGrid.add(stockHeader, 1, 2);

                // 第二行内容
                priceField = new TextField(String.valueOf(item.getPriceYuan()));
                priceField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");

                stockField = new TextField(String.valueOf(item.getStock()));
                stockField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");

                infoGrid.add(priceField, 0, 3);
                infoGrid.add(stockField, 1, 3);

                // 第三行
                Label barcodeHeader = new Label("条形码");
                barcodeHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                Label imageHeader = new Label("图片链接");
                imageHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                infoGrid.add(barcodeHeader, 0, 4);
                infoGrid.add(imageHeader, 1, 4);

                // 第三行内容
                barcodeField = new TextField(item.getBarcode() != null ? item.getBarcode() : "");
                barcodeField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");

                imageField = new TextField(item.getPictureLink() != null ? item.getPictureLink() : "");
                imageField.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");

                infoGrid.add(barcodeField, 0, 5);
                infoGrid.add(imageField, 1, 5);

                detailsContainer.getChildren().add(infoGrid);

                // 添加基本信息表格与描述之间的分割线
                Region separator2 = createSeparator();
                detailsContainer.getChildren().add(separator2);

                // 商品描述 - 编辑模式使用文本区域
                HBox descriptionBox = new HBox(10);
                descriptionBox.setAlignment(Pos.TOP_LEFT);

                Label descTitle = new Label("描述:");
                descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                descTitle.setMinWidth(60);

                descriptionArea = new TextArea(item.getDescription() != null ? item.getDescription() : "");
                descriptionArea.setWrapText(true);
                descriptionArea.setPrefRowCount(3);
                descriptionArea.setStyle("-fx-font-size: 14px; -fx-background-radius: 5; " +
                        "-fx-focus-color: #176B3A; -fx-faint-focus-color:transparent; " +
                        "-fx-border-radius: 5;");
                HBox.setHgrow(descriptionArea, Priority.ALWAYS);

                descriptionBox.getChildren().addAll(descTitle, descriptionArea);
                detailsContainer.getChildren().add(descriptionBox);

                detailBox.getChildren().add(detailsContainer);
            }
        }

        private void confirmChanges() {
            // 更新商品信息
            item.setItemName(nameField.getText());
            item.setCategory(categoryField.getText());
            item.setPrice(Math.round(Float.parseFloat(priceField.getText())*100));
            item.setStock(Integer.parseInt(stockField.getText()));
            item.setBarcode(barcodeField.getText());
            item.setPictureLink(imageField.getText());
            item.setDescription(descriptionArea.getText());

            // 更新商品信息
            updateItem(item);

            isEditing = false;
            isAnyCardEditing = false; // 取消全局编辑状态

            try {
                Thread.sleep(100); //等待数据库更新
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 刷新显示
            performSearch();
        }

        private void deleteItem() {
            // 确认删除对话框
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("确认删除");
            confirmDialog.setHeaderText("确认删除商品");
            confirmDialog.setContentText("确定要删除《" + item.getItemName() + "》吗？此操作不可撤销。");

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deleteAllItem(item);
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

    private void updateItem(Item item) {
        new Thread(() -> {
            try {
                // 构建更新请求
                Map<String, Object> data = new HashMap<>();
                data.put("item", item);
                Request request = new Request("updateItem", data);

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
                        alert.setContentText("更新商品信息失败: " + responseMap.get("message"));
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("更新错误");
                    alert.setContentText("更新商品信息时发生错误: " + e.getMessage());
                    alert.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteAllItem(Item item) {
        new Thread(() -> {
            try {
                // 构建删除请求
                Map<String, Object> data = new HashMap<>();
                data.put("itemId", item.getUuid());
                Request request = new Request("deleteItem", data);

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
                        alert.setContentText("删除商品失败: " + responseMap.get("message"));
                        alert.showAndWait();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("错误");
                    alert.setHeaderText("删除错误");
                    alert.setContentText("删除商品时发生错误: " + e.getMessage());
                    alert.showAndWait();
                });
                e.printStackTrace();
            }
        }).start();
    }
}