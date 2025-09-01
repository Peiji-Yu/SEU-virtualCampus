package Server.model;

public enum PoliticalStatus {
    PARTY_MEMBER("党员"),
    LEAGUE_MEMBER("团员"),
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
