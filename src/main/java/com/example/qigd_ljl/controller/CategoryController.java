package com.example.qigd_ljl.controller;

import com.example.qigd_ljl.annotation.OperationLog;
import com.example.qigd_ljl.common.Result;
import com.example.qigd_ljl.dto.category.CategoryRequest;
import com.example.qigd_ljl.dto.category.CategoryResponse;
import com.example.qigd_ljl.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "工单分类", description = "工单分类管理")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    @Operation(summary = "获取分类树")
    public Result<List<CategoryResponse>> getTree() {
        return Result.success(categoryService.list());
    }

    @GetMapping
    @Operation(summary = "获取所有分类（扁平列表）")
    public Result<List<CategoryResponse>> flatList() {
        return Result.success(categoryService.flatList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情")
    public Result<CategoryResponse> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建分类")
    @OperationLog(module = "分类管理", operation = "创建分类")
    public Result<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return Result.success(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    @OperationLog(module = "分类管理", operation = "更新分类")
    public Result<CategoryResponse> update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return Result.success(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    @OperationLog(module = "分类管理", operation = "删除分类")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}