package Server.model.book;

/**
 * 图书馆用户类
 * 仅记录用户的基本信息和状态
 * 借阅记录通过 BookRecord 的 userId 关联查询
 */
public class LibUser {
    private int userId;              // 用户一卡通号
    private int borrowed;            // 当前已借书数量
    private int maxBorrowed;         // 最大可借书数量
    private UserStatus userStatus;   // 用户状态（正常/挂失/冻结）

    // 无参构造
    public LibUser() {}

    // 全参构造
    public LibUser(int userId, int borrowed, int maxBorrowed, UserStatus userStatus) {
        this.userId = userId;
        this.borrowed = borrowed;
        this.maxBorrowed = maxBorrowed;
        this.userStatus = userStatus;
    }

    // Getter 和 Setter
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBorrowed() {
        return borrowed;
    }

    public void setBorrowed(int borrowed) {
        this.borrowed = borrowed;
    }

    public int getMaxBorrowed() {
        return maxBorrowed;
    }

    public void setMaxBorrowed(int maxBorrowed) {
        this.maxBorrowed = maxBorrowed;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    // toString 方法
    @Override
    public String toString() {
        return "LibUser{" +
                "userId=" + userId +
                ", borrowed=" + borrowed +
                ", maxBorrowed=" + maxBorrowed +
                ", userStatus=" + (userStatus != null ? userStatus.toString() : "未知状态") +
                '}';
    }
}
