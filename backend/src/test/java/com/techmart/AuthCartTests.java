package com.techmart;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthCartTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    private String json(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    private String register(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email, "password", "password123"))))
                .andExpect(status().isCreated())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private String newUserToken() throws Exception {
        return register(UUID.randomUUID() + "@example.com");
    }

    private int addToCart(String token, int productId, int qty) throws Exception {
        MvcResult r = mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", productId, "quantity", qty))))
                .andExpect(status().isCreated())
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString()).get("items").get(0).get("id").asInt();
    }

    @Test
    void cartRequiresLogin() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cart").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productsStayPublic() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void registerLoginAndDuplicateEmail() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        register(email);

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "X", "email", email, "password", "password123"))))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "X", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUpdateRemoveAndTotal() throws Exception {
        String token = newUserToken();
        int itemId = addToCart(token, 1, 2);   // product 1 costs 24999.00

        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.total").value(49998.0));

        mvc.perform(put("/api/cart/items/" + itemId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("quantity", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(74997.0));

        mvc.perform(delete("/api/cart/items/" + itemId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void addingSameProductTwiceMergesQuantity() throws Exception {
        String token = newUserToken();
        addToCart(token, 5, 1);
        addToCart(token, 5, 2);
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void stockAndQuantityAreValidated() throws Exception {
        String token = newUserToken();
        // more than the stock (product 4 has 9 in stock)
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 4, "quantity", 50))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 1, "quantity", 0))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 9999, "quantity", 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void clientSubmittedPriceIsIgnored() throws Exception {
        String token = newUserToken();
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("productId", 1, "quantity", 1, "price", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(24999.0));
    }

    @Test
    void userCannotTouchAnotherUsersCart() throws Exception {
        String alice = newUserToken();
        String bob = newUserToken();
        int aliceItem = addToCart(alice, 1, 1);

        // Bob tries to change / delete Alice's item by guessing its id
        mvc.perform(put("/api/cart/items/" + aliceItem).header("Authorization", "Bearer " + bob)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("quantity", 5))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/cart/items/" + aliceItem).header("Authorization", "Bearer " + bob))
                .andExpect(status().isForbidden());

        // Bob's own cart is unaffected, Alice's item is untouched
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + bob))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + alice))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalItems").value(1));
    }
}
