package WhereIsTey.chat_watcher;


import WhereIsTey.Streamer;
import WhereIsTey.User;

public interface OnUserMessage {
    void onUserMessage(Streamer streamer, User user, String message);
    void onToUserMessage(Streamer streamer, User fromUser, User toUser, String message);
}
