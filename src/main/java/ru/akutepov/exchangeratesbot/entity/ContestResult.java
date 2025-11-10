package ru.akutepov.exchangeratesbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
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
    private String workDescription;
    private LocalDateTime sendDate;
    @Enumerated(EnumType.STRING)
    private ContestResult.ContestResultStatus status;

    public enum ContestResultStatus {
        NOT_PAID,
        PAID;
    }

    @ManyToOne
    @JoinColumn(name = "contest_id")
    private Contests contest;

    public ContestResult() {}

    // Getters и setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getmentor() { return mentor; }
    public void setMentor(String mentor) {
        this.mentor = mentor;
    }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getWorkDescription() { return workDescription; }
    public void setWorkDescription(String workDescription) { this.workDescription = workDescription; }

    public Contests getContest() { return contest; }
    public void setContest(Contests contest) { this.contest = contest; }

    public LocalDateTime getCreatedAt() { return sendDate; }
    public void setCreatedAt(LocalDateTime sendDate) { this.sendDate = sendDate; }
}



