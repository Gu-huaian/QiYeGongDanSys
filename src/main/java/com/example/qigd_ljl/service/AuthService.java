package com.example.qigd_ljl.service;

import com.example.qigd_ljl.common.BusinessException;
import com.example.qigd_ljl.common.ErrorCode;
import com.example.qigd_ljl.dto.auth.LoginRequest;
import com.example.qigd_ljl.dto.auth.LoginResponse;
import com.example.qigd_ljl.entity.Permission;
import com.example.qigd_ljl.entity.Role;
import com.example.qigd_ljl.entity.User;
import com.example.qigd_ljl.repository.UserRepository;
import com.example.qigd_ljl.security.CustomUserDetails;
import com.example.qigd_ljl.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long expiration;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsernameWithRoles(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String token = jwtUtil.generateToken(userDetails.getId(), userDetails.getUsername());

        Set<String> roles = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            roles.add(role.getCode());
            for (Permission perm : role.getPermissions()) {
                permissions.add(perm.getCode());
            }
        }

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                roles,
                permissions,
                expiration / 1000
        );
    }
}