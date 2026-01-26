package ru.akutepov.exchangeratesbot.diplom;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDiplom {
    private final DiplomStrategy diplomStrategy;

//    @PostConstruct
//    public void post() {
//        System.out.println("TestDiplom main method executed");
//      var df=  diplomStrategy.downloadDiplom(95, "John Doe", DiplomTemplates.MUKAGALI_SCHOOL);
//    }
}
