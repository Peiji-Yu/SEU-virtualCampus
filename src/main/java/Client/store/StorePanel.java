package Client.store;

import Client.ClientNetworkHelper;
import Server.model.Request;
import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class StorePanel extends BorderPane {
    private final String cardNumber;
    private final String userType; // 判断是否管理员

    private final ObservableList<ItemRow> itemData = FXCollections.observableArrayList();
    private final ObservableList<CartRow> cartData = FXCollections.observableArrayList();

    // 订单数据
    private final ObservableList<OrderRow> myOrderData = FXCollections.observableArrayList();
    private final ObservableList<OrderRow> allOrderData = FXCollections.observableArrayList();

    // UI 组件
    private TableView<ItemRow> itemTable;
    private TableView<CartRow> cartTable;
    private TableView<OrderRow> myOrdersTable;
    private TableView<OrderRow> allOrdersTable;

    private Label cartTotalLabel;
    private TextField searchField;
    private ComboBox<String> categoryCombo; // 类别筛选
    private TextField idSearchField; // 按UUID查商品

    // 管理员商品表单
    private TextField formName, formPrice, formCategory, formStock, formBarcode, formPicture, formDescription, formUuid, formSalesVolume;

    // 订单备注输入
    private TextField orderRemarkField;

    private TabPane tabPane;

    public StorePanel(String cardNumber, String userType) {
        this.cardNumber = cardNumber;
        this.userType = userType;
        setPadding(new Insets(10));
        buildUI();
        // 初始加载
        loadAllItems();
        fetchCategories();
        fetchMyOrders();
        if (isAdmin()) fetchAllOrders();
    }

    private boolean isAdmin(){ return "admin".equalsIgnoreCase(userType); }

    private void buildUI(){
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().add(buildProductsTab());
        tabPane.getTabs().add(buildMyOrdersTab());
        if (isAdmin()) {
            tabPane.getTabs().add(buildAdminItemsTab());
            tabPane.getTabs().add(buildAllOrdersTab());
        }
        setCenter(tabPane);
    }

    /* ================= 商品浏览 + 购物车 ================= */
    private Tab buildProductsTab(){
        Tab t = new Tab("商品");
        VBox root = new VBox(10);

        HBox searchBar = new HBox(8);
        searchField = new TextField();
        searchField.setPromptText("关键字(名称/描述)");
        Button searchBtn = new Button("搜索");
        searchBtn.setOnAction(e -> doSearch());
        Button allBtn = new Button("全部"); allBtn.setOnAction(e -> { searchField.clear(); categoryCombo.getSelectionModel().selectFirst(); loadAllItems(); });
        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("类别");
        categoryCombo.getItems().add("全部");
        categoryCombo.getSelectionModel().selectFirst();
        categoryCombo.setOnAction(e -> categoryOrKeywordSearch());

        idSearchField = new TextField();
        idSearchField.setPromptText("按UUID查商品");
        Button idBtn = new Button("查询");
        idBtn.setOnAction(e -> getItemById());

        searchBar.getChildren().addAll(new Label("类别:"), categoryCombo, new Label("搜索:"), searchField, searchBtn, allBtn, idSearchField, idBtn);

        // 商品表
        itemTable = new TableView<>(itemData);
        TableColumn<ItemRow,String> cId = new TableColumn<>("UUID"); cId.setCellValueFactory(new PropertyValueFactory<>("uuid")); cId.setPrefWidth(170);
        TableColumn<ItemRow,String> cName = new TableColumn<>("名称"); cName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        TableColumn<ItemRow,String> cPrice = new TableColumn<>("单价(元)"); cPrice.setCellValueFactory(new PropertyValueFactory<>("priceYuan")); cPrice.setPrefWidth(80);
        TableColumn<ItemRow,Integer> cStock = new TableColumn<>("库存"); cStock.setCellValueFactory(new PropertyValueFactory<>("stock")); cStock.setPrefWidth(60);
        TableColumn<ItemRow,String> cCat = new TableColumn<>("类别"); cCat.setCellValueFactory(new PropertyValueFactory<>("category")); cCat.setPrefWidth(80);
        TableColumn<ItemRow,Integer> cSales = new TableColumn<>("销量"); cSales.setCellValueFactory(new PropertyValueFactory<>("salesVolume")); cSales.setPrefWidth(70);
        itemTable.getColumns().addAll(cId,cName,cPrice,cStock,cCat,cSales);
        itemTable.setPrefHeight(240);
        // 行描述提示
        itemTable.setRowFactory(tv -> {
            TableRow<ItemRow> row = new TableRow<>();
            Tooltip tip = new Tooltip();
            row.itemProperty().addListener((o,ov,nv)->{
                if(nv==null){ row.setTooltip(null); } else { tip.setText(Optional.ofNullable(nv.description).orElse("无描述")); row.setTooltip(tip); }
            });
            return row;
        });

        // 加入购物车
        HBox addBox = new HBox(8);
        Spinner<Integer> qtySpin = new Spinner<>(1,999,1);
        Button addBtn = new Button("加入购物车");
        addBtn.setOnAction(e -> addSelectedToCart(qtySpin.getValue()));
        addBox.getChildren().addAll(new Label("数量:"), qtySpin, addBtn);
        addBox.setAlignment(Pos.CENTER_LEFT);

        // 购物车表
        cartTable = new TableView<>(cartData);
        TableColumn<CartRow,String> ccName = new TableColumn<>("商品"); ccName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        TableColumn<CartRow,Integer> ccQty = new TableColumn<>("数量"); ccQty.setCellValueFactory(new PropertyValueFactory<>("quantity")); ccQty.setPrefWidth(60);
        TableColumn<CartRow,String> ccSub = new TableColumn<>("小计(元)"); ccSub.setCellValueFactory(new PropertyValueFactory<>("subtotalYuan")); ccSub.setPrefWidth(90);
        cartTable.getColumns().addAll(ccName,ccQty,ccSub);
        cartTable.setPrefHeight(160);

        HBox cartOps = new HBox(8);
        Button removeBtn = new Button("移除选中"); removeBtn.setOnAction(e -> removeSelected());
        Button clearBtn = new Button("清空"); clearBtn.setOnAction(e -> { cartData.clear(); updateCartTotal(); });
        cartTotalLabel = new Label("合计: 0.00 元");
        orderRemarkField = new TextField(); orderRemarkField.setPromptText("订单备注(可选)"); orderRemarkField.setPrefWidth(160);
        Button createOrderBtn = new Button("创建订单"); createOrderBtn.setOnAction(e -> createOrder());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        cartOps.getChildren().addAll(removeBtn, clearBtn, spacer, new Label("备注:"), orderRemarkField, cartTotalLabel, createOrderBtn);

        root.getChildren().addAll(searchBar, itemTable, addBox, new Label("购物车"), cartTable, cartOps);
        t.setContent(root);
        return t;
    }

    /* ================= 我的订单 ================= */
    private Tab buildMyOrdersTab(){
        Tab t = new Tab("我的订单");
        VBox root = new VBox(10);
        myOrdersTable = buildOrderTable(myOrderData, true, false);
        HBox op = new HBox(8);
        Button refresh = new Button("刷新"); refresh.setOnAction(e -> fetchMyOrders());
        op.getChildren().addAll(refresh);
        root.getChildren().addAll(op, myOrdersTable);
        t.setContent(root);
        return t;
    }

    /* ================= 管理员: 商品管理 ================= */
    private Tab buildAdminItemsTab(){
        Tab t = new Tab("商品管理");
        VBox root = new VBox(10);
        root.setPadding(new Insets(4));
        Label tips = new Label("选择行可编辑；新增不填UUID自动生成；销量可选");

        GridPane form = new GridPane(); form.setHgap(8); form.setVgap(6);
        formUuid = new TextField(); formUuid.setPromptText("UUID(更新/删除用)"); formUuid.setPrefWidth(280);
        formName = new TextField(); formName.setPromptText("名称");
        formPrice = new TextField(); formPrice.setPromptText("价格(元)");
        formCategory = new TextField(); formCategory.setPromptText("类别");
        formStock = new TextField(); formStock.setPromptText("库存");
        formBarcode = new TextField(); formBarcode.setPromptText("条码");
        formPicture = new TextField(); formPicture.setPromptText("图片链接");
        formDescription = new TextField(); formDescription.setPromptText("描述");
        formSalesVolume = new TextField(); formSalesVolume.setPromptText("销量(可选)");

        int r=0;
        form.add(new Label("UUID"),0,r); form.add(formUuid,1,r,3,1); r++;
        form.add(new Label("名称"),0,r); form.add(formName,1,r); form.add(new Label("价格"),2,r); form.add(formPrice,3,r); r++;
        form.add(new Label("类别"),0,r); form.add(formCategory,1,r); form.add(new Label("库存"),2,r); form.add(formStock,3,r); r++;
        form.add(new Label("条码"),0,r); form.add(formBarcode,1,r); form.add(new Label("图片"),2,r); form.add(formPicture,3,r); r++;
        form.add(new Label("描述"),0,r); form.add(formDescription,1,r,3,1); r++;
        form.add(new Label("销量"),0,r); form.add(formSalesVolume,1,r); r++;

        HBox btns = new HBox(10);
        Button add = new Button("添加"); add.setOnAction(e -> adminAddItem());
        Button update = new Button("更新"); update.setOnAction(e -> adminUpdateItem());
        Button del = new Button("删除"); del.setOnAction(e -> adminDeleteItem());
        Button fillSel = new Button("填充选中"); fillSel.setOnAction(e -> fillFormFromSelection());
        Button clear = new Button("清空"); clear.setOnAction(e -> clearForm());
        btns.getChildren().addAll(add, update, del, fillSel, clear);

        TableView<ItemRow> adminTable = new TableView<>(itemData);
        TableColumn<ItemRow,String> cId = new TableColumn<>("UUID"); cId.setCellValueFactory(new PropertyValueFactory<>("uuid")); cId.setPrefWidth(170);
        TableColumn<ItemRow,String> cName = new TableColumn<>("名称"); cName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        TableColumn<ItemRow,String> cPrice = new TableColumn<>("价格(元)"); cPrice.setCellValueFactory(new PropertyValueFactory<>("priceYuan")); cPrice.setPrefWidth(80);
        TableColumn<ItemRow,Integer> cStock = new TableColumn<>("库存"); cStock.setCellValueFactory(new PropertyValueFactory<>("stock")); cStock.setPrefWidth(60);
        TableColumn<ItemRow,String> cCat = new TableColumn<>("类别"); cCat.setCellValueFactory(new PropertyValueFactory<>("category")); cCat.setPrefWidth(80);
        TableColumn<ItemRow,Integer> cSales = new TableColumn<>("销量"); cSales.setCellValueFactory(new PropertyValueFactory<>("salesVolume")); cSales.setPrefWidth(70);
        adminTable.getColumns().addAll(cId,cName,cPrice,cStock,cCat,cSales);
        adminTable.setPrefHeight(280);
        adminTable.getSelectionModel().selectedItemProperty().addListener((o,ov,nv)->{ if(nv!=null) formUuid.setText(nv.uuid); });

        Button refresh = new Button("刷新商品"); refresh.setOnAction(e -> loadAllItems());

        root.getChildren().addAll(tips, form, btns, refresh, adminTable);
        t.setContent(new ScrollPane(root));
        return t;
    }

    /* ================= 管理员: 全部订单 ================= */
    private Tab buildAllOrdersTab(){
        Tab t = new Tab("全部订单");
        VBox root = new VBox(10);
        allOrdersTable = buildOrderTable(allOrderData, false, true);
        HBox op = new HBox(8);
        TextField orderIdField = new TextField(); orderIdField.setPromptText("按UUID查询");
        Button query = new Button("查询"); query.setOnAction(e -> getOrderById(orderIdField.getText().trim()));
        Button refresh = new Button("刷新"); refresh.setOnAction(e -> fetchAllOrders());
        TextField userCardField = new TextField(); userCardField.setPromptText("按卡号查该用户订单");
        Button userQuery = new Button("查用户"); userQuery.setOnAction(e -> adminGetUserOrders(userCardField.getText().trim()));
        op.getChildren().addAll(orderIdField, query, userCardField, userQuery, refresh);
        root.getChildren().addAll(op, allOrdersTable);
        t.setContent(root);
        return t;
    }

    /* ================= 构建订单表 ================= */
    private TableView<OrderRow> buildOrderTable(ObservableList<OrderRow> data, boolean userTable, boolean adminAll){
        TableView<OrderRow> tv = new TableView<>(data);
        TableColumn<OrderRow,String> cId = new TableColumn<>("订单UUID"); cId.setCellValueFactory(new PropertyValueFactory<>("uuid")); cId.setPrefWidth(180);
        if (adminAll){
            TableColumn<OrderRow,String> cCard = new TableColumn<>("卡号"); cCard.setCellValueFactory(new PropertyValueFactory<>("cardNumber")); cCard.setPrefWidth(80);
            tv.getColumns().add(cCard);
        }
        TableColumn<OrderRow,String> cAmt = new TableColumn<>("金额(元)"); cAmt.setCellValueFactory(new PropertyValueFactory<>("totalYuan")); cAmt.setPrefWidth(90);
        TableColumn<OrderRow,String> cStatus = new TableColumn<>("状态"); cStatus.setCellValueFactory(new PropertyValueFactory<>("status")); cStatus.setPrefWidth(70);
        TableColumn<OrderRow,String> cTime = new TableColumn<>("时间"); cTime.setCellValueFactory(new PropertyValueFactory<>("time")); cTime.setPrefWidth(140);
        TableColumn<OrderRow,String> cRemark = new TableColumn<>("备注"); cRemark.setCellValueFactory(new PropertyValueFactory<>("remark"));
        TableColumn<OrderRow,Void> cAct = new TableColumn<>("操作");
        cAct.setCellFactory(col -> new TableCell<>(){
            private final HBox box = new HBox(4);
            private final Button pay = new Button("支付");
            private final Button cancel = new Button("取消");
            private final Button refund = new Button("退款");
            private final Button detail = new Button("详情");
            { box.getChildren().addAll(detail,pay,cancel,refund); }
            @Override protected void updateItem(Void v, boolean empty){
                super.updateItem(v, empty);
                if (empty){ setGraphic(null); return; }
                OrderRow row = getTableView().getItems().get(getIndex());
                pay.setVisible(false); cancel.setVisible(false); refund.setVisible(false);
                pay.setOnAction(e -> payOrder(row.uuid));
                cancel.setOnAction(e -> cancelOrder(row.uuid));
                refund.setOnAction(e -> refundOrder(row.uuid));
                detail.setOnAction(e -> showOrderDetail(row));
                if ("待支付".equals(row.status)){
                    if (userTable || adminAll) { pay.setVisible(true); cancel.setVisible(true); }
                } else if ("已支付".equals(row.status)) {
                    if (adminAll) refund.setVisible(true);
                }
                setGraphic(box);
            }
        });
        tv.getColumns().addAll(cId,cAmt,cStatus,cTime,cRemark,cAct);
        return tv;
    }

    /* ==================== 数据类 ==================== */
    public static class ItemRow { public String uuid,itemName; public int priceFen; public String priceYuan; public int stock; public String category; public String description; public String barcode; public String pictureLink; public Integer salesVolume; public int getStock(){return stock;} public String getItemName(){return itemName;} public String getPriceYuan(){return priceYuan;} public String getCategory(){return category;} public String getUuid(){return uuid;} public Integer getSalesVolume(){return salesVolume;} }
    public static class CartRow { String uuid,itemName; int priceFen; int quantity; CartRow(String u,String n,int p,int q){uuid=u;itemName=n;priceFen=p;quantity=q;} public String getItemName(){return itemName;} public int getQuantity(){return quantity;} public String getSubtotalYuan(){return fenToYuan((long)priceFen*quantity);} public String getPriceYuan(){return fenToYuan(priceFen);} }
    public static class OrderRow { String uuid; String cardNumber; int totalFen; String totalYuan; String status; String time; String remark; List<OrderItemRow> items; public String getUuid(){return uuid;} public String getCardNumber(){return cardNumber;} public String getTotalYuan(){return totalYuan;} public String getStatus(){return status;} public String getTime(){return time;} public String getRemark(){return remark;} }
    public static class OrderItemRow { String itemUuid; int price; int amount; String itemName; }

    public static String fenToYuan(long fen){ return String.format(Locale.CHINA,"%.2f", fen/100.0); }

    /* ==================== 商品加载与搜索 ==================== */
    private void loadAllItems(){ new Thread(() -> fetchItems(null,null,false)).start(); }
    private void doSearch(){ String kw = searchField.getText().trim(); categoryOrKeywordSearch(); if(kw.isEmpty()) return; }
    private void categoryOrKeywordSearch(){ String kw = searchField.getText().trim(); String cat = categoryCombo.getSelectionModel().getSelectedItem(); if (cat!=null && cat.equals("全部")) cat=null; String finalCat = cat; if (kw.isEmpty() && cat==null){ loadAllItems(); return; } new Thread(() -> fetchItems(finalCat, kw, true)).start(); }

    private void fetchItems(String category, String keyword, boolean advanced){
        try {
            Map<String,Object> data = new HashMap<>();
            String type;
            if (category!=null && keyword!=null) { type="searchItemsByCategory"; data.put("category", category); data.put("keyword", keyword); }
            else if (category!=null && keyword==null){ type="getItemsByCategory"; data.put("category", category); }
            else if (category==null && keyword!=null){ type="searchItems"; data.put("keyword", keyword); }
            else { type="getAllItems"; }
            String resp = ClientNetworkHelper.send(new Request(type, data));
            JsonObject o = JsonParser.parseString(resp).getAsJsonObject();
            if (!o.has("code") || o.get("code").getAsInt()!=200) return;
            List<ItemRow> list = new ArrayList<>();
            if(o.get("data")!=null && o.get("data").isJsonArray()){
                JsonArray arr = o.getAsJsonArray("data");
                for (JsonElement el: arr){ if(!el.isJsonObject()) continue; JsonObject it = el.getAsJsonObject(); ItemRow r = parseItem(it); if(r!=null) list.add(r); }
            } else if (o.has("data") && o.get("data").isJsonObject()){
                ItemRow r = parseItem(o.getAsJsonObject("data")); if(r!=null) list.add(r);
            }
            Platform.runLater(() -> itemData.setAll(list));
        } catch (Exception ignore) {}
    }

    private ItemRow parseItem(JsonObject it){
        try {
            ItemRow r = new ItemRow();
            r.uuid = nullableString(it, "uuid");
            r.itemName = nullableString(it, "itemName");
            r.priceFen = it.has("price") && !it.get("price").isJsonNull()? it.get("price").getAsInt():0;
            r.priceYuan = fenToYuan(r.priceFen);
            r.stock = it.has("stock") && !it.get("stock").isJsonNull()? it.get("stock").getAsInt():0;
            r.category = nullableString(it, "category");
            r.description = nullableString(it, "description");
            r.barcode = nullableString(it, "barcode");
            r.pictureLink = nullableString(it, "pictureLink");
            r.salesVolume = it.has("salesVolume") && !it.get("salesVolume").isJsonNull()? it.get("salesVolume").getAsInt(): null;
            return r;
        } catch (Exception e){ return null; }
    }

    private String nullableString(JsonObject o, String k){ return o!=null && o.has(k)&&!o.get(k).isJsonNull()? o.get(k).getAsString():null; }

    private void getItemById(){ String id = idSearchField.getText().trim(); if(id.isEmpty()) return; new Thread(() -> {
        try { Map<String,Object> data = new HashMap<>(); data.put("itemId", id); String resp = ClientNetworkHelper.send(new Request("getItemById", data)); JsonObject o = JsonParser.parseString(resp).getAsJsonObject(); if(!o.has("code") || o.get("code").getAsInt()!=200) return; JsonObject it = o.getAsJsonObject("data"); ItemRow r = parseItem(it); if(r!=null){ Platform.runLater(()-> { itemData.setAll(Collections.singletonList(r)); }); } } catch (Exception ignored){}
    }).start(); }

    /* ==================== 购物车 ==================== */
    private void addSelectedToCart(int qty){ ItemRow sel = itemTable.getSelectionModel().getSelectedItem(); if (sel==null || qty<=0) return; for (CartRow c : cartData){ if (c.uuid.equals(sel.uuid)){ c.quantity+=qty; cartTable.refresh(); updateCartTotal(); return; } } cartData.add(new CartRow(sel.uuid, sel.itemName, sel.priceFen, qty)); updateCartTotal(); }
    private void removeSelected(){ CartRow sel = cartTable.getSelectionModel().getSelectedItem(); if (sel!=null){ cartData.remove(sel); updateCartTotal(); } }
    private void updateCartTotal(){ long sum = cartData.stream().mapToLong(c -> (long)c.priceFen*c.quantity).sum(); cartTotalLabel.setText("合计: "+fenToYuan(sum)+" 元"); }

    private void createOrder(){ if (cartData.isEmpty()) return; List<Map<String,Object>> items = new ArrayList<>(); for (CartRow c: cartData){ Map<String,Object> m=new HashMap<>(); m.put("itemId", c.uuid); m.put("amount", c.quantity); items.add(m);} Map<String,Object> payload=new HashMap<>(); payload.put("cardNumber", Integer.parseInt(cardNumber)); payload.put("remark", Optional.ofNullable(orderRemarkField.getText()).orElse("")); payload.put("items", items); new Thread(() -> {
        try { String resp = ClientNetworkHelper.send(new Request("createOrder", payload)); Platform.runLater(() -> { new Alert(Alert.AlertType.INFORMATION,"下单结果: "+resp).showAndWait(); cartData.clear(); updateCartTotal(); orderRemarkField.clear(); fetchMyOrders(); if(isAdmin()) fetchAllOrders(); }); } catch (Exception e){ /*忽略*/ }
    }).start(); }

    /* ==================== 订单相关 ==================== */
    private void fetchMyOrders(){ new Thread(() -> fetchOrders(false, null)).start(); }
    private void fetchAllOrders(){ new Thread(() -> fetchOrders(true, null)).start(); }
    private void adminGetUserOrders(String card){ if(card==null||card.isEmpty()) return; new Thread(() -> fetchOrders(false, card)).start(); }

    private void fetchOrders(boolean all, String specifiedUser){
        try {
            String type = all?"getAllOrders":"getUserOrders";
            Map<String,Object> data = new HashMap<>();
            if(!all){
                String c = specifiedUser!=null? specifiedUser : cardNumber;
                try { data.put("cardNumber", Integer.parseInt(c)); } catch (NumberFormatException ignored) { return; }
            }
            String resp = ClientNetworkHelper.send(new Request(type, data));
            JsonObject o = JsonParser.parseString(resp).getAsJsonObject(); if(!o.has("code") || o.get("code").getAsInt()!=200) return;
            JsonArray arr = o.getAsJsonArray("data"); if(arr==null) return; List<OrderRow> list = new ArrayList<>(); for(JsonElement el: arr){ if(!el.isJsonObject()) continue; OrderRow r = parseOrder(el.getAsJsonObject()); if(r!=null) list.add(r);} Platform.runLater(()-> { if(all) allOrderData.setAll(list); else { if(specifiedUser!=null) allOrderData.setAll(list); else myOrderData.setAll(list);} });
        } catch (Exception ignore) {}
    }

    private OrderRow parseOrder(JsonObject o){
        try {
            OrderRow r = new OrderRow();
            r.uuid = nullableString(o,"uuid");
            r.cardNumber = o.has("cardNumber")&&!o.get("cardNumber").isJsonNull()? String.valueOf(o.get("cardNumber").getAsInt()):"";
            r.totalFen = o.has("totalAmount")&&!o.get("totalAmount").isJsonNull()? o.get("totalAmount").getAsInt():0;
            r.totalYuan = fenToYuan(r.totalFen);
            r.status = nullableString(o,"status");
            r.remark = nullableString(o,"remark");
            r.time = nullableString(o,"time");
            r.items = new ArrayList<>();
            if(o.has("items") && o.get("items").isJsonArray()){
                for(JsonElement ie: o.getAsJsonArray("items")){
                    if(!ie.isJsonObject()) continue; JsonObject jo = ie.getAsJsonObject(); OrderItemRow ir = new OrderItemRow(); ir.itemUuid = nullableString(jo,"itemUuid"); ir.price = jo.has("itemPrice") && !jo.get("itemPrice").isJsonNull()? jo.get("itemPrice").getAsInt(): (jo.has("price")&&!jo.get("price").isJsonNull()? jo.get("price").getAsInt():0); ir.amount = jo.has("amount")&&!jo.get("amount").isJsonNull()? jo.get("amount").getAsInt():0; if(jo.has("item") && jo.get("item").isJsonObject()){ JsonObject nested = jo.getAsJsonObject("item"); ir.itemName = nullableString(nested, "itemName"); } r.items.add(ir);
                }
            }
            return r;
        } catch (Exception e){ return null; }
    }

    private void payOrder(String orderId){ if(orderId==null) return; Map<String,Object> data=new HashMap<>(); data.put("orderId", orderId); new Thread(() -> { try { String resp = ClientNetworkHelper.send(new Request("payOrder", data)); Platform.runLater(()-> { infoDialog("支付结果", resp); fetchMyOrders(); if(isAdmin()) fetchAllOrders(); }); } catch (Exception ignored){} }).start(); }
    private void cancelOrder(String orderId){ if(orderId==null) return; if(!confirm("确认取消该订单?")) return; Map<String,Object> data=new HashMap<>(); data.put("orderId", orderId); new Thread(() -> { try { String resp = ClientNetworkHelper.send(new Request("cancelOrder", data)); Platform.runLater(()-> { infoDialog("取消结果", resp); fetchMyOrders(); if(isAdmin()) fetchAllOrders(); }); } catch (Exception ignored){} }).start(); }
    private void refundOrder(String orderId){ if(orderId==null) return; TextInputDialog td = new TextInputDialog(); td.setHeaderText(null); td.setContentText("退款理由:"); Optional<String> reason = td.showAndWait(); if(reason.isEmpty()) return; Map<String,Object> data=new HashMap<>(); data.put("orderId", orderId); data.put("reason", reason.get()); new Thread(() -> { try { String resp = ClientNetworkHelper.send(new Request("refundOrder", data)); Platform.runLater(()-> { infoDialog("退款结果", resp); fetchAllOrders(); }); } catch (Exception ignored){} }).start(); }
    private void getOrderById(String orderId){ if(orderId==null||orderId.isEmpty()) return; Map<String,Object> data=new HashMap<>(); data.put("orderId", orderId); new Thread(() -> { try { String resp = ClientNetworkHelper.send(new Request("getOrder", data)); JsonObject o = JsonParser.parseString(resp).getAsJsonObject(); if(!o.has("code") || o.get("code").getAsInt()!=200){ Platform.runLater(() -> infoDialog("查询结果", resp)); return; } OrderRow r = parseOrder(o.getAsJsonObject("data")); if(r!=null) Platform.runLater(()-> { allOrderData.setAll(Collections.singletonList(r)); }); } catch (Exception ignored){} }).start(); }

    private void showOrderDetail(OrderRow row){ if(row==null) return; StringBuilder sb = new StringBuilder(); sb.append("订单ID: ").append(row.uuid).append('\n'); sb.append("状态: ").append(row.status).append('\n'); sb.append("金额: ").append(row.totalYuan).append(" 元\n"); sb.append("时间: ").append(row.time).append('\n'); sb.append("备注: ").append(Optional.ofNullable(row.remark).orElse("")); sb.append("\n商品列表:\n"); if(row.items!=null){ for(OrderItemRow it: row.items){ sb.append(" - ").append(Optional.ofNullable(it.itemName).orElse(it.itemUuid)).append(" x").append(it.amount).append(" 单价:").append(fenToYuan(it.price)).append("元").append('\n'); } } infoDialog("订单详情", sb.toString()); }

    /* ==================== 管理员商品操作 ==================== */
    private void adminAddItem(){ try { Integer priceFen = parsePriceFen(formPrice.getText()); Integer stock = parseIntSafe(formStock.getText(),0); String uuid = formUuid.getText().trim(); if(uuid.isEmpty()) uuid = java.util.UUID.randomUUID().toString(); Map<String,Object> item = new LinkedHashMap<>(); item.put("uuid", uuid); item.put("itemName", formName.getText().trim()); item.put("price", priceFen); item.put("pictureLink", formPicture.getText().trim()); item.put("stock", stock); item.put("description", formDescription.getText().trim()); item.put("category", formCategory.getText().trim()); item.put("barcode", formBarcode.getText().trim()); Integer sales = parseIntNullable(formSalesVolume.getText()); if(sales!=null) item.put("salesVolume", sales); Map<String,Object> data = new HashMap<>(); data.put("item", item); sendAsync("addItem", data, s -> { infoDialog("添加结果", s); loadAllItems(); }); } catch (Exception e){ infoDialog("错误", "数据格式错误: "+e.getMessage()); } }
    private void adminUpdateItem(){ if(formUuid.getText().trim().isEmpty()){ infoDialog("提示","请填写UUID"); return;} try { Integer priceFen = parsePriceFen(formPrice.getText()); Integer stock = parseIntSafe(formStock.getText(),0); Map<String,Object> item = new LinkedHashMap<>(); item.put("uuid", formUuid.getText().trim()); item.put("itemName", formName.getText().trim()); item.put("price", priceFen); item.put("pictureLink", formPicture.getText().trim()); item.put("stock", stock); item.put("description", formDescription.getText().trim()); item.put("category", formCategory.getText().trim()); item.put("barcode", formBarcode.getText().trim()); Integer sales = parseIntNullable(formSalesVolume.getText()); if(sales!=null) item.put("salesVolume", sales); Map<String,Object> data = new HashMap<>(); data.put("item", item); sendAsync("updateItem", data, s -> { infoDialog("更新结果", s); loadAllItems(); }); } catch (Exception e){ infoDialog("错误", "数据格式错误: "+e.getMessage()); } }
    private void adminDeleteItem(){ String uuid = formUuid.getText().trim(); if(uuid.isEmpty()){ infoDialog("提示","请填写UUID"); return;} if(!confirm("确认删除?")) return; Map<String,Object> data=new HashMap<>(); data.put("itemId", uuid); sendAsync("deleteItem", data, s -> { infoDialog("删除结果", s); loadAllItems(); }); }
    private void fillFormFromSelection(){ ItemRow sel = itemTable.getSelectionModel().getSelectedItem(); if(sel==null){ infoDialog("提示","先在商品页选择一行"); return;} formUuid.setText(sel.uuid); formName.setText(sel.itemName); formPrice.setText(sel.priceYuan); formCategory.setText(sel.category); formStock.setText(String.valueOf(sel.stock)); formBarcode.setText(Optional.ofNullable(sel.barcode).orElse("")); formPicture.setText(Optional.ofNullable(sel.pictureLink).orElse("")); formDescription.setText(Optional.ofNullable(sel.description).orElse("")); formSalesVolume.setText(sel.salesVolume==null?"":String.valueOf(sel.salesVolume)); }
    private void clearForm(){ Arrays.asList(formUuid,formName,formPrice,formCategory,formStock,formBarcode,formPicture,formDescription,formSalesVolume).forEach(tf->tf.setText("")); }

    private Integer parsePriceFen(String yuanStr){ if(yuanStr==null||yuanStr.trim().isEmpty()) return 0; double d = Double.parseDouble(yuanStr.trim()); return (int)Math.round(d*100); }
    private Integer parseIntSafe(String v,int def){ try { return Integer.parseInt(v.trim()); } catch (Exception e){ return def; } }
    private Integer parseIntNullable(String v){ if(v==null||v.trim().isEmpty()) return null; try { return Integer.parseInt(v.trim()); } catch (Exception e){ return null; } }

    /* ==================== 通用请求 / 类别 ==================== */
    private void fetchCategories(){ new Thread(() -> { try { String resp = ClientNetworkHelper.send(new Request("getAllCategories", new HashMap<>())); JsonObject o = JsonParser.parseString(resp).getAsJsonObject(); if(!o.has("code") || o.get("code").getAsInt()!=200) return; List<String> list = new ArrayList<>(); JsonArray arr = o.getAsJsonArray("data"); for(JsonElement e: arr) list.add(e.getAsString()); Platform.runLater(() -> { String sel = categoryCombo.getSelectionModel().getSelectedItem(); categoryCombo.getItems().setAll("全部"); categoryCombo.getItems().addAll(list); if(sel!=null && categoryCombo.getItems().contains(sel)) categoryCombo.getSelectionModel().select(sel); else categoryCombo.getSelectionModel().selectFirst(); }); } catch (Exception ignore){} }).start(); }

    private void sendAsync(String type, Map<String,Object> data, Consumer<String> after){ new Thread(() -> { try { String resp = ClientNetworkHelper.send(new Request(type, data)); Platform.runLater(() -> after.accept(resp)); } catch (Exception ignored){} }).start(); }

    private void infoDialog(String title,String msg){ Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK); a.setHeaderText(title); a.showAndWait(); }
    private boolean confirm(String msg){ Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.OK, ButtonType.CANCEL); a.setHeaderText(null); Optional<ButtonType> r = a.showAndWait(); return r.isPresent() && r.get()==ButtonType.OK; }
}
