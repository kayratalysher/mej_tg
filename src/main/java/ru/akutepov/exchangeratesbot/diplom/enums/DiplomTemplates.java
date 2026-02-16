package ru.akutepov.exchangeratesbot.diplom.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum DiplomTemplates {
    @Schema(description = "Мукагали-Мектеп конкурс диплом шаблоны")
    MUKAGALI_SCHOOL,
    @Schema(description = "Мукагали-Балабакша конкурс диплом шаблоны")
    MUKAGALI_BALSABAKSHA,
    @Schema(description = " Мукагали-Мектеп Алгыс конкурс диплом шаблоны")
    ALGYS_SCHOOL,
    @Schema(description = "Мукагали-Балабакша Алгыс конкурс диплом шаблоны")
    ALGYS_BALSABAKSHA,
    @Schema(description = "Кажмұқан конкурс диплом шаблоны")
    KAJMUKAN,
    BOYAULAR_ALGYS,
    BOYAULAR_DIPLOM,
    ARCHIVE
}
