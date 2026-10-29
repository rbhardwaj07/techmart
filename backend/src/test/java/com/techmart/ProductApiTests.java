package com.techmart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void listsAllProducts() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(14)))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].price").exists())
                .andExpect(jsonPath("$[0].imageUrl").exists())
                .andExpect(jsonPath("$[0].category").exists());
    }

    @Test
    void searchesByText() throws Exception {
        mvc.perform(get("/api/products").param("search", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3))); // 2 laptops by name + the charger whose description mentions laptops
    }

    @Test
    void filtersByCategory() throws Exception {
        mvc.perform(get("/api/products").param("categoryId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].category", everyItem(is("Audio"))));
    }

    @Test
    void filtersByPriceRangeAndSorts() throws Exception {
        mvc.perform(get("/api/products")
                        .param("minPrice", "1000").param("maxPrice", "3000").param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(2499.0))
                .andExpect(jsonPath("$[-1].price").value(1499.0));
    }

    @Test
    void returnsSingleProduct() throws Exception {
        mvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nova X5 Smartphone"))
                .andExpect(jsonPath("$.category").value("Smartphones"));
    }

    @Test
    void unknownProductIs404() throws Exception {
        mvc.perform(get("/api/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("9999")));
    }

    @Test
    void invalidIdIs400() throws Exception {
        mvc.perform(get("/api/products/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products/0")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidQueryParametersAre400() throws Exception {
        mvc.perform(get("/api/products").param("sort", "bogus")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("minPrice", "-5")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("minPrice", "500").param("maxPrice", "100"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("categoryId", "x")).andExpect(status().isBadRequest());
    }

    @Test
    void listsCategories() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }
}
