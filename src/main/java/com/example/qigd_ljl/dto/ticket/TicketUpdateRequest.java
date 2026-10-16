package com.example.qigd_ljl.dto.ticket;

import com.example.qigd_ljl.entity.AssignmentMode;
import com.example.qigd_ljl.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketUpdateRequest {

    private String title;

    private String description;

    private Long categoryId;

    private Long priorityId;

    private TicketStatus status;

    private Long assigneeId;

    private AssignmentMode assignmentMode;

    private LocalDateTime dueTime;

    private String customFields;
}