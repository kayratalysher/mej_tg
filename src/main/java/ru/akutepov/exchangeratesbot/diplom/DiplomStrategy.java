package ru.akutepov.exchangeratesbot.diplom;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.akutepov.exchangeratesbot.diplom.archive.ReportUtils;
import ru.akutepov.exchangeratesbot.diplom.archive.ReportUtilsSecond;
import ru.akutepov.exchangeratesbot.diplom.archive.ReportUtilsThird;
import ru.akutepov.exchangeratesbot.diplom.enums.DiplomTemplates;
import ru.akutepov.exchangeratesbot.diplom.strategy.ReportSchoolGenerate;


@Component
@RequiredArgsConstructor
public class DiplomStrategy {
    private final ReportSchoolGenerate schoolGenerate;
    private final ReportUtils reportUtils;
    private final ReportUtilsSecond reportUtilsSecond;
    private final ReportUtilsThird reportUtilsThird;
//    private final ReportUtilsSecond reportUtilsSecond;
//    private final ReportUtilsThird reportUtilsThird;

    public byte[] downloadDiplom(Integer score, String fullName, String jetekshi,DiplomTemplates type) {
        if (DiplomTemplates.MUKAGALI_SCHOOL.equals(type) || DiplomTemplates.MUKAGALI_BALSABAKSHA.equals(type)) {
            return schoolGenerate.generateAndSaveDiplom(type,score, fullName,jetekshi,false);
        }
        if (DiplomTemplates.KAJMUKAN.equals(type)){
            return schoolGenerate.generateAndSaveKajmukan(type,score, fullName,jetekshi,false);
        }

        if (DiplomTemplates.ALGYS_SCHOOL.equals(type) || DiplomTemplates.ALGYS_BALSABAKSHA.equals(type)) {
            return schoolGenerate.generateAndSaveAlgys(jetekshi,type,false);
        }

        if (DiplomTemplates.BOYAULAR_DIPLOM.equals(type)) {
            return schoolGenerate.generateAndSaveBoyaularDiplom(fullName,jetekshi,type,score,false);
        }

        if (DiplomTemplates.BOYAULAR_ALGYS.equals(type)) {
            return schoolGenerate.generateAndSaveBoyaularAlgys(fullName,jetekshi,type,score,true);
        }

        if (DiplomTemplates.ARCHIVE.equals(type)) {
            if (score==1)
                return reportUtils.generateDiplom(score,fullName);
            if (score==2)
                return reportUtilsSecond.generateDiplom(score,fullName);
            if (score==3)
                return reportUtilsThird.generateDiplom(score,fullName);
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
