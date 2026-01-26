package ru.akutepov.exchangeratesbot.diplom;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;
import ru.akutepov.exchangeratesbot.diplom.strategy.ReportSchoolGenerate;


@Component
@RequiredArgsConstructor
public class DiplomStrategy {
    private final ReportSchoolGenerate reportUtils;
//    private final ReportUtilsSecond reportUtilsSecond;
//    private final ReportUtilsThird reportUtilsThird;

    public byte[] downloadDiplom(Integer score, String fullName, DiplomTemplates type) {
        if (DiplomTemplates.MUKAGALI_SCHOOL.equals(type)) {
            return reportUtils.generateDiplom(score, fullName);
        }

//        if (type.equalsIgnoreCase("SECOND")) {
//            return reportUtilsSecond.generateDiplom(score, fullName);
//        }
//
//        if (type.equalsIgnoreCase("THIRD")) {
//            return reportUtilsThird.generateDiplom(score, fullName);
//        }

        return null;

    }
}
