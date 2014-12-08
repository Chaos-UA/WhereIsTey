package WhereIsTey.chat_watcher;

import WhereIsTey.MyUtil;
import WhereIsTey.Streamer;
import WhereIsTey.User;
import org.glassfish.tyrus.client.ClientManager;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.websocket.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

@ClientEndpoint
public class ChatWatcher {
    private Session session = null;
    private final Logger logger;
    private final Streamer streamer;
    private User userToWatch;
    private final OnUserMessage onUserMessage;

    public ChatWatcher(Streamer streamer, User userToWatch, Logger logger, OnUserMessage onUserMessage) {
        this.userToWatch = userToWatch;
        this.streamer = streamer;
        this.logger = logger;
        this.onUserMessage = onUserMessage;
    }

    public void openConnection() throws URISyntaxException, DeploymentException {
        logger.info(String.format("[%s] Opening connection to %s", streamer, MyUtil.getGoodGameServerWsUrl()));
        this.disconnect();
        session = ClientManager.createClient().connectToServer(this, new URI(MyUtil.getGoodGameServerWsUrl()));
    }

    public void reconnect() throws URISyntaxException, DeploymentException {
        openConnection();
    }

    public void disconnect() {
        if (session != null) {
            try {
                session.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                logger.info(e.toString());
            }
            finally {
                session = null;
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.disconnect();
    }

    public Streamer getStreamer() {
        return streamer;
    }

    public User getUserToWatch() {
        return userToWatch;
    }

    public void setUserToWatch(User userToWatch) {
        this.userToWatch = userToWatch;
    }

    public boolean isConnected() {
        Session session = this.session;
        return session != null && session.isOpen();
    }

    //


    @OnOpen
    public void onOpen(Session session) {
        logger.info(String.format("[%s] Connection has been opened", this.streamer.getChannelId()));
        //{"type":"join","data":{"channel_id":2059,"hidden":true}}
        JsonObject jsonJoin = Json.createObjectBuilder()
                .add("type", "join")
                .add("data", Json.createObjectBuilder()
                    .add("channel_id", this.streamer.getChannelId())
                    .add("hidden", true)
                ).build();
        try {
            logger.info(String.format("[%s] Sending message: %s", this.streamer.getName(), jsonJoin));
            session.getBasicRemote().sendText(jsonJoin.toString());
        } catch (IOException e) {
            e.printStackTrace();
            logger.info(e.toString());
            this.disconnect();
        }
    }

    private boolean isToUserMessage(String message) {
        String user = this.userToWatch.getName().toLowerCase();
        message = message.toLowerCase();
        String[] words = message.split("[\\s\\[\\]\\\\()!?,.^/\\+\\-]+");
        for (String word : words) {
            if (user.equals(word)) {
                return true;
            }
        }
        return false;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
      //  logger.info(String.format("[%s] Message has been received: %s", this.streamer.getName(), message));
        JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
        JsonString type = jsonObject.getJsonString("type");
        if (type.getString().equals("message")) {
            User user = new User(jsonObject.getJsonObject("data").getString("user_name"));
            String textMessage = jsonObject.getJsonObject("data").getString("text");
            if (this.userToWatch.equals(user)) {
                this.onUserMessage.onUserMessage(this.streamer, this.userToWatch, textMessage);
            }
            else if (this.isToUserMessage(textMessage)) {
                this.onUserMessage.onToUserMessage(this.streamer, user, this.userToWatch, textMessage);
            }
        }
        else if (type.getString().equals("error")) {
            logger.info(String.format("[%s] error has been received. Disconnecting", this.streamer.getName()));
            this.disconnect();
        }
        else if (!type.getString().equals("welcome")) {
            logger.info(String.format("[%s] The received message ignored: %s", this.streamer.getName(), message));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("[%s] Connection closed %s", streamer.getName(), closeReason));
        this.session = null;
    }


}
