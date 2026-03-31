package GUI;

import Data.AddressBook;
import Data.CustomerInfo;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AddressBookDialogController implements Initializable {

    @FXML
    public VBox root;
    @FXML
    public TextField searchTextField;
    @FXML
    public TableView<CustomerInfo> addressBookTable;
    @FXML
    public TableColumn<CustomerInfo, String> nameCol;
    @FXML
    public TableColumn<CustomerInfo, String> phoneCol;
    @FXML
    public TableColumn<CustomerInfo, String> emailCol;

    private AddressBook addressBook;
    private FilteredList<CustomerInfo> filteredList;
    private ReadOnlyObjectWrapper<CustomerInfo> selectedCustomerInfo;

    public void setAddressBook(AddressBook addressBook) {
        this.addressBook = addressBook;
        updateTable();
    }

    private void updateTable() {
        filteredList = new FilteredList<>(addressBook.getEntries());
        SortedList<CustomerInfo> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(addressBookTable.comparatorProperty());
        addressBookTable.setItems(sortedList);
    }

    public ReadOnlyObjectWrapper<CustomerInfo> selectedCustomerInfoProperty() {
        return selectedCustomerInfo;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                final Stage stage = (Stage) root.getScene().getWindow();
                stage.close();
            }
        });

        selectedCustomerInfo = new ReadOnlyObjectWrapper<>();

        addressBookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        nameCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        phoneCol.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        emailCol.setMaxWidth(1f * Integer.MAX_VALUE * 50);

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        // double click sets the selected customer (which in turn updates the listeners), and closes the dialog
        addressBookTable.setRowFactory(tableView -> {
            TableRow<CustomerInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectedCustomerInfo.setValue(row.getItem());
                    final Stage stage = (Stage) addressBookTable.getScene().getWindow();
                    stage.close();
                }
            });
            return row;
        });

        addressBookTable.setOnKeyPressed(event -> {
            final int selectedIndex = addressBookTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < filteredList.size()) {
                final CustomerInfo customerInfo = filteredList.get(selectedIndex);
                if (event.getCode().equals(KeyCode.DELETE)) {
                    addressBook.remove(customerInfo);
                    updateTable();
                }
            }
        });

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(customerInfo ->
                    newValue == null || newValue.isEmpty() || customerInfo.containsText(newValue));
        });
    }
}
