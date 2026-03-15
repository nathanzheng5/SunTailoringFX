package GUI;

import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private static final ThemeManager INSTANCE = new ThemeManager();

    private String lightThemeUrl;
    private String darkThemeUrl;
    private boolean darkMode = true;
    private final List<Scene> scenes = new ArrayList<>();

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public void init(String lightThemeUrl, String darkThemeUrl) {
        this.lightThemeUrl = lightThemeUrl;
        this.darkThemeUrl = darkThemeUrl;
    }

    public void registerScene(Scene scene) {
        scenes.add(scene);
        applyTheme(scene);
        scene.windowProperty().addListener((obs, oldWin, newWin) -> {
            if (newWin != null) {
                newWin.setOnHidden(e -> scenes.remove(scene));
            }
        });
    }

    public void setDarkMode(boolean dark) {
        darkMode = dark;
        scenes.forEach(this::applyTheme);
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    private void applyTheme(Scene scene) {
        scene.getStylesheets().remove(lightThemeUrl);
        scene.getStylesheets().remove(darkThemeUrl);
        scene.getStylesheets().add(darkMode ? darkThemeUrl : lightThemeUrl);
    }
}