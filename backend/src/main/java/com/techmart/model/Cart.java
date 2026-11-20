package com.techmart.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A user's shopping cart. Each user has exactly one (unique user_id). */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    protected Cart() {
    }

    public Cart(Integer userId) {
        this.userId = userId;
    }

    public Integer getId() { return id; }
    public Integer getUserId() { return userId; }
}
