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
        ThemeManager.getInstance().init(
                getClass().getResource("StyleSheets/LightTheme.css").toExternalForm(),
                getClass().getResource("StyleSheets/DarkTheme.css").toExternalForm());

        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SunTailoringGUI.fxml"));
        Parent root = fxmlLoader.load();
        final SunTailoringGUIController controller = fxmlLoader.getController();
        primaryStage.setTitle(APP_TITLE);
        primaryStage.getIcons().add(Assets.ST_LOGO);
        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            controller.stopSummaryTimer();
            controller.saveAddressBook();
        });
    }


    public static void main(String[] args) {
        launch(args);
    }
}