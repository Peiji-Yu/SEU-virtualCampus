package Client.model.student;

public enum StudentStatus {
    ENROLLED("在籍"),
    GRADUATED("毕业"),
    DROPPED("退学"),
    SUSPENDED("休学");

    private final String description;

    StudentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
