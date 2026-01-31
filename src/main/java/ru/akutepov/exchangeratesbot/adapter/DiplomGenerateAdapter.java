package ru.akutepov.exchangeratesbot.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.akutepov.exchangeratesbot.diplom.DiplomStrategy;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;
import ru.akutepov.exchangeratesbot.diplom.strategy.ReportSchoolGenerate;
import ru.akutepov.exchangeratesbot.entity.ContestResult;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiplomGenerateAdapter {
    private final DiplomStrategy  diplomStrategy;

    public byte[] downloadDiploma(String fullName, String jetekshi, DiplomTemplates template, int diplomCategory) {
        return diplomStrategy.downloadDiplom(diplomCategory, fullName, jetekshi,template);
    }

    public byte[] downloadDiplomAlgis(String jetekshi, DiplomTemplates template) {
        return diplomStrategy.downloadSchoolAlgys(template,jetekshi);
    }

}
