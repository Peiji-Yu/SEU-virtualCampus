package Server.model.shop;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 订单实体类（支持多种商品）
 */
public class StoreOrder {
    public static final String STATUS_PENDING = "待支付";
    public static final String STATUS_PAID = "已支付";
    public static final String STATUS_CANCELLED = "已取消";
    public static final String STATUS_REFUNDED = "已退款";

    private String uuid;                   // 订单编号
    private Integer cardNumber;          // 用户一卡通号
    private Integer totalAmount;         // 订单总金额（以分为单位）
    private LocalDateTime time;          // 订单时间
    private String status;               // 订单状态：待支付、已支付、已取消、已退款
    private String remark;               // 订单备注
    private List<StoreOrderItem> items;  // 订单商品列表

    // 构造方法
    public StoreOrder() {}

    public StoreOrder(Integer cardNumber, Integer totalAmount, String remark, List<StoreOrderItem> items) {
        String timePart = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                .format(java.time.LocalDateTime.now());
        String randomPart = String.format("%03d", (int)(Math.random() * 1000));
        this.uuid = timePart + randomPart;

        this.cardNumber = cardNumber;
        this.totalAmount = totalAmount;
        this.time = LocalDateTime.now();
        this.status = STATUS_PENDING;
        this.remark = remark;
        this.items = items;
    }

    // Getter和Setter方法
    public String getId() {
        return uuid;
    }

    public void setId(String id) {
        this.uuid = id;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<StoreOrderItem> getItems() { return items; }

    public void setItems(List<StoreOrderItem> items) {
        this.items = items;
    }

    public Integer getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Integer cardNumber) {
        this.cardNumber = cardNumber;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StoreOrder{" +
                "id=" + uuid +
                ", cardNumber=" + cardNumber +
                ", totalAmount=" + totalAmount +
                ", time=" + time +
                ", status='" + status + '\'' +
                ", remark='" + remark + '\'' +
                ", items=" + items +
                '}';
    }
}
