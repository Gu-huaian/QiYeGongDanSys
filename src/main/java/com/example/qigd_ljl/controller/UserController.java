package com.example.qigd_ljl.controller;

import com.example.qigd_ljl.annotation.OperationLog;
import com.example.qigd_ljl.common.PageResult;
import com.example.qigd_ljl.common.Result;
import com.example.qigd_ljl.dto.user.UserCreateRequest;
import com.example.qigd_ljl.dto.user.UserUpdateRequest;
import com.example.qigd_ljl.dto.user.UserResponse;
import com.example.qigd_ljl.dto.user.UserSimpleRequest;
import com.example.qigd_ljl.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户增删改查、密码修改等")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "分页查询用户列表")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Result<PageResult<UserResponse>> pageUsers(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        return Result.success(userService.page(keyword, page, size));
    }

    @GetMapping("/all")
    @Operation(summary = "获取所有用户（下拉选择用）")
    public Result<List<UserResponse>> listAllUsers() {
        return Result.success(userService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public Result<UserResponse> getUser(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @GetMapping("/{id}/subordinates")
    @Operation(summary = "获取直属下级列表")
    public Result<List<UserSimpleRequest>> getSubordinates(@PathVariable Long id) {
        return Result.success(userService.listSubordinates(id));
    }

    @PostMapping
    @Operation(summary = "创建用户")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "用户管理", operation = "创建用户")
    public Result<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "用户管理", operation = "更新用户")
    public Result<UserResponse> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    @OperationLog(module = "用户管理", operation = "删除用户")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "修改密码")
    @OperationLog(module = "用户管理", operation = "修改密码")
    public Result<Void> changePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.changePassword(id, body.get("oldPassword"), body.get("newPassword"));
        return Result.success();
    }
}