package Client.panel.store.admin;

import Client.ClientNetworkHelper;
import Client.model.store.Order;
import Client.model.store.OrderItem;
import Client.util.adapter.UUIDAdapter;
import Client.model.Request;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ManageOrderPanel extends BorderPane {
    private VBox ordersContainer;
    private TextField searchField;
    private Label resultsLabel;

    private Gson gson;

    // 当前展开的卡片
    private OrderCard currentExpandedCard;

    public ManageOrderPanel() {
        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        // 初始加载所有订单
        searchOrders();
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题和搜索区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("管理订单");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查看和管理所有订单");
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

        searchField = createStyledTextField("输入订单ID或用户卡号进行搜索");
        // 限制搜索框只能输入数字
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                searchField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchButton = new Button("搜索");
        searchButton.setStyle("-fx-background-color: #176B3A; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-pref-width: 120px; -fx-pref-height: 45px; -fx-background-radius: 5;");
        searchButton.setOnAction(e -> searchOrders());

        searchRow.getChildren().addAll(searchField, searchButton);

        // 结果标签
        resultsLabel = new Label("找到 0 个订单");
        resultsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");

        searchBox.getChildren().addAll(headtitleBox, searchRow, resultsLabel);
        topContainer.getChildren().addAll(searchBox);
        setTop(topContainer);

        // 中心订单展示区域
        ordersContainer = new VBox(15);
        ordersContainer.setPadding(new Insets(0, 28, 5, 28));

        ScrollPane scrollPane = new ScrollPane(ordersContainer);
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

    private void searchOrders() {
        String keyword = searchField.getText().trim();

        // 根据关键字类型调用不同的接口
        if (keyword.isEmpty()) {
            // 搜索框为空，获取所有订单
            loadAllOrders();
        } else if (keyword.length() == 9) {
            // 9位数字，认为是卡号
            try {
                int cardNumber = Integer.parseInt(keyword);
                loadUserOrders(cardNumber);
            } catch (NumberFormatException e) {
                showAlert("错误", "卡号格式不正确");
            }
        } else {
            // 其他情况，按订单ID搜索
            loadOrderById(keyword);
        }
    }

    private void loadAllOrders() {
        new Thread(() -> {
            try {
                // 构建获取所有订单请求
                Request request = new Request("getAllOrders", null);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取订单列表
                    Type orderListType = new TypeToken<List<Order>>(){}.getType();
                    List<Order> orderList = gson.fromJson(gson.toJson(responseMap.get("data")), orderListType);

                    Platform.runLater(() -> {
                        displayOrders(orderList);
                    });
                } else {
                    System.err.println("加载失败: " + responseMap.get("message"));
                }
            } catch (Exception e) {
                System.err.println("通信错误: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadUserOrders(int cardNumber) {
        new Thread(() -> {
            try {
                // 构建获取用户订单请求
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", cardNumber);
                Request request = new Request("getUserOrders", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取订单列表
                    Type orderListType = new TypeToken<List<Order>>(){}.getType();
                    List<Order> orderList = gson.fromJson(gson.toJson(responseMap.get("data")), orderListType);

                    Platform.runLater(() -> {
                        displayOrders(orderList);
                    });
                } else {
                    System.err.println("加载失败: " + responseMap.get("message"));
                }
            } catch (Exception e) {
                System.err.println("通信错误: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadOrderById(String orderId) {
        new Thread(() -> {
            try {
                // 构建获取订单详情请求
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", orderId);
                Request request = new Request("getOrder", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取单个订单
                    Order order = gson.fromJson(gson.toJson(responseMap.get("data")), Order.class);

                    Platform.runLater(() -> {
                        displayOrders(Collections.singletonList(order));
                    });
                } else {
                    Platform.runLater(() -> {
                        System.err.println("未找到订单: " + responseMap.get("message"));
                        displayOrders(Collections.emptyList());
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    System.err.println("通信错误: " + e.getMessage());
                    displayOrders(Collections.emptyList());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void displayOrders(List<Order> orderList) {
        ordersContainer.getChildren().clear();
        currentExpandedCard = null;

        if (orderList == null || orderList.isEmpty()) {
            resultsLabel.setText("找到 0 个订单");
            return;
        }

        for (Order order : orderList) {
            OrderCard card = new OrderCard(order);

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

            ordersContainer.getChildren().add(card);
        }
        resultsLabel.setText("找到 " + orderList.size() + " 个订单");
    }

    private void refundOrder(String orderId) {
        // 创建退款确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("退款订单");
        alert.setHeaderText("确认要对订单 " + orderId + " 执行退款操作吗？");
        alert.setContentText("此操作不可撤销");

        // 设置对话框样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 14px;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    // 构建退款订单请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", orderId);
                    Request request = new Request("refundOrder", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        Platform.runLater(() -> {
                            showAlert("成功", "订单退款成功");
                            // 刷新订单列表
                            searchOrders();
                        });
                    } else {
                        Platform.runLater(() -> {
                            showAlert("错误", "退款失败: " + responseMap.get("message"));
                        });
                    }
                } catch (Exception e) {
                    System.err.println("退款过程中发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 设置对话框样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-font-size: 14px;");

        alert.showAndWait();
    }

    // 检查订单是否在7天内创建
    private boolean isOrderWithin7Days(Order order) {
        try {
            // 解析订单时间
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            LocalDateTime orderTime = LocalDateTime.parse(order.getTime(), formatter);

            // 获取当前时间
            LocalDateTime now = LocalDateTime.now();

            // 计算天数差
            long daysBetween = ChronoUnit.DAYS.between(orderTime, now);

            return daysBetween < 7;
        } catch (Exception e) {
            System.err.println("解析订单时间错误: " + e.getMessage());
            return false;
        }
    }

    // 内部类：订单卡片
    private class OrderCard extends VBox {
        private final Order order;
        private boolean expanded = false;
        private VBox itemsContainer;
        private HBox buttonBox;

        public OrderCard(Order order) {
            this.order = order;
            initializeUI();
        }

        private void initializeUI() {
            setPadding(new Insets(15));
            setStyle("-fx-background-color: white; -fx-background-radius: 5; " +
                    "-fx-border-color: #dddddd; -fx-border-radius: 5; -fx-border-width: 1;");
            setSpacing(10);

            // 订单基本信息区域（始终显示）
            HBox headerBox = new HBox();
            headerBox.setAlignment(Pos.CENTER_LEFT);
            headerBox.setSpacing(15);

            // 左侧订单信息
            VBox leftBox = new VBox(5);
            leftBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(leftBox, Priority.ALWAYS);

            Label idLabel = new Label("订单号: " + order.getUuid());
            idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label timeLabel = new Label("时间: " + order.getTime().replace("T", " "));
            timeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

            Label userLabel = new Label("用户卡号: " + order.getCardNumber());
            userLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

            leftBox.getChildren().addAll(idLabel, timeLabel, userLabel);

            // 右侧价格和状态信息
            VBox rightBox = new VBox(5);
            rightBox.setAlignment(Pos.CENTER_RIGHT);

            Label totalLabel = new Label("¥" + order.getTotalYuan());
            totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label statusLabel = new Label(order.getStatus());
            statusLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) +
                    "; -fx-font-weight: bold; -fx-font-size: 14px;");

            rightBox.getChildren().addAll(totalLabel, statusLabel);

            headerBox.getChildren().addAll(leftBox, rightBox);

            // 商品信息容器（默认折叠）
            itemsContainer = new VBox(10);
            itemsContainer.setPadding(new Insets(10, 0, 0, 0));
            itemsContainer.setVisible(false);
            itemsContainer.setManaged(false);

            // 底部操作按钮区域（始终显示）
            buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            buttonBox.setPadding(new Insets(10, 0, 0, 0));

            // 根据订单状态显示退款按钮（仅在已支付且7天内创建的订单显示）
            if ("已支付".equals(order.getStatus()) && isOrderWithin7Days(order)) {
                Button refundBtn = new Button("退款");
                refundBtn.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-pref-width: 60; -fx-background-radius: 5;");
                refundBtn.setOnAction(e -> refundOrder(order.getUuid()));
                buttonBox.getChildren().add(refundBtn);
            }

            getChildren().addAll(headerBox, itemsContainer, buttonBox);
        }

        public void toggleExpand() {
            expanded = !expanded;
            itemsContainer.setVisible(expanded);
            itemsContainer.setManaged(expanded);

            if (expanded) {
                addOrderItems();
            } else {
                itemsContainer.getChildren().clear();
            }
        }

        public void collapse() {
            if (expanded) {
                expanded = false;
                itemsContainer.setVisible(false);
                itemsContainer.setManaged(false);
                itemsContainer.getChildren().clear();
            }
        }

        public boolean isExpanded() {
            return expanded;
        }

        private void addOrderItems() {
            itemsContainer.getChildren().clear();

            // 添加分割线
            Region separator = createSeparator();
            itemsContainer.getChildren().add(separator);

            // 添加订单项
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                for (int i = 0; i < order.getItems().size(); i++) {
                    OrderItem item = order.getItems().get(i);
                    HBox itemBox = createItemBox(item);
                    itemsContainer.getChildren().add(itemBox);

                    // 在每两个商品之间添加分割线
                    if (i < order.getItems().size() - 1) {
                        Region itemSeparator = createSeparator();
                        itemsContainer.getChildren().add(itemSeparator);
                    }
                }
            } else {
                // 加载订单项详情
                loadOrderItems();

                // 显示加载中的占位符
                Label loadingLabel = new Label("加载商品信息中...");
                loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
                itemsContainer.getChildren().add(loadingLabel);
            }
        }

        private HBox createItemBox(OrderItem item) {
            HBox itemBox = new HBox(15);
            itemBox.setAlignment(Pos.CENTER_LEFT);

            // 商品图片
            ImageView imageView = new ImageView();
            imageView.setFitWidth(60);
            imageView.setFitHeight(60);
            imageView.setPreserveRatio(true);

            String image_url = item.getItem().getPictureLink();
            if (image_url != null && !image_url.isEmpty()) {
                try {
                    Image image = new Image(image_url, true);
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
            VBox itemInfo = new VBox(5);
            itemInfo.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(itemInfo, Priority.ALWAYS);

            Label nameLabel = new Label(item.getItem().getItemName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label descLabel = new Label(item.getItem().getDescription() != null ?
                    item.getItem().getDescription() : "暂无描述");
            descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            itemInfo.getChildren().addAll(nameLabel, descLabel);

            // 价格和数量
            VBox priceBox = new VBox(5);
            priceBox.setAlignment(Pos.CENTER_RIGHT);

            Label priceLabel = new Label(String.format("¥%.2f", item.getItemPrice() / 100.0));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #000000;");

            Label quantityLabel = new Label("x" + item.getAmount());
            quantityLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

            priceBox.getChildren().addAll(priceLabel, quantityLabel);

            itemBox.getChildren().addAll(imageView, itemInfo, priceBox);

            return itemBox;
        }

        private void loadOrderItems() {
            new Thread(() -> {
                try {
                    // 构建获取订单详细信息请求
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", order.getUuid());
                    Request request = new Request("getOrder", data);

                    // 使用ClientNetworkHelper发送请求
                    String response = ClientNetworkHelper.send(request);

                    // 解析响应
                    Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                    int code = ((Double) responseMap.get("code")).intValue();

                    if (code == 200) {
                        // 提取订单详情
                        Order detail_order = gson.fromJson(gson.toJson(responseMap.get("data")), Order.class);
                        order.setItems(detail_order.getItems());

                        // 更新UI
                        Platform.runLater(() -> {
                            // 重新添加订单项
                            if (expanded) {
                                addOrderItems();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // 创建分割线辅助方法
    private Region createSeparator() {
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);
        return separator;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "待支付": return "#f39c12";
            case "已支付": return "#27ae60";
            case "已取消": return "#e74c3c";
            case "已退款": return "#95a5a6";
            default: return "#34495e";
        }
    }
}