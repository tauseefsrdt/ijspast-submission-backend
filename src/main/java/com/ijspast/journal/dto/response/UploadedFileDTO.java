package com.ijspast.journal.dto.response;

public class UploadedFileDTO {

    private String name;
    private long size;
    private String type;
    private String uploadedAt;
    private String url;
    private Integer version;

    public UploadedFileDTO() {
    }

    public UploadedFileDTO(String name, long size, String type, String uploadedAt, String url, Integer version) {
        this.name = name;
        this.size = size;
        this.type = type;
        this.uploadedAt = uploadedAt;
        this.url = url;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(String uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
