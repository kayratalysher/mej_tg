package ru.akutepov.exchangeratesbot.diplom;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;
import ru.akutepov.exchangeratesbot.diplom.strategy.ReportSchoolGenerate;


@Component
@RequiredArgsConstructor
public class DiplomStrategy {
    private final ReportSchoolGenerate schoolGenerate;
//    private final ReportUtilsSecond reportUtilsSecond;
//    private final ReportUtilsThird reportUtilsThird;

    public byte[] downloadDiplom(Integer score, String fullName, String jetekshi,DiplomTemplates type) {
        if (DiplomTemplates.MUKAGALI_SCHOOL.equals(type) || DiplomTemplates.MUKAGALI_BALSABAKSHA.equals(type)) {
            return schoolGenerate.generateAndSaveDiplom(type,score, fullName,jetekshi,false);
        }

        return null;

    }


    public byte[] downloadSchoolAlgys(DiplomTemplates type,String jetekshi) {
        if (DiplomTemplates.ALGYS_SCHOOL.equals(type) || DiplomTemplates.ALGYS_BALSABAKSHA.equals(type)) {
            return schoolGenerate.generateAndSaveAlgys(jetekshi,type,false);
        }

        return null;
    }
}
