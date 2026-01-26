//package ru.akutepov.exchangeratesbot.diplom;
//
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.text.PDFTextStripper;
//import org.apache.pdfbox.text.TextPosition;
//import org.springframework.core.io.ClassPathResource;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//
//public class PDFCoordinateExtractor extends PDFTextStripper {
//
//    private List<TextPosition> currentWord = new ArrayList<>();
//
//    public PDFCoordinateExtractor() throws IOException {
//        super();
//    }
//
//
//
//    @Override
//    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
//        for (TextPosition text : textPositions) {
//            String character = text.getUnicode();
//
//            if (character.trim().isEmpty()) {
//                // Пробел - выводим накопленное слово
//                printWord();
//            } else {
//                currentWord.add(text);
//            }
//        }
//        // Выводим последнее слово в строке
//        printWord();
//    }
//
//    private void printWord() {
//        if (!currentWord.isEmpty()) {
//            StringBuilder word = new StringBuilder();
//            TextPosition first = currentWord.get(0);
//            TextPosition last = currentWord.get(currentWord.size() - 1);
//
//            for (TextPosition tp : currentWord) {
//                word.append(tp.getUnicode());
//            }
//
//            float x = first.getX();
//            float y = first.getY();
//            float fontSize = first.getFontSize();
//            float width = last.getX() + last.getWidth() - first.getX();
//
//            System.out.println(String.format(
//                    "Слово: '%s' | X: %.2f | Y: %.2f | Ширина: %.2f | Шрифт: %.2f",
//                    word.toString(), x, y, width, fontSize
//            ));
//
//            currentWord.clear();
//        }
//    }
//
//    public static void main(String[] args) {
//        try {
//            var diplom=DiplomStrategy
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
