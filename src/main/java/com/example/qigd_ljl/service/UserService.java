package com.example.qigd_ljl.service;

import com.example.qigd_ljl.common.BusinessException;
import com.example.qigd_ljl.common.ErrorCode;
import com.example.qigd_ljl.common.PageResult;
import com.example.qigd_ljl.converter.UserConverter;
import com.example.qigd_ljl.dto.user.UserCreateRequest;
import com.example.qigd_ljl.dto.user.UserUpdateRequest;
import com.example.qigd_ljl.dto.user.UserResponse;
import com.example.qigd_ljl.dto.user.UserSimpleRequest;
import com.example.qigd_ljl.entity.Role;
import com.example.qigd_ljl.entity.User;
import com.example.qigd_ljl.repository.RoleRepository;
import com.example.qigd_ljl.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserResponse> page(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            String like = "%" + keyword + "%";
            return cb.or(
                    cb.like(root.get("username"), like),
                    cb.like(root.get("realName"), like),
                    cb.like(root.get("email"), like)
            );
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        Page<UserResponse> responsePage = userPage.map(UserConverter::toResponse);
        return PageResult.of(responsePage);
    }

    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(UserConverter::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserConverter.toResponse(user);
    }

    public List<UserSimpleRequest> listSubordinates(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findBySupervisorId(id).stream()
                .map(u -> new UserSimpleRequest(
                        u.getId(), u.getUsername(), u.getRealName(), u.getDepartment()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setEnabled(true);

        if (request.getSupervisorId() != null) {
            user.setSupervisor(userRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));
        }

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }

        User saved = userRepository.save(user);
        return UserConverter.toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getSupervisorId() != null) {
            user.setSupervisor(userRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND)));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }

        User saved = userRepository.save(user);
        return UserConverter.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}