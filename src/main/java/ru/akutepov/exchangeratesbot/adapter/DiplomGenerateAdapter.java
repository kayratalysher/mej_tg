package ru.akutepov.exchangeratesbot.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiplomGenerateAdapter {
    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] downloadDiploma(String fullName, String type, int score) {

        String baseUrl = "https://api-cabinet-beta.sapadc.kz/v1/core/download-diplom";

        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .queryParam("type", "SECOND")
                .queryParam("score", score)
                .queryParam("fullName", fullName)
                .toUriString();

        log.info("➡️ GET {}", url);

        HttpHeaders headers = new HttpHeaders();
//        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        // ---- LOG RESPONSE ----
        log.info("⬅️ Response status: {}", response.getStatusCode());
        log.info("⬅️ Content-Type: {}", response.getHeaders().getContentType());

        byte[] body = response.getBody();
        log.info("⬅️ Body size: {} bytes", body != null ? body.length : 0);

        if (body == null || body.length == 0) {
            throw new RuntimeException("API вернул пустой файл");
        }

        return body;
    }
}
