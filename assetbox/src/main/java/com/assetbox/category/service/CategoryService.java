package com.assetbox.category.service;

import com.assetbox.category.domain.Category;
import com.assetbox.category.dto.CategoryCreateRequest;
import com.assetbox.category.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

    Category requireExists(Long id);

    List<CategoryResponse> roots();

    List<CategoryResponse> children(Long parentId);

    CategoryResponse create(CategoryCreateRequest request);

    CategoryResponse rename(Long id, String name);

    CategoryResponse reorder(Long id, int sortOrder);

    void delete(Long id);
}
