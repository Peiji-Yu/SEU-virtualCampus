package Server.model;

/**
 * 用户实体类
 * 对应数据库中的user表
 */
public class User {
    private Long id;
    private String password; // 加密后的密码

    // 构造方法
    public User() {}

    public User(Long id, String password) {
        this.id = id;
        this.password = password;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
                "id=" + id +
                ", password='" + password + '\'' +
                '}';
    }
}