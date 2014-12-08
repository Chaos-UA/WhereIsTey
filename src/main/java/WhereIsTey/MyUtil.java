package WhereIsTey;


import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

import javax.websocket.DeploymentException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyUtil {
    private static final String GOOD_GAME_SERVER_WS_URL = "ws://chat.goodgame.ru:8081/chat/websocket";


    public static String getGoodGameServerWsUrl() {
        return GOOD_GAME_SERVER_WS_URL;
    }

    public static String getUrlContentWithJavaScript(String url) throws IOException {
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_24);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.setJavaScriptTimeout(1000);
        webClient.getOptions().setTimeout(10_000);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        try {
            return webClient.getPage(url).getWebResponse().getContentAsString();
        }
        finally {
            webClient.closeAllWindows();
        }
    }

    public static String getUrlContentWithoutJavaScript(String url) throws IOException {
        URL website = new URL(url);



        StringBuilder response = new StringBuilder();
        try {

            String inputLine;
            URLConnection connection = website.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return response.toString();
    }

    public static Set<Streamer> getOnlineStreamers() throws IOException {
        String content = getUrlContentWithJavaScript("http://goodgame.ru/channels/page/1/");
        int pageCount = getPageCount(content);
        int page = 1;
        HashSet<Streamer> streamers = new HashSet<>();
        for (int i = page; i < pageCount; i++) {
            if (content == null) {
                content = getUrlContentWithJavaScript(String.format("http://goodgame.ru/channels/page/%s/", i));
            }
            appendToStreamers(streamers, content);
            content = null;
        }
        return streamers;
    }

    public static void loadChannelIdInParallel(Set<Streamer> streamers, final OnMessageOccur onMessageOccur) throws InterruptedException {
        final ArrayList<Streamer> streamersLeft = new ArrayList<>(streamers);
        int cores = Runtime.getRuntime().availableProcessors();
        final CountDownLatch countDownLatch = new CountDownLatch(streamers.size());
        for (int i = 0; i < cores; i++) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true) {
                        final Streamer streamer;
                        synchronized (streamersLeft) {
                            if (!streamersLeft.isEmpty()) {
                                streamer = streamersLeft.remove(0);
                                countDownLatch.countDown();
                            }
                            else {
                                return;
                            }
                        }
                        try {
                            streamer.getChannelId();
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                            onMessageOccur.onMessage(ex.toString());
                        }
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
        countDownLatch.await();
    }

    public static Set<Streamer> getStreamers(String channelsContent) {
        Set<Streamer> streamers = new HashSet<>();
        appendToStreamers(streamers, channelsContent);
        return streamers;
    }

    public static void appendToStreamers(Set<Streamer> streamers, String channelsContent) {
        Pattern pattern = Pattern.compile("goodgame.ru/channel/([^/]+)/");
        Matcher matcher = pattern.matcher(channelsContent);
        while (matcher.find()) {
            streamers.add(new Streamer(matcher.group(1)));
        }
    }

    public static int getPageCount(String channelsContent) {
        Pattern pattern = Pattern.compile("page/(\\d+)/");
        System.out.println(channelsContent);
        Matcher matcher = pattern.matcher(channelsContent);
        int pages = 1;
        while (matcher.find()) {
            int page = Integer.parseInt(matcher.group(1));
            if (page > pages) {
                pages = page;
            }
        }
        return pages;
    }

    public static Set<User> getUsersInChat(Streamer streamer, GoodGameWebSocketClient goodGameWebSocketClient) throws URISyntaxException, DeploymentException, IOException {
        return goodGameWebSocketClient.getUsersInChat(streamer);
    }

    public static Set<Streamer> getStreamsWhereUserInChat(Set<Streamer> streamers, User userToFind, GoodGameWebSocketClient goodGameWebSocketClient) throws URISyntaxException, DeploymentException, IOException {
        Set<Streamer> streamersWhereUserInChat = new HashSet<>();
        for (Streamer streamer : streamers) {
            Set<User> users = getUsersInChat(streamer, goodGameWebSocketClient);
            if (users.contains(userToFind)) {
                streamersWhereUserInChat.add(streamer);
            }
        }
        return streamersWhereUserInChat;
    }

    public static Streamer getStreamerByChannelId(int channelId) throws IOException {
        String content = MyUtil.getUrlContentWithoutJavaScript(String.format("http://goodgame.ru/api/getggchannelstatus?id=%s&fmt=json", channelId)).trim();
        if (content.equals("[]")) {
            return null;
        }
      /*  Pattern pattern = Pattern.compile("stream_id[^\\d]+(\\d+)");
        Matcher matcher = pattern.matcher(content);

        Integer channelId = null;
        System.out.println("");
        if (matcher.find()) {
            channelId = Integer.parseInt(matcher.group(1));
        }
        System.out.println(channelId);*/
        // userName
        Pattern pattern = Pattern.compile("goodgame.ru\\\\/channel\\\\/([^\\\\]+)\\\\/");
        Matcher matcher = pattern.matcher(content);
        String streamerName = null;
        if (matcher.find()) {
            streamerName = matcher.group(1);
        }
        //
        if (streamerName == null) {
            throw new RuntimeException("Can`t find streamer name");
        }
        Streamer streamer = new Streamer(streamerName);
        streamer.setChannelId(channelId);
        return streamer;
    }
}










