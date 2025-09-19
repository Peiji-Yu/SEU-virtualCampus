package Client.model.login;

/**
 * 用户实体类
 * 对应数据库中的user表
 */
public class User {
    private String identity;
    private Integer cardNumber;
    private String password; // 加密后的密码
    private String name;

    // 构造方法
    public User() {}

    public User(String identity, Integer cardNumber, String password, String name) {
        this.identity = identity;
        this.cardNumber = cardNumber;
        this.password = password;
        this.name = name;
    }

    // Getter和Setter方法
    public String getIdentity() { return identity; }

    public void setIdentity(String identity) { this.identity = identity; }

    public Integer getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Integer cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getPassword() { return password; }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "User{" +
                "cardNumber=" + cardNumber + '\'' +
                ", identity='" + identity +
                ", password='" + password + '\'' +
                '}';
    }
}