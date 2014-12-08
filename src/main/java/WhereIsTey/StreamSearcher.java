package WhereIsTey;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Лучше не надо. Будет слишком много каналов для поиска пользователя
 */
public class StreamSearcher implements Runnable {
    private static final long WAIT_TIME_OUT_ON_ERROR_MS = 5000;
    private final Set<Streamer> channelsToFlush = new HashSet<>();
    private final BlockingQueue<Streamer> blockingQueue = new ArrayBlockingQueue<>(100, false);
    private final Logger logger;

    private int currentChannelId = 0;
    private static final int LAST_CHANNEL_ID = Integer.MAX_VALUE;

    public StreamSearcher(Logger logger) {
        this.logger = logger;
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        logger.info("Start searching streams by identifiers");
        thread.start();
    }

    public void flushTo(Set<Streamer> streamers) {
        synchronized (this.channelsToFlush) {
            streamers.addAll(this.channelsToFlush);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (currentChannelId >= LAST_CHANNEL_ID) {
                    logger.info("Searching streams by identifiers has been finished");
                    return;
                }
                Streamer streamer = MyUtil.getStreamerByChannelId(currentChannelId);
                if (streamer != null) {
                    synchronized (this.channelsToFlush) {
                        channelsToFlush.add(streamer);
                    }
                }
                currentChannelId++;

            }
            catch (Exception ex) {
                ex.printStackTrace();
                logger.info(ex.toString());
                try {
                    Thread.sleep(WAIT_TIME_OUT_ON_ERROR_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.info(e.toString());
                }
            }
        }
    }
}
