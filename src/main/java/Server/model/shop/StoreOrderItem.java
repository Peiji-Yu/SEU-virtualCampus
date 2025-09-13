package Server.model.shop;

import java.util.UUID;

/**
 * 订单商品项实体类
 */
public class StoreOrderItem {
    private UUID uuid;           // 订单项ID
    private UUID orderUuid;      // 订单ID
    private UUID itemUuid;       // 商品ID
    private Integer itemPrice;   // 商品单价（以分为单位）
    private Integer amount;      // 商品数量
    private StoreItem item;  // 商品

    // 构造方法
    public StoreOrderItem() {}

    public StoreOrderItem(UUID orderUuid, UUID itemUuid, Integer itemPrice, Integer amount) {
        this.uuid = UUID.randomUUID();
        this.orderUuid = orderUuid;
        this.itemUuid = itemUuid;
        this.itemPrice = itemPrice;
        this.amount = amount;
    }

    // Getter和Setter方法
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getOrderUuid() {
        return orderUuid;
    }

    public void setOrderUuid(UUID orderUuid) {
        this.orderUuid = orderUuid;
    }

    public UUID getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(UUID itemUuid) {
        this.itemUuid = itemUuid;
    }

    public Integer getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(Integer itemPrice) {
        this.itemPrice = itemPrice;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public StoreItem getItem() { return item; }

    public void setItem(StoreItem item) { this.item = item; }

    @Override
    public String toString() {
        return "StoreOrderItem{" +
                "uuid=" + uuid +
                ", orderUuid=" + orderUuid +
                ", itemUuid=" + itemUuid +
                ", itemPrice=" + itemPrice +
                ", amount=" + amount +
                '}';
    }
}
