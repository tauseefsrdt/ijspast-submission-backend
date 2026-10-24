package com.ijspast.journal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.RevisionSubmissionRequest;
import com.ijspast.journal.dto.response.ApiResponse;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.service.ManuscriptService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/author")
public class AuthorController {

    private final ManuscriptService manuscriptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthorController(ManuscriptService manuscriptService) {
        this.manuscriptService = manuscriptService;
    }

    @GetMapping("/manuscripts")
    public ResponseEntity<ApiResponse<List<ManuscriptResponseDTO>>> getMyManuscripts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<ManuscriptResponseDTO> list = manuscriptService.getAuthorManuscripts(status, search);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/manuscripts/{id}")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> getManuscriptById(@PathVariable String id) {
        ManuscriptResponseDTO manuscript = manuscriptService.getAuthorManuscriptById(id);
        return ResponseEntity.ok(ApiResponse.success(manuscript));
    }

    @PostMapping("/manuscripts/{id}/revision")
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> submitRevisionJson(
            @PathVariable String id,
            @Valid @RequestBody RevisionSubmissionRequest request) {
        ManuscriptResponseDTO updated = manuscriptService.submitAuthorRevision(id, request, null);
        return ResponseEntity.ok(ApiResponse.success("Revision submitted successfully", updated));
    }

    @PostMapping(value = "/manuscripts/{id}/revision/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ManuscriptResponseDTO>> submitRevisionMultipart(
            @PathVariable String id,
            @RequestParam("response") String authorResponse,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        RevisionSubmissionRequest req = new RevisionSubmissionRequest();
        req.setResponse(authorResponse);
        ManuscriptResponseDTO updated = manuscriptService.submitAuthorRevision(id, req, file);
        return ResponseEntity.ok(ApiResponse.success("Revision submitted successfully with document upload", updated));
    }
}
