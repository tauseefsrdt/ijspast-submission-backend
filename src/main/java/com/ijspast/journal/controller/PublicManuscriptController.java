package com.ijspast.journal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ijspast.journal.dto.request.AuthorInfoRequest;
import com.ijspast.journal.dto.request.FileMetadataRequest;
import com.ijspast.journal.dto.request.ManuscriptSubmissionRequest;
import com.ijspast.journal.dto.response.ApiResponse;
import com.ijspast.journal.dto.response.SubmissionResponseDTO;
import com.ijspast.journal.service.ManuscriptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class PublicManuscriptController {

    private final ManuscriptService manuscriptService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PublicManuscriptController(ManuscriptService manuscriptService) {
        this.manuscriptService = manuscriptService;
    }

    @PostMapping("/manuscripts")
    public ResponseEntity<ApiResponse<SubmissionResponseDTO>> submitManuscriptJson(
            @RequestParam(value = "manuscriptId", required = false) String manuscriptIdParam,
            @Valid @RequestBody ManuscriptSubmissionRequest request) {
        if (manuscriptIdParam != null && !manuscriptIdParam.isBlank() && request.getId() == null) {
            request.setId(manuscriptIdParam.trim());
        }
        SubmissionResponseDTO response = manuscriptService.submitManuscript(request, null);
        HttpStatus status = response.isResubmission() ? HttpStatus.OK : HttpStatus.CREATED;
        String message = response.isResubmission()
                ? "Existing manuscript updated successfully with corrections"
                : "Manuscript submitted successfully";
        return ResponseEntity.status(status)
                .body(ApiResponse.success(message, response));
    }

    @PutMapping("/manuscripts/{id}")
    public ResponseEntity<ApiResponse<SubmissionResponseDTO>> updateManuscriptJson(
            @PathVariable String id,
            @Valid @RequestBody ManuscriptSubmissionRequest request) {
        request.setId(id);
        SubmissionResponseDTO response = manuscriptService.submitManuscript(request, null);
        return ResponseEntity.ok(ApiResponse.success("Existing manuscript updated successfully with corrections", response));
    }

    @PostMapping(value = "/public/manuscripts/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SubmissionResponseDTO>> submitManuscriptMultipart(
            @RequestParam(value = "manuscriptId", required = false) String manuscriptIdParam,
            @RequestParam("payload") String payloadJson,
            @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {
        ManuscriptSubmissionRequest request = objectMapper.readValue(payloadJson, ManuscriptSubmissionRequest.class);
        if (manuscriptIdParam != null && !manuscriptIdParam.isBlank() && request.getId() == null) {
            request.setId(manuscriptIdParam.trim());
        }
        SubmissionResponseDTO response = manuscriptService.submitManuscript(request, file);
        HttpStatus status = response.isResubmission() ? HttpStatus.OK : HttpStatus.CREATED;
        String message = response.isResubmission()
                ? "Existing manuscript updated successfully with corrections"
                : "Manuscript submitted successfully";
        return ResponseEntity.status(status)
                .body(ApiResponse.success(message, response));
    }
}
