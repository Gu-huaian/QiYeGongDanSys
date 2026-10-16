package com.example.qigd_ljl.service;

import com.example.qigd_ljl.common.BusinessException;
import com.example.qigd_ljl.common.ErrorCode;
import com.example.qigd_ljl.converter.TicketConverter;
import com.example.qigd_ljl.dto.priority.PriorityRequest;
import com.example.qigd_ljl.dto.priority.PriorityResponse;
import com.example.qigd_ljl.entity.TicketPriority;
import com.example.qigd_ljl.repository.TicketPriorityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriorityService {

    private final TicketPriorityRepository priorityRepository;

    public List<PriorityResponse> list() {
        return priorityRepository.findByEnabledTrueOrderByLevelAsc().stream()
                .map(TicketConverter::toPriorityResponse)
                .collect(Collectors.toList());
    }

    public List<PriorityResponse> listAll() {
        return priorityRepository.findAll().stream()
                .map(TicketConverter::toPriorityResponse)
                .collect(Collectors.toList());
    }

    public PriorityResponse getById(Long id) {
        TicketPriority priority = priorityRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRIORITY_NOT_FOUND));
        return TicketConverter.toPriorityResponse(priority);
    }

    @Transactional
    public PriorityResponse create(PriorityRequest request) {
        if (priorityRepository.existsByCode(request.getCode())) {
            throw new BusinessException("优先级编码已存在");
        }

        TicketPriority priority = new TicketPriority();
        priority.setName(request.getName());
        priority.setCode(request.getCode());
        priority.setLevel(request.getLevel());
        priority.setColor(request.getColor());
        priority.setDescription(request.getDescription());
        priority.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        TicketPriority saved = priorityRepository.save(priority);
        return TicketConverter.toPriorityResponse(saved);
    }

    @Transactional
    public PriorityResponse update(Long id, PriorityRequest request) {
        TicketPriority priority = priorityRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRIORITY_NOT_FOUND));

        if (request.getName() != null) {
            priority.setName(request.getName());
        }
        if (request.getCode() != null && !request.getCode().equals(priority.getCode())) {
            if (priorityRepository.existsByCode(request.getCode())) {
                throw new BusinessException("优先级编码已存在");
            }
            priority.setCode(request.getCode());
        }
        if (request.getLevel() != null) {
            priority.setLevel(request.getLevel());
        }
        if (request.getColor() != null) {
            priority.setColor(request.getColor());
        }
        if (request.getDescription() != null) {
            priority.setDescription(request.getDescription());
        }
        if (request.getEnabled() != null) {
            priority.setEnabled(request.getEnabled());
        }

        TicketPriority saved = priorityRepository.save(priority);
        return TicketConverter.toPriorityResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!priorityRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PRIORITY_NOT_FOUND);
        }
        priorityRepository.deleteById(id);
    }
}