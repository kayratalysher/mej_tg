package ru.akutepov.exchangeratesbot.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.akutepov.exchangeratesbot.model.FileDTO;
import ru.akutepov.exchangeratesbot.model.FilterRequest;
import ru.akutepov.exchangeratesbot.service.FileService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping
    public Page<FileDTO> search(@RequestBody(required = false) FilterRequest filterRequest,
                                @PageableDefault Pageable page) {
        return fileService.search(filterRequest, page);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UUID saveFile(@RequestPart String fileJson, @RequestPart("file") List<MultipartFile> files) {
       return fileService.saveFile(fileJson, files);
    }

    @PutMapping("/{id}")
    public void updateFileData(@PathVariable("id") UUID id, @RequestBody FileDTO fileDTO) {
        fileService.updateFileData(id, fileDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteFile(@PathVariable("id") UUID id) {
        fileService.deleteFile(id);
    }

    @GetMapping("/download/{id}")
    public String downloadFile(@PathVariable UUID id) {
        return fileService.downloadFileBase64(id);
    }

    @GetMapping("/types")
    public List<String> getFileTypes() {
        return fileService.findDistinctFileTypes();
    }

}
