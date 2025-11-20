package dsn.webmail.entity;

public enum ReplyType {
    ACKNOWLEDGE("확인"),
    APPROVE("승인"),
    DECLINE("거절"),
    QUESTION("질문"),
    INFORM("정보제공"),
    REQUEST("요청");

    private final String displayName;

    ReplyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
