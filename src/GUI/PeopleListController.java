package GUI;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.List;

public class PeopleListController {

    @FXML
    private ListView<String> listView;

    /**
     * This method is called after the FXML file has been loaded.
     * Use it to initialize the UI and load data into the ListView.
     */
    @FXML
    public void initialize() {
        // Example data (you can replace this with actual dynamic data)
        List<String> peopleNames = List.of("John Doe", "Jane Smith", "Michael Johnson", "Emily Davis", "Chris Wilson");

        // Add data to the ListView
        listView.getItems().addAll(peopleNames);
    }
}