package com.example.qigd_ljl.dto.ticket;

import com.example.qigd_ljl.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketQueryRequest {

    private String keyword;

    private TicketStatus status;

    private Long categoryId;

    private Long priorityId;

    private Long assigneeId;

    private Long creatorId;

    private Boolean overdue;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer page = 0;

    private Integer size = 10;

    private String sortBy = "createdAt";

    private String sortDirection = "desc";
}