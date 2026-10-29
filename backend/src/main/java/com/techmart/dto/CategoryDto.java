package com.techmart.dto;

import com.techmart.model.Category;

public record CategoryDto(Integer id, String name) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }
}
