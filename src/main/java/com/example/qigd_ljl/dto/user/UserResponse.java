package com.example.qigd_ljl.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String department;
    private Boolean enabled;
    private Set<RoleSimpleResponse> roles;
    private UserSimpleRequest supervisor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}