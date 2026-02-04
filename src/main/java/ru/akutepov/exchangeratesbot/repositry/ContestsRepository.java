package ru.akutepov.exchangeratesbot.repositry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.akutepov.exchangeratesbot.entity.ContestResult;
import ru.akutepov.exchangeratesbot.entity.ContestType;
import ru.akutepov.exchangeratesbot.entity.Contests;
import ru.akutepov.exchangeratesbot.entity.ParticipantStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContestsRepository extends JpaRepository<Contests, Long> {

    List<Contests> findAllByStatusAndContestType(
            Contests.ContestsStatus status,
            ContestType contestType
    );

    // если будет несколько типов школ
    List<Contests> findAllByStatusAndContestTypeIn(
            Contests.ContestsStatus status,
            List<ContestType> contestTypes
    );
}