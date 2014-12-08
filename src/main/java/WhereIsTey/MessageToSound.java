package WhereIsTey;


public class MessageToSound {
    private final String message;
    private final String language;

    public MessageToSound(String message, String language) {
        this.message = message;
        this.language = language;
    }

    public String getMessage() {
        return message;
    }

    public String getLanguage() {
        return language;
    }
}
