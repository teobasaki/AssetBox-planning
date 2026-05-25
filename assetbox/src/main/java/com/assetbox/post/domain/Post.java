package com.assetbox.post.domain;

import com.assetbox.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "posts")
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long authorId;
    private Long categoryId;
    private Long linkedRequestId;
    private String title;
    private String content;
    private long viewCount;
    private long likeCount;
    private boolean deleted;

    protected Post() {
    }

    public Long getId() {
        return id;
    }
}
