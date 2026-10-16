package com.example.qigd_ljl.service;

import com.example.qigd_ljl.common.BusinessException;
import com.example.qigd_ljl.common.ErrorCode;
import com.example.qigd_ljl.converter.RoleConverter;
import com.example.qigd_ljl.dto.role.PermissionResponse;
import com.example.qigd_ljl.dto.role.RoleRequest;
import com.example.qigd_ljl.dto.role.RoleResponse;
import com.example.qigd_ljl.entity.Permission;
import com.example.qigd_ljl.entity.Role;
import com.example.qigd_ljl.repository.PermissionRepository;
import com.example.qigd_ljl.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleResponse> list() {
        return roleRepository.findAll().stream()
                .map(RoleConverter::toResponse)
                .collect(Collectors.toList());
    }

    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream()
                .map(RoleConverter::toPermissionResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
        return RoleConverter.toResponse(role);
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException("角色编码已存在");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setDescription(request.getDescription());
        role.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            role.setPermissions(permissions);
        }

        Role saved = roleRepository.save(role);
        return RoleConverter.toResponse(saved);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getCode() != null && !request.getCode().equals(role.getCode())) {
            if (roleRepository.existsByCode(request.getCode())) {
                throw new BusinessException("角色编码已存在");
            }
            role.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getEnabled() != null) {
            role.setEnabled(request.getEnabled());
        }
        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
            role.setPermissions(permissions);
        }

        Role saved = roleRepository.save(role);
        return RoleConverter.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        roleRepository.deleteById(id);
    }
}