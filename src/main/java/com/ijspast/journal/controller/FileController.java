package com.ijspast.journal.controller;

import com.ijspast.journal.entity.ManuscriptFile;
import com.ijspast.journal.service.FileStorageService;
import com.ijspast.journal.service.ManuscriptService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final ManuscriptService manuscriptService;
    private final FileStorageService fileStorageService;

    public FileController(ManuscriptService manuscriptService, FileStorageService fileStorageService) {
        this.manuscriptService = manuscriptService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        ManuscriptFile file = manuscriptService.getAuthorizedFile(fileId);
        Resource resource = fileStorageService.loadFileAsResource(file.getFileKey());

        String contentType = file.getType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
}
