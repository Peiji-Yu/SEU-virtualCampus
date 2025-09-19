package Client.panel.store.admin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Client.model.store.SalesStatItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import Client.ClientNetworkHelper;
import Client.panel.store.util.StoreUtils;
import Client.model.Request;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class SalesStatsPanel extends BorderPane {
    private Gson gson;
    private Label todaySalesLabel;
    private Label totalSalesLabel;
    private HBox tabButtonContainer;
    private Button todayTabButton;
    private Button totalTabButton;
    private StackPane contentContainer;
    private VBox todayStatsContainer;
    private VBox totalStatsContainer;
    private ObservableList<SalesStatItem> todayStatsData = FXCollections.observableArrayList();
    private ObservableList<SalesStatItem> totalStatsData = FXCollections.observableArrayList();

    public SalesStatsPanel() {
        // 初始化Gson适配器
        GsonBuilder gsonBuilder = new GsonBuilder();
        gson = gsonBuilder.create();

        initializeUI();
        loadSalesData();
    }

    private void initializeUI() {
        setPadding(new Insets(40, 80, 20, 80));
        setStyle("-fx-background-color: white;");

        // 顶部标题区域
        VBox topContainer = new VBox(5);
        topContainer.setPadding(new Insets(30, 30, 0, 30));

        // 标题
        Label titleLabel = new Label("销售统计");
        titleLabel.setFont(Font.font(32));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000;");

        Label subtitleLabel = new Label("查看商品销售情况和收入统计");
        subtitleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");

        VBox headtitleBox = new VBox(5, titleLabel, subtitleLabel);
        headtitleBox.setAlignment(Pos.CENTER_LEFT);
        headtitleBox.setPadding(new Insets(0, 0, 20, 0));

        // 销售总额卡片容器
        HBox salesCardsContainer = new HBox(20);
        salesCardsContainer.setAlignment(Pos.CENTER_LEFT);

        // 今日销售总额卡片
        VBox todaySalesCard = createSalesCard("今日销售总额", "加载中...");
        todaySalesLabel = (Label) todaySalesCard.getChildren().get(1);

        // 全部销售总额卡片
        VBox totalSalesCard = createSalesCard("全部销售总额", "加载中...");
        totalSalesLabel = (Label) totalSalesCard.getChildren().get(1);

        salesCardsContainer.getChildren().addAll(todaySalesCard, totalSalesCard);

        topContainer.getChildren().addAll(headtitleBox, salesCardsContainer);
        setTop(topContainer);

        // 销售统计表格区域
        VBox tableContainer = new VBox(15);
        tableContainer.setPadding(new Insets(20, 30, 0, 30));

        Label tableTitle = new Label("商品销售明细");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // 创建选项卡按钮容器
        tabButtonContainer = new HBox();
        tabButtonContainer.setSpacing(0);
        tabButtonContainer.setAlignment(Pos.CENTER_LEFT);

        // 创建今日销售按钮
        todayTabButton = new Button("当日销售");
        todayTabButton.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 5 0 0 5;");
        todayTabButton.setCursor(Cursor.HAND);

        // 创建全部销售按钮
        totalTabButton = new Button("全部销售");
        totalTabButton.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 0 5 5 0;");
        totalTabButton.setCursor(Cursor.HAND);

        // 添加按钮到容器
        tabButtonContainer.getChildren().addAll(todayTabButton, totalTabButton);

        // 创建内容容器
        contentContainer = new StackPane();

        // 初始化滚动面板

        // 当日销售标签页
        todayStatsContainer = new VBox();
        todayStatsContainer.setPadding(new Insets(10));
        ScrollPane todayScrollPane = new ScrollPane(todayStatsContainer);
        todayScrollPane.setFitToWidth(true);
        todayScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");

        // 全部销售标签页
        totalStatsContainer = new VBox();
        totalStatsContainer.setPadding(new Insets(10));
        ScrollPane totalScrollPane = new ScrollPane(totalStatsContainer);
        totalScrollPane.setFitToWidth(true);
        totalScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");

        // 默认显示今日销售
        contentContainer.getChildren().addAll(todayScrollPane, totalScrollPane);
        todayScrollPane.setVisible(true);
        totalScrollPane.setVisible(false);

        // 添加按钮点击事件
        todayTabButton.setOnAction(e -> {
            todayTabButton.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 10 20; -fx-background-radius: 5 0 0 5;");
            totalTabButton.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; -fx-font-weight: bold; " +
                    "-fx-padding: 10 20; -fx-background-radius: 0 5 5 0;");
            todayScrollPane.setVisible(true);
            totalScrollPane.setVisible(false);
        });

        totalTabButton.setOnAction(e -> {
            totalTabButton.setStyle("-fx-background-color: #176B3A; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-padding: 10 20; -fx-background-radius: 0 5 5 0;");
            todayTabButton.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; -fx-font-weight: bold; " +
                    "-fx-padding: 10 20; -fx-background-radius: 5 0 0 5;");
            todayScrollPane.setVisible(false);
            totalScrollPane.setVisible(true);
        });

        // 将按钮和内容添加到界面
        VBox tabContentContainer = new VBox(10);
        tabContentContainer.getChildren().addAll(tabButtonContainer, contentContainer);

        tableContainer.getChildren().addAll(tableTitle, tabContentContainer);
        setCenter(tableContainer);
    }

    private VBox createSalesCard(String titleText, String amountText) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; " +
                "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1;");
        card.setMinWidth(250);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 16px; -fx-text-fill: #6c757d;");

        Label amountLabel = new Label(amountText);
        amountLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #176B3A;");

        card.getChildren().addAll(title, amountLabel);
        return card;
    }

    public void loadSalesData() {
        new Thread(() -> {
            try {
                // 获取今日销售总额
                Request todaySalesRequest = new Request("getTodaySales", null);
                String todaySalesResponse = ClientNetworkHelper.send(todaySalesRequest);
                Map<String, Object> todaySalesResponseMap = gson.fromJson(todaySalesResponse, Map.class);
                int todaySalesCode = ((Double) todaySalesResponseMap.get("code")).intValue();

                if (todaySalesCode == 200) {
                    Object todaySalesData = todaySalesResponseMap.get("data");
                    int todaySalesFen = todaySalesData != null ? ((Double) todaySalesData).intValue() : 0;
                    String todaySalesYuan = StoreUtils.fenToYuan(todaySalesFen);

                    Platform.runLater(() -> {
                        todaySalesLabel.setText("¥" + todaySalesYuan);
                    });
                }

                // 获取全部销售总额
                Request totalSalesRequest = new Request("getSales", null);
                String totalSalesResponse = ClientNetworkHelper.send(totalSalesRequest);
                Map<String, Object> totalSalesResponseMap = gson.fromJson(totalSalesResponse, Map.class);
                int totalSalesCode = ((Double) totalSalesResponseMap.get("code")).intValue();

                if (totalSalesCode == 200) {
                    Object totalSalesData = totalSalesResponseMap.get("data");
                    int totalSalesFen = totalSalesData != null ? ((Double) totalSalesData).intValue() : 0;
                    String totalSalesYuan = StoreUtils.fenToYuan(totalSalesFen);

                    Platform.runLater(() -> {
                        totalSalesLabel.setText("¥" + totalSalesYuan);
                    });
                }

                // 获取当日销售统计
                Request todayStatsRequest = new Request("getTodaySalesStats", null);
                String todayStatsResponse = ClientNetworkHelper.send(todayStatsRequest);
                Map<String, Object> todayStatsResponseMap = gson.fromJson(todayStatsResponse, Map.class);
                int todayStatsCode = ((Double) todayStatsResponseMap.get("code")).intValue();

                if (todayStatsCode == 200) {
                    Type salesStatsListType = new TypeToken<List<SalesStatItem>>(){}.getType();
                    List<SalesStatItem> todaySalesStats = new ArrayList<>();

                    if (todayStatsResponseMap.get("data") != null) {
                        todaySalesStats = gson.fromJson(
                                gson.toJson(todayStatsResponseMap.get("data")),
                                salesStatsListType
                        );
                    }

                    List<SalesStatItem> finalTodaySalesStats = todaySalesStats;
                    Platform.runLater(() -> {
                        todayStatsData.clear();
                        todayStatsData.addAll(finalTodaySalesStats);
                        displayStatsTable(todayStatsContainer, todayStatsData);
                    });
                }

                // 获取全部销售统计
                Request totalStatsRequest = new Request("getSalesStats", null);
                String totalStatsResponse = ClientNetworkHelper.send(totalStatsRequest);
                Map<String, Object> totalStatsResponseMap = gson.fromJson(totalStatsResponse, Map.class);
                int totalStatsCode = ((Double) totalStatsResponseMap.get("code")).intValue();

                if (totalStatsCode == 200) {
                    Type salesStatsListType = new TypeToken<List<SalesStatItem>>(){}.getType();
                    List<SalesStatItem> totalSalesStats = new ArrayList<>();

                    if (totalStatsResponseMap.get("data") != null) {
                        totalSalesStats = gson.fromJson(
                                gson.toJson(totalStatsResponseMap.get("data")),
                                salesStatsListType
                        );
                    }

                    List<SalesStatItem> finalTotalSalesStats = totalSalesStats;
                    Platform.runLater(() -> {
                        totalStatsData.clear();
                        totalStatsData.addAll(finalTotalSalesStats);
                        displayStatsTable(totalStatsContainer, totalStatsData);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    todaySalesLabel.setText("加载失败");
                    totalSalesLabel.setText("加载失败");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void displayStatsTable(VBox container, ObservableList<SalesStatItem> data) {
        container.getChildren().clear();

        if (data.isEmpty()) {
            Label noDataLabel = new Label("暂无销售数据");
            noDataLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
            noDataLabel.setPadding(new Insets(20));
            container.getChildren().add(noDataLabel);
            return;
        }

        // 创建表格容器
        VBox tableContainer = new VBox();
        tableContainer.setFillWidth(true);

        // 创建表头
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setPadding(new Insets(0, 0, 5, 0));

        // 设置列约束，使三列等宽
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(40);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(30);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(30);
        headerGrid.getColumnConstraints().addAll(col1, col2, col3);

        // 表头
        Label headerName = new Label("商品名称");
        headerName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label headerAmount = new Label("销售数量");
        headerAmount.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label headerRevenue = new Label("销售收入");
        headerRevenue.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        headerGrid.add(headerName, 0, 0);
        headerGrid.add(headerAmount, 1, 0);
        headerGrid.add(headerRevenue, 2, 0);

        tableContainer.getChildren().add(headerGrid);

        // 添加表头与内容之间的分割线
        Region headerSeparator = createSeparator();
        tableContainer.getChildren().add(headerSeparator);

        // 添加数据行
        for (SalesStatItem item : data) {
            GridPane rowGrid = new GridPane();
            rowGrid.setHgap(10);
            rowGrid.setPadding(new Insets(5, 0, 5, 0));

            // 使用相同的列约束
            rowGrid.getColumnConstraints().addAll(col1, col2, col3);

            Label nameLabel = new Label(item.getItemName());
            nameLabel.setStyle("-fx-font-size: 14px;");

            Label amountLabel = new Label(String.valueOf(item.getTotalAmount()));
            amountLabel.setStyle("-fx-font-size: 14px;");

            Label revenueLabel = new Label(item.getTotalRevenueYuan());
            revenueLabel.setStyle("-fx-font-size: 14px;");

            rowGrid.add(nameLabel, 0, 0);
            rowGrid.add(amountLabel, 1, 0);
            rowGrid.add(revenueLabel, 2, 0);

            tableContainer.getChildren().add(rowGrid);

            // 为每个项目添加分割线
            if (data.indexOf(item) < data.size() - 1) {
                Region itemSeparator = createSeparator();
                tableContainer.getChildren().add(itemSeparator);
            }
        }

        container.getChildren().add(tableContainer);
    }

    // 创建分割线辅助方法
    private Region createSeparator() {
        Region separator = new Region();
        separator.setStyle("-fx-background-color: #cccccc; -fx-pref-height: 1px;");
        separator.setMaxWidth(Double.MAX_VALUE);
        return separator;
    }
}