package Html;

import Data.CustomerInfo;
import Data.Invoice;
import Data.Item;
import Utils.Utils;

import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Properties;

public class InvoicePrinter implements Printable {

    private static float BIG_FONT_SIZE = 16;
    private static float MEDIUM_FONT_SIZE = 10;
    private static float SMALL_FONT_SIZE = 8;
    private static int MAX_NUM_CHARS_PER_LINE = 35;
    private final Invoice invoice;

    private int x = 0;
    private int y = 0;

    public InvoicePrinter(Invoice invoice, Properties config) {
        this.invoice = invoice;

        String property = config.getProperty("invoice.printer.big.font.size");
        if (property != null) {
            BIG_FONT_SIZE = Float.parseFloat(property);
        }

        property = config.getProperty("invoice.printer.medium.font.size");
        if (property != null) {
            MEDIUM_FONT_SIZE = Float.parseFloat(property);
        }

        property = config.getProperty("invoice.printer.small.font.size");
        if (property != null) {
            SMALL_FONT_SIZE = Float.parseFloat(property);
        }

        property = config.getProperty("invoice.printer.max.num.chars.per.line");
        if (property != null) {
            MAX_NUM_CHARS_PER_LINE = Integer.parseInt(property);
        }

    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        // title "Invoice 123456"
        String txt = "Invoice " + invoice.getInvoiceNumber();
        y = (int) (BIG_FONT_SIZE + 1);
        g2d.setFont(g2d.getFont().deriveFont(BIG_FONT_SIZE));
        graphics.drawString(txt, x, y);

        // invoice date, due date, paid, customer name, phone
        txt = "Date: " + invoice.getInvoiceDate();
        g2d.setFont(g2d.getFont().deriveFont(SMALL_FONT_SIZE));
        y += (int) SMALL_FONT_SIZE + 5;
        graphics.drawString(txt, x, y);

        txt = "Pick up: " + invoice.getDueDate() + " - " + (invoice.getPaid() ? "Paid" : "Not Paid");
        y += (int) SMALL_FONT_SIZE + 1;
        graphics.drawString(txt, x, y);

        CustomerInfo customerInfo = invoice.getCustomerInfo();
        txt = customerInfo.getName();
        if (customerInfo.getPhone() != null && !customerInfo.getPhone().isEmpty()) {
            txt += " - " + customerInfo.getPhone();
        }
        y += (int) SMALL_FONT_SIZE + 1;
        graphics.drawString(txt, x, y);

        // draw a dash
        drawDash(g2d, graphics);

        // item list
        for (Item item : invoice.getItems()) {
            txt = item.getName();
            if (txt.length() > MAX_NUM_CHARS_PER_LINE) {
                txt = txt.substring(0, MAX_NUM_CHARS_PER_LINE - 3) + "...";
            }
            txt += " x " + item.getQuantity();
            y += (int) SMALL_FONT_SIZE + 1;
            graphics.drawString(txt, x, y);
        }

        // draw a dash
        drawDash(g2d, graphics);

        // subtotal, tax, credit, and total
        txt = "Subtotal: $ " + Utils.formatCurrency(invoice.getSubtotal());
        y += (int) SMALL_FONT_SIZE + 3;
        graphics.drawString(txt, x, y);

        txt = "Tax: $ " + Utils.formatCurrency(invoice.getTax());
        y += (int) SMALL_FONT_SIZE + 1;
        graphics.drawString(txt, x, y);

        if (invoice.getCredit() > 0) {
            txt = "Credit: $ " + Utils.formatCurrency(invoice.getCredit());
            y += (int) SMALL_FONT_SIZE + 1;
            graphics.drawString(txt, x, y);
        }

        g2d.setFont(g2d.getFont().deriveFont(MEDIUM_FONT_SIZE));
        txt = "Total: $ " + Utils.formatCurrency(invoice.getTotal());
        y += (int) MEDIUM_FONT_SIZE + 1;
        graphics.drawString(txt, x, y);

        // draw a dash
        drawDash(g2d, graphics);

        // Self info
        g2d.setFont(g2d.getFont().deriveFont(MEDIUM_FONT_SIZE));
        txt = "Sun Tailoring - 604-683-6817";
        y += (int) MEDIUM_FONT_SIZE + 1;
        graphics.drawString(txt, x, y);

        g2d.setFont(g2d.getFont().deriveFont(SMALL_FONT_SIZE));
        txt = "www.suntailoringvancouver.com";
        y += (int) SMALL_FONT_SIZE + 1;
        graphics.drawString(txt, x, y);
        y += (int) SMALL_FONT_SIZE + 1;
        y += (int) SMALL_FONT_SIZE + 1;
        txt = "Not completely satisfied?";
        graphics.drawString(txt, x, y);
        txt = "We will fix it for FREE within 2 weeks";
        y += (int) SMALL_FONT_SIZE + 1;
        graphics.drawString(txt, x, y);

        drawDash(g2d, graphics);
        drawDash(g2d, graphics);
        return PAGE_EXISTS;
    }

    private void drawDash(Graphics2D g2d, Graphics graphics) {
        // draw a dash
        g2d.setFont(g2d.getFont().deriveFont(SMALL_FONT_SIZE));
        y += (int) SMALL_FONT_SIZE + 1;
        graphics.drawString("--------------------------------------------------", x, y);
    }
}
