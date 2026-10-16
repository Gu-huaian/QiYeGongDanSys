package com.example.qigd_ljl.service;

import com.example.qigd_ljl.common.BusinessException;
import com.example.qigd_ljl.common.ErrorCode;
import com.example.qigd_ljl.converter.TicketConverter;
import com.example.qigd_ljl.dto.category.CategoryRequest;
import com.example.qigd_ljl.dto.category.CategoryResponse;
import com.example.qigd_ljl.entity.TicketCategory;
import com.example.qigd_ljl.repository.TicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final TicketCategoryRepository categoryRepository;

    public List<CategoryResponse> list() {
        List<TicketCategory> categories = categoryRepository.findAll();
        return buildTree(categories);
    }

    public List<CategoryResponse> flatList() {
        return categoryRepository.findAll().stream()
                .map(TicketConverter::toCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getById(Long id) {
        TicketCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return TicketConverter.toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("分类名称已存在");
        }

        TicketCategory category = new TicketCategory();
        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setDescription(request.getDescription());
        category.setParentId(request.getParentId());
        category.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        TicketCategory saved = categoryRepository.save(category);
        return TicketConverter.toCategoryResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        TicketCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getCode() != null) {
            category.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }
        if (request.getEnabled() != null) {
            category.setEnabled(request.getEnabled());
        }

        TicketCategory saved = categoryRepository.save(category);
        return TicketConverter.toCategoryResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }

    private List<CategoryResponse> buildTree(List<TicketCategory> categories) {
        Map<Long, CategoryResponse> responseMap = categories.stream()
                .collect(Collectors.toMap(
                        TicketCategory::getId,
                        TicketConverter::toCategoryResponse
                ));

        List<CategoryResponse> roots = new ArrayList<>();
        for (CategoryResponse resp : responseMap.values()) {
            if (resp.getParentId() == null) {
                roots.add(resp);
            } else {
                CategoryResponse parent = responseMap.get(resp.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(resp);
                } else {
                    roots.add(resp);
                }
            }
        }
        return roots;
    }
}