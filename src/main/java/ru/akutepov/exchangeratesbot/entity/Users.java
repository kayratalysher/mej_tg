package ru.akutepov.exchangeratesbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String phone;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fio;
    private LocalDateTime created;
    private LocalDateTime lastSession;
    private Long chatId;
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    public enum UserStatus {
        ACTIVE,
        BLOCKED,
        DELETED;
    }
}
