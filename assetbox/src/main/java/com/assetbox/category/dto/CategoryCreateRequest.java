package com.assetbox.category.dto;

public record CategoryCreateRequest(String name, Long parentId, int sortOrder) {
}
