package com.example.qigd_ljl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_custom_field")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketCustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomFieldType fieldType;

    @Column(length = 200)
    private String options;

    @Column(nullable = false)
    private Boolean required = false;

    @Column(name = "is_for_all")
    private Boolean forAllCategories = true;

    @Column(name = "category_ids", length = 500)
    private String categoryIds;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

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
    }
}