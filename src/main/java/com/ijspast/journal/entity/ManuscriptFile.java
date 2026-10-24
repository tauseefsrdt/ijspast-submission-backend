package com.ijspast.journal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manuscript_files")
public class ManuscriptFile {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = 128)
    private String type;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "is_revised", nullable = false)
    private boolean isRevised = false;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    public ManuscriptFile() {
    }

    public ManuscriptFile(String id, String name, long size, String type, String fileKey, int version, boolean isRevised) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.type = type;
        this.fileKey = fileKey;
        this.version = version;
        this.isRevised = isRevised;
    }

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Manuscript getManuscript() {
        return manuscript;
    }

    public void setManuscript(Manuscript manuscript) {
        this.manuscript = manuscript;
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

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isRevised() {
        return isRevised;
    }

    public void setRevised(boolean revised) {
        isRevised = revised;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
