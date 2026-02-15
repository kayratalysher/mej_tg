package ru.akutepov.exchangeratesbot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.akutepov.exchangeratesbot.diplom.DiplomStrategy;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/diplom")
@RequiredArgsConstructor
@Slf4j
public class DiplomController {
    private final DiplomStrategy diplomStrategy;

    @GetMapping("/generate")
    public ResponseEntity<byte[]> generateDiplom(
        @RequestParam Integer oryn,
        @RequestParam String fullName,
        @RequestParam String jetekshi,
        @RequestParam DiplomTemplates type
    ){
        var bytes = diplomStrategy.downloadDiplom(oryn, fullName,jetekshi, type);
        var fileName = String.format("diplom_%s.pdf", fullName.replaceAll(" ", "_"));
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .body(bytes);
    }

    @PostMapping("/create")
    public void createDiplom(){
//        System.out.println(map);
        //логика создания диплома
    }
}
