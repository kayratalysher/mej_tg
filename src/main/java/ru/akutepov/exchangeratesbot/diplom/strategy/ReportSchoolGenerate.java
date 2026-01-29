package ru.akutepov.exchangeratesbot.diplom.strategy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.akutepov.exchangeratesbot.diplom.DiplomId;
import ru.akutepov.exchangeratesbot.diplom.DiplomRepo;
import ru.akutepov.exchangeratesbot.model.FileDTO;
import ru.akutepov.exchangeratesbot.service.FileService;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportSchoolGenerate {

    private final DiplomRepo diplomRepo;
    private final FileService fileService;

    private final ReportConfig config = new ReportConfig(); // конфиг по умолчанию
    /**
     * Конфиг для координат и шрифтов
     */
    @Getter
    public static class ReportConfig {
        // ФИО
        private float fioCenterX = 1488f;
        private float fioYFromTop = 2343f;
        private float fioFontSize = 100f;
        private Color colorFio = new Color(50, 50, 160);

        // Номер диплома
        private float diplomCenterX = 850f;
        private float diplomYFromTop = 3805f;
        private float diplomFontSize = 60f;
        private Color colorDiplom = new Color(50, 50, 160);

        // Жетекши
        private float jetekshiCenterX = 1491f;
        private float jetekshiYFromTop = 2894f;
        private float jetekshiFontSize = 100f;
        private Color colorJetekshi = new Color(50, 50, 160);

        // Жетекши
        private float qrCenterX = 2218f;
        private float qrYFromTop = 477f;
        private float qrFontSize = 100f;
        private Color colorQr = new Color(50, 50, 160);
    }

    private InputStream getTemplateStream(int place) {
        var fileId = switch (place) {
            case 1 -> "1f962d4a-7c73-4391-b67d-f1f51f257136";
            case 2 -> "5f5406f3-a65e-46db-b12d-d624db3e06e2";
            case 3 -> "bc8c3a9e-d0e4-4ba4-8f44-fe255e87f704";
            case 0 -> "ef9ad97e-e394-4c4b-9d49-416fb0e3af12";
            default -> throw new IllegalArgumentException("Invalid place: " + place);
        };
        return new java.io.ByteArrayInputStream(fileService.downloadFileBytes(UUID.fromString(fileId)));
    }


    public byte[] generateAndSaveDiplom(Integer score, String fullName, String jetekshi, boolean debugDrawPoints) {
        var diplom = diplomRepo.save(new DiplomId(fullName, jetekshi));
        String diplomId = "MJ" + String.format("%03d", diplom.getId());
        byte[] pdfBytes = generateDiplom(diplomId,score, fullName, jetekshi, debugDrawPoints);

        log.info("Generated diplom {} for {} with score {}", diplomId, fullName, score);
        String fileName = diplomId+".pdf";
        FileDTO fileDTO = new FileDTO();
        fileDTO.setFilename(fileName);
        fileDTO.setStoredName(fileName);
        fileDTO.setComment("Диплом для " + fullName);
        fileDTO.setFileType("pdf");
        fileDTO.setBucket("public");


        MultipartFile file = new MultipartFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "application/pdf";
            }

            @Override
            public boolean isEmpty() {
                return pdfBytes.length == 0;
            }

            @Override
            public long getSize() {
                return pdfBytes.length;
            }

            @Override
            public byte[] getBytes() {
                return pdfBytes;
            }

            @Override
            public InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(pdfBytes);
            }

            @Override
            public void transferTo(java.io.File dest) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
        UUID fileId = fileService.saveFile(fileDTO,List.of(file));
        log.info("Saved diplom {} to file {}", diplomId, fileId);
        return pdfBytes;
    }
    /**
     * Генерация диплома с настройкой координат через конфиг
     */
    public byte[] generateDiplom(String diplomId,Integer score, String fullName, String jetekshi, boolean debugDrawPoints) {
        int place = score == 0 ? 0 : score >= 80 ? 1 : score >= 60 ? 2 : 3;


        try {
            InputStream templateStream = getTemplateStream(place);
            PDDocument document = PDDocument.load(templateStream);
            PDPage page = document.getPage(0);
            float pageHeight = page.getMediaBox().getHeight();

            InputStream fontStream = new ClassPathResource("files/font.ttf").getInputStream();
            PDType0Font font = PDType0Font.load(document, fontStream);

            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);

            // --- ФИО ---
            writeText(contentStream, font, config.fioFontSize, fullName,
                    config.fioCenterX, pageHeight - config.fioYFromTop, config.colorFio, debugDrawPoints);

            // --- Номер диплома ---
            writeText(contentStream, font, config.diplomFontSize, diplomId,
                    config.diplomCenterX, pageHeight - config.diplomYFromTop, config.colorDiplom, debugDrawPoints);

            // --- Жетекши ---
            if (jetekshi != null && !jetekshi.isBlank()) {
                writeText(contentStream, font, config.jetekshiFontSize, jetekshi,
                        config.jetekshiCenterX, pageHeight - config.jetekshiYFromTop, config.colorJetekshi, debugDrawPoints);
            }
            // --- QR-код ---
            String qrLink = "https://files.mangilikel-jastary.kz/public/" + diplomId+".pdf";
            drawQrCode(contentStream, document, page, qrLink,
                    405, 400, config.qrCenterX, config.qrYFromTop);

            contentStream.close();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации диплома: " + e.getMessage(), e);
        }
    }

    private void writeText(PDPageContentStream cs, PDType0Font font, float fontSize,
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

    private void drawQrCode(PDPageContentStream contentStream, PDDocument document, PDPage page,
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

    private byte[] bufferedImageToBytes(BufferedImage image) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }
    }


}
