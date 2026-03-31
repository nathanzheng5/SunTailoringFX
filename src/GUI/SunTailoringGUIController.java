package GUI;

import Data.*;
import Html.Element;
import Html.InvoiceHtml;
import Html.InvoicePrinter;
import Utils.GmailSender;
import Utils.GoogleDriveUploader;
import Utils.PathUtils;
import Utils.PropertiesConfiguration;
import Utils.Utils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.CurrencyStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SunTailoringGUIController implements Initializable {

    @FXML
    public BorderPane rootPane;

    @FXML
    public MenuItem quickJacketsSettingsMenuItem;
    @FXML
    public MenuItem quickPantsSettingsMenuItem;
    @FXML
    public MenuItem quickShirtsSettingsMenuItem;
    @FXML
    public MenuItem quickDryCleansSettingsMenuItem;
    @FXML
    public MenuItem quickDressSettingsMenuItem;
    @FXML
    public MenuItem quickOthersSettingsMenuItem;
    public CheckMenuItem sendEmailWhenDoneCheckMenuItem;
    @FXML
    public CheckMenuItem darkThemeMenuItem;
    public Label toDoTomorrowCount;

    @FXML
    private TextField findInvoiceNumberTextField;
    @FXML
    private Button newInvoiceButton;

    @FXML
    private TextField invoiceNumberTextField;
    @FXML
    private DatePicker invoiceDatePicker;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private CheckBox paidCheckBox;
    @FXML
    private CheckBox doneCheckBox;
    @FXML
    private CheckBox pickedUpCheckBox;

    @FXML
    public TextField customerNameTextField;
    @FXML
    public TextField customerPhoneTextField;
    @FXML
    public TextField customerEmailTextField;

    @FXML
    public Label subtotalLabel;
    @FXML
    public Label taxLabel;
    @FXML
    public Label totalLabel;
    @FXML
    public TextField creditTextField;

    @FXML
    public Button saveInvoiceButton;
    @FXML
    public Button mailInvoiceButton;
    @FXML
    public Button printInvoiceButton;

    @FXML
    public ComboBox<Item> quickJacketComboBox;
    @FXML
    public ComboBox<Item> quickPantComboBox;
    @FXML
    public ComboBox<Item> quickShirtComboBox;
    @FXML
    public ComboBox<Item> quickDryCleanComboBox;
    @FXML
    public ComboBox<Item> quickDressComboBox;
    @FXML
    public ComboBox<Item> quickOtherComboBox;

    @FXML
    public TableView<Item> itemsTable;
    @FXML
    public TableColumn<Item, String> itemsTableNameCol;
    @FXML
    public TableColumn<Item, Integer> itemsTableQuantityCol;
    @FXML
    public TableColumn<Item, Double> itemsTableUnitPriceCol;
    @FXML
    public TableColumn<Item, Double> itemsTablePriceCol;

    private static final PropertiesConfiguration config = PropertiesConfiguration.getInstance();
    private static final InvoiceStore invoiceStore = InvoiceStore.getInstance();
    private static final ExpenseStore expenseStore = ExpenseStore.getInstance();

    private final Invoice activeInvoice;
    private Invoice baselineInvoice;
    private ActiveInvoiceState activeInvoiceState;
    private boolean suspendActiveInvoiceStateUpdate;
    private boolean suspendQuickItemComboBoxAction;
    private final AddressBook addressBook;

    private final Timer summaryTimer;

    private enum ActiveInvoiceState {
        NEW,
        EDITED,
        SAVED
    }

    private void setActiveInvoiceState(ActiveInvoiceState newState) {
        if (!suspendActiveInvoiceStateUpdate) {
            ActiveInvoiceState oldState = activeInvoiceState;
            switch (newState) {
                case NEW:
                case SAVED:
                    activeInvoiceState = newState;
                    break;
                case EDITED:
                    // any edit to a new invoice won't change the state to the EDITED state
                    if (oldState != ActiveInvoiceState.NEW) {
                        if (activeInvoice.equals(baselineInvoice)) {
                            activeInvoiceState = ActiveInvoiceState.SAVED;
                        } else {
                            activeInvoiceState = newState;
                        }
                    }
                    break;
                default:
                    break;
            }

            if (activeInvoiceState != oldState) {
                updateInvoiceNumberTextFieldBackgroundColor();
                saveInvoiceButton.setDisable(activeInvoiceState == ActiveInvoiceState.SAVED);
            }
        }
    }

    private void updateInvoiceNumberTextFieldBackgroundColor() {
        if (invoiceNumberTextField != null) {
            boolean dark = ThemeManager.getInstance().isDarkMode();
            switch (activeInvoiceState) {
                case NEW:
                    invoiceNumberTextField.setStyle("-fx-background-color: " + (dark ? "#2d5a2d" : "lightgreen"));
                    break;
                case EDITED:
                    invoiceNumberTextField.setStyle("-fx-background-color: " + (dark ? "#5a2d2d" : "lightpink"));
                    break;
                case SAVED:
                    // Clear inline style so the active theme CSS takes over
                    invoiceNumberTextField.setStyle("");
                    break;
                default:
                    break;
            }
        }
    }

    public SunTailoringGUIController() {
        activeInvoice = Invoice.createEmptyInvoice(generateInvoiceNumber());
        baselineInvoice = activeInvoice.copy();
        activeInvoiceState = ActiveInvoiceState.NEW;
        updateInvoiceNumberTextFieldBackgroundColor();

        this.addressBook = AddressBook.load();

        summaryTimer = new Timer();
        summaryTimer.schedule(new TimerTask() {
            private Summary oldSummary = null;
            @Override
            public void run() {
                final Summary summary = new Summary(invoiceStore);
                if (summary.changedFrom(oldSummary)) {
                    try {
                        GmailSender.DEFAULT.sendMail(getSummaryRecipient(), "Sun Tailoring Summary", summary.toHtml());
                    } catch (Exception ignore) {}
                }
                oldSummary = summary;
            }
        }, 60*60*1000, 60*60*1000);
    }

    public void sendSummary() {
        final Summary summary = new Summary(invoiceStore);
        try {
            GmailSender.DEFAULT.sendMail(getSummaryRecipient(), "Sun Tailoring Summary", summary.toHtml());
        } catch (Exception ignore) {}
    }

    private String getSummaryRecipient() {
        return config.getProperty("default.summary.recipient", "nathanzheng87@gmail.com");
    }

    public void stopSummaryTimer() {
        if (summaryTimer != null) {
            summaryTimer.cancel();
            summaryTimer.purge();
        }
    }

    public void openInvoice() {
        String invoiceNumber = findInvoiceNumberTextField.getText();
        final Invoice invoice = invoiceStore.get(invoiceNumber);
        if (invoice != null) {
            setActiveInvoice(invoice, ActiveInvoiceState.SAVED);
        } else {
            GuiUtils.showInfoAlertAndWait(invoiceNumber + " does not exist");
        }
    }

    public void invoiceNumberTextFieldClicked() {
//        findInvoiceNumberTextField.setText("");
    }

    public void createNewInvoice() {
        String invoiceNumber = generateInvoiceNumber();
        setActiveInvoice(Invoice.createEmptyInvoice(invoiceNumber), ActiveInvoiceState.NEW);
        clearQuickItemComboBoxSelection();
    }

    private void clearQuickItemComboBoxSelection() {
        quickJacketComboBox.getSelectionModel().clearSelection();
        quickPantComboBox.getSelectionModel().clearSelection();
        quickShirtComboBox.getSelectionModel().clearSelection();
        quickDressComboBox.getSelectionModel().clearSelection();
        quickDryCleanComboBox.getSelectionModel().clearSelection();
        quickOtherComboBox.getSelectionModel().clearSelection();
    }

    private String generateInvoiceNumber() {
        final LocalDate now = LocalDate.now();
        final String base = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
        int invoiceNum = 0;
        String invoiceNumber = base + String.format("%02d", invoiceNum);
        while (invoiceStore.contains(invoiceNumber)) {
            invoiceNum++;
            if (invoiceNum > 99) {
                return "InvalidInvoiceNumber";
            }
            invoiceNumber = base + String.format("%02d", invoiceNum);
        }
        return invoiceNumber;
    }

    public boolean saveActiveInvoice() {
        boolean doSave = true;
        if (activeInvoiceState == ActiveInvoiceState.EDITED) {
            doSave = GuiUtils.showConfirmationAlertAndWait("Are you sure that you want to modify existing invoice "
                    + activeInvoice.getInvoiceNumber() + "?");

        }
        if (doSave) {
            boolean wasDone = baselineInvoice.getDone();

            invoiceStore.save(activeInvoice);
            baselineInvoice = activeInvoice.copy();
            setActiveInvoiceState(ActiveInvoiceState.SAVED);
            addressBook.add(activeInvoice.getCustomerInfo().copy());

            // update the to do tomorrow label - todo: it doesn't need to be done every time you save an invoice, but it's OK because it will be in a background thread
            new Thread(createUpdateToDoTmrTask()).start();

            // send email when invoice is marked DONE, if the settings allows so
            String sentMail = "";
            if (getSendEmailWhenMarkedDone() && !wasDone && activeInvoice.getDone()) {
                String email = activeInvoice.getCustomerInfo().getEmail();
                if (!email.isEmpty()) {
                    String subject = "Your Sun Tailoring Order " + activeInvoice.getInvoiceNumber() + " is ready for pick up";
                    String message = "Dear " + activeInvoice.getCustomerInfo().getName() + ",<br><br>" +
                            "Your order has been completed and ready for pick up! Please see below for your invoice." +
                            "<br><br>Thank you for choosing Sun Tailoring!" +
                            "<br><br>Nathan,<br>Sun Tailoring<br><br>";
                    String body = composeEmailContent(message, activeInvoice);

                    new Thread(createMailSendTask(email, subject, body)).start();
                    sentMail = email;
                }
            }

            String info = "Saved Invoice " + activeInvoice.getInvoiceNumber();
            if (!sentMail.isEmpty()) {
                info += ". Sending email to " + sentMail + "...";
            }
            GuiUtils.showInfoAlertAndWait(info);
        } else {
            System.out.println("Cancelled saving " + activeInvoice.getInvoiceNumber());
        }
        return doSave;
    }

    private Task<Integer> createUpdateToDoTmrTask() {
        return new Task<>() {
            @Override
            protected Integer call() {
                return (int) invoiceStore.all().stream()
                        .filter(invoice -> !invoice.isDryCleanOnly() && !invoice.getDone())
                        .filter(invoice -> Utils.getNextBusinessDayDateRange(LocalDate.now())
                                .contains(invoice.getDueDate()))
                        .count();
            }

            @Override
            protected void succeeded() {
                toDoTomorrowCount.setText(Integer.toString(getValue()));
            }
        };
    }

    private Task<Void> createMailSendTask(String email, String subject, String body) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                GmailSender.DEFAULT.sendMail(email,
                        Collections.emptyList(),
                        subject,
                        body,
                        "");
                return null;
            }

            @Override
            protected void succeeded() {
                GuiUtils.showInfoAlertAndWait("Successfully sent email.");
            }

            @Override
            protected void failed() {
                GuiUtils.showWarningAlertAndWait("Failed sending email to " + email +
                        ". You can turn off automatic email sending in the Settings.");
            }
        };
    }

    private <T> void setupPromptTextButtonCell(ComboBox<T> comboBox) {
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? comboBox.getPromptText() : item.toString());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void quickItemComboBoxOnAction(ActionEvent actionEvent) {
        if (!suspendQuickItemComboBoxAction) {
            final Object source = actionEvent.getSource();
            if (source == quickJacketComboBox || source == quickPantComboBox || source == quickShirtComboBox
                    || source == quickDryCleanComboBox || source == quickDressComboBox || source == quickOtherComboBox) {
                final ComboBox<Item> comboBox = (ComboBox<Item>) source;
                final Item selectedItem = comboBox.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    activeInvoice.getItems().add(selectedItem.copy());
                    setActiveInvoiceState(ActiveInvoiceState.EDITED);
                    Platform.runLater(() -> comboBox.setValue(null));
                }
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemsTableNameCol.setMaxWidth(1f * Integer.MAX_VALUE * 55);
        itemsTableQuantityCol.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        itemsTableUnitPriceCol.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        itemsTablePriceCol.setMaxWidth(1f * Integer.MAX_VALUE * 15);

        setupPromptTextButtonCell(quickJacketComboBox);
        setupPromptTextButtonCell(quickPantComboBox);
        setupPromptTextButtonCell(quickShirtComboBox);
        setupPromptTextButtonCell(quickDressComboBox);
        setupPromptTextButtonCell(quickDryCleanComboBox);
        setupPromptTextButtonCell(quickOtherComboBox);

        updateInvoiceNumberTextFieldBackgroundColor();

        rootPane.setOnKeyPressed(keyEvent -> {
            if (Shortcut.CTRL_S.getKeyCombo().match(keyEvent)) {
                saveActiveInvoice();
            } else if (Shortcut.CTRL_F.getKeyCombo().match(keyEvent)) {
                showInvoiceStoreDialog();
            } else if (Shortcut.CTRL_N.getKeyCombo().match(keyEvent)) {
                createNewInvoice();
            } else if (Shortcut.CTRL_P.getKeyCombo().match(keyEvent)) {
                printActiveInvoice();
            } else if (Shortcut.F1.getKeyCombo().match(keyEvent)) {
                showHelpDialog();
            } else if (Shortcut.CTRL_X.getKeyCombo().match(keyEvent)) {
                showExpenseStoreDialog();
            } else if (Shortcut.CTRL_SHIFT_F.getKeyCombo().match(keyEvent)) {
                showCalendarDialog();
            } else if (Shortcut.CTRL_O.getKeyCombo().match(keyEvent)) {
                markActiveInvoiceCompleted();
            }
        });

        invoiceNumberTextField.textProperty().bindBidirectional(activeInvoice.invoiceNumberProperty());
        invoiceDatePicker.valueProperty().bindBidirectional(activeInvoice.invoiceDateProperty());
        invoiceDatePicker.setConverter(new LocalDateConverter());
        invoiceDatePicker.setOnAction(e -> setActiveInvoiceState(ActiveInvoiceState.EDITED));
        dueDatePicker.valueProperty().bindBidirectional(activeInvoice.dueDateProperty());
        dueDatePicker.setConverter(new LocalDateConverter());
        dueDatePicker.setOnAction(e -> setActiveInvoiceState(ActiveInvoiceState.EDITED));

        doneCheckBox.selectedProperty().bindBidirectional(activeInvoice.doneProperty());
        doneCheckBox.setOnAction(e -> setActiveInvoiceState(ActiveInvoiceState.EDITED));
        paidCheckBox.selectedProperty().bindBidirectional(activeInvoice.paidProperty());
        paidCheckBox.setOnAction(e -> setActiveInvoiceState(ActiveInvoiceState.EDITED));
        pickedUpCheckBox.selectedProperty().bindBidirectional(activeInvoice.pickedUpProperty());
        pickedUpCheckBox.setOnAction(e -> setActiveInvoiceState(ActiveInvoiceState.EDITED));

        CustomerInfo customerInfo = activeInvoice.getCustomerInfo();
        customerNameTextField.textProperty().bindBidirectional(customerInfo.nameProperty());
        customerPhoneTextField.textProperty().bindBidirectional(customerInfo.phoneProperty());
        customerEmailTextField.textProperty().bindBidirectional(customerInfo.emailProperty());
        customerInfo.nameProperty().addListener((observable, oldValue, newValue) -> setActiveInvoiceState(ActiveInvoiceState.EDITED));
        customerInfo.phoneProperty().addListener((observable, oldValue, newValue) -> setActiveInvoiceState(ActiveInvoiceState.EDITED));
        customerInfo.emailProperty().addListener((observable, oldValue, newValue) -> setActiveInvoiceState(ActiveInvoiceState.EDITED));

        // use a formatter to commit on Enter / loss of focus
        TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(), 0);
        creditTextField.setTextFormatter(formatter);
        formatter.valueProperty().bindBidirectional(activeInvoice.creditProperty());
        activeInvoice.creditProperty().addListener(event -> setActiveInvoiceState(ActiveInvoiceState.EDITED));

        TextFormatter<Number> currencyFormatter = new TextFormatter<>(new CurrencyStringConverter());

        subtotalLabel.textProperty().bindBidirectional(activeInvoice.subtotalProperty(), new CurrencyStringConverter());
        taxLabel.textProperty().bindBidirectional(activeInvoice.taxProperty(), new CurrencyStringConverter());
        totalLabel.textProperty().bindBidirectional(activeInvoice.totalProperty(), new CurrencyStringConverter());

        itemsTable.setItems(activeInvoice.getItems());
        itemsTableNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        itemsTableQuantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        itemsTableUnitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        itemsTablePriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        // table edit
        itemsTableNameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        itemsTableNameCol.setOnEditCommit(event -> {
            Item selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            selectedItem.setName(event.getNewValue());
            setActiveInvoiceState(ActiveInvoiceState.EDITED);
        });
        itemsTableQuantityCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        itemsTableQuantityCol.setOnEditCommit(event -> {
            Item selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            selectedItem.setQuantity(event.getNewValue());
            setActiveInvoiceState(ActiveInvoiceState.EDITED);
        });
        itemsTableUnitPriceCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        itemsTableUnitPriceCol.setOnEditCommit(event -> {
            Item selectedItem = itemsTable.getSelectionModel().getSelectedItem();
            selectedItem.setUnitPrice(event.getNewValue());
            setActiveInvoiceState(ActiveInvoiceState.EDITED);
        });

        itemsTable.setOnKeyPressed(event -> {
            final int selectedIndex = itemsTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < activeInvoice.getItems().size()) {
                if (event.getCode().equals(KeyCode.DELETE)) {
                    activeInvoice.getItems().remove(selectedIndex);
                    setActiveInvoiceState(ActiveInvoiceState.EDITED);
                } else if (Shortcut.CTRL_D.getKeyCombo().match(event)) {
                    Item item = activeInvoice.getItems().get(selectedIndex);
                    activeInvoice.getItems().add(selectedIndex + 1, item.copy());
                    setActiveInvoiceState(ActiveInvoiceState.EDITED);
                }
            }
        });

        quickJacketComboBox.setItems(loadQuickItems("Jacket").getItems());
        quickPantComboBox.setItems(loadQuickItems("Pant").getItems());
        quickShirtComboBox.setItems(loadQuickItems("Shirt").getItems());
        quickDryCleanComboBox.setItems(loadQuickItems("Dry Clean").getItems());
        quickDressComboBox.setItems(loadQuickItems("Dress").getItems());
        quickOtherComboBox.setItems(loadQuickItems("Other").getItems());

        newInvoiceButton.setTooltip(new Tooltip("Ctrl + N"));
        saveInvoiceButton.setTooltip(new Tooltip("Ctrl + S"));
        printInvoiceButton.setTooltip(new Tooltip("Ctrl + P"));

        sendEmailWhenDoneCheckMenuItem.setSelected(getSendEmailWhenMarkedDone());

        new Thread(createUpdateToDoTmrTask()).start();
    }

    private void markActiveInvoiceCompleted() {
        paidCheckBox.setSelected(true);
        doneCheckBox.setSelected(true);
        pickedUpCheckBox.setSelected(true);
        setActiveInvoiceState(ActiveInvoiceState.EDITED);
    }

    public void showAddressBookDialog(Event event) {
        event.consume();
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AddressBookDialog.fxml"));
            final Parent root = fxmlLoader.load();
            final AddressBookDialogController addressBookDialogController = fxmlLoader.getController();
            addressBookDialogController.setAddressBook(addressBook);
            addressBookDialogController.selectedCustomerInfoProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    activeInvoice.getCustomerInfo().setFrom(newValue);
                    setActiveInvoiceState(ActiveInvoiceState.EDITED);
                }
            });
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Address Book");
            stage.getIcons().add(Assets.ADDRESS_BOOK_ICON);
            stage.setScene(createScene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            GuiUtils.showWarningAlertAndWait("Failed loading address book dialog");
        }
    }

    public void saveAddressBook() {
        addressBook.save();
    }

    public void showQuickItemsSettingsDialog(ActionEvent actionEvent) {
        final Object source = actionEvent.getSource();
        String quickItemsName = "";
        ComboBox<Item> quickItemComboBox = null;
        if (source == quickJacketsSettingsMenuItem) {
            quickItemsName = "Jacket";
            quickItemComboBox = quickJacketComboBox;

        } else if (source == quickPantsSettingsMenuItem) {
            quickItemsName = "Pant";
            quickItemComboBox = quickPantComboBox;

        } else if (source == quickShirtsSettingsMenuItem) {
            quickItemsName = "Shirt";
            quickItemComboBox = quickShirtComboBox;

        } else if (source == quickDryCleansSettingsMenuItem) {
            quickItemsName = "Dry Clean";
            quickItemComboBox = quickDryCleanComboBox;

        } else if (source == quickDressSettingsMenuItem) {
            quickItemsName = "Dress";
            quickItemComboBox = quickDressComboBox;

        } else if (source == quickOthersSettingsMenuItem) {
            quickItemsName = "Other";
            quickItemComboBox = quickOtherComboBox;

        } else {
            assert false;
        }
        QuickItems quickItems = loadQuickItems(quickItemsName);
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("QuickItemsSettingsDialog.fxml"));
            final Parent root = fxmlLoader.load();
            final QuickItemsSettingsDialogController quickItemsSettingsDialogController = fxmlLoader.getController();
            quickItemsSettingsDialogController.setQuickItems(quickItemsName, quickItems);

            Stage stage = new Stage();
            final ComboBox<Item> finalQuickItemComboBox = quickItemComboBox;
            stage.setOnCloseRequest(event -> {
                final ObservableList<Item> items = quickItemsSettingsDialogController.getQuickItems().getItems();
                suspendQuickItemComboBoxAction = true;
                finalQuickItemComboBox.setItems(items);
                suspendQuickItemComboBoxAction = false;
            });
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Configure Quick " + quickItemsName);
            stage.getIcons().add(Assets.SETTINGS_ICON);
            stage.setScene(createScene(root));
            stage.show();

        } catch (Exception e) {
            GuiUtils.showWarningAlertAndWait("Failed loading quick items dialog");
        }
    }

    private QuickItems loadQuickItems(String name) {
        return QuickItems.load(name);
    }

    public void showInvoiceStoreDialog() {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InvoiceStoreDialog.fxml"));
            final Parent root = fxmlLoader.load();
            final InvoiceStoreDialogController controller = fxmlLoader.getController();

            InvoiceStoreFilter invoiceStoreFilter = InvoiceStoreFilter.load();

            controller.setInvoiceStore(invoiceStore, invoiceStoreFilter);
            controller.selectedInvoiceProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    setActiveInvoice(newValue, ActiveInvoiceState.SAVED);
                }
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Invoice Store");
            stage.getIcons().add(Assets.STORE_ICON);
            stage.setScene(createScene(root));
            stage.show();
            stage.setOnCloseRequest(e -> controller.saveFilters());

        } catch (Exception e) {
            GuiUtils.showWarningAlertAndWait("Failed loading invoice store dialog");
        }
    }

    public void showExpenseStoreDialog() {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ExpenseStoreDialog.fxml"));
            final Parent root = fxmlLoader.load();
            final ExpenseStoreDialogController controller = fxmlLoader.getController();
            controller.setExpenseStore(expenseStore);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Expense Store");
            stage.getIcons().add(Assets.STORE_ICON);
            stage.setScene(createScene(root));
            stage.show();

        } catch (Exception e) {
            GuiUtils.showWarningAlertAndWait("Failed loading expense store dialog");
        }
    }

    public void showCalendarDialog() {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CalendarDialog.fxml"));
            final Parent root = fxmlLoader.load();
            final CalendarDialogController controller = fxmlLoader.getController();
            controller.setInvoiceStore(invoiceStore);

            controller.selectedInvoiceNumberProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    setActiveInvoice(invoiceStore.get(newValue), ActiveInvoiceState.SAVED);
                }
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Calendar");
            stage.getIcons().add(Assets.CALENDAR_ICON);
            stage.setScene(createScene(root));
            stage.show();

        } catch (Exception e) {
            GuiUtils.showWarningAlertAndWait("Failed loading calendar dialog");
        }
    }

    public void showHelpDialog() {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("HelpDialog.fxml"));
            final Parent root = fxmlLoader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Help");
            stage.getIcons().add(Assets.HELP_ICON);
            stage.setScene(createScene(root));
            stage.show();

        } catch (Exception e) {
            GuiUtils.showWarningAlertAndWait("Failed loading help dialog");
        }
    }

    public void showMailDialog() {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MailDialog.fxml"));
            final Parent root = fxmlLoader.load();
            final MailDialogController controller = fxmlLoader.getController();
            controller.setAddressBook(addressBook);

            final String email = activeInvoice.getCustomerInfo().getEmail();
            if (!email.isEmpty()) {
                controller.setTo(email);
            }
            final String cc = PropertiesConfiguration.getInstance().getProperty("invoice.maker.default.mail.cc", "");
            if (!cc.isEmpty()) {
                controller.setCC(cc);
            }
            final String subject = String.format(config.getProperty("invoice.maker.default.mail.subject", "Invoice #%s"), activeInvoice.getInvoiceNumber());
            controller.setSubject(subject);

            String message = "Dear " + activeInvoice.getCustomerInfo().getName() + ",<br><br>" +
                    "Thank you for choosing Sun Tailoring. Please see below for your invoice. You may reply directly or give us a call if you have any question.<br><br>" +
                    "Nathan,<br>Sun Tailoring<br><br>";
            controller.setBodyHtml(composeEmailContent(message, activeInvoice));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Mail");
            stage.getIcons().add(Assets.EMAIL_ICON);
            stage.setScene(createScene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            GuiUtils.showWarningAlertAndWait("Failed loading mail dialog");
        }
    }

    private static String composeEmailContent(String message, Invoice invoice) {
        Element html = new Element("html");
        InvoiceHtml.buildHead(html);
        InvoiceHtml.buildBody(html, invoice, message);
        return html.print();
    }

    public void showStatsDialog() {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatsDialog.fxml"));
            final Parent root = fxmlLoader.load();
            final StatsDialogController controller = fxmlLoader.getController();
            controller.setInvoiceStore(invoiceStore, expenseStore);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Invoice Store Statistics");
            stage.getIcons().add(Assets.STATS_ICON);
            stage.setScene(createScene(root));
            stage.show();

        } catch (Exception e) {
            GuiUtils.showWarningAlertAndWait("Failed loading invoice store dialog");
        }
    }

    @FXML
    public void toggleDarkTheme() {
        ThemeManager.getInstance().setDarkMode(darkThemeMenuItem.isSelected());
        updateInvoiceNumberTextFieldBackgroundColor();
    }

    private Scene createScene(Parent root) {
        Scene scene = new Scene(root);
        ThemeManager.getInstance().registerScene(scene);
        return scene;
    }

    private void setActiveInvoice(Invoice target, ActiveInvoiceState state) {
        suspendActiveInvoiceStateUpdate = true; // prevent unnecessary state updates
        activeInvoice.cloneFrom(target);
        baselineInvoice = activeInvoice.copy();
        suspendActiveInvoiceStateUpdate = false;
        setActiveInvoiceState(state);
    }

    public void printActiveInvoice() {
        // save active invoice first
        boolean activeInvoiceSaved = activeInvoiceState == ActiveInvoiceState.SAVED || saveActiveInvoice();

        if (activeInvoiceSaved) {
            final InvoicePrinter invoicePrinter = new InvoicePrinter(activeInvoice, config);
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(invoicePrinter);

            PrintRequestAttributeSet set = new HashPrintRequestAttributeSet();
            set.add(OrientationRequested.PORTRAIT);
            set.add(MediaSizeName.INVOICE);
            try {
                job.print(set);
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSendEmailWhenMarkedDone() {
        boolean send = sendEmailWhenDoneCheckMenuItem.isSelected();
        config.setProperty("mail.send_when_invoice_marked_done", Boolean.toString(send));
        config.save();
    }

    public boolean getSendEmailWhenMarkedDone() {
        return Boolean.parseBoolean(config.getProperty("mail.send_when_invoice_marked_done", "false"));
    }

    @FXML
    public void uploadDatabaseToGoogleDrive() {
        String clientId = config.getProperty("google.drive.client_id", "");
        String clientSecret = config.getProperty("google.drive.client_secret", "");

        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            TextInputDialog idDialog = new TextInputDialog();
            idDialog.setTitle("Google Drive Setup");
            idDialog.setHeaderText(
                    "Enter your Google OAuth2 Client ID\n\n" +
                    "To get credentials:\n" +
                    "  1. Go to console.cloud.google.com\n" +
                    "  2. Create a project and enable the Google Drive API\n" +
                    "  3. Go to Credentials \u2192 Create OAuth 2.0 Client ID (Desktop App type)\n" +
                    "  4. Copy the Client ID below:");
            idDialog.setContentText("Client ID:");
            java.util.Optional<String> idResult = idDialog.showAndWait();
            if (!idResult.isPresent() || idResult.get().trim().isEmpty()) return;
            clientId = idResult.get().trim();

            TextInputDialog secretDialog = new TextInputDialog();
            secretDialog.setTitle("Google Drive Setup");
            secretDialog.setHeaderText("Enter the Client Secret for your OAuth2 credential:");
            secretDialog.setContentText("Client Secret:");
            java.util.Optional<String> secretResult = secretDialog.showAndWait();
            if (!secretResult.isPresent() || secretResult.get().trim().isEmpty()) return;
            clientSecret = secretResult.get().trim();

            config.setProperty("google.drive.client_id", clientId);
            config.setProperty("google.drive.client_secret", clientSecret);
            config.save();
        }

        String refreshToken = config.getProperty("google.drive.refresh_token", "");
        boolean needsAuth = refreshToken.isEmpty();

        if (needsAuth && !GuiUtils.showConfirmationAlertAndWait(
                "Your browser will open for Google sign-in to authorize\n" +
                "access to Google Drive. Click OK to continue.")) {
            return;
        }

        final String fClientId = clientId;
        final String fClientSecret = clientSecret;
        final String fRefreshToken = refreshToken;

        Task<Void> uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                GoogleDriveUploader uploader = new GoogleDriveUploader(fClientId, fClientSecret, fRefreshToken);
                if (!uploader.isAuthorized()) {
                    String newToken = uploader.authorize();
                    config.setProperty("google.drive.refresh_token", newToken);
                    config.save();
                }
                uploader.uploadFile(new File(PathUtils.DB_PATH));
                return null;
            }
        };

        uploadTask.setOnSucceeded(e -> GuiUtils.showInfoAlertAndWait("Database backed up to Google Drive successfully."));
        uploadTask.setOnFailed(e -> GuiUtils.showWarningAlertAndWait("Upload failed: " + uploadTask.getException().getMessage()));

        Thread t = new Thread(uploadTask);
        t.setDaemon(true);
        t.start();
    }

}
