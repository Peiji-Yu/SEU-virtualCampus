package Server.model.shop;

/**
 * 一卡通实体类
 */
public class FinanceCard {
    private Integer cardNumber;  // 一卡通号
    private Integer balance;     // 余额（以分为单位）
    private String status;       // 状态：正常、冻结、挂失

    // 构造方法、Getter和Setter
    public FinanceCard() {}

    public FinanceCard(Integer cardNumber, Integer balance, String status) {
        this.cardNumber = cardNumber;
        this.balance = balance;
        this.status = status;
    }

    // Getter和Setter方法
    public Integer getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Integer cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Integer getBalance() {
        return balance;
    }

    public void setBalance(Integer balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "FinanceCard{" +
                "cardNumber=" + cardNumber +
                ", balance=" + balance +
                ", status='" + status + '\'' +
                '}';
    }
}
