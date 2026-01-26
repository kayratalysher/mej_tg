package ru.akutepov.exchangeratesbot.diplom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiplomRepo extends JpaRepository<DiplomId,Long> {
}
