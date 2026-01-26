//package ru.akutepov.exchangeratesbot.diplom.strategy;
//
//import lombok.RequiredArgsConstructor;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.PDPageContentStream;
//import org.apache.pdfbox.pdmodel.font.PDType0Font;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.stereotype.Component;
//import ru.akutepov.exchangeratesbot.diplom.DiplomId;
//import ru.akutepov.exchangeratesbot.diplom.DiplomRepo;
//
//import java.awt.*;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//
//@Component
//@RequiredArgsConstructor
//public class ReportUtilsThird {
//    private final DiplomRepo diplomRepo;
//
//
//    public byte[] generateDiplom(Integer score, String fullName) {
//        int place=3;
//        if (score>=80){
//            place=1;
//        } else if (score>=60){
//            place=2;
//        }
//        //MJ001
//        var  diplom=diplomRepo.save(new DiplomId(fullName));
//        String diplomId = "MJ" + String.format("%03d", diplom.getId());
//
//        try {
//            // Определяем файл шаблона в зависимости от места
//            String templateFile = "files/third/" + place + ".pdf";
//
//            // Загружаем PDF шаблон из resources
//            InputStream inputStream = new ClassPathResource(templateFile).getInputStream();
//            PDDocument document = PDDocument.load(inputStream);
//
//            // Получаем первую страницу
//            PDPage page = document.getPage(0);
//            float pageWidth = page.getMediaBox().getWidth();
//            float pageHeight = page.getMediaBox().getHeight();
//
//            // Загружаем шрифт с поддержкой кириллицы
//            InputStream fontStream = new ClassPathResource("files/font.ttf").getInputStream();
//            PDType0Font font = PDType0Font.load(document, fontStream);
//
//            PDPageContentStream contentStream = new PDPageContentStream(
//                    document, page, PDPageContentStream.AppendMode.APPEND, true, true
//            );
//
//            // ФИО - центрирование по горизонтали и вертикали
//            float fioFontSize = 105f; // Увеличенный размер для дипломов второго типа
//            contentStream.setFont(font, fioFontSize);
//            contentStream.setNonStrokingColor(new Color(0, 0, 0)); // Черный цвет
//
//
//            float fioTextWidth = font.getStringWidth(fullName) / 1000 * fioFontSize;
//
//            // Координаты центра области для ФИО - по центру страницы
//            float fioCenterX = pageWidth / 2; // Центр страницы по горизонтали
//            float fioX = fioCenterX - (fioTextWidth / 2);
//            float fioY = 1310; // Верхняя треть страницы
//            System.out.println("fioX: " + fioX + ", fioY: " + fioY+", fontSize: " + fioTextWidth);
//            contentStream.beginText();
//            contentStream.newLineAtOffset(fioX, fioY);
//            contentStream.showText(fullName);
//            contentStream.endText();
//
//            // Номер диплома
//            float diplomFontSize = 45f; // Увеличенный размер для видимости
//            contentStream.setFont(font, diplomFontSize);
//
//            // Устанавливаем цвет для номера диплома
//            contentStream.setNonStrokingColor(new Color(0, 0, 0)); // Черный цвет
//
//            float diplomTextWidth = font.getStringWidth(diplomId) / 1000 * diplomFontSize;
//
//
//            float diplomCenterX=23; // Центр страницы
//            float diplomX = 780;
//            float diplomY = 340; // Внизу страницы
//
//            contentStream.beginText();
//            contentStream.newLineAtOffset(diplomX, diplomY);
//            contentStream.showText(diplomId);
//            contentStream.endText();
//
//            contentStream.close();
//
//            // Сохраняем документ в byte array
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            document.save(outputStream);
//            document.close();
//
//            return outputStream.toByteArray();
//
//        } catch (Exception e) {
//            throw new RuntimeException("Ошибка при генерации диплома: " + e.getMessage(), e);
//        }
//    }
//}
