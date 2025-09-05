package Server.model.book;


public enum UserStatus {
    BORROWING("有借阅"),
    FREE("空闲"),
    OVERDUE("逾期"),
    TRUSTBREAK("失信");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}