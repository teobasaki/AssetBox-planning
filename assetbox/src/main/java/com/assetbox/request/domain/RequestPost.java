package com.assetbox.request.domain;

import com.assetbox.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "requests")
public class RequestPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requesterId;
    private Long assigneeId;
    private Long linkedPostId;
    private Long referenceFileId;
    private String title;
    private String content;
    private String assetType;
    private String preferredStyle;
    private String engine;
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.REQUESTED;

    private boolean deleted;

    protected RequestPost() {
    }
}
