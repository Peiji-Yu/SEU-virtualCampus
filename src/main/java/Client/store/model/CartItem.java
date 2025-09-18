package Client.store.model;

import Client.store.util.StoreUtils;

// 购物车项类
public class CartItem {
    private String uuid;
    private String itemName;
    private int priceFen;
    private String pictureLink;
    private int quantity;

    public CartItem() {}

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public int getPriceFen() { return priceFen; }
    public void setPriceFen(int priceFen) { this.priceFen = priceFen; }

    public String getPictureLink() { return pictureLink; }
    public void setPictureLink(String pictureLink) { this.pictureLink = pictureLink; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getSubtotalYuan() {
        return StoreUtils.fenToYuan((long) priceFen * quantity);
    }

    public String getPriceYuan() {
        return StoreUtils.fenToYuan(priceFen);
    }
}
