package com.example.qigd_ljl.controller;

import com.example.qigd_ljl.annotation.OperationLog;
import com.example.qigd_ljl.common.Result;
import com.example.qigd_ljl.dto.priority.PriorityRequest;
import com.example.qigd_ljl.dto.priority.PriorityResponse;
import com.example.qigd_ljl.service.PriorityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/priorities")
@RequiredArgsConstructor
@Tag(name = "工单优先级", description = "工单优先级管理")
public class PriorityController {

    private final PriorityService priorityService;

    @GetMapping
    @Operation(summary = "获取启用的优先级列表")
    public Result<List<PriorityResponse>> list() {
        return Result.success(priorityService.list());
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有优先级（含禁用）")
    public Result<List<PriorityResponse>> listAll() {
        return Result.success(priorityService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取优先级详情")
    public Result<PriorityResponse> getById(@PathVariable Long id) {
        return Result.success(priorityService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建优先级")
    @OperationLog(module = "优先级管理", operation = "创建优先级")
    public Result<PriorityResponse> create(@Valid @RequestBody PriorityRequest request) {
        return Result.success(priorityService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新优先级")
    @OperationLog(module = "优先级管理", operation = "更新优先级")
    public Result<PriorityResponse> update(@PathVariable Long id, @Valid @RequestBody PriorityRequest request) {
        return Result.success(priorityService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除优先级")
    @OperationLog(module = "优先级管理", operation = "删除优先级")
    public Result<Void> delete(@PathVariable Long id) {
        priorityService.delete(id);
        return Result.success();
    }
}