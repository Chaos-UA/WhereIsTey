package WhereIsTey.chat_watcher;

import WhereIsTey.Streamer;
import WhereIsTey.User;
import WhereIsTey.gui.MainWindowController;
import com.gtranslate.Language;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class ChatsWatcherController implements OnUserMessage {
    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("[HH:mm:ss]");
    private final static long RECONNECTOR_SLEEP_MS = 10000;
    private final Logger logger;// = Logger.getLogger(ChatsWatcherController.class.getName());
    private Set<ChatWatcher> chatWatchers = new HashSet<>();
    private final MainWindowController mainWindowController;
    private final ThreadReconnector threadReconnector = new ThreadReconnector();



    public ChatsWatcherController(MainWindowController mainWindowController, Logger logger) {
        this.logger = logger;
        this.mainWindowController = mainWindowController;
        logger.info("Starting reconnector thread");
        threadReconnector.setDaemon(true);
        threadReconnector.start();
    }

    public synchronized void clear() {
        for (ChatWatcher chatWatcher : this.chatWatchers) {
            try {
                chatWatcher.disconnect();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                logger.info(ex.toString());
            }
        }
        this.chatWatchers.clear();
    }

    public synchronized void setStreamersWhereUserIs(Set<Streamer> streamersWhereUserIs, User userToWatch) {
        streamersWhereUserIs = new HashSet<>(streamersWhereUserIs); // make copy
        Iterator<ChatWatcher> chatWatcherIterator = chatWatchers.iterator();

        while (chatWatcherIterator.hasNext()) {
            ChatWatcher chatWatcher = chatWatcherIterator.next();
            if (streamersWhereUserIs.contains(chatWatcher.getStreamer())) {
                chatWatcher.setUserToWatch(userToWatch);
                streamersWhereUserIs.remove(chatWatcher.getStreamer());
            }
            else { // remove
                chatWatcher.disconnect();
                chatWatcherIterator.remove();
            }
        }
        // append new
        for (Streamer streamer : streamersWhereUserIs) {
            ChatWatcher chatWatcher = new ChatWatcher(streamer, userToWatch, logger, this);
            chatWatchers.add(chatWatcher);
        }
    }

    @Override
    public void onUserMessage(Streamer streamer, User user, String message) {
        mainWindowController.appendUserMessage(String.format("%s [%s] %s: %s\n",
                SIMPLE_DATE_FORMAT.format(new Date()), streamer, user, message));
        String soundIt = String.format("На стриме у %s пользователь %s написал сообщение. Цитирою:",
                streamer, user
        );
        synchronized (mainWindowController) { // google won't sound too long string
            mainWindowController.soundIt(soundIt, Language.RUSSIAN);
            mainWindowController.soundIt(message, Language.RUSSIAN);
        }
    }

    @Override
    public void onToUserMessage(Streamer streamer, User fromUser, User toUser, String message) {
        mainWindowController.appendUserMessage(String.format("%s [%s] %s: %s\n",
                SIMPLE_DATE_FORMAT.format(new Date()), streamer, fromUser, message)
        );
        String soundIt = String.format("На стриме у %s пользователь %s написал пользователю %s сообщение. Цитирою:",
                streamer, fromUser, toUser
        );
        synchronized (mainWindowController) { // google won't sound too long string
            mainWindowController.soundIt(soundIt, Language.RUSSIAN);
            mainWindowController.soundIt(message, Language.RUSSIAN);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.threadReconnector.destroyIt();
        super.finalize();
    }

    class ThreadReconnector extends Thread {
        private boolean destroy = false;

        public void destroyIt() {
            destroy = true;
            this.interrupt();
        }

        @Override
        public void run() {
            while (true) {
                try {
                  //  logger.info("Recconector");
                    if (destroy) {
                        logger.info("Stopping reconnector thread");
                        return;
                    }
                    synchronized (ChatsWatcherController.this) {
                        for (ChatWatcher chatWatcher : chatWatchers) {
                            if (!chatWatcher.isConnected()) {
                                chatWatcher.reconnect();
                            }
                        }
                    }
                    Thread.sleep(RECONNECTOR_SLEEP_MS);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    logger.info(ex.toString());
                }
            }
        }
    }
}