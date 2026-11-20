package com.techmart.dto;

import com.techmart.model.CartItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cart request bodies deliberately have NO price field: prices always come from the products table.
 * (If a client sends a "price" anyway, Jackson ignores it.)
 */
public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(
            @NotNull @Min(1) Integer productId,
            @NotNull @Min(1) @Max(100) Integer quantity) {
    }

    public record UpdateItemRequest(@NotNull @Min(1) @Max(100) Integer quantity) {
    }

    public record CartItemDto(
            Integer id,
            Integer productId,
            String name,
            String imageUrl,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal lineTotal,
            Integer stock) {

        public static CartItemDto from(CartItem item) {
            BigDecimal unitPrice = item.getProduct().getPrice();
            return new CartItemDto(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getImageUrl(),
                    unitPrice,
                    item.getQuantity(),
                    unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
                    item.getProduct().getStock());
        }
    }

    public record CartDto(List<CartItemDto> items, int totalItems, BigDecimal total) {
        public static CartDto from(List<CartItem> entities) {
            List<CartItemDto> items = entities.stream().map(CartItemDto::from).toList();
            int totalItems = items.stream().mapToInt(CartItemDto::quantity).sum();
            BigDecimal total = items.stream().map(CartItemDto::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartDto(items, totalItems, total);
        }
    }
}
