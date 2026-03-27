package Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {
    public static final Path SAVE_DIR_PATH = Paths.get("Save");

    public static final Path EXPENSES_DIR_PATH = Paths.get("Expenses");

    public static final Path SETTINGS_DIR_PATH = Paths.get("Settings");

    public static final String DB_PATH = SETTINGS_DIR_PATH + "/suntailoring.db";

    public static final File ADDRESS_BOOK_DAT_FILE = new File(SETTINGS_DIR_PATH + "/" + "addressBook.dat");
    public static final File INVOICE_STORE_FILTER_DAT_FILE = new File(SETTINGS_DIR_PATH + "/" + "invoiceStoreFilters.dat");

    public static File getQuickItemsDatFile(String name) {
        return new File(SETTINGS_DIR_PATH + "/" + "quick" + name.replaceAll(" ", "") + "Settings.dat");
    }

    public static void createDirectoryIfNecessary(Path path) throws IOException {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }


}
