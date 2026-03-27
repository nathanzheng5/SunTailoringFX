package Utils;

import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeException;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import net.sourceforge.barbecue.output.OutputException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BarbecueTest {
    public static void main(String[] args) throws BarcodeException, IOException, OutputException {
        Barcode barcode = BarcodeFactory.createCode128A("2019010112");
        barcode.setDrawingText(false);
        barcode.setBarWidth((int) 0.5);
        barcode.setBarHeight(6);
        BufferedImage image = BarcodeImageHandler.getImage(barcode);
        File file = new File("barcode.png");
        ImageIO.write(image, "png", file);
    }

}
