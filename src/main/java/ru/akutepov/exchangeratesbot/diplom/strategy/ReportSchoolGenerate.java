package ru.akutepov.exchangeratesbot.diplom.strategy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.akutepov.exchangeratesbot.diplom.DiplomId;
import ru.akutepov.exchangeratesbot.diplom.DiplomRepo;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;
import ru.akutepov.exchangeratesbot.diplom.generator.BoyaularGenerator;
import ru.akutepov.exchangeratesbot.model.FileDTO;
import ru.akutepov.exchangeratesbot.service.FileService;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static ru.akutepov.exchangeratesbot.diplom.generator.DiplomHelper.drawQrCode;
import static ru.akutepov.exchangeratesbot.diplom.generator.DiplomHelper.writeText;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportSchoolGenerate {

    private final DiplomRepo diplomRepo;
    private final FileService fileService;
    private final BoyaularGenerator boyaularGenerator;
    private static final String ALGIS_TEMPLATE_MEKTEP_ID = "ef9ad97e-e394-4c4b-9d49-416fb0e3af12";
    private static final String ALGIS_TEMPLATE_BALABAKSHA_ID = "f550d648-8bbf-4b9f-a4cd-8d8b517e588d";
    private final ReportConfig config = new ReportConfig(); // конфиг по умолчанию

    public byte[] generateAndSaveAlgys(String jetekshi,DiplomTemplates templates, boolean debugDrawPoints) {
        var diplom = diplomRepo.save(new DiplomId("Алғыс хат", jetekshi));
        String diplomId = "SCH" + String.format("%03d", diplom.getId());
        byte[] pdfBytes = generateAlgis(diplomId,templates,jetekshi, debugDrawPoints);

        log.info("Generated algys {} for {}", diplomId, jetekshi);
        String fileName = diplomId+".pdf";
        FileDTO fileDTO = new FileDTO();
        fileDTO.setFilename(fileName);
        fileDTO.setStoredName(fileName);
        fileDTO.setComment("Алғыс хат для " + jetekshi);
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
        log.info("Saved algis {} to file {}", diplomId, fileId);
        return pdfBytes;
    }

    private byte[] generateAlgis(String diplomId, DiplomTemplates templates,String jetekshi, boolean debugDrawPoints) {

        try {
            String  templateFileId = templates.equals(DiplomTemplates.ALGYS_BALSABAKSHA)
                    ? ALGIS_TEMPLATE_BALABAKSHA_ID : ALGIS_TEMPLATE_MEKTEP_ID;


            InputStream templateStream = new java.io.ByteArrayInputStream(fileService.downloadFileBytes(UUID.fromString(templateFileId)));
            PDDocument document = PDDocument.load(templateStream);
            PDPage page = document.getPage(0);
            float pageHeight = page.getMediaBox().getHeight();

            InputStream fontStream = new ClassPathResource("files/font.ttf").getInputStream();
            PDType0Font font = PDType0Font.load(document, fontStream);

            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);

            // --- ФИО ---
            writeText(contentStream, font, config.fioFontSize, jetekshi,
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

    public byte[] generateAndSaveKajmukan(DiplomTemplates type, Integer score, String fullName, String jetekshi, boolean debugDrawPoints) {
        var diplom = diplomRepo.save(new DiplomId(fullName, jetekshi));
        String diplomId = "MJ" + String.format("%03d", diplom.getId());
        byte[] pdfBytes = generateDiplomKajmukan(type,diplomId,score, fullName, debugDrawPoints);

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

    public byte[] generateAndSaveBoyaularDiplom(String fullName, String jetekshi, DiplomTemplates type, Integer score,boolean debugDrawPoints) {
        var diplom = diplomRepo.save(new DiplomId(fullName, jetekshi));
        String diplomId = "MJ" + String.format("%03d", diplom.getId());
        byte[] pdfBytes = boyaularGenerator.generateBoyaularDiplom(type,diplomId,score, fullName,jetekshi, debugDrawPoints);

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

    public byte[] generateAndSaveBoyaularAlgys(String fullName, String jetekshi, DiplomTemplates type, Integer score, boolean debugDrawPoints) {
        var diplom = diplomRepo.save(new DiplomId(fullName, jetekshi));
        String diplomId = "MJ" + String.format("%03d", diplom.getId());
        byte[] pdfBytes = boyaularGenerator.generateBoyaularAlgys(type,diplomId,score, fullName,jetekshi, debugDrawPoints);

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
        log.info("Saved algys {} to file {}", diplomId, fileId);
        return pdfBytes;
    }


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
        private float jetekshiFontSize = 60f;
        private Color colorJetekshi = new Color(50, 50, 160);

        // Жетекши
        private float qrCenterX = 2218f;
        private float qrYFromTop = 477f;
        private float qrFontSize = 100f;
        private Color colorQr = new Color(50, 50, 160);

        // Жетекши
        private float qrAlgisX = 2218f;
        private float qrAlgisYFromTop = 360f;
        private float qrAlgisFontSize = 100f;
        private Color colorqrAlgis = new Color(50, 50, 160);

        private float algisX = 1491f;
        private float algisYFromTop = 1758f;
        private float algisFontSize = 100f;
        private Color coloralgis = new Color(50, 50, 160);

    }

    private InputStream getTemplateStream(int place,DiplomTemplates type) {
        var fileId = switch (place) {
            case 1 -> type.equals(DiplomTemplates.MUKAGALI_SCHOOL) ? "1f962d4a-7c73-4391-b67d-f1f51f257136" : "bc0e503b-6d51-429b-9219-bfc2f84bdda2";
            case 2 -> type.equals(DiplomTemplates.MUKAGALI_SCHOOL) ? "5f5406f3-a65e-46db-b12d-d624db3e06e2" : "8d5a8daf-16b1-4930-97ba-26baa385939c";
            case 3 -> type.equals(DiplomTemplates.MUKAGALI_SCHOOL) ? "bc8c3a9e-d0e4-4ba4-8f44-fe255e87f704" : "f834105b-6ab4-4cdd-b313-b65f135b76c2";
            default -> throw new IllegalArgumentException("Invalid place: " + place);
        };
        return new java.io.ByteArrayInputStream(fileService.downloadFileBytes(UUID.fromString(fileId)));
    }

    private InputStream getTemplateStreamKajmukan(int place) {
        var fileId = switch (place) {
            case 1 -> "a59ef21b-7554-4f44-b234-db111770f6e3";
            case 2 -> "387860f4-9e8f-4559-94ec-b9633e193ff2";
            case 3 -> "97b6a744-2b83-499e-83cb-d3a96e06f326";
            default -> throw new IllegalArgumentException("Invalid place: " + place);
        };
        return new java.io.ByteArrayInputStream(fileService.downloadFileBytes(UUID.fromString(fileId)));
    }


    public byte[] generateAndSaveDiplom(DiplomTemplates type,Integer score, String fullName, String jetekshi, boolean debugDrawPoints) {
        var diplom = diplomRepo.save(new DiplomId(fullName, jetekshi));
        String diplomId = "MJ" + String.format("%03d", diplom.getId());
        byte[] pdfBytes = generateDiplom(type,diplomId,score, fullName, jetekshi, debugDrawPoints);

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
    public byte[] generateDiplom(DiplomTemplates type,String diplomId,Integer score, String fullName, String jetekshi, boolean debugDrawPoints) {


        try {
            InputStream templateStream = getTemplateStream(score,type);
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

    public byte[] generateDiplomKajmukan(DiplomTemplates type,String diplomId,Integer score, String fullName,boolean debugDrawPoints) {

        try {
            InputStream templateStream =  getTemplateStreamKajmukan(score);
            PDDocument document = PDDocument.load(templateStream);
            PDPage page = document.getPage(0);
            float pageHeight = page.getMediaBox().getHeight();

            InputStream fontStream = new ClassPathResource("files/caveat.ttf").getInputStream();
            PDType0Font font = PDType0Font.load(document, fontStream);

            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);

            // --- ФИО ---
            writeText(contentStream, font, 120, fullName,
                    1444f, pageHeight - 1130f, config.colorFio, debugDrawPoints);

            // --- Номер диплома ---
            writeText(contentStream, font, config.diplomFontSize, diplomId,
                    818f, pageHeight - 3614f, config.colorDiplom, debugDrawPoints);


            // --- QR-код ---
            String qrLink = "https://files.mangilikel-jastary.kz/public/" + diplomId+".pdf";
            drawQrCode(contentStream, document, page, qrLink,
                    405, 400, config.qrCenterX, 650f);

            contentStream.close();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации диплома: " + e.getMessage(), e);
        }
    }

}
