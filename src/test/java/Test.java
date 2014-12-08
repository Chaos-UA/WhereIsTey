
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class Test extends Application {
    static String goodgame = "http://goodgame.ru/channel/Mantich/";

    public static void main(String[] args) throws Exception {

    }

    @Override
    public void start(Stage stage) throws Exception {
        WebView webView = new WebView();
        webView.getEngine().load("http://goodgame.ru/");
        stage.setScene(new Scene(webView));
        stage.show();
        System.out.println(webView.getEngine().getDocument());
    }
}
