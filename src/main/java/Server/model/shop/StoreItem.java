package Server.model.shop;

import java.util.UUID;

/**
 * 商品实体类
 * 对应数据库中的store_item表
 */
public class StoreItem {
    private UUID uuid;           // 商品编号
    private String itemName;     // 商品名称
    private Integer price;       // 商品价格（以分为单位）
    private String pictureLink;  // 商品图片链接
    private Integer stock;       // 商品库存数量
    private Integer salesVolume; // 商品销量
    private String description;  // 商品信息描述
    private String category;     // 商品类别
    private String barcode;      // 商品条形码

    // 构造方法
    public StoreItem() {}

    public StoreItem(String itemName, Integer price, Integer stock, String description, String category) {
        this.uuid = UUID.randomUUID();
        this.itemName = itemName;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.category = category;
        this.salesVolume = 0;
    }

    // Getter和Setter方法
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getPictureLink() {
        return pictureLink;
    }

    public void setPictureLink(String pictureLink) {
        this.pictureLink = pictureLink;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getSalesVolume() {
        return salesVolume;
    }

    public void setSalesVolume(Integer salesVolume) {
        this.salesVolume = salesVolume;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    @Override
    public String toString() {
        return "StoreItem{" +
                "uuid=" + uuid +
                ", itemName='" + itemName + '\'' +
                ", price=" + price +
                ", pictureLink='" + pictureLink + '\'' +
                ", stock=" + stock +
                ", salesVolume=" + salesVolume +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", barcode='" + barcode + '\'' +
                '}';
    }
}
