package com.techmart.dto;

import com.techmart.model.Product;

import java.math.BigDecimal;

/** What the API returns for a product (keeps the JPA entity out of the JSON). */
public record ProductDto(
        Integer id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Integer categoryId,
        String category,
        Integer stock) {

    public static ProductDto from(Product p) {
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getImageUrl(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getStock());
    }
}
