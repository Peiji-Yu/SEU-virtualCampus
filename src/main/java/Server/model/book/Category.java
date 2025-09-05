package Server.model.book;

public enum Category {
    SCIENCE("科学"),
    LITERATURE("文学"),
    HISTORY("历史"),
    TECHNOLOGY("技术"),
    ART("艺术");

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}