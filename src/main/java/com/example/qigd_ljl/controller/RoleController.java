package com.example.qigd_ljl.controller;

import com.example.qigd_ljl.annotation.OperationLog;
import com.example.qigd_ljl.common.Result;
import com.example.qigd_ljl.dto.role.PermissionResponse;
import com.example.qigd_ljl.dto.role.RoleRequest;
import com.example.qigd_ljl.dto.role.RoleResponse;
import com.example.qigd_ljl.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "角色权限管理", description = "角色和权限的增删改查")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "获取所有角色")
    public Result<List<RoleResponse>> listRoles() {
        return Result.success(roleService.list());
    }

    @GetMapping("/permissions")
    @Operation(summary = "获取所有权限")
    public Result<List<PermissionResponse>> listPermissions() {
        return Result.success(roleService.listPermissions());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    public Result<RoleResponse> getRole(@PathVariable Long id) {
        return Result.success(roleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建角色")
    @OperationLog(module = "角色管理", operation = "创建角色")
    public Result<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return Result.success(roleService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @OperationLog(module = "角色管理", operation = "更新角色")
    public Result<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return Result.success(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @OperationLog(module = "角色管理", operation = "删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.delete(id);
        return Result.success();
    }
}