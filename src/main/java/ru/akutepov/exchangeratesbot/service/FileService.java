package ru.akutepov.exchangeratesbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.akutepov.exchangeratesbot.adapter.MinioAdapter;
import ru.akutepov.exchangeratesbot.entity.FileEntity;
import ru.akutepov.exchangeratesbot.model.FileDTO;
import ru.akutepov.exchangeratesbot.model.FilterRequest;
import ru.akutepov.exchangeratesbot.repositry.FileRepository;
import ru.akutepov.exchangeratesbot.specification.FileSpecification;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class FileService {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final FileRepository fileRepository;
    private final MinioAdapter minioAdapter;


    public Page<FileDTO> search(FilterRequest filter,Pageable pageable) {
        Specification<FileEntity> specification = FileSpecification.search(filter.getFilters());
        return fileRepository.findAll(specification, pageable)
                .map(this::entityToDto);
    }

    public UUID saveFile(String fileJson, List<MultipartFile> files, HttpServletRequest request) {
        FileDTO fileDTO;
        try {
            fileDTO = mapper.readValue(fileJson, FileDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to DTO", e);
        }

        for (MultipartFile file : files) {
            String originalName = fileDTO.getFilename() != null ? fileDTO.getFilename() : file.getOriginalFilename();
            LocalDateTime now = LocalDateTime.now();
            String storedName = now + "_" + file.getOriginalFilename();
            String fileType = getFileExtension(file.getOriginalFilename());

            FileEntity entity = FileEntity.builder()
                    .filename(originalName)
                    .storedName(storedName)
                    .mimeType(file.getContentType())
                    .fileType(fileType)
                    .fileSize(file.getSize())
                    .comment(fileDTO.getComment())
                    .isDeleted(false)
                    .build();

            entity.setUploadedAt(now);

            FileEntity savedEntity = fileRepository.save(entity);

            savedEntity.setObjectId(savedEntity.getId());
            savedEntity.setObjectType(savedEntity.getFileType());

            minioAdapter.uploadFile(file, storedName, savedEntity.getId(), savedEntity.getFileType());

          return  fileRepository.save(savedEntity).getId();
        }

        return null;
    }

    public void updateFileData(UUID id, FileDTO fileDTO) {

        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found with id" + id));

        file.setFilename(fileDTO.getFilename());
        file.setComment(fileDTO.getComment());
        file.setUploadedAt(LocalDateTime.now());
        fileRepository.save(file);
    }

    public void deleteFile(UUID id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found with id" + id));
        file.setIsDeleted(true);
        file.setDeletedAt(LocalDateTime.now());
        fileRepository.save(file);
    }

    public byte[] downloadFileBytes(UUID id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found with id" + id));

       return minioAdapter.downloadFile(file.getStoredName());

    }

    public String downloadFileBase64(UUID id) {
        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found with id" + id));

        byte[] imageBytes = minioAdapter.downloadFile(file.getStoredName());
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public List<String> findDistinctFileTypes() {
        return fileRepository.findDistinctFileTypes();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1 || filename.lastIndexOf('.') == 0) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public FileDTO entityToDto(FileEntity fileEntity) {
        return FileDTO.builder()
                .id(fileEntity.getId())
                .filename(fileEntity.getFilename())
                .storedName(fileEntity.getStoredName())
                .fileType(fileEntity.getFileType())
                .mimeType(fileEntity.getMimeType())
                .fileSize(fileEntity.getFileSize())
                .uploadedAt(fileEntity.getUploadedAt())
                .objectType(fileEntity.getObjectType())
                .objectId(fileEntity.getObjectId())
                .comment(fileEntity.getComment())
                .build();
    }
}
