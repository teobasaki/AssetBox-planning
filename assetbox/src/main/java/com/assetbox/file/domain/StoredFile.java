package com.assetbox.file.domain;

import com.assetbox.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class StoredFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FilePurpose purpose;

    private Long ownerId;
    private Long uploadedBy;
    private String originalName;
    private String storedPath;
    private String extension;
    private String contentType;
    private long sizeBytes;
    private boolean deleted;

    protected StoredFile() {
    }

    public Long getId() {
        return id;
    }
}
