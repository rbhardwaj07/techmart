package com.techmart.repository;

import com.techmart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartIdOrderById(Integer cartId);

    Optional<CartItem> findByCartIdAndProductId(Integer cartId, Integer productId);
}
