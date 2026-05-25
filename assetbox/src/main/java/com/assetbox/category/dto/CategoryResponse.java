package com.assetbox.category.dto;

public record CategoryResponse(Long id, String name, Long parentId, int depth, int sortOrder) {
}
