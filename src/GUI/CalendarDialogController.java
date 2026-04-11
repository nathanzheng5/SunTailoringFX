package GUI;

import Data.Invoice;
import Data.InvoiceStore;
import Data.Item;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.converter.CurrencyStringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class CalendarDialogController implements Initializable {

    public GridPane calendarGrid;
    public HBox weekdayHeader;
    public ScrollPane scrollPane;
    public Button lastMonthButton;
    public Button nextMonthButton;
    public Label currentMonthLabel;
    public VBox root;
    public RadioButton dueDateRadio;
    public RadioButton inDateRadio;

    private LocalDate selectedDate;
    private boolean viewByDueDate = true;

    private InvoiceStore invoiceStore;
    private ReadOnlyObjectWrapper<String> selectedInvoiceNumber;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
                final Stage stage = (Stage) root.getScene().getWindow();
                stage.close();
            }
        });

        initializeCalendarGrid();
        initializeWeekdayHeader();
        selectedInvoiceNumber = new ReadOnlyObjectWrapper<>();

        ToggleGroup viewToggle = new ToggleGroup();
        dueDateRadio.setToggleGroup(viewToggle);
        inDateRadio.setToggleGroup(viewToggle);
    }

    public void setInvoiceStore(InvoiceStore invoiceStore) {
        this.invoiceStore = invoiceStore;
        selectedDate = LocalDate.now().withDayOfMonth(15);  // set to 15th - every month has 15th
        generateCalendar();
    }

    private void initializeCalendarGrid() {
        int numRows = 6;
        int numCols = 7;
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                VBox vPane = new VBox();
                vPane.getStyleClass().add("calendar_pane");
                vPane.setMinWidth(weekdayHeader.getPrefWidth() / 7);

                GridPane.setVgrow(vPane, Priority.ALWAYS);

                calendarGrid.add(vPane, col, row);
            }
        }

        // set up row constraints
        for (int i = 0; i < 7; i++) {
            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setMinHeight(scrollPane.getHeight() / 7);
            calendarGrid.getRowConstraints().add(rowConstraints);
        }
    }

    private void initializeWeekdayHeader() {
        int weekdays = 7;
        String[] weekAbbr = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (int i = 0; i < weekdays; i++) {
            StackPane pane = new StackPane();
            pane.getStyleClass().add("weekday-header");

            // Make panes take up equal space
            HBox.setHgrow(pane, Priority.ALWAYS);
            pane.setMaxWidth(Double.MAX_VALUE);

            // Note: After adding a label to this, it tries to resize itself..
            // So I'm setting a minimum width.
            pane.setMinWidth(weekdayHeader.getPrefWidth() / 7);

            weekdayHeader.getChildren().add(pane);

            pane.getChildren().add(new Label(weekAbbr[i]));
        }
    }

    private void generateCalendar() {
        currentMonthLabel.setText(selectedDate.getMonth().toString() + " " + selectedDate.getYear());
        loadCalendarLabels();
        populateMonthWithInvoices();
    }

    private void loadCalendarLabels() {
        LocalDate firstDayOfMonthDate = selectedDate.withDayOfMonth(1);

        int firstDay = firstDayOfMonthDate.getDayOfWeek().getValue() + 1;
        int daysInMonth = selectedDate.getMonth().length(selectedDate.isLeapYear());
        int offset = firstDay;
        int gridCount = 1;
        int dayLabelCount = 1;

        for (Node node : calendarGrid.getChildren()) {
            VBox dayBox = (VBox) node;
            dayBox.getChildren().clear();
            dayBox.getStyleClass().removeAll("today_box");
            dayBox.setStyle("-fx-background-color: white");
            dayBox.setStyle("-fx-font: 14px");
            dayBox.setStyle("-fx-padding: 3px");
            dayBox.setSpacing(2.0);

            if (gridCount < offset) {
                gridCount++;
                // darken days before the first day of the month
                dayBox.getStyleClass().add("inactive_day_box");
            } else {
                if (dayLabelCount > daysInMonth) {
                    // darken days after the last day of the month
                    dayBox.getStyleClass().add("inactive_day_box");

                } else {
                    LocalDate now = LocalDate.now();
                    if (dayLabelCount == now.getDayOfMonth()
                            && selectedDate.getMonth().getValue() == now.getMonth().getValue()
                            && selectedDate.getYear() == now.getYear()) {
                        dayBox.getStyleClass().add("today_box");
                    }

                    // make a new day label
                    Label dayLabel = new Label(Integer.toString(dayLabelCount));
                    dayLabel.setPadding(new Insets(5));
                    String dayNumberColor = ThemeManager.getInstance().isDarkMode() ? "#e0e0e0" : "darkslategray";
                    dayLabel.setStyle("-fx-text-fill: " + dayNumberColor + ";");

                    dayBox.getChildren().add(dayLabel);
                }

                dayLabelCount++;
            }
        }
    }

    public void onViewModeChanged(ActionEvent event) {
        viewByDueDate = dueDateRadio.isSelected();
        generateCalendar();
    }

    private void populateMonthWithInvoices() {
        LocalDate start = selectedDate.withDayOfMonth(1);
        LocalDate end = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth());

        Map<Integer, List<Invoice>> map = new HashMap<>();

        invoiceStore.all().stream()
                .filter(invoice -> {
                    LocalDate date = viewByDueDate ? invoice.getDueDate() : invoice.getInvoiceDate();
                    return !(date.isBefore(start) || date.isAfter(end));
                })
                .forEach(invoice -> {
                    LocalDate date = viewByDueDate ? invoice.getDueDate() : invoice.getInvoiceDate();
                    int dayOfMonth = date.getDayOfMonth();
                    if (map.get(dayOfMonth) == null) {
                        map.put(dayOfMonth, new ArrayList<>());
                    }
                    map.get(dayOfMonth).add(invoice);
                });

        calendarGrid.getChildren().stream().map(node -> ((VBox) node))
                .filter(dayBox -> !dayBox.getChildren().isEmpty())
                .forEach(dayBox -> {
                    Label dayLabel = (Label) dayBox.getChildren().get(0);
                    int dayNumber = Integer.parseInt(dayLabel.getText());
                    List<Invoice> invoicesOfThisDay = map.get(dayNumber);
                    if (invoicesOfThisDay != null) {
                        invoicesOfThisDay.stream().forEach(invoice -> {
                            Label invoiceLabel = new Label(invoice.getInvoiceNumber());
                            invoiceLabel.getStyleClass().add(invoice.getDone() ? "invoice_label" :
                                    invoice.isDryCleanOnly() ? "invoice_label_dry_clean_only" : "invoice_label_not_done");
                            invoiceLabel.setMaxWidth(Double.MAX_VALUE);

                            // double click on the label will close this window and load the selected invoice in main window
                            invoiceLabel.setOnMouseClicked(event -> {
                                if (event.getClickCount() == 2) {
                                    selectedInvoiceNumber.setValue(invoiceLabel.getText());
                                    final Stage stage = (Stage) invoiceLabel.getScene().getWindow();
                                    stage.close();
                                }
                            });

                            Tooltip tooltip = new Tooltip(buildInvoiceTooltip(invoice));
                            tooltip.setStyle("-fx-font-size: 20");
                            invoiceLabel.setTooltip(tooltip);

                            // todo: mouse enter to display invoice summary. mouse exit to dismiss.
//                            invoiceLabel.setOnMouseEntered(event -> System.out.println("mouse entered"));
//                            invoiceLabel.setOnMouseExited(event -> System.out.println("mouse exited"));

                            dayBox.getChildren().add(invoiceLabel);
                        });
                    }
                });

    }

    public ReadOnlyObjectWrapper<String> selectedInvoiceNumberProperty() {
        return selectedInvoiceNumber;
    }

    private static String buildInvoiceTooltip(Invoice invoice) {
        String retVal = invoice.getCustomerInfo().getName();
        retVal += "\nIn: " + invoice.getInvoiceDate() + ", Due: " + invoice.getDueDate();
        retVal += "\nTotal: " + new CurrencyStringConverter().toString(invoice.getTotal());
        retVal += "\n" + (invoice.getPaid() ? "Paid" : "Not Paid")
                + ", " + (invoice.getDone() ? "Done" : "Not Done")
                + ", " + (invoice.getPickedUp() ? "Picked Up" : "Not Picked Up");
        for (Item item : invoice.getItems()) {
            retVal += "\n" + item.shortSummary();
        }
        return retVal;
    }

    public void lastMonth(ActionEvent event) {
        event.consume();
        selectedDate = selectedDate.minusMonths(1);
        generateCalendar();
    }

    public void nextMonth(ActionEvent event) {
        event.consume();
        selectedDate = selectedDate.plusMonths(1);
        generateCalendar();
    }
}
