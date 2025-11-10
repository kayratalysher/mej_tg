package ru.akutepov.exchangeratesbot.repositry;

import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.akutepov.exchangeratesbot.entity.Users;

import java.util.Optional;

@Repository
public interface UsersRepositroy extends JpaRepository<Users,Long> {
    Optional<Users> findByUsername(String username);
}
