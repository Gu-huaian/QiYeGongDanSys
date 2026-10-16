package com.example.qigd_ljl.controller;

import com.example.qigd_ljl.common.PageResult;
import com.example.qigd_ljl.common.Result;
import com.example.qigd_ljl.entity.OperationLog;
import com.example.qigd_ljl.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "系统操作日志查询")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class LogController {

    private final LogService logService;

    @GetMapping
    @Operation(summary = "分页查询操作日志")
    public Result<PageResult<OperationLog>> searchLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return Result.success(logService.search(username, module, operation, status, startTime, endTime,
                page, size, sortBy, sortDirection));
    }
}