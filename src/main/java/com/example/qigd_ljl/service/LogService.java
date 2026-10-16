package com.example.qigd_ljl.service;

import com.example.qigd_ljl.common.PageResult;
import com.example.qigd_ljl.entity.OperationLog;
import com.example.qigd_ljl.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogService {

    private final OperationLogRepository logRepository;

    public PageResult<OperationLog> search(String username, String module, String operation,
                                           Integer status, LocalDateTime startTime, LocalDateTime endTime,
                                           int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "createdAt"
        );
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OperationLog> result = logRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (StringUtils.hasText(username)) {
                predicates.add(cb.like(
                        cb.lower(root.get("user").get("username")),
                        "%" + username.toLowerCase() + "%"
                ));
            }
            if (StringUtils.hasText(module)) {
                predicates.add(cb.like(root.get("module"), "%" + module + "%"));
            }
            if (StringUtils.hasText(operation)) {
                predicates.add(cb.like(root.get("operation"), "%" + operation + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);

        return PageResult.of(result.getContent(), result.getTotalElements(), result.getTotalPages(),
                result.getNumber(), result.getSize());
    }
}