import WhereIsTey.GoodGameWebSocketClient;
import WhereIsTey.MyUtil;
import WhereIsTey.Streamer;
import WhereIsTey.User;
import com.gtranslate.Audio;
import com.gtranslate.Language;
import javazoom.jl.decoder.JavaLayerException;
import org.junit.Assert;
import org.junit.Test;

import javax.websocket.DeploymentException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.logging.Logger;


public class GeneralTests {
    private final Logger logger = Logger.getLogger(GeneralTests.class.getName());


    @org.junit.Test
    public void testGetStreamerChannelId() throws IOException {
        Streamer streamer = new Streamer("Tey");
        int channelId = streamer.getChannelId();
        logger.info(streamer.getName() + "`s channel id: " + channelId);
        Assert.assertNotEquals(channelId, 0);

    }

    @org.junit.Test
    public void testGetOnlineStreamers() throws IOException {
        Set<Streamer> streamers = MyUtil.getOnlineStreamers();
        logger.info(String.format("There are %s streamers online: %s", streamers.size(), streamers));
        Assert.assertTrue(streamers.size() > 0);
    }

    @org.junit.Test
    public void testGetChannelPagesCount() throws IOException {
        int pageCount = MyUtil.getPageCount(MyUtil.getUrlContentWithJavaScript("http://goodgame.ru/channels/page/1/"));
        logger.info(String.format("There are %s channel pages", pageCount));
        Assert.assertTrue(pageCount > 1);
    }

    @org.junit.Test
    public void testGetUsersInChat() throws IOException, URISyntaxException, DeploymentException {
        Streamer streamer = new Streamer("Tey");
        Set<User> usersInChat = MyUtil.getUsersInChat(streamer, new GoodGameWebSocketClient(logger));
        logger.info(String.format("There are %s users in %s`s chat: %s", usersInChat.size(), streamer.getName(), usersInChat));
        Assert.assertTrue(usersInChat.size() > 1);
    }

    @org.junit.Test
    public void testGetStreamerByChannelId() throws IOException, URISyntaxException, DeploymentException {
        Streamer streamer = MyUtil.getStreamerByChannelId(5);
        logger.info(String.format("Channel %s belong to %s", streamer.getChannelId(), streamer.getName()));
        Assert.assertTrue(streamer.getName().equals("Tey"));
    }

    @Test
    public void testTextToSpeech() throws IOException, JavaLayerException {
        Audio audio = Audio.getInstance();
        InputStream isTextToSpeech = audio.getAudio("Tey has been found", Language.ENGLISH);
        audio.play(isTextToSpeech);
        isTextToSpeech.close();
    }
}
