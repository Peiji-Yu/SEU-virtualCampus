package Server.model.shop;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 一卡通交易记录实体类
 */
public class CardTransaction {
    private UUID uuid;           // 交易记录ID
    private Integer cardNumber;  // 一卡通号
    private Integer amount;      // 交易金额（以分为单位）
    private LocalDateTime time;           // 交易时间
    private String type;         // 交易类型：充值、消费、退款
    private String description;  // 交易描述
    private String referenceId;  // 关联的业务ID

    // 构造方法、Getter和Setter
    public CardTransaction() {}

    public CardTransaction(Integer cardNumber, Integer amount, String type, String description, String referenceId) {
        this.uuid = UUID.randomUUID();
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.time = LocalDateTime.now();
        this.type = type;
        this.description = description;
        this.referenceId = referenceId;
    }

    // Getter和Setter方法
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Integer getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Integer cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public String toString() {
        return "CardTransaction{" +
                "uuid=" + uuid +
                ", cardNumber=" + cardNumber +
                ", amount=" + amount +
                ", time=" + time +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", referenceId='" + referenceId + '\'' +
                '}';
    }
}
