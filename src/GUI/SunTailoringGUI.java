package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SunTailoringGUI extends Application {

    private static final String APP_TITLE = "Sun Tailoring";

    @Override
    public void start(Stage primaryStage) throws Exception {
        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SunTailoringGUI.fxml"));
        Parent root = fxmlLoader.load();
        final Controller controller = fxmlLoader.getController();
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            controller.saveAddressBook();
        });
        primaryStage.setMaximized(true);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
