package Client.model.library;

import java.time.LocalDate;

/**
 * 借阅记录类
 * 记录某本书的借阅情况
 */
public class BookRecord {
    private String uuid;           // 书籍唯一副本的UUID
    private int userId;            // 借书人一卡通号
    private LocalDate borrowTime;  // 借书时间
    private LocalDate dueTime;     // 应还时间
    private String name;             // 对应书籍标题

    // 无参构造
    public BookRecord() {}

    // 全参构造
    public BookRecord(String uuid, int userId, LocalDate borrowTime, LocalDate dueTime) {
        this.uuid = uuid;
        this.userId = userId;
        this.borrowTime = borrowTime;
        this.dueTime = dueTime;
    }

    // Getter 和 Setter
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDate getBorrowTime() {
        return borrowTime;
    }

    public void setBorrowTime(LocalDate borrowTime) {
        this.borrowTime = borrowTime;
    }

    public LocalDate getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalDate dueTime) {
        this.dueTime = dueTime;
    }

    public String getName() {return name;}

    public void setName(String name) {this.name = name;}


    // toString 方法
    @Override
    public String toString() {
        return "BookRecord{" +
                "uuid='" + uuid + '\'' +
                ", userId=" + userId +
                ", borrowTime=" + (borrowTime != null ? borrowTime.toString() : "未记录") +
                ", dueTime=" + (dueTime != null ? dueTime.toString() : "未设定") +
                '}';
    }
}
