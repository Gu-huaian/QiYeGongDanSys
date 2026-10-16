package com.example.qigd_ljl.dto.user;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    private String password;

    private String realName;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String department;

    private Long supervisorId;

    private Boolean enabled;

    private Set<Long> roleIds;
}