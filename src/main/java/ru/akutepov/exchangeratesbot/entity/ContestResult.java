package ru.akutepov.exchangeratesbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contest_results")
public class ContestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;
    private String fullName;     // Аты-жөні
    private String grade;        // Сыныбы
    private String phone;        // Ұялы телефон
    private String mentor;       // Жетекшісі
    private String school;       // Мектебі
    private Integer channelMessageId;
    private String workFileName;
    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;
    private LocalDateTime certificateNotifyAt;

    @Column(name = "diploma_category")
    private Integer diplomaCategory; // 1, 2 или 3

    @Column(name = "work_file_id")
    private String workFileId; // новое поле для файла

    private LocalDateTime createdAt;
    public ParticipantStatus getStatus() {
        return status;
    }


    @ManyToOne
    @JoinColumn(name = "contest_id")
    private Contests contest;

    public void setStatus(ParticipantStatus status) {
        this.status = status;
    }

    // Если хочешь, можешь добавить поле для описания работы
    // private String workDescription;

}
