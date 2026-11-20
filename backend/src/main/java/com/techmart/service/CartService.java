package com.techmart.service;

import com.techmart.config.AppMetrics;
import com.techmart.dto.CartDtos.CartDto;
import com.techmart.exception.BadRequestException;
import com.techmart.exception.ForbiddenException;
import com.techmart.exception.ResourceNotFoundException;
import com.techmart.model.Cart;
import com.techmart.model.CartItem;
import com.techmart.model.Product;
import com.techmart.repository.CartItemRepository;
import com.techmart.repository.CartRepository;
import com.techmart.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * All cart logic. Every method receives the userId of the AUTHENTICATED user (taken from the JWT by the
 * controller, never from the request body or URL), so a user can only ever reach their own cart.
 */
@Service
@Transactional
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;
    private final AppMetrics metrics;

    public CartService(CartRepository carts, CartItemRepository items, ProductRepository products,
                       AppMetrics metrics) {
        this.metrics = metrics;
        this.carts = carts;
        this.items = items;
        this.products = products;
    }

    public CartDto getCart(int userId) {
        return toDto(getOrCreateCart(userId));
    }

    public CartDto addItem(int userId, int productId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " not found"));

        CartItem existing = items.findByCartIdAndProductId(cart.getId(), productId).orElse(null);
        int newQuantity = quantity + (existing == null ? 0 : existing.getQuantity());
        checkStock(product, newQuantity);

        if (existing == null) {
            items.save(new CartItem(cart.getId(), product, newQuantity));
        } else {
            existing.setQuantity(newQuantity);
        }
        metrics.cartOperation("add");
        metrics.itemsAdded(quantity);
        log.info("cart_item_added", kv("event", "cart_item_added"), kv("userId", userId),
                kv("productId", productId), kv("quantity", quantity));
        return toDto(cart);
    }

    public CartDto updateItem(int userId, int itemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = ownedItem(cart, itemId);
        checkStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        metrics.cartOperation("update");
        log.info("cart_item_updated", kv("event", "cart_item_updated"), kv("userId", userId),
                kv("itemId", itemId), kv("quantity", quantity));
        return toDto(cart);
    }

    public CartDto removeItem(int userId, int itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = ownedItem(cart, itemId);
        items.delete(item);
        items.flush();
        metrics.cartOperation("remove");
        log.info("cart_item_removed", kv("event", "cart_item_removed"), kv("userId", userId),
                kv("itemId", itemId));
        return toDto(cart);
    }

    // ---------------------------------------------------------------- helpers

    private Cart getOrCreateCart(int userId) {
        return carts.findByUserId(userId).orElseGet(() -> carts.save(new Cart(userId)));
    }

    /** Loads an item and makes sure it belongs to the caller's cart (authorization check). */
    private CartItem ownedItem(Cart cart, int itemId) {
        CartItem item = items.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item with id " + itemId + " not found"));
        if (!item.getCartId().equals(cart.getId())) {
            log.warn("cart_access_denied", kv("event", "cart_access_denied"),
                    kv("userId", cart.getUserId()), kv("itemId", itemId));
            throw new ForbiddenException("You cannot access this cart item");
        }
        return item;
    }

    private void checkStock(Product product, int quantity) {
        if (product.getStock() <= 0) {
            throw new BadRequestException("'" + product.getName() + "' is out of stock");
        }
        if (quantity > product.getStock()) {
            throw new BadRequestException("Only " + product.getStock() + " units of '"
                    + product.getName() + "' are available");
        }
    }

    private CartDto toDto(Cart cart) {
        return CartDto.from(items.findByCartIdOrderById(cart.getId()));
    }
}
