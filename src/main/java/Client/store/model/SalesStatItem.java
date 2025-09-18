package Client.store.model;

import Client.store.util.StoreUtils;

// 销售统计项数据模型
public class SalesStatItem {
    private String itemUuid;
    private String itemName;
    private int totalAmount;
    private int totalRevenue; // 以分为单位

    public String getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(String itemUuid) {
        this.itemUuid = itemUuid;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(int totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public String getTotalRevenueYuan() {
        return "¥" + StoreUtils.fenToYuan(totalRevenue);
    }
}
