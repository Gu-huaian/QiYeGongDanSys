package com.example.qigd_ljl.dto.priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriorityRequest {

    @NotBlank(message = "优先级名称不能为空")
    private String name;

    @NotBlank(message = "优先级编码不能为空")
    private String code;

    @NotNull(message = "优先级级别不能为空")
    private Integer level;

    private String color;

    private String description;

    private Boolean enabled;
}