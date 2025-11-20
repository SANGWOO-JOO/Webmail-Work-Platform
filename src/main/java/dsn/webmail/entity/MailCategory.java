package dsn.webmail.entity;

public enum MailCategory {
    ANNOUNCEMENT("공지"),
    NOTION_ALERT("노션 알림"),
    WORK_REQUEST("업무 요청"),
    INFORMATION("내용 전달"),
    INQUIRY("질의 사항"),
    UNKNOWN("미분류");

    private final String displayName;

    MailCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
