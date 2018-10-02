package Data;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO: CLASS JAVA DOC HERE
 */
public class Invoice {

    private static final double SERVICE_TAX_RATE = 0.05;

    private StringProperty invoiceNumber;
    private LongProperty invoiceDate;
    private LongProperty dueDate;
    private StringProperty customerInfo;

    private ListProperty<Item> items;

    private DoubleProperty credit;

    private BooleanProperty paid;
    private BooleanProperty done;
    private BooleanProperty pickedUp;

    // derived properties
    private DoubleProperty subtotal;
    private DoubleProperty tax;
    private DoubleProperty total;

    public Invoice(String invoiceNumber, long invoiceDate, long dueDate, String customerInfo,
                   List<Item> items,
                   double credit, boolean paid, boolean done, boolean pickedUp) {
        this.invoiceNumber = new SimpleStringProperty(invoiceNumber);
        this.invoiceDate = new SimpleLongProperty(invoiceDate);
        this.dueDate = new SimpleLongProperty(dueDate);
        this.customerInfo = new SimpleStringProperty(customerInfo);

        this.items = new SimpleListProperty<>(FXCollections.observableArrayList(items));

        this.credit = new SimpleDoubleProperty(credit);

        this.paid = new SimpleBooleanProperty(paid);
        this.done = new SimpleBooleanProperty(done);
        this.pickedUp = new SimpleBooleanProperty(pickedUp);

        subtotal = new SimpleDoubleProperty(0);
        tax = new SimpleDoubleProperty(0);
        total = new SimpleDoubleProperty(0);

        subtotal.bind(Bindings.createDoubleBinding(() ->
                this.items.stream().collect(Collectors.summingDouble(Item::getPrice)),
                this.items));
        tax.bind(Bindings.multiply(subtotal, SERVICE_TAX_RATE));
        total.bind(Bindings.add(subtotal, tax).subtract(this.credit));
    }

    public String getInvoiceNumber() {
        return invoiceNumber.get();
    }

    public StringProperty invoiceNumberProperty() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber.set(invoiceNumber);
    }

    public long getInvoiceDate() {
        return invoiceDate.get();
    }

    public LongProperty invoiceDateProperty() {
        return invoiceDate;
    }

    public void setInvoiceDate(long invoiceDate) {
        this.invoiceDate.set(invoiceDate);
    }

    public long getDueDate() {
        return dueDate.get();
    }

    public LongProperty dueDateProperty() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate.set(dueDate);
    }

    public String getCustomerInfo() {
        return customerInfo.get();
    }

    public StringProperty customerInfoProperty() {
        return customerInfo;
    }

    public void setCustomerInfo(String customerInfo) {
        this.customerInfo.set(customerInfo);
    }

    public ObservableList<Item> getItems() {
        return items.get();
    }

    public ListProperty<Item> itemsProperty() {
        return items;
    }

    public double getCredit() {
        return credit.get();
    }

    public DoubleProperty creditProperty() {
        return credit;
    }

    public void setCredit(double credit) {
        this.credit.set(credit);
    }

    public boolean getPaid() {
        return paid.get();
    }

    public BooleanProperty paidProperty() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid.set(paid);
    }

    public boolean getDone() {
        return done.get();
    }

    public BooleanProperty doneProperty() {
        return done;
    }

    public void setDone(boolean done) {
        this.done.set(done);
    }

    public boolean getPickedUp() {
        return pickedUp.get();
    }

    public BooleanProperty pickedUpProperty() {
        return pickedUp;
    }

    public void setPickedUp(boolean pickedUp) {
        this.pickedUp.set(pickedUp);
    }

    public double getSubtotal() {
        return subtotal.get();
    }

    public DoubleProperty subtotalProperty() {
        return subtotal;
    }

    public double getTax() {
        return tax.get();
    }

    public DoubleProperty taxProperty() {
        return tax;
    }

    public double getTotal() {
        return total.get();
    }

    public DoubleProperty totalProperty() {
        return total;
    }
}
