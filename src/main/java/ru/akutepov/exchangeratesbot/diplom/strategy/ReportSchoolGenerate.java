package ru.akutepov.exchangeratesbot.diplom.strategy;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.akutepov.exchangeratesbot.adapter.MinioAdapter;
import ru.akutepov.exchangeratesbot.diplom.DiplomId;
import ru.akutepov.exchangeratesbot.diplom.DiplomRepo;
import ru.akutepov.exchangeratesbot.service.FileService;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportSchoolGenerate {
    private final DiplomRepo diplomRepo;
//    private final MinioAdapter minioAdapter;
    private final FileService fileService;

    private InputStream getTemplateStream(int place) {
        var fileId=switch (place) {
            case 1 -> "1f962d4a-7c73-4391-b67d-f1f51f257136";
            case 2 -> "5f5406f3-a65e-46db-b12d-d624db3e06e2";
            case 3 -> "bc8c3a9e-d0e4-4ba4-8f44-fe255e87f704";
            case 0 -> "ef9ad97e-e394-4c4b-9d49-416fb0e3af12";
            default -> throw new IllegalArgumentException("Invalid place: " + place);
        };

        var templateBytes= fileService.downloadFileBytes(UUID.fromString(fileId));
        return new java.io.ByteArrayInputStream(templateBytes);
    }


    public byte[] generateDiplom(Integer score, String fullName) {
        int place=3;

        if (score == 0) {
            place = 0;
        } else if (score >= 80) {
            place = 1;
        } else if (score >= 60) {
            place = 2;
        }
        //MJ001
        var  diplom=diplomRepo.save(new DiplomId(fullName));
        String diplomId = "MJ" + String.format("%03d", diplom.getId());

        try {
            // Загружаем PDF шаблон из resources
            InputStream inputStream = getTemplateStream(place);
            PDDocument document = PDDocument.load(inputStream);

            // Получаем первую страницу
            PDPage page = document.getPage(0);
            float pageHeight = page.getMediaBox().getHeight();

            // Загружаем шрифт с поддержкой кириллицы
            InputStream fontStream = new ClassPathResource("files/font.ttf").getInputStream();
            PDType0Font font = PDType0Font.load(document, fontStream);

            PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true
            );

            // ФИО - центрирование по горизонтали
            float fioFontSize = 20.90f;
            contentStream.setFont(font, fioFontSize);
            contentStream.setNonStrokingColor(new Color(50, 50, 160)); //светло тено синий


            float fioTextWidth = font.getStringWidth(fullName) / 1000 * fioFontSize;

            // Координаты центра области для ФИО
            float fioCenterX = 420.64f; // Центр области
            float fioX = fioCenterX - (fioTextWidth / 2);
            float fioY = pageHeight - 318.85f;

            contentStream.beginText();
            contentStream.newLineAtOffset(fioX, fioY);
            contentStream.showText(fullName);
            contentStream.endText();

            // Номер диплома
            float diplomFontSize = 13.69f;
            contentStream.setFont(font, diplomFontSize);

            // Устанавливаем цвет для номера диплома
            contentStream.setNonStrokingColor(new Color(50, 50, 160));

            float diplomTextWidth = font.getStringWidth(diplomId) / 1000 * diplomFontSize;


            float diplomCenterX = 194.42f; // Центр области
            float diplomX = diplomCenterX - (diplomTextWidth / 2);
            float diplomY = pageHeight - 557.07f;

            contentStream.beginText();
            contentStream.newLineAtOffset(diplomX, diplomY);
            contentStream.showText(diplomId);
            contentStream.endText();

            contentStream.close();

            // Сохраняем документ в byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации диплома: " + e.getMessage(), e);
        }
    }
}
