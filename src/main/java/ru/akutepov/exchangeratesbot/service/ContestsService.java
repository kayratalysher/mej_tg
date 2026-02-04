package ru.akutepov.exchangeratesbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.akutepov.exchangeratesbot.entity.Contests;
import ru.akutepov.exchangeratesbot.repositry.ContestsRepository;
import ru.akutepov.exchangeratesbot.entity.ContestType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;



@Service
@RequiredArgsConstructor
public class ContestsService {
    private final ContestsRepository contestsRepository;

    public List<Contests> getActiveSchoolContests() {
        return contestsRepository.findAllByStatusAndContestType(
                Contests.ContestsStatus.ACTIVE,
                ContestType.MEKTEP_MAKATAEV
        );
    }

    // вариант если несколько школьных типов
    public List<Contests> getAllActiveSchool() {
        return contestsRepository.findAllByStatusAndContestTypeIn(
                Contests.ContestsStatus.ACTIVE,
                List.of(
                        ContestType.MEKTEP_MAKATAEV
                )
        );
    }

    public Contests getById(Long id) {
        return contestsRepository.findById(id).orElseThrow();
    }
}
