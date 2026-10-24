package com.ijspast.journal.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file);

    String storeFile(byte[] bytes, String originalFileName);

    Resource loadFileAsResource(String fileKey);

    void deleteFile(String fileKey);
}
