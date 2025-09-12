package Client.store.student;

import Client.ClientNetworkHelper;
import Client.store.util.StoreUtils;
import Client.store.util.model.Order;
import Client.store.util.model.OrderItem;
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
import javafx.scene.layout.*;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;

public class CustomerOrderPanel extends BorderPane {
    private final String cardNumber;
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private VBox ordersContainer;
    private TextField searchField;
    private Label statusLabel;
    private Label resultsLabel;
    private Button refreshBtn;

    private Gson gson;

    public CustomerOrderPanel(String cardNumber) {
        this.cardNumber = cardNumber;

        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        gsonBuilder.registerTypeAdapter(UUID.class, new UUIDAdapter());
        gson = gsonBuilder.create();

        initializeUI();
        loadOrders();
    }

    private void initializeUI() {
        setPadding(new Insets(15));

        // 顶部搜索区域
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        topBox.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("搜索订单号...");
        searchField.setPrefWidth(250);

        Button searchBtn = new Button("搜索");
        searchBtn.setOnAction(e -> searchOrders());

        refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> loadOrders());

        topBox.getChildren().addAll(new Label("订单搜索:"), searchField, searchBtn, refreshBtn);
        setTop(topBox);

        // 结果数量标签
        resultsLabel = new Label("加载中...");
        resultsLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 0 0 10 0;");

        // 中心订单展示区域
        ordersContainer = new VBox(15);
        ordersContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(ordersContainer);
        scrollPane.setFitToWidth(true);

        VBox centerBox = new VBox(10);
        centerBox.getChildren().addAll(resultsLabel, scrollPane);
        setCenter(centerBox);

