package com.example.qigd_ljl.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String code;

    private String description;

    private Long parentId;

    private Boolean enabled;
}