package com.techmart.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlled-failure endpoints used ONLY for the Part 3 monitoring demonstration.
 * They exist only while app.debug-endpoints.enabled=true.
 */
@RestController
@RequestMapping("/api/debug")
@ConditionalOnProperty(name = "app.debug-endpoints.enabled", havingValue = "true")
public class DebugController {

    /** GET /api/debug/error -> deliberate unexpected exception (HTTP 500, logged, counted). */
    @GetMapping("/error")
    public Map<String, String> error() {
        throw new IllegalStateException("Controlled failure triggered for the monitoring demo");
    }

    /** GET /api/debug/slow?ms=1500 -> a deliberately slow response (max 5 s) to show latency. */
    @GetMapping("/slow")
    public Map<String, Object> slow(@RequestParam(defaultValue = "1500") long ms) throws InterruptedException {
        long delay = Math.min(Math.max(ms, 0), 5000);
        Thread.sleep(delay);
        return Map.of("sleptMs", delay);
    }
}
