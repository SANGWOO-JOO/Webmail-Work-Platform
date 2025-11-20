package dsn.webmail.entity;

public enum ReplyTone {
    FORMAL("정중한"),
    NEUTRAL("중립적"),
    FRIENDLY("친근한"),
    CONCISE("간결한");

    private final String displayName;

    ReplyTone(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
