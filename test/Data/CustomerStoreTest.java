package Data;

import org.junit.Test;

import java.util.Collections;

/**
 * TODO: CLASS JAVA DOC HERE
 */
public class CustomerStoreTest {

    @Test
    public void test() {
        CustomerStore customerStore = new CustomerStore(Collections.emptyMap());
        Customer nathan = new Customer(1, "nathan", "604-657-7930", "nathanzheng87@gmail.com");
        customerStore.update(nathan);
        customerStore.update(new Customer(0, "jason", "", ""));
        customerStore.printAll();

        System.out.println();
        nathan.setName("nathan zheng");
        customerStore.update(nathan);
        customerStore.printAll();

        System.out.println();
        customerStore.update(new Customer(0, "harry", "", ""));
        customerStore.printAll();

        System.out.println();
    }
}
