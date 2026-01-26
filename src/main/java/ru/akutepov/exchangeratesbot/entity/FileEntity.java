package ru.akutepov.exchangeratesbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_storage")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String filename;

    private String storedName;

    private String fileType;

    private String mimeType;

    private Long fileSize;

    private String uploadedByName;

    @Column(columnDefinition = "text")
    private String comment;

    private String objectType;

    private UUID objectId;
    private LocalDateTime uploadedAt;
    private LocalDateTime deletedAt;


    private Boolean isDeleted = false;
}
