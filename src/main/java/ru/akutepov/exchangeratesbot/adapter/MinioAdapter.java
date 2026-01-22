package ru.akutepov.exchangeratesbot.adapter;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class MinioAdapter {
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucket;

    public MinioAdapter(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void uploadFile(MultipartFile file, String filename, UUID objectId, String objectType) {
        try (InputStream inputStream = file.getInputStream()) {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(filename)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .userMetadata(Map.of(
                                    "object-id", String.valueOf(objectId),
                                    "object-type", objectType
                            ))
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error during uploading file: " + file.getOriginalFilename(), e);
        }
    }

    public byte[] downloadFile(String filename) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(filename).build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file", e);
        }
    }
}
