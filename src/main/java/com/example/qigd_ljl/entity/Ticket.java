package com.example.qigd_ljl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ticketNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TicketCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id")
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssignmentMode assignmentMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    // 协作成员：被邀请协助处理该工单的用户
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ticket_collaborator",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> collaborators = new HashSet<>();

    @Column(name = "due_time")
    private LocalDateTime dueTime;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @Column(name = "closed_time")
    private LocalDateTime closedTime;

    @Column(name = "is_overdue")
    private Boolean overdue = false;

    @Column(name = "custom_fields", columnDefinition = "JSON")
    private String customFields;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (dueTime != null && status != TicketStatus.CLOSED && status != TicketStatus.RESOLVED) {
            overdue = LocalDateTime.now().isAfter(dueTime);
        }
    }
}