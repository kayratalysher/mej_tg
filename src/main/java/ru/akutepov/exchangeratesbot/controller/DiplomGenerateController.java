package ru.akutepov.exchangeratesbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.akutepov.exchangeratesbot.diplom.DiplomStrategy;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@Slf4j
@RequiredArgsConstructor
public class DiplomGenerateController {
    private final DiplomStrategy diplomStrategy;

    @GetMapping(value = "/download-diplom", produces = "application/pdf")
    public ResponseEntity<byte[]> download(
//            @RequestParam(required = false) String type,
            @RequestParam Integer score,
            @RequestParam String fullName,
            @RequestParam String jetekshi
    ) {
        var bytes = diplomStrategy.downloadDiplom(score, fullName,jetekshi, DiplomTemplates.MUKAGALI_SCHOOL);
        var fileName = String.format("diplom_%s.pdf", fullName.replaceAll(" ", "_"));
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .body(bytes);
    }
}
