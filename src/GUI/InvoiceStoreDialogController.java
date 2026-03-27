package GUI;

import Data.Invoice;
import Data.InvoiceStore;
import Utils.PathUtils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.CurrencyStringConverter;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InvoiceStoreDialogController implements Initializable {

    public VBox root;
    public TextField searchByCustomerTextField;

    public ToggleGroup dueDateToggleGroup;
    public RadioButton dueAnyRadioButton;
    public RadioButton dueTodayRadioButton;
    public RadioButton dueTomorrowRadioButton;
    public RadioButton due3DaysRadioButton;
    public RadioButton due1WeekRadioButton;

    public ToggleGroup inDateToggleGroup;
    public RadioButton inAnyRadioButton;
    public RadioButton inTodayRadioButton;
    public RadioButton inYesterdayRadioButton;
    public RadioButton in3DaysRadioButton;
    public RadioButton in1WeekRadioButton;
    public RadioButton in1MonthRadioButton;
    public RadioButton in2MonthRadioButton;

    @FXML
    public TableView<Invoice> invoiceStoreTable;
    @FXML
    public TableColumn<Invoice, String> invoiceNumberCol;
    @FXML
    public TableColumn<Invoice, LocalDate> invoiceDateCol;
    @FXML
    public TableColumn<Invoice, LocalDate> dueDateCol;
    @FXML
    public TableColumn<Invoice, String> customerInfoCol;
    @FXML
    public TableColumn<Invoice, String> itemsCol;
    @FXML
    public TableColumn<Invoice, String> totalCol;
    @FXML
    public TableColumn<Invoice, Boolean> paidCol;
    @FXML
    public TableColumn<Invoice, Boolean> doneCol;
    @FXML
    public TableColumn<Invoice, Boolean> pickedUpCol;
    @FXML
    public CheckBox notDoneOnlyCheckBox;
    @FXML
    public CheckBox hideDryCleanOnlyCheckBox;
    @FXML
    public Label numInvoicesLabel;
    @FXML
    public Label invoicesTotalLabel;
    public Label filterLabel;
    public Button clearCustomerTextFieldButton;

    public Button quickFilterDueTomorrowButton;
    public Button quickFilterDue3DaysButton;
    public Button quickFilterInTodayButton;
    public Button quickFilterIn7DaysButton;

    private InvoiceStore invoiceStore;

    private FilteredList<Invoice> filteredInvoices;
    private ReadOnlyObjectWrapper<Invoice> selectedInvoice;

    public void setInvoiceStore(InvoiceStore invoiceStore, @Nullable InvoiceStoreFilter filter) {
        this.invoiceStore = invoiceStore;

        filteredInvoices = new FilteredList<>(invoiceStore.all(), invoice -> true);
        setupInvoiceStoreTable();

        if (filter != null) {
            configureComponentsBasedOnFilter(filter);
            filter();
        }
    }

    private void configureComponentsBasedOnFilter(InvoiceStoreFilter filter) {
        searchByCustomerTextField.setText(filter.customerText);
        notDoneOnlyCheckBox.setSelected(filter.showNotDoneOnly);
        hideDryCleanOnlyCheckBox.setSelected(filter.hideDryCleanOnly);
        Toggle dueDateToggle = dueAnyRadioButton;
        switch (filter.dueDateSelectionIndex) {
            case 0:
                dueDateToggle = dueTodayRadioButton;
                break;
            case 1:
                dueDateToggle = dueTomorrowRadioButton;
                break;
            case 2:
                dueDateToggle = due3DaysRadioButton;
                break;
            case 3:
                dueDateToggle = due1WeekRadioButton;
                break;
            default:
                break;
        }
        dueDateToggleGroup.selectToggle(dueDateToggle);

        Toggle inDateToggle = inAnyRadioButton;
        switch (filter.inDateSelectionIndex) {
            case 0:
                inDateToggle = inTodayRadioButton;
                break;
            case 1:
                inDateToggle = inYesterdayRadioButton;
                break;
            case 2:
                inDateToggle = in3DaysRadioButton;
                break;
            case 3:
                inDateToggle = in1WeekRadioButton;
                break;
            case 4:
                inDateToggle = in1MonthRadioButton;
                break;
            case 5:
                inDateToggle = in2MonthRadioButton;
            default:
                break;
        }
        inDateToggleGroup.selectToggle(inDateToggle);
    }

    private void setupInvoiceStoreTable() {
        SortedList<Invoice> sortedList = new SortedList<>(filteredInvoices);
        sortedList.comparatorProperty().bind(invoiceStoreTable.comparatorProperty());
        invoiceStoreTable.setItems(sortedList);
        updateTotalLabels();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        selectedInvoice = new ReadOnlyObjectWrapper<>();

        root.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                final Stage stage = (Stage) root.getScene().getWindow();
                stage.close();
            }
        });

        invoiceNumberCol.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        invoiceDateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        invoiceDateCol.setCellFactory(column -> new FormattedLocalDateTableCell());
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        dueDateCol.setCellFactory(column -> new FormattedLocalDateTableCell());
        customerInfoCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getCustomerInfo().toString()));
        itemsCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getItemsAsString()));
        totalCol.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(new CurrencyStringConverter().toString(cellData.getValue().getTotal())));
        paidCol.setCellValueFactory(new PropertyValueFactory<>("paid"));
        paidCol.setCellFactory(column -> new CheckBoxTableCell<>());
        doneCol.setCellValueFactory(new PropertyValueFactory<>("done"));
        doneCol.setCellFactory(column -> new CheckBoxTableCell<>());
        pickedUpCol.setCellValueFactory(new PropertyValueFactory<>("pickedUp"));
        pickedUpCol.setCellFactory(column -> new CheckBoxTableCell<>());

        invoiceStoreTable.setRowFactory(table -> {
            final TableRow<Invoice> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectedInvoice.setValue(row.getItem());
                    final Stage stage = (Stage) table.getScene().getWindow();
                    saveFilters();
                    stage.close();
                }
            });
            return row;
        });

        searchByCustomerTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            filter();
        });

        updateFilterLabel();
    }

    private Predicate<Invoice> getAllPredicates() {
        return getCustomerTextPredicate()
                .and(getNotDoneOnlyPredicate())
                .and(getHideDryCleanOnlyPredicate())
                .and(getDueDatePredicate())
                .and(getInvoiceDatePredicate());
    }

    public void saveFilters() {
        String customerText = searchByCustomerTextField.getText().trim();
        boolean showNotDoneOnly = notDoneOnlyCheckBox.isSelected();
        boolean hideDryCleanOnly = hideDryCleanOnlyCheckBox.isSelected();

        int dueDateSelectionIndex = -1;
        Toggle dueDateToggle = dueDateToggleGroup.getSelectedToggle();
        if (dueDateToggle != null) {
            if (dueDateToggle == dueTodayRadioButton) {
                dueDateSelectionIndex = 0;
            } else if (dueDateToggle == dueTomorrowRadioButton) {
                dueDateSelectionIndex = 1;
            } else if (dueDateToggle == due3DaysRadioButton) {
                dueDateSelectionIndex = 2;
            } else if (dueDateToggle == due1WeekRadioButton) {
                dueDateSelectionIndex = 3;
            }
        }

        int inDateSelectionIndex = -1;
        Toggle inDateToggle = inDateToggleGroup.getSelectedToggle();
        if (inDateToggle != null) {
            if (inDateToggle == inTodayRadioButton) {
                inDateSelectionIndex = 0;
            } else if (inDateToggle == inYesterdayRadioButton) {
                inDateSelectionIndex = 1;
            } else if (inDateToggle == in3DaysRadioButton) {
                inDateSelectionIndex = 2;
            } else if (inDateToggle == in1WeekRadioButton) {
                inDateSelectionIndex = 3;
            } else if (inDateToggle == in1MonthRadioButton) {
                inDateSelectionIndex = 4;
            } else if (inDateToggle == in2MonthRadioButton) {
                inDateSelectionIndex = 5;
            }
        }

        InvoiceStoreFilter invoiceStoreFilter = new InvoiceStoreFilter(
                customerText, showNotDoneOnly, hideDryCleanOnly, dueDateSelectionIndex, inDateSelectionIndex);

        invoiceStoreFilter.save();
    }

    private Predicate<Invoice> getCustomerTextPredicate() {
        String customerInfoSearchText = searchByCustomerTextField.getText();
        return invoice -> customerInfoSearchText == null || customerInfoSearchText.isEmpty() || invoice.getCustomerInfo().containsText(customerInfoSearchText);
    }

    private Predicate<Invoice> getNotDoneOnlyPredicate() {
        boolean notDoneOnly = notDoneOnlyCheckBox.isSelected();
        return invoice -> !notDoneOnly || !invoice.getDone();
    }

    private Predicate<Invoice> getHideDryCleanOnlyPredicate() {
        boolean hideDryCleanOnly = hideDryCleanOnlyCheckBox.isSelected();
        return invoice -> !hideDryCleanOnly || !invoice.isDryCleanOnly();
    }

    private Predicate<Invoice> getDueDatePredicate() {
        final Toggle selectedToggle = dueDateToggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            final LocalDate today = LocalDate.now();
            if (selectedToggle == dueTodayRadioButton) {
                return invoice -> invoice.getDueDate().equals(today);
            } else if (selectedToggle == dueTomorrowRadioButton) {
                LocalDate endDate = today.plusDays(1);
                if (endDate.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                    endDate = endDate.plusDays(1);
                }
                final LocalDate finalEndDate = endDate;
                return invoice -> !(invoice.getDueDate().isBefore(today) || invoice.getDueDate().isAfter(finalEndDate));
            } else if (selectedToggle == due3DaysRadioButton) {
                return invoice -> !(invoice.getDueDate().isBefore(today) || invoice.getDueDate().isAfter(today.plusDays(3)));
            } else if (selectedToggle == due1WeekRadioButton) {
                return invoice -> !(invoice.getDueDate().isBefore(today) || invoice.getDueDate().isAfter(today.plusWeeks(1)));
            }
        }
        return invoice -> true;
    }

    private Predicate<Invoice> getInvoiceDatePredicate() {
        final Toggle selectedToggle = inDateToggleGroup.getSelectedToggle();
        if (selectedToggle != null) {
            final LocalDate today = LocalDate.now();
            if (selectedToggle == inTodayRadioButton) {
                return invoice -> invoice.getInvoiceDate().equals(today);
            } else if (selectedToggle == inYesterdayRadioButton) {
                return invoice -> !(invoice.getInvoiceDate().isBefore(today.minusDays(1)) || invoice.getInvoiceDate().isAfter(today));
            } else if (selectedToggle == in3DaysRadioButton) {
                return invoice -> !(invoice.getInvoiceDate().isBefore(today.minusDays(3)) || invoice.getInvoiceDate().isAfter(today));
            } else if (selectedToggle == in1WeekRadioButton) {
                return invoice -> !(invoice.getInvoiceDate().isBefore(today.minusWeeks(1)) || invoice.getInvoiceDate().isAfter(today));
            } else if (selectedToggle == in1MonthRadioButton) {
                return invoice -> !(invoice.getInvoiceDate().isBefore(today.minusMonths(1)) || invoice.getInvoiceDate().isAfter(today));
            } else if (selectedToggle == in2MonthRadioButton) {
                return invoice -> !(invoice.getInvoiceDate().isBefore(today.minusMonths(2)) || invoice.getInvoiceDate().isAfter(today));
            }
        }
        return invoice -> true;
    }

    public ReadOnlyObjectWrapper<Invoice> selectedInvoiceProperty() {
        return selectedInvoice;
    }

    public void filter() {
        filteredInvoices.setPredicate(getAllPredicates());
        updateFilterLabel();
        updateTotalLabels();
    }

    private void updateTotalLabels() {
        final ObservableList<Invoice> tableItems = invoiceStoreTable.getItems();
        numInvoicesLabel.setText(Integer.toString(tableItems.size()));
        Double total = tableItems.stream().collect(Collectors.summingDouble(Invoice::getTotal));
        invoicesTotalLabel.setText(NumberFormat.getCurrencyInstance().format(total));
    }

    private void updateFilterLabel() {
        String label = "Displaying ";
        if (searchByCustomerTextField.getText().isEmpty()
                && !notDoneOnlyCheckBox.isSelected()
                && !hideDryCleanOnlyCheckBox.isSelected()
                && dueDateToggleGroup.getSelectedToggle() == dueAnyRadioButton
                && inDateToggleGroup.getSelectedToggle() == inAnyRadioButton) {
            label += "all invoices";

        } else {
            label += "invoices that are ";
            List<String> filterLabels = new ArrayList<>();

            RadioButton inRadioButton = (RadioButton) inDateToggleGroup.getSelectedToggle();
            if (inRadioButton != inAnyRadioButton) {
                filterLabels.add("in " + inRadioButton.getText());
            }

            RadioButton dueRadioButton = (RadioButton) dueDateToggleGroup.getSelectedToggle();
            if (dueRadioButton != dueAnyRadioButton) {
                filterLabels.add("due " + dueRadioButton.getText());
            }

            if (notDoneOnlyCheckBox.isSelected()) {
                filterLabels.add("not done");
            }

            if (hideDryCleanOnlyCheckBox.isSelected()) {
                filterLabels.add("not dry clean only");
            }

            if (!searchByCustomerTextField.getText().isEmpty()) {
                filterLabels.add("containing \'" + searchByCustomerTextField.getText().trim() + "\'");
            }

            for (int i = 0; i < filterLabels.size(); i++) {
                String element = filterLabels.get(i);
                if (i == 0) {
                    label += element;
                } else if (i == filterLabels.size() - 1) {
                    label += ", and " + element;
                } else {
                    label += ", " + element;
                }
            }
        }
        filterLabel.setText(label);
    }

    public void clearCustomerTextField() {
        searchByCustomerTextField.clear();
    }

    public void quickFilterDueTomorrow() {
        searchByCustomerTextField.clear();
        notDoneOnlyCheckBox.setSelected(true);
        hideDryCleanOnlyCheckBox.setSelected(true);
        dueDateToggleGroup.selectToggle(dueTomorrowRadioButton);
        inDateToggleGroup.selectToggle(inAnyRadioButton);
        filter();
    }

    public void quickFilterDue3Days() {
        searchByCustomerTextField.clear();
        notDoneOnlyCheckBox.setSelected(true);
        hideDryCleanOnlyCheckBox.setSelected(true);
        dueDateToggleGroup.selectToggle(due3DaysRadioButton);
        inDateToggleGroup.selectToggle(inAnyRadioButton);
        filter();
    }

    public void quickFilterInToday() {
        searchByCustomerTextField.clear();
        notDoneOnlyCheckBox.setSelected(false);
        hideDryCleanOnlyCheckBox.setSelected(false);
        dueDateToggleGroup.selectToggle(dueAnyRadioButton);
        inDateToggleGroup.selectToggle(inTodayRadioButton);
        filter();
    }

    public void quickFilterIn3Days() {
        searchByCustomerTextField.clear();
        notDoneOnlyCheckBox.setSelected(false);
        hideDryCleanOnlyCheckBox.setSelected(false);
        dueDateToggleGroup.selectToggle(dueAnyRadioButton);
        inDateToggleGroup.selectToggle(in1WeekRadioButton);
        filter();
    }

    private class FormattedLocalDateTableCell extends TableCell<Invoice, LocalDate> {
        private final LocalDateConverter formatter = new LocalDateConverter();

        @Override
        protected void updateItem(LocalDate localDate, boolean empty) {
            super.updateItem(localDate, empty);
            if (localDate != null && !empty) {
                setText(formatter.toString(localDate));
            } else {
                setText(null);
                setStyle("");
            }
        }
    }
}
