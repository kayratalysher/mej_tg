package ru.akutepov.exchangeratesbot.diplom.archive;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.akutepov.exchangeratesbot.diplom.DiplomId;
import ru.akutepov.exchangeratesbot.diplom.DiplomRepo;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class ReportUtils {
    private final DiplomRepo diplomRepo;


    public byte[] generateDiplom(Integer place, String fullName) {

        //MJ001
        var  diplom=diplomRepo.save(new DiplomId(fullName));
        String diplomId = "MJ" + String.format("%03d", diplom.getId());

        try {
            // Определяем файл шаблона в зависимости от места
            String templateFile = "files/" + place + ".pdf";

            // Загружаем PDF шаблон из resources
            InputStream inputStream = new ClassPathResource(templateFile).getInputStream();
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
