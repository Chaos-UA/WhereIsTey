package WhereIsTey;

import WhereIsTey.gui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Level;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.setProperty("file.encoding", "UTF-8");
       // LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
     /*   SwingUtilities.invokeAndWait(new Runnable() {   problem with JOptionPane
            @Override
            public void run() {
                try {
                    String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
                    UIManager.setLookAndFeel(systemLookAndFeel);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });     */
        new MainWindow().show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
