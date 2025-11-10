package ru.akutepov.exchangeratesbot.repositry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.akutepov.exchangeratesbot.entity.Contests;

@Repository
public interface ContestsRepositroy extends JpaRepository<Contests,Long> {
}
