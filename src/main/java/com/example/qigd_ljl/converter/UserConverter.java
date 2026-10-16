package com.example.qigd_ljl.converter;

import com.example.qigd_ljl.dto.user.RoleSimpleResponse;
import com.example.qigd_ljl.dto.user.UserResponse;
import com.example.qigd_ljl.dto.user.UserSimpleRequest;
import com.example.qigd_ljl.entity.Role;
import com.example.qigd_ljl.entity.User;

import java.util.stream.Collectors;

public class UserConverter {

    public static UserResponse toResponse(User user) {
        if (user == null) return null;

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setDepartment(user.getDepartment());
        response.setEnabled(user.getEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        if (user.getRoles() != null) {
            response.setRoles(user.getRoles().stream()
                    .map(UserConverter::toRoleSimpleResponse)
                    .collect(Collectors.toSet()));
        }

        if (user.getSupervisor() != null) {
            User sup = user.getSupervisor();
            response.setSupervisor(new UserSimpleRequest(
                    sup.getId(), sup.getUsername(), sup.getRealName(), sup.getDepartment()));
        }

        return response;
    }

    public static RoleSimpleResponse toRoleSimpleResponse(Role role) {
        if (role == null) return null;
        return new RoleSimpleResponse(role.getId(), role.getName(), role.getCode());
    }
}