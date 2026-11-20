package com.techmart;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Part 3: request ids, application metrics, controlled failure, and "no secrets in logs". */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MonitoringTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private MeterRegistry registry;

    private String json(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    private double count(String name, String... tags) {
        return registry.counter(name, tags).count();
    }

    private String register(String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Mon User", "email", UUID.randomUUID() + "@example.com",
                                "password", password))))
                .andExpect(status().isCreated())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void everyResponseHasARequestId() throws Exception {
        mvc.perform(get("/api/products")).andExpect(header().exists("X-Request-Id"));
        mvc.perform(get("/api/products").header("X-Request-Id", "my-trace-id-123"))
                .andExpect(header().string("X-Request-Id", "my-trace-id-123"));
    }

    @Test
    void controlledFailureReturns500AndIsCounted() throws Exception {
        double before = count("techmart.errors", "type", "unexpected");
        mvc.perform(get("/api/debug/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong on the server"));
        assertEquals(before + 1, count("techmart.errors", "type", "unexpected"));
    }

    @Test
    void cartOperationsAreCounted() throws Exception {
        String token = register("password123");
        double adds = count("techmart.cart.operations", "operation", "add");
        double items = count("techmart.cart.items.added");

        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 1, "quantity", 2))))
                .andExpect(status().isCreated());

        assertEquals(adds + 1, count("techmart.cart.operations", "operation", "add"));
        assertEquals(items + 2, count("techmart.cart.items.added"));
    }

    @Test
    void failedLoginIsCounted() throws Exception {
        double before = count("techmart.auth.attempts", "type", "login", "result", "failure");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@example.com", "password", "whatever123"))))
                .andExpect(status().isUnauthorized());
        assertEquals(before + 1, count("techmart.auth.attempts", "type", "login", "result", "failure"));
    }

    @Test
    void unknownUrlIs404NotAServerError() throws Exception {
        String token = register("password123");
        mvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void passwordsAndTokensAreNeverLogged(CapturedOutput output) throws Exception {
        String password = "Sup3rS3cretPassw0rd!";
        String token = register(password);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", "someone@example.com", "password", password))));
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token));

        assertFalse(output.getAll().contains(password), "password found in logs");
        assertFalse(output.getAll().contains(token), "token found in logs");
    }
}
