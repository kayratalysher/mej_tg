package ru.akutepov.exchangeratesbot.diplom;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "diplom")
@NoArgsConstructor
@Getter
public class DiplomId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID diplomId;
    private String fullName;
    private LocalDateTime createdDate;


    public DiplomId(String fullName) {
        this.createdDate = LocalDateTime.now();
        this.fullName = fullName;
    }
}