        // 底部状态栏
        statusLabel = new Label("就绪");
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        setBottom(statusLabel);
    }

    private void loadOrders() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    setStatus("加载中...");
                    refreshBtn.setDisable(true);
                });

                // 构建获取订单请求
                Map<String, Object> data = new HashMap<>();
                data.put("cardNumber", Integer.parseInt(cardNumber));
                Request request = new Request("getUserOrders", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("服务器响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取订单列表
                    Type orderListType = new TypeToken<List<Order>>(){}.getType();
                    List<Order> orderList = gson.fromJson(gson.toJson(responseMap.get("data")), orderListType);

                    orders.setAll(orderList);

                    // 在UI线程中更新界面
                    Platform.runLater(() -> {
                        displayOrders(orders);
                        setStatus("加载完成");
                        refreshBtn.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("加载失败: " + responseMap.get("message"));
                        resultsLabel.setText("加载失败");
                        refreshBtn.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("通信错误: " + e.getMessage());
                    resultsLabel.setText("通信错误");
                    refreshBtn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void searchOrders() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            displayOrders(orders);
            return;
        }

        List<Order> filteredOrders = new ArrayList<>();
        for (Order order : orders) {
            if (order.getUuid().toLowerCase().contains(keyword)) {
                filteredOrders.add(order);
            }
        }

        displayOrders(filteredOrders);
    }

    private void displayOrders(List<Order> orderList) {
        Platform.runLater(() -> {
            ordersContainer.getChildren().clear();

            if (orderList.isEmpty()) {
                Label emptyLabel = new Label("没有找到订单");
                ordersContainer.getChildren().add(emptyLabel);
                resultsLabel.setText("找到 0 条订单记录");
                return;
            }

            for (Order order : orderList) {
                HBox orderCard = createOrderCard(order);
                ordersContainer.getChildren().add(orderCard);
            }

            resultsLabel.setText("找到 " + orderList.size() + " 条订单记录");
        });
    }

    private HBox createOrderCard(Order order) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #ddd; -fx-border-radius: 8; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.CENTER_LEFT);

        // 订单基本信息
        VBox infoBox = new VBox(5);
        Label idLabel = new Label("订单号: " + order.getUuid());
        idLabel.setStyle("-fx-font-weight: bold;");

        Label timeLabel = new Label("时间: " + order.getTime());
        Label totalLabel = new Label("金额: " + order.getTotalYuan() + "元");
        Label statusLabel = new Label("状态: " + order.getStatus());
        statusLabel.setStyle("-fx-text-fill: " + getStatusColor(order.getStatus()) + ";");

        infoBox.getChildren().addAll(idLabel, timeLabel, totalLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 操作按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button detailBtn = new Button("详情");
        detailBtn.setOnAction(e -> showOrderDetail(order));

        // 根据订单状态显示不同按钮
        if ("待支付".equals(order.getStatus())) {
            Button payBtn = new Button("支付");
            payBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            payBtn.setOnAction(e -> payOrder(order.getUuid()));

            Button cancelBtn = new Button("取消");
            cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            cancelBtn.setOnAction(e -> cancelOrder(order.getUuid()));

            buttonBox.getChildren().addAll(detailBtn, payBtn, cancelBtn);
        } else if ("已支付".equals(order.getStatus())) {
            Button refundBtn = new Button("申请退款");
            refundBtn.setOnAction(e -> refundOrder(order.getUuid()));

            buttonBox.getChildren().addAll(detailBtn, refundBtn);
        } else {
            buttonBox.getChildren().add(detailBtn);
        }

        card.getChildren().addAll(infoBox, spacer, buttonBox);

        return card;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "待支付": return "#ff9800";
            case "已支付": return "#4CAF50";
            case "已取消": return "#f44336";
            case "已退款": return "#9e9e9e";
            default: return "#000000";
        }
    }

    private void showOrderDetail(Order order) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("加载订单详情中..."));

                // 构建获取订单详情请求
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", order.getUuid());
                Request request = new Request("getOrderDetail", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("订单详情响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    // 提取订单详情
                    Order detailedOrder = gson.fromJson(gson.toJson(responseMap.get("data")), Order.class);

                    Platform.runLater(() -> {
                        // 创建并显示订单详情对话框
                        Dialog<Void> dialog = new Dialog<>();
                        dialog.setTitle("订单详情");
                        dialog.setHeaderText("订单号: " + detailedOrder.getUuid());

                        // 创建详情内容
                        VBox content = new VBox(10);
                        content.setPadding(new Insets(10));

                        Label timeLabel = new Label("时间: " + detailedOrder.getTime());
                        Label totalLabel = new Label("总金额: " + detailedOrder.getTotalYuan() + "元");
                        Label statusLabel = new Label("状态: " + detailedOrder.getStatus());
                        statusLabel.setStyle("-fx-text-fill: " + getStatusColor(detailedOrder.getStatus()) + ";");

                        if (detailedOrder.getRemark() != null && !detailedOrder.getRemark().isEmpty()) {
                            Label remarkLabel = new Label("备注: " + detailedOrder.getRemark());
                            content.getChildren().add(remarkLabel);
                        }

                        // 添加商品列表
                        Label itemsLabel = new Label("商品列表:");
                        itemsLabel.setStyle("-fx-font-weight: bold;");

                        VBox itemsBox = new VBox(5);
                        for (OrderItem item : detailedOrder.getItems()) {
                            HBox itemBox = new HBox(10);
                            Label itemName = new Label(item.getItem().getItemName() + " x " + item.getAmount());
                            Label itemPrice = new Label(StoreUtils.fenToYuan(item.getPrice() * item.getAmount()) + "元");

                            itemBox.getChildren().addAll(itemName, itemPrice);
                            itemsBox.getChildren().add(itemBox);
                        }

                        content.getChildren().addAll(timeLabel, totalLabel, statusLabel, itemsLabel, itemsBox);

                        dialog.getDialogPane().setContent(content);
                        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                        dialog.showAndWait();

                        setStatus("订单详情加载完成");
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("加载订单详情失败: " + responseMap.get("message"));
                        showAlert("错误", "加载订单详情失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("加载订单详情错误: " + e.getMessage());
                    showAlert("错误", "加载订单详情时发生错误: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void payOrder(String orderId) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("支付中..."));

                // 构建支付订单请求
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", orderId);
                Request request = new Request("payOrder", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("支付响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("支付成功");
                        showAlert("成功", "订单支付成功");
                        // 刷新订单列表
                        loadOrders();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("支付失败: " + responseMap.get("message"));
                        showAlert("错误", "支付失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("支付错误: " + e.getMessage());
                    showAlert("错误", "支付过程中发生错误: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void cancelOrder(String orderId) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("取消中..."));

                // 构建取消订单请求
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", orderId);
                Request request = new Request("cancelOrder", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("取消响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("取消成功");
                        showAlert("成功", "订单取消成功");
                        // 刷新订单列表
                        loadOrders();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("取消失败: " + responseMap.get("message"));
                        showAlert("错误", "取消失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("取消错误: " + e.getMessage());
                    showAlert("错误", "取消过程中发生错误: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void refundOrder(String orderId) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> setStatus("申请退款中..."));

                // 构建退款请求
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", orderId);
                Request request = new Request("refundOrder", data);

                // 使用ClientNetworkHelper发送请求
                String response = ClientNetworkHelper.send(request);
                System.out.println("退款响应: " + response);

                // 解析响应
                Map<String, Object> responseMap = gson.fromJson(response, Map.class);
                int code = ((Double) responseMap.get("code")).intValue();

                if (code == 200) {
                    Platform.runLater(() -> {
                        setStatus("退款申请已提交");
                        showAlert("成功", "退款申请已提交，等待处理");
                        // 刷新订单列表
                        loadOrders();
                    });
                } else {
                    Platform.runLater(() -> {
                        setStatus("退款申请失败: " + responseMap.get("message"));
                        showAlert("错误", "退款申请失败: " + responseMap.get("message"));
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus("退款申请错误: " + e.getMessage());
                    showAlert("错误", "退款申请过程中发生错误: " + e.getMessage());
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
        statusLabel.setText(message);
    }
}