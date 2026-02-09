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

    public List<Contests> getActiveKindergartenContests() {
        return contestsRepository.findAllByStatusAndContestType(
                Contests.ContestsStatus.ACTIVE,
                ContestType.BALABAKSHA_MAKATAEV
        );
    }

    public List<Contests> getActiveOthersContests() {
        return contestsRepository.findAllByStatusAndContestType(
                Contests.ContestsStatus.ACTIVE,
                ContestType.OTHER
        );
    }

    public Contests getById(Long id) {
        return contestsRepository.findById(id).orElseThrow();
    }
}
