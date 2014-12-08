package WhereIsTey.gui;

import WhereIsTey.Streamer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MainWindow extends Stage {
    private final static int MAX_ROW_COUNT_TO_CLEAR = 100;

    private final Button btnWatch = new Button("Watch");
    private final TextField tfUserName = new TextField("Tey");
    private final TextField tfIntervalMs = new TextField("30 000");
    private final MyTextArea textAreaLog = new MyTextArea();
    private final MyTextArea textAreaUserMessage = new MyTextArea();
    private final PanelStreams panelStreams = new PanelStreams();
    private final Font titleFont;
    private MainWindowController mainWindowController = new MainWindowController(this);

    {

        EventHandler<ActionEvent> actionEvent = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                mainWindowController.watch();
            }
        };
        btnWatch.setOnAction(actionEvent);
        tfUserName.setOnAction(actionEvent);
        tfIntervalMs.setOnAction(actionEvent);

        Label lbl = new Label();
        Font font = lbl.getFont();
        titleFont = Font.font(font.getName(), FontWeight.BOLD, font.getSize());
    }

    public MainWindow() throws IOException {
        this.setTitle("Where is Tey?");
        this.getIcons().add(new Image(MainWindow.class.getResource("mini-happy.png").toString()));
        this.setScene(new Scene(new PanelMain(), 800, 600));
    }

    class MyTextArea extends TextArea {
        MyTextArea() {

        }

        @Override
        public void appendText(String s) {
            if (this.getText().split("\n").length >= MAX_ROW_COUNT_TO_CLEAR) {
                this.clear();
            }
            super.appendText(s);
        }
    }

    public PanelStreams getPanelStreams() {
        return panelStreams;
    }

    public Button getBtnWatch() {
        return btnWatch;
    }

    public TextField getTfUserName() {
        return tfUserName;
    }

    public TextField getTfIntervalMs() {
        return tfIntervalMs;
    }

    public TextArea getTextAreaLog() {
        return textAreaLog;
    }

    public MainWindowController getMainWindowController() {
        return mainWindowController;
    }

    class PanelWatch extends HBox {
        PanelWatch() {
            this.setPadding(new Insets(4));

          //  this.setHgap(5); this.setVgap(5);
            tfUserName.setMaxWidth(Double.MAX_VALUE);
            tfIntervalMs.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tfUserName, Priority.ALWAYS);
         //   tfUserName.set
            Label lblUser = new Label("User: ");
            lblUser.setLabelFor(tfUserName);
            Label lblInterval = new Label(" Interval ms: ");
            lblInterval.setLabelFor(tfIntervalMs);
            HBox.setHgrow(tfIntervalMs, Priority.ALWAYS);
            this.getChildren().addAll(lblUser, tfUserName, lblInterval, tfIntervalMs, btnWatch);
        }
    }

    class PanelMain extends BorderPane {
        PanelMain() {
           // this.setPadding(new Insets(5));
           // this.setHgap(5); this.setVgap(5);
            this.setTop(new PanelWatch());
            SplitPane splitPane = new SplitPane();
            splitPane.setOrientation(Orientation.VERTICAL);
            final ScrollPane scrollPaneStreams = new ScrollPane();
            scrollPaneStreams.setContent(panelStreams);

            BorderPane paneStreamers = new BorderPane();

            Label lbl = new Label("Streams where user has been found");
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setFont(titleFont);
         //   lbl.setAlignment(Pos.);
            paneStreamers.setTop(lbl);
            paneStreamers.setCenter(scrollPaneStreams);
          //  ScrollPane scrollPaneLog = new ScrollPane();
          //  scrollPaneLog.setContent(textAreaLog);
          //  scrollPaneStreams.setContent(pnlMain);
            scrollPaneStreams.setStyle("-fx-background-color: rgba(0,0,0,0);");
            scrollPaneStreams.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
                @Override public void changed(ObservableValue<? extends Bounds> observableValue, Bounds oldBounds, Bounds newBounds) {
                    scrollPaneStreams.setFitToWidth(newBounds.getWidth() > panelStreams.prefWidth(Region.USE_PREF_SIZE));
                    scrollPaneStreams.setFitToHeight(newBounds.getHeight() > panelStreams.prefHeight(Region.USE_PREF_SIZE));
                }
            });


            //

            lbl = new Label("User messages");
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setFont(titleFont);

            BorderPane paneUserMessages = new BorderPane();
            paneUserMessages.setTop(lbl);
            paneUserMessages.setCenter(textAreaUserMessage);

            //

            lbl = new Label("Log");
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setFont(titleFont);

            BorderPane paneLog = new BorderPane();
            paneLog.setTop(lbl);
            paneLog.setCenter(textAreaLog);

            splitPane.getItems().addAll(paneStreamers, paneUserMessages, paneLog);
            splitPane.setDividerPositions(0.33, 0.66);
            this.setCenter(splitPane);
        }
    }

    public MyTextArea getTextAreaUserMessage() {
        return textAreaUserMessage;
    }

    public class PanelStreams extends GridPane implements EventHandler<ActionEvent> {
        private boolean empty = true;

        PanelStreams() {
            try {
                this.setStreams(new HashSet<Streamer>(1));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean isEmpty() {
            return empty;
        }

        public void setStreams(Set<Streamer> streams) throws IOException {
            empty = streams.isEmpty();
            this.getChildren().clear();
            int v = 0;
            for (Streamer streamer : streams) {
                Button btnOpenStream = new Button("Open");

                btnOpenStream.getProperties().put("streamer", streamer);
                btnOpenStream.setTooltip(new Tooltip(String.format("Open %s`s stream [channel id: %s]", streamer.getName(), streamer.getChannelId())));
                btnOpenStream.setOnAction(this);

                TextField tfUrl = new TextField(streamer.getChannelUrl());
                tfUrl.setEditable(false);
                tfUrl.setMaxWidth(Double.MAX_VALUE);
                Label lbl = new Label(String.format("channel id: %s", streamer.getChannelId()));
                GridPane.setConstraints(btnOpenStream ,1,v,1,1,HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
                GridPane.setConstraints(tfUrl ,2,v,1,1,HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
                GridPane.setConstraints(lbl ,3,v++,1,1,HPos.CENTER, VPos.CENTER, Priority.NEVER, Priority.NEVER);
            //    HBox.setHgrow(tfUrl, Priority.ALWAYS);
            //    HBox hBox = new HBox();
           //     hBox.setMaxWidth(Double.MAX_VALUE);

                this.getChildren().addAll(btnOpenStream, tfUrl, lbl);

               // this.getChildren().addAll(hBox);
            }
        }

        @Override
        public void handle(ActionEvent actionEvent) {
            Button btn = (Button) actionEvent.getSource();
            Streamer streamer = (Streamer) btn.getProperties().get("streamer");
            mainWindowController.openStream(streamer);
        }
    }
}
