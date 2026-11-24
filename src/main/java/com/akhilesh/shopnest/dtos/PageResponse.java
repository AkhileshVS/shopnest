package com.akhilesh.shopnest.dtos;

import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) implements Serializable {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        List<T> content = new ArrayList<>(page.getContent().stream().map(mapper).toList());
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
