package com.techmart.controller;

import com.techmart.dto.CartDtos.AddItemRequest;
import com.techmart.dto.CartDtos.CartDto;
import com.techmart.dto.CartDtos.UpdateItemRequest;
import com.techmart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * All endpoints need a valid token (see SecurityConfig). The user id comes from the token, so there is
 * no way to name another user's cart in the URL or body.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    /** GET /api/cart */
    @GetMapping
    public CartDto getCart(Authentication auth) {
        return service.getCart(userId(auth));
    }

    /** POST /api/cart/items  { "productId": 1, "quantity": 2 } */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartDto addItem(Authentication auth, @Valid @RequestBody AddItemRequest body) {
        return service.addItem(userId(auth), body.productId(), body.quantity());
    }

    /** PUT /api/cart/items/{itemId}  { "quantity": 3 } */
    @PutMapping("/items/{itemId}")
    public CartDto updateItem(Authentication auth, @PathVariable int itemId,
                              @Valid @RequestBody UpdateItemRequest body) {
        return service.updateItem(userId(auth), itemId, body.quantity());
    }

    /** DELETE /api/cart/items/{itemId} */
    @DeleteMapping("/items/{itemId}")
    public CartDto removeItem(Authentication auth, @PathVariable int itemId) {
        return service.removeItem(userId(auth), itemId);
    }

    private int userId(Authentication auth) {
        return (Integer) auth.getPrincipal();
    }
}
