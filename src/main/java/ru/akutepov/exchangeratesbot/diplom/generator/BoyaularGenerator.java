package ru.akutepov.exchangeratesbot.diplom.generator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;
import ru.akutepov.exchangeratesbot.service.FileService;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

import static ru.akutepov.exchangeratesbot.diplom.generator.DiplomHelper.drawQrCode;
import static ru.akutepov.exchangeratesbot.diplom.generator.DiplomHelper.writeText;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoyaularGenerator {
    private final FileService fileService;
    private final ReportConfig config = new ReportConfig();

    public byte[] generateBoyaularDiplom(DiplomTemplates type, String diplomId, Integer score, String fullName, String jetekshi, boolean debugDrawPoints) {

        try {
            InputStream templateStream = getDiplomTemplateStream(score);
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
            String qrLink = "https://files.mangilikel-jastary.kz/public/" + diplomId + ".pdf";
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

    public byte[] generateBoyaularAlgys(DiplomTemplates type, String diplomId, Integer score, String fullName, String jetekshi, boolean debugDrawPoints) {

        try {
            String  templateFileId = "8dc9407d-7b23-419f-bac9-10fbac4b7240";

            InputStream templateStream = new java.io.ByteArrayInputStream(fileService.downloadFileBytes(UUID.fromString(templateFileId)));
            PDDocument document = PDDocument.load(templateStream);
            PDPage page = document.getPage(0);
            float pageHeight = page.getMediaBox().getHeight();

            InputStream fontStream = new ClassPathResource("files/font.ttf").getInputStream();
            PDType0Font font = PDType0Font.load(document, fontStream);

            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);

            // --- ФИО ---
            writeText(contentStream, font, config.algisFontSize, jetekshi,
                    config.algisX, pageHeight - config.algisYFromTop, config.colorqrAlgis, debugDrawPoints);

            // --- QR-код ---
            String qrLink = "https://files.mangilikel-jastary.kz/public/" + diplomId+".pdf";
            drawQrCode(contentStream, document, page, qrLink,
                    405, 410, config.qrAlgisX, config.qrAlgisYFromTop);

            contentStream.close();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации диплома: " + e.getMessage(), e);
        }
    }

    @Getter
    private static class ReportConfig {
        // ФИО
        private float fioCenterX = 1488f;
        private float fioYFromTop = 1396f;
        private float fioFontSize = 90f;
        private Color colorFio = new Color(50, 50, 160);

        // Номер диплома
        private float diplomCenterX = 850f;
        private float diplomYFromTop = 3805f;
        private float diplomFontSize = 60f;
        private Color colorDiplom = new Color(50, 50, 160);

        // Жетекши
        private float jetekshiCenterX = 1491f;
        private float jetekshiYFromTop = 2810f;
        private float jetekshiFontSize = 60f;
        private Color colorJetekshi = new Color(50, 50, 160);

        private float qrCenterX = 2218f;
        private float qrYFromTop = 477f;
        private float qrFontSize = 100f;
        private Color colorQr = new Color(50, 50, 160);

        private float qrAlgisX = 2218f;
        private float qrAlgisYFromTop = 475f;
        private float qrAlgisFontSize = 100f;
        private Color colorqrAlgis = new Color(50, 50, 160);

        private float algisX = 1491f;
        private float algisYFromTop = 1345f;
        private float algisFontSize = 80f;
        private Color coloralgis = new Color(50, 50, 160);
    }
    private InputStream getDiplomTemplateStream(int place) {
        var fileId = switch (place) {
            case 1 -> "0bb6892a-f874-43cb-a167-c51bea69fef0";
            case 2 -> "8c126f71-c2d1-4fd6-ad65-2a1622e27bed";
            case 3 -> "81515703-84ce-4776-b571-505e52b6c71c";
            default -> throw new IllegalArgumentException("Invalid place: " + place);
        };
        return new java.io.ByteArrayInputStream(fileService.downloadFileBytes(UUID.fromString(fileId)));
    }
}
