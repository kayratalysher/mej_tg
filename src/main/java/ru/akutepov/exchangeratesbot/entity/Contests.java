package ru.akutepov.exchangeratesbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contests {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String imageUrl;        // URL или путь к афише
    private String fileUrl;         // URL или путь к файлу (положение)
    private BigDecimal price;
    private LocalDate startDate;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    private ContestsStatus status;
    private ContestType contestType;
    public enum ContestsStatus {
        ACTIVE,
        INACTIVE;
    }
}
