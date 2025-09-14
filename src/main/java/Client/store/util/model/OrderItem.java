package Client.store.util.model;

// 订单项类
public class OrderItem {
    private String uuid;
    private String orderUuid;
    private String itemUuid;
    private int itemPrice; // 总价(分)
    private int amount;
    private Item item;

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getOrderUuid() {
        return orderUuid;
    }
    public void setOrderUuid(String orderUuid) {
        this.orderUuid = orderUuid;
    }

    public String getItemUuid() {
        return itemUuid;
    }
    public void setItemUuid(String itemUuid) {
        this.itemUuid = itemUuid;
    }

    public int getItemPrice() {
        return itemPrice;
    }
    public void setItemPrice(int price) {
        this.itemPrice = price;
    }

    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
    }
}