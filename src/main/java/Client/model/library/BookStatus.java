package Client.model.library;


public enum BookStatus {
    INLIBRARY("在馆"),
    LEND("借出"),
    LOST("丢失"),
    REPAIR("修复");

    private final String description;

    BookStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
