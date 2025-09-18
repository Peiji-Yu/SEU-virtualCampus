package Client.store.model;

import Client.store.util.StoreUtils;
import java.util.List;

// 订单类
public class Order {
    private String uuid;
    private int totalAmount; // 总金额(分)
    private String status;
    private String time;
    private String remark;
    private List<OrderItem> items;
    private int cardNumber;

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getTotalAmount() {
        return totalAmount;
    }
    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }
    public String getTotalYuan() {
        return StoreUtils.fenToYuan(totalAmount);
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }

    public String getRemark() {
        return remark;
    }
    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<OrderItem> getItems() {
        return items;
    }
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public int getCardNumber() {
        return cardNumber;
    }
    public void setCardNumber(int cardNumber) {
        this.cardNumber = cardNumber;
    }
}