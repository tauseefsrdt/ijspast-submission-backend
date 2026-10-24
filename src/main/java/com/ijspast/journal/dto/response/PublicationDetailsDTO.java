package com.ijspast.journal.dto.response;

public class PublicationDetailsDTO {

    private String doi;
    private String volume;
    private String issue;
    private String publishedDate;
    private String pages;

    public PublicationDetailsDTO() {
    }

    public PublicationDetailsDTO(String doi, String volume, String issue, String publishedDate) {
        this.doi = doi;
        this.volume = volume;
        this.issue = issue;
        this.publishedDate = publishedDate;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }
}
