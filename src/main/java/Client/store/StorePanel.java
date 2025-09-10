package Client.store;

import Client.ClientNetworkHelper;
import Server.model.Request;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.*;

/**
 * 简化占位版 StorePanel（补回缺失文件以恢复编译）
 * 仅保留核心：列出商品 + 加入购物车 + 创建订单（接口参数与后端保持一致）。
 * 后续可再替换为完整版美化实现。
 */
public class StorePanel extends BorderPane {
    private final String cardNumber;
    private final String userType;

    private final ObservableList<ItemRow> itemData = FXCollections.observableArrayList();
    private final ObservableList<CartRow> cartData = FXCollections.observableArrayList();

    private TableView<ItemRow> itemTable;
    private TableView<CartRow> cartTable;
    private Label cartTotalLabel;
    private TextField searchField;

    public StorePanel(String cardNumber, String userType) {
        this.cardNumber = cardNumber;
        this.userType = userType;
        setPadding(new Insets(10));
        buildUI();
        loadAllItems();
    }

    private void buildUI() {
        VBox root = new VBox(10);

        // 搜索行
        HBox top = new HBox(8);
        searchField = new TextField();
        searchField.setPromptText("关键字");
        Button searchBtn = new Button("搜索");
        searchBtn.setOnAction(e -> doSearch());
        Button allBtn = new Button("全部");
        allBtn.setOnAction(e -> loadAllItems());
        top.getChildren().addAll(new Label("搜索:"), searchField, searchBtn, allBtn);

        // 商品表
        itemTable = new TableView<>(itemData);
        TableColumn<ItemRow,String> cName = new TableColumn<>("名称"); cName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        TableColumn<ItemRow,String> cPrice = new TableColumn<>("单价(元)"); cPrice.setCellValueFactory(new PropertyValueFactory<>("priceYuan"));
        itemTable.getColumns().addAll(cName,cPrice);
        itemTable.setPrefHeight(250);

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
        TableColumn<CartRow,Integer> ccQty = new TableColumn<>("数量"); ccQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        TableColumn<CartRow,String> ccSub = new TableColumn<>("小计(元)"); ccSub.setCellValueFactory(new PropertyValueFactory<>("subtotalYuan"));
        cartTable.getColumns().addAll(ccName,ccQty,ccSub);
        cartTable.setPrefHeight(160);

        HBox cartOps = new HBox(8);
        Button removeBtn = new Button("移除选中");
        removeBtn.setOnAction(e -> removeSelected());
        Button clearBtn = new Button("清空");
        clearBtn.setOnAction(e -> { cartData.clear(); updateCartTotal(); });
        Button createOrderBtn = new Button("创建订单");
        createOrderBtn.setOnAction(e -> createOrder());
        cartTotalLabel = new Label("合计: 0.00 元");
        HBox spacer = new HBox(); HBox.setHgrow(spacer, Priority.ALWAYS);
        cartOps.getChildren().addAll(removeBtn, clearBtn, spacer, cartTotalLabel, createOrderBtn);

        root.getChildren().addAll(top, itemTable, addBox, new Label("购物车"), cartTable, cartOps);
        setCenter(root);
    }

    /* 数据类 */
    public static class ItemRow { String uuid,itemName; int priceFen; String priceYuan; }
    public static class CartRow { String uuid,itemName; int priceFen; int quantity; CartRow(String u,String n,int p,int q){uuid=u;itemName=n;priceFen=p;quantity=q;} String getItemName(){return itemName;} int getQuantity(){return quantity;} String getSubtotalYuan(){return fenToYuan((long)priceFen*quantity);} String getPriceYuan(){return fenToYuan(priceFen);} }

    /* 行访问器(反射列需要 getter) */
    public static String fenToYuan(long fen){ return String.format(Locale.CHINA,"%.2f", fen/100.0); }

    /* 加载商品 */
    private void loadAllItems(){ new Thread(() -> fetchItems(null)).start(); }
    private void doSearch(){ String kw = searchField.getText().trim(); new Thread(() -> fetchItems(kw.isEmpty()?null:kw)).start(); }

    private void fetchItems(String kw){
        try {
            Map<String,Object> data = new HashMap<>();
            String type = kw==null?"getAllItems":"searchItems";
            if (kw!=null) data.put("keyword", kw);
            String resp = ClientNetworkHelper.send(new Request(type, data));
            JsonObject o = JsonParser.parseString(resp).getAsJsonObject();
            if (o.get("code").getAsInt()!=200) return;
            List<ItemRow> list = new ArrayList<>();
            o.getAsJsonArray("data").forEach(el -> {
                try {
                    JsonObject it = el.getAsJsonObject();
                    ItemRow r = new ItemRow();
                    r.uuid = it.get("uuid").getAsString();
                    r.itemName = it.get("itemName").getAsString();
                    r.priceFen = it.get("price").getAsInt();
                    r.priceYuan = fenToYuan(r.priceFen);
                    list.add(r);
                } catch (Exception ignore) {}
            });
            Platform.runLater(() -> { itemData.setAll(list); });
        } catch (Exception ignore) {}
    }

    private void addSelectedToCart(int qty){
        ItemRow sel = itemTable.getSelectionModel().getSelectedItem();
        if (sel==null || qty<=0) return;
        for (CartRow c : cartData){ if (c.uuid.equals(sel.uuid)){ c.quantity+=qty; cartTable.refresh(); updateCartTotal(); return; } }
        cartData.add(new CartRow(sel.uuid, sel.itemName, sel.priceFen, qty));
        updateCartTotal();
    }

    private void removeSelected(){ CartRow sel = cartTable.getSelectionModel().getSelectedItem(); if (sel!=null){ cartData.remove(sel); updateCartTotal(); } }

    private void updateCartTotal(){ long sum = cartData.stream().mapToLong(c -> (long)c.priceFen*c.quantity).sum(); cartTotalLabel.setText("合计: "+fenToYuan(sum)+" 元"); }

    private void createOrder(){ if (cartData.isEmpty()) return; List<Map<String,Object>> items = new ArrayList<>(); for (CartRow c: cartData){ Map<String,Object> m=new HashMap<>(); m.put("itemId", c.uuid); m.put("amount", c.quantity); items.add(m);} Map<String,Object> payload=new HashMap<>(); payload.put("cardNumber", Integer.parseInt(cardNumber)); payload.put("remark", ""); payload.put("items", items); new Thread(() -> {
        try { String resp = ClientNetworkHelper.send(new Request("createOrder", payload)); Platform.runLater(() -> { cartData.clear(); updateCartTotal(); new Alert(Alert.AlertType.INFORMATION,"下单结果: "+resp).showAndWait(); }); } catch (Exception e){ /*忽略*/ }
    }).start(); }
}
