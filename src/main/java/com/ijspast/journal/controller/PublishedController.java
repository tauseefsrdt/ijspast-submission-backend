package com.ijspast.journal.controller;

import com.ijspast.journal.dto.response.ApiResponse;
import com.ijspast.journal.dto.response.ManuscriptResponseDTO;
import com.ijspast.journal.service.ManuscriptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/published")
public class PublishedController {

    private final ManuscriptService manuscriptService;

    public PublishedController(ManuscriptService manuscriptService) {
        this.manuscriptService = manuscriptService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ManuscriptResponseDTO>>> getPublishedPapers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String domain) {
        List<ManuscriptResponseDTO> papers = manuscriptService.getPublishedManuscripts(search, domain);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }
}
