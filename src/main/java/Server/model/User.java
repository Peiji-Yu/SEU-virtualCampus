package Server.model;

/**
 * 用户实体类
 * 对应数据库中的user表
 */
public class User {
    private Integer cardNumber;
    private String password; // 加密后的密码

    // 构造方法
    public User() {}

    public User(Integer cardNumber, String password) {
        this.cardNumber = cardNumber;
        this.password = password;
    }

    // Getter和Setter方法
    public Integer getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Integer id) {
        this.cardNumber = cardNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "cardNumber=" + cardNumber +
                ", password='" + password + '\'' +
                '}';
    }
}