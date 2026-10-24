package com.ijspast.journal.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ManuscriptSubmissionRequest {

    private String id;

    private String manuscriptId;

    @Valid
    @NotNull(message = "Author information is required")
    private AuthorInfoRequest author;

    @NotBlank(message = "Manuscript title is required")
    private String title;

    @NotBlank(message = "Subject domain is required")
    private String domain;

    @NotBlank(message = "Abstract is required")
    private String abstractText;

    @NotBlank(message = "Keywords are required")
    private String keywords;

    private String coauthors;

    private FileMetadataRequest file;

    private boolean declaration = true;

    public ManuscriptSubmissionRequest() {
    }

    public String getId() {
        if (id != null && !id.isBlank()) {
            return id.trim();
        }
        if (manuscriptId != null && !manuscriptId.isBlank()) {
            return manuscriptId.trim();
        }
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getManuscriptId() {
        return getId();
    }

    public void setManuscriptId(String manuscriptId) {
        this.manuscriptId = manuscriptId;
    }

    public AuthorInfoRequest getAuthor() {
        return author;
    }

    public void setAuthor(AuthorInfoRequest author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getCoauthors() {
        return coauthors;
    }

    public void setCoauthors(String coauthors) {
        this.coauthors = coauthors;
    }

    public FileMetadataRequest getFile() {
        return file;
    }

    public void setFile(FileMetadataRequest file) {
        this.file = file;
    }

    public boolean isDeclaration() {
        return declaration;
    }

    public void setDeclaration(boolean declaration) {
        this.declaration = declaration;
    }
}
