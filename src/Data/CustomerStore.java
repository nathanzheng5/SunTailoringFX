package Data;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.util.Collection;
import java.util.Map;

public class CustomerStore {
    private static final long serialVersionUID = 1L;

    private final ObservableMap<Integer, Customer> customerIdMap;
    private int maxId;

    public CustomerStore(Map<Integer, Customer> startingEntries) {
        customerIdMap = FXCollections.observableHashMap();
        customerIdMap.putAll(startingEntries);

        maxId = customerIdMap.keySet().stream().max(Integer::compare).orElse(0);

        customerIdMap.addListener((Observable observable) -> {
            maxId = customerIdMap.keySet().stream().max(Integer::compare).orElse(0);
        });
    }

    public void update(Customer customer) {
        int id = customer.id;
        if (id == 0) {
            id = ++maxId;
            customer = new Customer(id, customer.getName(), customer.getPhone(), customer.getEmail());
            assert !customerIdMap.containsKey(id);
        }
        customerIdMap.put(id, customer);
    }

    public Customer get(int id) {
        return customerIdMap.get(id);
    }

    public Collection<Customer> values() {
        return customerIdMap.values();
    }

    void printAll() {
        customerIdMap.values().forEach(customer -> System.out.println(customer.id + " - "
                + customer.getName() + ", " + customer.getPhone() + ", " + customer.getEmail()));
    }

}
