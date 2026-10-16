package com.example.qigd_ljl.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleRequest {
    private Long id;
    private String username;
    private String realName;
    private String department;
}