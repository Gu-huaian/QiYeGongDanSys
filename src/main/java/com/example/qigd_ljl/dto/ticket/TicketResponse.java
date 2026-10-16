package com.example.qigd_ljl.dto.ticket;

import com.example.qigd_ljl.entity.AssignmentMode;
import com.example.qigd_ljl.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private CategorySimpleResponse category;
    private PrioritySimpleResponse priority;
    private TicketStatus status;
    private String statusLabel;
    private AssignmentMode assignmentMode;
    private String assignmentModeLabel;
    private UserSimpleResponse creator;
    private UserSimpleResponse assignee;
    private List<UserSimpleResponse> collaborators;
    private LocalDateTime dueTime;
    private LocalDateTime resolvedTime;
    private LocalDateTime closedTime;
    private Boolean overdue;
    private String customFields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySimpleResponse {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrioritySimpleResponse {
        private Long id;
        private String name;
        private String color;
        private Integer level;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSimpleResponse {
        private Long id;
        private String username;
        private String realName;
    }
}