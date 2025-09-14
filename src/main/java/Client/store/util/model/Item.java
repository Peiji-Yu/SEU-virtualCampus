package Client.store.util.model;

import Client.store.util.StoreUtils;

// 商品项类
public class Item {
    private String uuid;
    private String itemName;
    private String category;
    private int price;
    private String pictureLink;
    private int stock;
    private int salesVolume;
    private String description;
    private String barcode;

    // 构造函数、getter和setter
    public Item() {}

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getPictureLink() { return pictureLink; }
    public void setPictureLink(String pictureLink) { this.pictureLink = pictureLink; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSalesVolume() { return salesVolume; }
    public void setSalesVolume(int salesVolume) { this.salesVolume = salesVolume; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getPriceYuan() {
        return StoreUtils.fenToYuan(price);
    }
}
