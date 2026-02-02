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
public interface ContestResultRepository extends JpaRepository<ContestResult,Long> {
    Optional<ContestResult> findTopByChatIdOrderByCreatedAtDesc(Long chatId);

    List<ContestResult> findByStatus(ParticipantStatus participantStatus);

    List<ContestResult> findAllByStatusAndCertificateNotifyAtBefore(ParticipantStatus participantStatus, LocalDateTime now);
    List<ContestResult> findAllByStatusAndContestTypeAndCertificateNotifyAtBefore(ParticipantStatus participantStatus, ContestType contestType, LocalDateTime now);
}