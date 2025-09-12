package Client.store;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class StoreMainPanel extends BorderPane {
    private final String cardNumber;
    private final String userType;
    private TabPane storeTabPane;

    public StoreMainPanel(String cardNumber, String userType) {
        this.cardNumber = cardNumber;
        this.userType = userType;
        buildUI();
    }

    private boolean isAdmin() {
        return "admin".equalsIgnoreCase(userType);
    }

    private void buildUI() {
        // 商店功能栏（右侧，颜色#EEEEEE）
        VBox storeSidebar = new VBox();
        storeSidebar.setStyle("-fx-background-color: #EEEEEE; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1px;");
        storeSidebar.setPrefWidth(320);
        storeSidebar.setPadding(new Insets(0, 0, 0, 0));
        storeSidebar.setAlignment(Pos.TOP_CENTER);

        // TabPane：购物、我的订单、管理工具
        storeTabPane = new TabPane();
        storeTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        storeTabPane.getTabs().add(buildShoppingTab());
        storeTabPane.getTabs().add(buildMyOrdersTab());
        if (isAdmin()) {
            storeTabPane.getTabs().add(buildAdminTab());
        }
        storeSidebar.getChildren().add(storeTabPane);
        setRight(storeSidebar);
    }

    // 商品卡片数据结构
    class Product {
        String id;
        String name;
        double price;
        String imagePath;
        String description;
        public Product(String id, String name, double price, String imagePath, String description) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imagePath = imagePath;
            this.description = description;
        }
    }

    // 购物车项数据结构
    class CartItem {
        Product product;
        int quantity;
        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }

    private Tab buildShoppingTab() {
        Tab tab = new Tab("购物");
        VBox shoppingRoot = new VBox(12);
        shoppingRoot.setAlignment(Pos.TOP_CENTER);
        shoppingRoot.setPadding(new Insets(16));

        // 商品卡片区
        FlowPane productCardPane = new FlowPane();
        productCardPane.setHgap(18);
        productCardPane.setVgap(18);
        productCardPane.setPadding(new Insets(8));
        productCardPane.setPrefWrapLength(900);
        productCardPane.setStyle("-fx-background-color: transparent;");

        // 模拟商品数据
        List<Product> products = new ArrayList<>();
        products.add(new Product("1", "Java编程思想", 89.00, "/Image/java_book.jpg", "经典Java教材"));
        products.add(new Product("2", "Python编程", 75.50, "/Image/python_book.jpg", "Python入门到精通"));
        products.add(new Product("3", "算法导论", 120.00, "/Image/algorithms_book.jpg", "算法经典教材"));
        products.add(new Product("4", "设计模式", 68.00, "/Image/design_patterns.jpg", "软件开发设计模式"));
        products.add(new Product("5", "数据库系统", 79.00, "/Image/database_book.jpg", "数据库系统概念"));
        products.add(new Product("6", "操作系统", 92.00, "/Image/os_book.jpg", "操作系统原理"));
        products.add(new Product("7", "编译原理", 88.50, "/Image/compiler_book.jpg", "编译原理与技术"));

        // 购物车数据
        List<CartItem> cartItems = new ArrayList<>();

        // 商品卡片生成
        for (Product product : products) {
            VBox card = new VBox(6);
            card.setPrefSize(180, 240);
            card.setStyle("-fx-background-color: #fff; -fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6,0,0,2);");
            card.setPadding(new Insets(10));
            card.setAlignment(Pos.TOP_CENTER);
            ImageView imageView = new ImageView();
            try {
                imageView.setImage(new Image(product.imagePath, 120, 120, true, true));
            } catch (Exception e) {
                imageView.setImage(new Image("/Image/default.jpg", 120, 120, true, true));
            }
            Label nameLabel = new Label(product.name);
            nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
            Label priceLabel = new Label("￥" + product.price);
            priceLabel.setTextFill(Color.web("#1976d2"));
            priceLabel.setStyle("-fx-font-size: 14px;");
            Label descLabel = new Label(product.description);
            descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            descLabel.setWrapText(true);
            Button addBtn = new Button("加入购物车");
            addBtn.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-background-radius: 6;");
            addBtn.setOnAction(e -> {
                // 加入购物车逻辑
                boolean found = false;
                for (CartItem item : cartItems) {
                    if (item.product.id.equals(product.id)) {
                        item.quantity++;
                        found = true;
                        break;
                    }
                }
                if (!found) cartItems.add(new CartItem(product, 1));
                updateCartBar(cartItems);
            });
            card.getChildren().addAll(imageView, nameLabel, priceLabel, descLabel, addBtn);
            productCardPane.getChildren().add(card);
        }
        shoppingRoot.getChildren().add(productCardPane);

        // 底部购物车上滑栏
        StackPane cartBarContainer = new StackPane();
        cartBarContainer.setPrefHeight(80);
        cartBarContainer.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #e2e8f0; -fx-border-width: 1px 0 0 0; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8,0,0,2);");
        cartBarContainer.setAlignment(Pos.BOTTOM_CENTER);
        shoppingRoot.getChildren().add(cartBarContainer);

        // 购物车栏内容更新方法
        updateCartBar(cartItems, cartBarContainer);

        tab.setContent(shoppingRoot);
        return tab;
    }

    // 更新购物车栏内容
    private void updateCartBar(List<CartItem> cartItems) {
        // ...不带容器参数的旧方法，兼容性保留...
    }
    private void updateCartBar(List<CartItem> cartItems, StackPane cartBarContainer) {
        cartBarContainer.getChildren().clear();
        int totalCount = cartItems.stream().mapToInt(item -> item.quantity).sum();
        double totalPrice = cartItems.stream().mapToDouble(item -> item.product.price * item.quantity).sum();
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 24, 8, 24));
        Label info = new Label("已选 " + totalCount + " 项商品，共 ￥" + String.format("%.2f", totalPrice));
        info.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        Button expandBtn = new Button("展开购物车");
        expandBtn.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-background-radius: 6;");
        expandBtn.setOnAction(e -> showCartDetail(cartItems, cartBarContainer));
        bar.getChildren().addAll(info, expandBtn);
        cartBarContainer.getChildren().add(bar);
    }
    // 展开购物车详细内容
    private void showCartDetail(List<CartItem> cartItems, StackPane cartBarContainer) {
        VBox detailBox = new VBox(10);
        detailBox.setStyle("-fx-background-color: #fff; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12,0,0,2);");
        detailBox.setMaxHeight(320);
        detailBox.setAlignment(Pos.TOP_CENTER);
        for (CartItem item : cartItems) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(item.product.name);
            name.setPrefWidth(120);
            Label price = new Label("￥" + item.product.price);
            price.setPrefWidth(60);
            Label qty = new Label("x" + item.quantity);
            qty.setPrefWidth(40);
            Button removeBtn = new Button("移除");
            removeBtn.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-background-radius: 6;");
            removeBtn.setOnAction(e -> {
                cartItems.remove(item);
                updateCartBar(cartItems, cartBarContainer);
            });
            row.getChildren().addAll(name, price, qty, removeBtn);
            detailBox.getChildren().add(row);
        }
        HBox bottomBar = new HBox(16);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        Button clearBtn = new Button("清空购物车");
        clearBtn.setStyle("-fx-background-color: #bdbdbd; -fx-text-fill: white; -fx-background-radius: 6;");
        clearBtn.setOnAction(e -> {
            cartItems.clear();
            updateCartBar(cartItems, cartBarContainer);
        });
        Button orderBtn = new Button("创建订单");
        orderBtn.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-background-radius: 6;");
        bottomBar.getChildren().addAll(clearBtn, orderBtn);
        detailBox.getChildren().add(bottomBar);
        cartBarContainer.getChildren().clear();
        cartBarContainer.getChildren().add(detailBox);
    }

    private Tab buildMyOrdersTab() {
        Tab tab = new Tab("我的订单");
        VBox ordersRoot = new VBox(12);
        ordersRoot.setAlignment(Pos.TOP_CENTER);
        ordersRoot.setPadding(new Insets(16));

        // 订单列表区
        TableView<OrderRow> orderTable = new TableView<>();
        orderTable.setPrefWidth(600);
        orderTable.setPrefHeight(400);
        TableColumn<OrderRow, String> idCol = new TableColumn<>("订单号");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        TableColumn<OrderRow, String> itemsCol = new TableColumn<>("商品明细");
        itemsCol.setCellValueFactory(data -> data.getValue().itemsProperty());
        TableColumn<OrderRow, String> priceCol = new TableColumn<>("金额");
        priceCol.setCellValueFactory(data -> data.getValue().priceProperty());
        TableColumn<OrderRow, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());
        TableColumn<OrderRow, String> timeCol = new TableColumn<>("下单时间");
        timeCol.setCellValueFactory(data -> data.getValue().timeProperty());
        TableColumn<OrderRow, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button detailBtn = new Button("详情");
            private final Button cancelBtn = new Button("取消");
            {
                detailBtn.setOnAction(e -> showOrderDetail(getTableView().getItems().get(getIndex())));
                cancelBtn.setOnAction(e -> cancelOrder(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, detailBtn, cancelBtn);
                    setGraphic(box);
                }
            }
        });
        orderTable.getColumns().addAll(idCol, itemsCol, priceCol, statusCol, timeCol, actionCol);

        // 模拟订单数据
        orderTable.getItems().addAll(
            new OrderRow("20230911001", "Java编程思想 x1, Python编程 x2", "164.50", "已支付", "2025-09-11 10:23"),
            new OrderRow("20230911002", "算法导论 x1", "120.00", "待支付", "2025-09-10 15:12")
        );

        ordersRoot.getChildren().add(orderTable);
        tab.setContent(ordersRoot);
        return tab;
    }

    // 订单数据结构
    public static class OrderRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty items;
        private final SimpleStringProperty price;
        private final SimpleStringProperty status;
        private final SimpleStringProperty time;
        public OrderRow(String id, String items, String price, String status, String time) {
            this.id = new SimpleStringProperty(id);
            this.items = new SimpleStringProperty(items);
            this.price = new SimpleStringProperty(price);
            this.status = new SimpleStringProperty(status);
            this.time = new SimpleStringProperty(time);
        }
        public ObservableValue<String> idProperty() { return id; }
        public ObservableValue<String> itemsProperty() { return items; }
        public ObservableValue<String> priceProperty() { return price; }
        public ObservableValue<String> statusProperty() { return status; }
        public ObservableValue<String> timeProperty() { return time; }
        public String getId() { return id.get(); }
        public String getItems() { return items.get(); }
        public String getPrice() { return price.get(); }
        public String getStatus() { return status.get(); }
        public String getTime() { return time.get(); }
        public void setStatus(String s) { status.set(s); }
    }

    // 订单详情弹窗
    private void showOrderDetail(OrderRow order) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("订单详情");
        alert.setHeaderText("订单号: " + order.getId());
        alert.setContentText("商品明细: " + order.getItems() + "\n金额: ￥" + order.getPrice() + "\n状态: " + order.getStatus() + "\n下单时间: " + order.getTime());
        alert.showAndWait();
    }

    // 取消订单逻辑（仅模拟）
    private void cancelOrder(OrderRow order) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "确定要取消订单 " + order.getId() + " 吗？", ButtonType.YES, ButtonType.NO);
        alert.setTitle("取消订单");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                order.setStatus("已取消");
            }
        });
    }

    private Tab buildAdminTab() {
        Tab tab = new Tab("管理工具");
        VBox adminRoot = new VBox(18);
        adminRoot.setAlignment(Pos.TOP_CENTER);
        adminRoot.setPadding(new Insets(16));

        // 商品管理区
        Label productManageLabel = new Label("商品管理");
        productManageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        TableView<ProductRow> productTable = new TableView<>();
        productTable.setPrefWidth(600);
        productTable.setPrefHeight(260);
        TableColumn<ProductRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        TableColumn<ProductRow, String> nameCol = new TableColumn<>("名称");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        TableColumn<ProductRow, String> priceCol = new TableColumn<>("价格");
        priceCol.setCellValueFactory(data -> data.getValue().priceProperty());
        TableColumn<ProductRow, String> stockCol = new TableColumn<>("库存");
        stockCol.setCellValueFactory(data -> data.getValue().stockProperty());
        TableColumn<ProductRow, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("修改");
            private final Button delBtn = new Button("删除");
            {
                editBtn.setOnAction(e -> showEditProductDialog(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> getTableView().getItems().remove(getIndex()));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, editBtn, delBtn);
                    setGraphic(box);
                }
            }
        });
        productTable.getColumns().addAll(idCol, nameCol, priceCol, stockCol, actionCol);
        // 模拟商品数据
        productTable.getItems().addAll(
            new ProductRow("1", "Java编程思想", "89.00", "20"),
            new ProductRow("2", "Python编程", "75.50", "15")
        );

        // 添加商品表单
        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField(); nameField.setPromptText("名称");
        TextField priceField = new TextField(); priceField.setPromptText("价格");
        TextField stockField = new TextField(); stockField.setPromptText("库存");
        Button addBtn = new Button("添加商品");
        addBtn.setStyle("-fx-background-color: #43a047; -fx-text-fill: white; -fx-background-radius: 6;");
        addBtn.setOnAction(e -> {
            String name = nameField.getText();
            String price = priceField.getText();
            String stock = stockField.getText();
            if (!name.isEmpty() && !price.isEmpty() && !stock.isEmpty()) {
                productTable.getItems().add(new ProductRow(
                    String.valueOf(productTable.getItems().size()+1), name, price, stock));
                nameField.clear(); priceField.clear(); stockField.clear();
            }
        });
        addBox.getChildren().addAll(new Label("添加商品:"), nameField, priceField, stockField, addBtn);

        adminRoot.getChildren().addAll(productManageLabel, productTable, addBox);

        // 订单管理区
        Label orderManageLabel = new Label("全部订单管理");
        orderManageLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        TableView<OrderRow> allOrderTable = new TableView<>();
        allOrderTable.setPrefWidth(600);
        allOrderTable.setPrefHeight(220);
        TableColumn<OrderRow, String> idCol2 = new TableColumn<>("订单号");
        idCol2.setCellValueFactory(data -> data.getValue().idProperty());
        TableColumn<OrderRow, String> itemsCol2 = new TableColumn<>("商品明细");
        itemsCol2.setCellValueFactory(data -> data.getValue().itemsProperty());
        TableColumn<OrderRow, String> priceCol2 = new TableColumn<>("金额");
        priceCol2.setCellValueFactory(data -> data.getValue().priceProperty());
        TableColumn<OrderRow, String> statusCol2 = new TableColumn<>("状态");
        statusCol2.setCellValueFactory(data -> data.getValue().statusProperty());
        TableColumn<OrderRow, String> timeCol2 = new TableColumn<>("下单时间");
        timeCol2.setCellValueFactory(data -> data.getValue().timeProperty());
        TableColumn<OrderRow, Void> actionCol2 = new TableColumn<>("操作");
        actionCol2.setCellFactory(col -> new TableCell<>() {
            private final Button detailBtn = new Button("详情");
            private final Button setStatusBtn = new Button("设为已完成");
            {
                detailBtn.setOnAction(e -> showOrderDetail(getTableView().getItems().get(getIndex())));
                setStatusBtn.setOnAction(e -> getTableView().getItems().get(getIndex()).status.set("已完成"));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8, detailBtn, setStatusBtn);
                    setGraphic(box);
                }
            }
        });
        allOrderTable.getColumns().addAll(idCol2, itemsCol2, priceCol2, statusCol2, timeCol2, actionCol2);
        // 模拟全部订单数据
        allOrderTable.getItems().addAll(
            new OrderRow("20230911001", "Java编程思想 x1, Python编程 x2", "164.50", "已支付", "2025-09-11 10:23"),
            new OrderRow("20230911002", "算法导论 x1", "120.00", "待支付", "2025-09-10 15:12")
        );

        adminRoot.getChildren().addAll(orderManageLabel, allOrderTable);
        tab.setContent(adminRoot);
        return tab;
    }

    // 商品管理数据结构
    public static class ProductRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty price;
        private final SimpleStringProperty stock;
        public ProductRow(String id, String name, String price, String stock) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.price = new SimpleStringProperty(price);
            this.stock = new SimpleStringProperty(stock);
        }
        public ObservableValue<String> idProperty() { return id; }
        public ObservableValue<String> nameProperty() { return name; }
        public ObservableValue<String> priceProperty() { return price; }
        public ObservableValue<String> stockProperty() { return stock; }
        public String getId() { return id.get(); }
        public String getName() { return name.get(); }
        public String getPrice() { return price.get(); }
        public String getStock() { return stock.get(); }
        public void setName(String s) { name.set(s); }
        public void setPrice(String s) { price.set(s); }
        public void setStock(String s) { stock.set(s); }
    }

    // 商品编辑弹窗
    private void showEditProductDialog(ProductRow product) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("修改商品");
        dialog.setHeaderText("商品ID: " + product.getId());
        TextField nameField = new TextField(product.getName());
        TextField priceField = new TextField(product.getPrice());
        TextField stockField = new TextField(product.getStock());
        VBox box = new VBox(10, new Label("名称:"), nameField, new Label("价格:"), priceField, new Label("库存:"), stockField);
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                product.setName(nameField.getText());
                product.setPrice(priceField.getText());
                product.setStock(stockField.getText());
            }
            return null;
        });
        dialog.showAndWait();
    }
}
