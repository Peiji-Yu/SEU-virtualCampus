package Server.model.student;

public enum PoliticalStatus {
    PARTY_MEMBER("中共党员"),
    LEAGUE_MEMBER("共青团员"),
    MASSES("群众"),
    OTHER("其他");

    private final String description;

    PoliticalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
