package Html;

import Data.CustomerInfo;
import Data.Invoice;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InvoiceHtmlTest {

    @Test
    public void testMaskPhoneNumberStandard() {
        assertEquals("###-###-4567", InvoiceHtml.maskPhoneNumber("604-123-4567"));
        assertEquals("###-###-4567", InvoiceHtml.maskPhoneNumber("6041234567"));
        assertEquals("###-###-4567", InvoiceHtml.maskPhoneNumber("123-4567"));
    }

    @Test
    public void testMaskPhoneNumberWithNonDigits() {
        assertEquals("###-###-4567", InvoiceHtml.maskPhoneNumber("604-123-4567 (cell)"));
        assertEquals("###-###-4567", InvoiceHtml.maskPhoneNumber("604-123-4567 ext 12"));
        assertEquals("###-###-4567", InvoiceHtml.maskPhoneNumber("  604-123-4567  "));
    }

    @Test
    public void testMaskPhoneNumberShortStrings() {
        // Previously threw StringIndexOutOfBoundsException for strings with length < 4
        assertEquals("123", InvoiceHtml.maskPhoneNumber("123"));
        assertEquals("12", InvoiceHtml.maskPhoneNumber("12"));
        assertEquals("1", InvoiceHtml.maskPhoneNumber("1"));
        assertEquals("N/A", InvoiceHtml.maskPhoneNumber("N/A"));
        assertEquals("-", InvoiceHtml.maskPhoneNumber("-"));
        assertEquals("", InvoiceHtml.maskPhoneNumber(""));
        assertEquals("", InvoiceHtml.maskPhoneNumber("   "));
        assertEquals("", InvoiceHtml.maskPhoneNumber(null));
    }

    @Test
    public void testMaskPhoneNumberExactlyFourDigits() {
        assertEquals("###-###-1234", InvoiceHtml.maskPhoneNumber("1234"));
    }

    @Test
    public void testBuildBodyDoesNotCrashOnShortPhone() {
        Element root = new Element("html");
        CustomerInfo shortPhoneCustomer = new CustomerInfo("Test Customer", "123", "test@test.com");
        Invoice invoice = new Invoice("INV-001", LocalDate.now(), LocalDate.now().plusDays(7),
                shortPhoneCustomer, Collections.emptyList(), 0, false, false, false);

        InvoiceHtml.buildBody(root, invoice, "Hello");
        String printed = root.print();
        assertTrue(printed.contains("Test Customer"));
        assertTrue(printed.contains("123"));
    }

    @Test
    public void testBuildBodyDoesNotCrashOnNonNumericShortPhone() {
        Element root = new Element("html");
        CustomerInfo naPhoneCustomer = new CustomerInfo("Test Customer", "N/A", "test@test.com");
        Invoice invoice = new Invoice("INV-002", LocalDate.now(), LocalDate.now().plusDays(7),
                naPhoneCustomer, Collections.emptyList(), 0, false, false, false);

        InvoiceHtml.buildBody(root, invoice, "Hello");
        String printed = root.print();
        assertTrue(printed.contains("Test Customer"));
        assertTrue(printed.contains("N/A"));
    }
}
