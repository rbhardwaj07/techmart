package com.techmart.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application-specific metrics (the HTTP metrics come automatically from Spring Boot Actuator).
 * In Prometheus they appear as:
 *   techmart_cart_operations_total{operation="add|update|remove"}
 *   techmart_cart_items_added_total
 *   techmart_auth_attempts_total{type="login|register|token", result="success|failure"}
 *   techmart_errors_total{type="database|unexpected"}
 * Counters are created at 0 on startup so dashboards show 0 instead of "no data".
 */
@Component
public class AppMetrics {

    private final MeterRegistry registry;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
        for (String op : List.of("add", "update", "remove")) {
            registry.counter("techmart.cart.operations", "operation", op);
        }
        registry.counter("techmart.cart.items.added");
        for (String type : List.of("login", "register")) {
            registry.counter("techmart.auth.attempts", "type", type, "result", "success");
            registry.counter("techmart.auth.attempts", "type", type, "result", "failure");
        }
        registry.counter("techmart.auth.attempts", "type", "token", "result", "failure");
        for (String type : List.of("database", "unexpected")) {
            registry.counter("techmart.errors", "type", type);
        }
    }

    public void cartOperation(String operation) {
        registry.counter("techmart.cart.operations", "operation", operation).increment();
    }

    public void itemsAdded(int quantity) {
        registry.counter("techmart.cart.items.added").increment(quantity);
    }

    public void authAttempt(String type, boolean success) {
        registry.counter("techmart.auth.attempts", "type", type, "result", success ? "success" : "failure")
                .increment();
    }

    public void error(String type) {
        registry.counter("techmart.errors", "type", type).increment();
    }
}
