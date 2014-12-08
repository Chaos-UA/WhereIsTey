package WhereIsTey;

import org.glassfish.tyrus.client.ClientManager;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.websocket.*;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ClientEndpoint
public class GoodGameWebSocketClient {


  //  private static final int RECONNECT_INTERVAL_MS = 5000;
    private static final int WAIT_TIME_OUT_MS = 10000;

    private Session session = null;

    // private final Logger logger = Logger.getLogger(this.getClass().getName());
    private Integer chatChannelId = null;
    private CountDownLatch countDownLatch = new CountDownLatch(0);
    private final Set<User> usersInChat = new HashSet<>();
    private final Logger logger;

    public GoodGameWebSocketClient(Logger logger) {
        this.logger = logger;
    }

    public Integer getChatChannelId() {
        if (chatChannelId == null) {

        }
        return chatChannelId;
    }

    public void openConnection() throws URISyntaxException, DeploymentException {
        logger.info(String.format("Opening connection to %s", MyUtil.getGoodGameServerWsUrl()));
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

    public Session getSession() {
        return session;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info(String.format("Connection has been opened [%s]", MyUtil.getGoodGameServerWsUrl()));
        countDownLatch.countDown();
        //   logger.info(String.format("Getting %s`s users in chat", streamer.getName()));
      /*  JsonObject jsonObject = Json.createObjectBuilder()
                .add("type", "get_users_list")
                .add("user_id", 0)
                .add("data", Json.createObjectBuilder()
                        .add("channel_id", streamer.getChannelId()))
                .build();
        String message = jsonObject.toString();
        session.getAsyncRemote().sendText(message);*/
        // String message = "{\"type\":\"get_users_list\",\"data\":{\"channel_id\":7308}}";
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // logger.info("Message has been received: " + message);
        JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
        JsonString type = jsonObject.getJsonString("type");
        System.out.print(message);
        if (type.getString().equals("users_list")) {
            JsonArray jsonUsers = jsonObject.getJsonObject("data").getJsonArray("users");
            for (int i = 0; i < jsonUsers.size(); i++) {
                JsonObject jsonUser = jsonUsers.getJsonObject(i);
                JsonString jsonUserName = jsonUser.getJsonString("name");
                usersInChat.add(new User(jsonUserName.getString()));
            }
            countDownLatch.countDown();
        }


    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        countDownLatch.countDown();
        logger.info(String.format("Connection closed %s", closeReason));
        this.session = null;
        // trying to reconnect
      /*  try {
            Thread.currentThread().sleep(RECONNECT_INTERVAL_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
        finally {
            try {
                reconnect();
            } catch (Exception e) {
                e.printStackTrace();
                logger.info(e.getMessage());
            }
        }*/
    }

    public synchronized Set<User> getUsersInChat(Streamer streamer) throws IOException {
        this.usersInChat.clear();
        if (session == null) {
            try {
                openConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            JsonObject jsonObject = Json.createObjectBuilder()
                    .add("type", "get_users_list")
                    .add("user_id", 0)
                    .add("data", Json.createObjectBuilder()
                            .add("channel_id", streamer.getChannelId()))
                    .build();
            this.countDownLatch = new CountDownLatch(1);
            this.session.getBasicRemote().sendText(jsonObject.toString());
            countDownLatch.await(WAIT_TIME_OUT_MS, TimeUnit.MILLISECONDS);
            //boolean result = this.countDownLatch.await(WAIT_TIME_OUT_MS, TimeUnit.MILLISECONDS);
            //if (result) {
             //   logger.info(String.format("[%s] Get userlist timeout!!!", streamer));
          //}
         //   disconnect();
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.info(e.toString());
        }
        return this.usersInChat;
    }
}










