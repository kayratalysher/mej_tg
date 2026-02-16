package ru.akutepov.exchangeratesbot.diplom.generator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class DiplomHelper {

    public static void writeText(PDPageContentStream cs, PDType0Font font, float fontSize,
                           String text, float centerX, float centerY, Color color, boolean drawPoint) throws Exception {
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float textX = centerX - textWidth / 2f;

        cs.beginText();
        cs.newLineAtOffset(textX, centerY);
        cs.showText(text);
        cs.endText();

        if (drawPoint) {
            cs.setNonStrokingColor(Color.RED);
            cs.addRect(centerX - 2, centerY - 2, 4, 4);
            cs.fill();
        }
    }


    public static void drawQrCode(PDPageContentStream contentStream, PDDocument document, PDPage page,
                            String qrText, float width, float height, float marginX, float marginY) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, bufferedImageToBytes(qrImage), "qr");

        float pageWidth = page.getMediaBox().getWidth();
        float x = pageWidth - width - marginX;
        float y = marginY;

        contentStream.drawImage(pdImage, x, y, width, height);
    }

    public static byte[] bufferedImageToBytes(BufferedImage image) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }
    }

}
