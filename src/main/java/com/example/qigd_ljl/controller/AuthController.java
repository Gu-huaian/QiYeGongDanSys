package com.example.qigd_ljl.controller;

import com.example.qigd_ljl.common.Result;
import com.example.qigd_ljl.dto.auth.LoginRequest;
import com.example.qigd_ljl.dto.auth.LoginResponse;
import com.example.qigd_ljl.security.CustomUserDetails;
import com.example.qigd_ljl.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、获取当前用户信息等")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名和密码登录，返回 JWT Token")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", userDetails.getId());
        data.put("username", userDetails.getUsername());
        data.put("realName", userDetails.getRealName());
        data.put("roles", userDetails.getAuthorities());
        return Result.success(data);
    }
}