package com.ijspast.journal.service;

import com.ijspast.journal.exception.BadRequestException;
import com.ijspast.journal.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create upload directory", ex);
            throw new BadRequestException("Could not create the directory where uploaded files will be stored.");
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot store an empty file.");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.pdf");
        if (originalFileName.contains("..")) {
            throw new BadRequestException("Filename contains invalid path sequence: " + originalFileName);
        }

        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }

        String storedFileName = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return storedFileName;
        } catch (IOException ex) {
            log.error("Failed to store file {}", originalFileName, ex);
            throw new BadRequestException("Could not store file " + originalFileName + ". Please try again.");
        }
    }

    @Override
    public String storeFile(byte[] bytes, String originalFileName) {
        String cleanName = StringUtils.cleanPath(originalFileName != null ? originalFileName : "document.pdf");
        String extension = "";
        int i = cleanName.lastIndexOf('.');
        if (i > 0) {
            extension = cleanName.substring(i);
        }

        String storedFileName = UUID.randomUUID().toString() + extension;
        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.write(targetLocation, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return storedFileName;
        } catch (IOException ex) {
            log.error("Failed to store bytes for {}", cleanName, ex);
            throw new BadRequestException("Could not store file " + cleanName);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileKey) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileKey).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or unreadable: " + fileKey);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File path is invalid: " + fileKey);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileKey).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Could not delete file: {}", fileKey);
        }
    }
}
