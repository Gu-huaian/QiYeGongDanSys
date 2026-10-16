package com.example.qigd_ljl.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer number;
    private Integer size;

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    public static <T> PageResult<T> of(List<T> content, long totalElements, int totalPages, int number, int size) {
        return new PageResult<>(content, totalElements, totalPages, number, size);
    }
}