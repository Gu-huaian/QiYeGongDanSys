package com.example.qigd_ljl.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSimpleResponse {

    private Long id;
    private String name;
    private String code;
}