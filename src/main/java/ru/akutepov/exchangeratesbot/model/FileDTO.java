package ru.akutepov.exchangeratesbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDTO {

    private UUID id;
    private String filename;
    private String storedName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String objectType;
    private UUID objectId;
    private String comment;
}
