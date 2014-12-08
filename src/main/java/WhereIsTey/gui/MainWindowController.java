package WhereIsTey.gui;

import WhereIsTey.*;
import WhereIsTey.chat_watcher.ChatsWatcherController;
import com.gtranslate.Audio;
import com.gtranslate.Language;
import javafx.application.Platform;
import javazoom.jl.decoder.JavaLayerException;

import javax.swing.*;
import javax.websocket.DeploymentException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MainWindowController implements OnMessageOccur {
    /**
     * in milliseconds
     */
    private static long UPDATE_INTERVAL_BETWEEN_SUCCESSES = 30000;
    private final MainWindow mainWindow;
    private final Logger logger = Logger.getLogger(MainWindowController.class.getName());
    private final Set<Streamer> streamers = new HashSet<>();
    private final GoodGameWebSocketClient goodGameWebSocketClient;
    private final ChatsWatcherController chatsWatcherController;
   // private final StreamSearcher streamSearcher = new StreamSearcher(logger);
  //  private final Set<Streamer> streamersWhereUserFound = new HashSet<>();
    private MyTimer myTimer = new MyTimer();
    private boolean firstSuccessedCycle = true;
    private final BlockingQueue<MessageToSound> messagesToSound = new ArrayBlockingQueue<>(100, true);
    private final MyThreadSound myThreadSound = new MyThreadSound();

    {
        logger.addHandler(new Handler() {

            @Override
            public void publish(final LogRecord record) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        mainWindow.getTextAreaLog().appendText(record.getMessage());
                        mainWindow.getTextAreaLog().appendText("\n");
                    }
                };
                if (Platform.isFxApplicationThread()) {
                    runnable.run();
                }
                else {
                    Platform.runLater(runnable);
                }
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        });
    }

    public MainWindowController(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.goodGameWebSocketClient = new GoodGameWebSocketClient(logger);
        this.chatsWatcherController = new ChatsWatcherController(this, this.logger);
        logger.info("Starting text to voice thread");
        myThreadSound.setDaemon(true);
        myThreadSound.start();
    }

    public void appendUserMessage(final String message) {
        if (Platform.isFxApplicationThread()) {
            throw new RuntimeException("Should not be called from fx thread");
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    mainWindow.getTextAreaUserMessage().appendText(message);
                }
                finally {
                    countDownLatch.countDown();
                }
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.info(e.toString());
        }
    }

    @Override
    public void onMessage(String error) {
        logger.info(error);
    }

    class MyThreadSound extends Thread {
        MyThreadSound() {

        }

        @Override
        public void run() {
            while (true) {
                try {
                    MessageToSound messageToSound = messagesToSound.take();
                    Audio audio = Audio.getInstance();
                    try (InputStream isTextToSpeech = audio.getAudio(messageToSound.getMessage(), messageToSound.getLanguage())) {
                        audio.play(isTextToSpeech);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    logger.info(ex.toString());
                }
            }
        }
    }

    class MyTimer implements Runnable {
        private User user;
        private Thread thread = null;

        MyTimer() {
        }

        public void start(User user) {
            this.user = user;
            this.stop();
            this.thread = new Thread(this);
            this.thread.setDaemon(true);
            this.thread.start();
        }


        public void stop() {
            if (this.thread != null) {
                this.thread.stop();
                this.thread = null;
            }
        }



        @Override
        public void run() {
            firstSuccessedCycle = true;
            while (true) {
                try {
                    onTimer(this.user);
                }
                catch (ThreadDeath e) {
                    logger.info(e.toString());
                    return;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    logger.info(e.toString());
                }
                try {
                    logger.info("Cycle complete");
                    System.gc();
                    logger.info(String.format("Sleeping for %s milliseconds\n", UPDATE_INTERVAL_BETWEEN_SUCCESSES));
                    Thread.sleep(UPDATE_INTERVAL_BETWEEN_SUCCESSES);
                }
                catch (ThreadDeath e) {
                    logger.info(e.toString());
                    return;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    logger.info(e.toString());
                }
            }
        }
    }

    synchronized public void soundIt(String message, String language) {
        try {
            for (String msg : message.split("[!?,.()]+")) {
                messagesToSound.put(new MessageToSound(msg, language));
            }
          //  messagesToSound.put(new MessageToSound(message, language));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            logger.info(ex.toString());
        }
    }

    private void onTimer(User userName) throws IOException, InterruptedException, URISyntaxException, DeploymentException, JavaLayerException {
        if (Platform.isFxApplicationThread()) {
            throw new RuntimeException("Have to be called from not fx thread");
        }
      //  streamSearcher.flushTo(streamers);

        logger.info("Getting online streamers");
        final Set<Streamer> onlineStreamers = MyUtil.getOnlineStreamers();
        logger.info(String.format("There are %s streamers online: %s", onlineStreamers.size(), onlineStreamers));
        for (Streamer streamer : onlineStreamers) {
            if (!streamers.contains(streamer)) {
                streamers.add(streamer);
            }
        }
      //  logger.info(String.format("There are %s channels found: %s", streamers.size(), streamers));


        Thread.sleep(5000);

        logger.info("Getting channel identifiers");
        Iterator<Streamer> streamerIterator = streamers.iterator();
        while (streamerIterator.hasNext()) {
            Streamer streamer = streamerIterator.next();
            try {
                streamer.getChannelId();
            }
            catch (Exception ex) {
                logger.info(String.format("[%s] Can't get channel id. Removing streamer", streamer));
                streamerIterator.remove();
            }

        }
       // MyUtil.loadChannelIdInParallel(streamers, this); // gg update problem

        logger.info(String.format("Getting streamers where %s in chat", userName));
        final Set<Streamer> streamersWhereUserInChat = MyUtil.getStreamsWhereUserInChat(streamers, userName, goodGameWebSocketClient);
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        boolean previousCycleUserFound = !mainWindow.getPanelStreams().isEmpty();

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {

                    mainWindow.getPanelStreams().setStreams(streamersWhereUserInChat);

                } catch (IOException e) {
                    e.printStackTrace();
                    logger.info(e.toString());
                }
                finally {
                    countDownLatch.countDown();
                }
            }
        });

        chatsWatcherController.setStreamersWhereUserIs(streamersWhereUserInChat, userName);
        countDownLatch.await();
        if (streamersWhereUserInChat.isEmpty()) {
            logger.info(String.format("%s has not been found", userName.getName()));
        }
        else {
            logger.info(String.format("Streams where %s has been found: %s", userName.getName(), streamersWhereUserInChat));
        }

        if (previousCycleUserFound && streamersWhereUserInChat.isEmpty()) {
            soundIt(String.format("%s has been lost. Repeat! %s has been lost!", userName.getName(), userName.getName()), Language.ENGLISH);
        }
        else if (!previousCycleUserFound && !streamersWhereUserInChat.isEmpty()) {
            soundIt(String.format("%s has been found. Repeat! %s has been found!", userName.getName(), userName.getName()), Language.ENGLISH);
        }
        else if (firstSuccessedCycle && streamersWhereUserInChat.isEmpty()) {
            soundIt(String.format("%s has not been found. Repeat! %s has not been found!", userName.getName(), userName.getName()), Language.ENGLISH);
        }
        firstSuccessedCycle = false;
    }

    public void startWatching() {
        final User userName = new User(mainWindow.getTfUserName().getText().trim());
        mainWindow.getTfUserName().setText(userName.getName());
        String interval = mainWindow.getTfIntervalMs().getText().trim().replaceAll(" ", "");
        if (userName.getName().isEmpty()) {
            JOptionPane.showMessageDialog(null, "User name can't be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            UPDATE_INTERVAL_BETWEEN_SUCCESSES = Long.parseLong(interval);
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Interval should be numeric value", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        logger.info(String.format("Start watching for %s", userName));
        this.mainWindow.getTfUserName().setDisable(true);
        this.mainWindow.getTfIntervalMs().setDisable(true);
        this.mainWindow.getBtnWatch().setText("Stop watching");
        try {
            this.mainWindow.getPanelStreams().setStreams(new HashSet<Streamer>(1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.myTimer.start(userName);
    }

    public void stopWatching() {
        logger.info(String.format("Stop watching for %s\n", mainWindow.getTfUserName().getText()));
        myTimer.stop();
        chatsWatcherController.clear();
        this.mainWindow.getTfUserName().setDisable(false);
        this.mainWindow.getTfIntervalMs().setDisable(false);
        this.mainWindow.getBtnWatch().setText("Watch");
    }

    public void openStream(Streamer streamer) {
        try {
            URI uri = new URI(streamer.getChannelUrl());
            if (!DesktopAPI.browse(uri)) {
                logger.info("Can't browse on " + System.getProperty("os.name") + " platform");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.toString());
        }
    }

    public void watch() {
        if (this.mainWindow.getBtnWatch().getText().equals("Watch")) {
            startWatching();
        }
        else if (this.mainWindow.getBtnWatch().getText().equals("Stop watching")) {
            stopWatching();
        }
        else throw new RuntimeException();
    }

    @Override
    protected void finalize() throws Throwable {
        this.myThreadSound.stop();
        this.myTimer.stop();
        super.finalize();
    }
}
