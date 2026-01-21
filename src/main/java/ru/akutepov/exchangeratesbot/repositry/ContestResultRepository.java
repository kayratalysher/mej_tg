package ru.akutepov.exchangeratesbot.repositry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.akutepov.exchangeratesbot.entity.ContestResult;
import ru.akutepov.exchangeratesbot.entity.Contests;
import java.util.Optional;

@Repository
public interface ContestResultRepository extends JpaRepository<ContestResult,Long> {
    Optional<ContestResult> findTopByChatIdOrderByCreatedAtDesc(Long chatId);
}