package com.example.qigd_ljl.converter;

import com.example.qigd_ljl.dto.role.PermissionResponse;
import com.example.qigd_ljl.dto.role.RoleResponse;
import com.example.qigd_ljl.entity.Permission;
import com.example.qigd_ljl.entity.Role;

import java.util.stream.Collectors;

public class RoleConverter {

    public static RoleResponse toResponse(Role role) {
        if (role == null) return null;

        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setCode(role.getCode());
        response.setDescription(role.getDescription());
        response.setEnabled(role.getEnabled());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());

        if (role.getPermissions() != null) {
            response.setPermissions(role.getPermissions().stream()
                    .map(RoleConverter::toPermissionResponse)
                    .collect(Collectors.toSet()));
        }

        return response;
    }

    public static PermissionResponse toPermissionResponse(Permission permission) {
        if (permission == null) return null;
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getModule(),
                permission.getDescription()
        );
    }
}