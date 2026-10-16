package com.example.qigd_ljl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_operation_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String module;

    @Column(length = 100)
    private String operation;

    @Column(length = 100)
    private String method;

    @Column(length = 500)
    private String requestUrl;

    @Column(length = 10)
    private String requestMethod;

    @Column(columnDefinition = "TEXT")
    private String requestParams;

    @Column(columnDefinition = "TEXT")
    private String responseResult;

    @Column(length = 20)
    private String ip;

    @Column(length = 200)
    private String userAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "cost_time")
    private Long costTime;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}