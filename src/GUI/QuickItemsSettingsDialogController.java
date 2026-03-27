package GUI;

import Data.Item;
import Data.QuickItems;
import Utils.PathUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class QuickItemsSettingsDialogController implements Initializable {

    public VBox root;
    public Button saveButton;
    public Button addButton;
    public TableView<Item> quickItemsTable;
    public TableColumn<Item, String> itemNameCol;
    public TableColumn<Item, Double> itemUnitPriceCol;

    private final BooleanProperty modified = new SimpleBooleanProperty(false);

    private String name;
    private QuickItems quickItems;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setOnKeyPressed(keyEvent -> {
            if (Shortcut.CTRL_S.getKeyCombo().match(keyEvent)) {
                saveQuickItems();
            } else if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                final Stage stage = (Stage) root.getScene().getWindow();
                stage.close();
            }
        });
        saveButton.disableProperty().bind(modified.not());
    }

    public void setQuickItems(String name, QuickItems quickItems) {
        this.name = name;
        this.quickItems = quickItems;
        quickItemsTable.setItems(quickItems.getItems());

        itemNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        itemNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        itemNameCol.setOnEditCommit(event -> {
            quickItemsTable.getSelectionModel().getSelectedItem().setName(event.getNewValue());
            modified.setValue(true);
        });

        itemUnitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        itemUnitPriceCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        itemUnitPriceCol.setOnEditCommit(event -> {
            quickItemsTable.getSelectionModel().getSelectedItem().setUnitPrice(event.getNewValue());
            modified.setValue(true);
        });

        quickItemsTable.setOnKeyPressed(event -> {
            final int selectedIndex = quickItemsTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < quickItemsTable.getItems().size()) {
                if (event.getCode().equals(KeyCode.DELETE)) {
                    quickItems.getItems().remove(selectedIndex);
                    modified.setValue(true);
                }
            }
        });
    }

    public QuickItems getQuickItems() {
        return quickItems;
    }

    public void saveQuickItems() {
        quickItems.save(name);
        modified.setValue(false);
        System.out.println("Successfully saved Quick " + name);
    }

    public void addQuickItem(ActionEvent actionEvent) {
        actionEvent.consume();
        quickItems.getItems().add(new Item(name + " - ", 1, 0));
        modified.setValue(true);
    }
}
