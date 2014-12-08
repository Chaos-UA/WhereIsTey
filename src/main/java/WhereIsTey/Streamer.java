package WhereIsTey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Streamer {
    private final String name;
    private int channelId;

    public Streamer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getChannelUrl() {
        return "http://goodgame.ru/channel/" + name + "";
    }

    public String getChatUrl() {
        return "http://goodgame.ru/chat/" + name + "/";
    }

    public void setChannelId(int id) {
        this.channelId = id;
    }

    public int getChannelId()  {
        if (this.channelId == 0) {
            try {
                String jsonContent = MyUtil.getUrlContentWithoutJavaScript("http://goodgame.ru/api/getggchannelstatus?id=" + name + "&fmt=json");
                Pattern pattern = Pattern.compile("\"stream_id\":\\s*\"(\\d+)\"");
                Matcher matcher = pattern.matcher(jsonContent);
                if (matcher.find()) {
                    this.channelId = Integer.parseInt(matcher.group(1));
                }
                else {
                    String chatContent = MyUtil.getUrlContentWithJavaScript(getChatUrl());
                    //
                    pattern = Pattern.compile("channelId:\\s*(\\d+)");
                    matcher = pattern.matcher(chatContent);

                    if (matcher.find()) {
                        this.channelId = Integer.parseInt(matcher.group(1));
                    }
                    else {
                        throw new RuntimeException("Can't find channel id");
                    }
                }
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return this.channelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Streamer streamer = (Streamer) o;

        if (!name.equals(streamer.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
