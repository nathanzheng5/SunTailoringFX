package Html;

import Data.CustomerInfo;
import Data.Invoice;
import Data.Item;
import Utils.Utils;

public class InvoiceHtml {

    public static void buildHead(Element parent) {
        Element head = new Element("head");
        Element style = new Element("style");
        head.addChild(style);

        CssElement container = new CssElement("container");
        container.addAttribute(AttributeLib.width800);
        style.addChild(container);

        CssElement wholeBlock = new CssElement("wholeBlock");
        wholeBlock.addAttribute(AttributeLib.width800);
        style.addChild(wholeBlock);

        CssElement halfBlock = new CssElement("halfBlock");
        halfBlock.addAttribute(AttributeLib.width400);
        halfBlock.addAttribute(AttributeLib.floatLeft);
        style.addChild(halfBlock);

        CssElement quarterBlock = new CssElement("quarterBlock");
        quarterBlock.addAttribute(AttributeLib.width200);
        quarterBlock.addAttribute(AttributeLib.floatLeft);
        style.addChild(quarterBlock);

        CssElement logo = new CssElement("logo");
        logo.addAttribute(AttributeLib.floatRight);
        logo.addAttribute(AttributeLib.borderNone);
        style.addChild(logo);

        CssElement tableHeaderLeft = new CssElement("tableHeaderLeft");
        tableHeaderLeft.addAttribute(new Attribute("width", "500px"));
        tableHeaderLeft.addAttribute(AttributeLib.borderLeft);
        tableHeaderLeft.addAttribute(AttributeLib.borderTop);
        tableHeaderLeft.addAttribute(AttributeLib.bgColor);
        style.addChild(tableHeaderLeft);

        CssElement tableHeaderMiddle = new CssElement("tableHeaderMiddle");
        tableHeaderMiddle.addAttribute(new Attribute("width", "100px"));
        tableHeaderMiddle.addAttribute(AttributeLib.borderTop);
        tableHeaderMiddle.addAttribute(AttributeLib.bgColor);
        style.addChild(tableHeaderMiddle);

        CssElement tableHeaderRight = new CssElement("tableHeaderRight");
        tableHeaderRight.addAttribute(new Attribute("width", "100px"));
        tableHeaderRight.addAttribute(AttributeLib.borderRight);
        tableHeaderRight.addAttribute(AttributeLib.borderTop);
        tableHeaderRight.addAttribute(AttributeLib.bgColor);
        style.addChild(tableHeaderRight);

        CssElement tableColLeft = new CssElement("tableColLeft");
        tableColLeft.addAttribute(new Attribute("width", "500px"));
        tableColLeft.addAttribute(AttributeLib.borderLeft);
        style.addChild(tableColLeft);

        CssElement tableColMiddle = new CssElement("tableColMiddle");
        tableColMiddle.addAttribute(new Attribute("width", "100px"));
        style.addChild(tableColMiddle);

        CssElement tableColRight = new CssElement("tableColRight");
        tableColRight.addAttribute(new Attribute("width", "100px"));
        tableColRight.addAttribute(AttributeLib.borderRight);
        style.addChild(tableColRight);

        CssElement divider1 = new CssElement("divider1");
        divider1.addAttribute(AttributeLib.borderLeft);
        style.addChild(divider1);

        CssElement divider2 = new CssElement("divider2");
        divider2.addAttribute(AttributeLib.borderRight);
        divider2.addAttribute(AttributeLib.borderTop);
        style.addChild(divider2);

        CssElement tableBottomRow = new CssElement("tableBottomRow");
        tableBottomRow.addAttribute(AttributeLib.borderLeft);
        tableBottomRow.addAttribute(AttributeLib.borderRight);
        tableBottomRow.addAttribute(AttributeLib.borderBottom);
        style.addChild(tableBottomRow);

        CssElement table = new CssElement("table", false);
        table.addAttribute(new Attribute("border-collapse", "collapse"));
        table.addAttribute(AttributeLib.floatLeft);
        style.addChild(table);

        CssElement th = new CssElement("th", false);
        th.addAttribute(new Attribute("text-align", "left"));
        th.addAttribute(new Attribute("padding-left", "10px"));
        style.addChild(th);

        CssElement td = new CssElement("td", false);
        td.addAttribute(new Attribute("text-align", "left"));
        td.addAttribute(new Attribute("padding-left", "10px"));
        td.addAttribute(new Attribute("height","10px"));
        style.addChild(td);

        parent.addChild(head);
    }

    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }
        String basePhone = phone.replaceAll("(?i)\\s*(ext|x|extension)\\b.*", "");
        String digits = basePhone.replaceAll("\\D", "");
        if (digits.length() >= 4) {
            return "###-###-" + digits.substring(digits.length() - 4);
        }
        return phone.trim();
    }

    public static void buildBody(Element parent, Invoice invoice, String message) {
        Element body = new Element("body");
        parent.addChild(body);

        Element container = new Element("div");
        container.addAttribute(new Attribute("id", "container"));
        body.addChild(container);

        Element div = new Element("div");
        div.addAttribute(new Attribute("id", "wholeBlock"));
        div.setContent(message);
        container.addChild(div);

        div = new Element("div");
        div.addAttribute(new Attribute("id", "wholeBlock"));
        div.addAttribute(new Attribute("style", "font-size:24px; font-weight:bold"));
        div.setContent("Sun Tailoring Invoice " + invoice.getInvoiceNumber());
        container.addChild(div);

        div = new Element("div");
        div.addAttribute(new Attribute("id", "halfBlock"));
        div.setContent("www.suntailoringvancouver.com<br>604-683-6817<br>U-13 555 W Hasting St<br> Vancouver, BC V6B 4N6");
        container.addChild(div);

        div = new Element("div");
        div.addAttribute(new Attribute("id", "halfBlock"));
        // modify customer address to hash phone numbers and hide emails
        CustomerInfo customerInfo = invoice.getCustomerInfo();
        String billToString = customerInfo.getName();
        String maskedPhone = maskPhoneNumber(customerInfo.getPhone());
        if (!maskedPhone.isEmpty()) {
            billToString += "<br>" + maskedPhone;
        }
        div.setContent("<b>Invoice Date:</b> " + invoice.getInvoiceDate() + "<br>" +
            "<b>Due Date:</b> " + invoice.getDueDate() + "<br>" +
            "<b>Bill To:</b> " + billToString.replaceAll("\n", "<br>"));
        container.addChild(div);

        buildTable(container, invoice);
    }

    private static void buildTable(Element parent, Invoice invoice) {
        Element table = new Element("table");
        parent.addChild(table);

        // header row
        Element headerRow = new Element("tr");
        Element th = new Element("th", "Item Description");
        th.addAttribute(new Attribute("id", "tableHeaderLeft"));
        headerRow.addChild(th);
        th = new Element("th", "Quantity");
        th.addAttribute(new Attribute("id", "tableHeaderMiddle"));
        headerRow.addChild(th);
        th = new Element("th", "Unit Price");
        th.addAttribute(new Attribute("id", "tableHeaderMiddle"));
        headerRow.addChild(th);
        th = new Element("th", "Price");
        th.addAttribute(new Attribute("id", "tableHeaderRight"));
        headerRow.addChild(th);
        table.addChild(headerRow);

        // item list
        for (Item item : invoice.getItems()) {
            table.addChild(itemToHtmlRow(item));
        }
                // divider
        Element dividerRow = new Element("tr");
        Element dividerCell = new Element("td");
        dividerCell.addAttribute(new Attribute("id", "divider1"));
        dividerCell.addAttribute(new Attribute("colspan", "2"));
        dividerRow.addChild(dividerCell);
        dividerCell = new Element("td");
        dividerCell.addAttribute(new Attribute("id", "divider2"));
        dividerCell.addAttribute(new Attribute("colspan", "2"));
        dividerRow.addChild(dividerCell);
        table.addChild(dividerRow);

        // summary
        Element subtotalRow = new Element("tr");
        Element td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColLeft"));
        subtotalRow.addChild(td);
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColMiddle"));
        subtotalRow.addChild(td);
        td = new Element("td", "Subtotal:");
        td.addAttribute(new Attribute("id", "tableColMiddle"));
        subtotalRow.addChild(td);
        td = new Element("td", Utils.formatCurrency(invoice.getSubtotal()));
        td.addAttribute(new Attribute("id", "tableColRight"));
        subtotalRow.addChild(td);
        table.addChild(subtotalRow);

        Element taxRow = new Element("tr");
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColLeft"));
        taxRow.addChild(td);
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColMiddle"));
        taxRow.addChild(td);
        td = new Element("td", "Tax (5%):");
        td.addAttribute(new Attribute("id", "tableColMiddle"));
        taxRow.addChild(td);
        td = new Element("td", Utils.formatCurrency(invoice.getTax()));
        td.addAttribute(new Attribute("id", "tableColRight"));
        taxRow.addChild(td);
        table.addChild(taxRow);

        if (invoice.getCredit() > 0) {
            Element creditRow = new Element("tr");
            td = new Element("td");
            td.addAttribute(new Attribute("id", "tableColLeft"));
            creditRow.addChild(td);
            td = new Element("td");
            td.addAttribute(new Attribute("id", "tableColMiddle"));
            creditRow.addChild(td);
            td = new Element("td", "Credit:");
            td.addAttribute(new Attribute("id", "tableColMiddle"));
            creditRow.addChild(td);
            td = new Element("td", "(" + Utils.formatCurrency(invoice.getCredit()) + ")");
            td.addAttribute(new Attribute("id", "tableColRight"));
            creditRow.addChild(td);
            table.addChild(creditRow);
        }

        Element totalRow = new Element("tr");
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColLeft"));
        totalRow.addChild(td);
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColMiddle"));
        totalRow.addChild(td);
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColMiddle"));
        Element bold = new Element("b", "Total:");
        td.addChild(bold);
        totalRow.addChild(td);
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableColRight"));
        bold = new Element("b", Utils.formatCurrency(invoice.getTotal()));
        td.addChild(bold);
        totalRow.addChild(td);
        table.addChild(totalRow);

        // bottom row
        Element bottomRow = new Element("tr");
        td = new Element("td");
        td.addAttribute(new Attribute("id", "tableBottomRow"));
        td.addAttribute(new Attribute("colspan", "4"));
        bottomRow.addChild(td);
        table.addChild(bottomRow);
    }

    private static Element itemToHtmlRow(Item item) {
        Element row = new Element("tr");

        Element cell = new Element("td", item.getName());
        cell.addAttribute(new Attribute("id", "tableColLeft"));
        row.addChild(cell);

        cell = new Element("td", Integer.toString(item.getQuantity()));
        cell.addAttribute(new Attribute("id", "tableColMiddle"));
        row.addChild(cell);

        cell = new Element("td", Utils.formatCurrency(item.getUnitPrice()));
        cell.addAttribute(new Attribute("id", "tableColMiddle"));
        row.addChild(cell);

        cell = new Element("td", Utils.formatCurrency(item.getPrice()));
        cell.addAttribute(new Attribute("id", "tableColRight"));
        row.addChild(cell);

        return row;

    }

    private static void addInvoiceInfoTitle(Element parent, String title) {
        Element div = new Element("div");
        div.addAttribute(new Attribute("id", "quarterBlock"));
        div.addAttribute(new Attribute("style", "font-weight:bold"));
        div.setContent(title);
        parent.addChild(div);
    }

    private static void addInvoiceInfoContent(Element parent, String content) {
        Element div = new Element("div");
        div.addAttribute(new Attribute("id", "quarterBlock"));
        div.setContent(content);
        parent.addChild(div);
    }

}
