package com.techmart.controller;

import com.techmart.dto.CategoryDto;
import com.techmart.dto.ProductDto;
import com.techmart.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    /** GET /api/products?search=&categoryId=&minPrice=&maxPrice=&sort= */
    @GetMapping("/products")
    public List<ProductDto> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort) {
        return service.findProducts(search, categoryId, minPrice, maxPrice, sort);
    }

    /** GET /api/products/{id} */
    @GetMapping("/products/{id}")
    public ProductDto getProduct(@PathVariable int id) {
        return service.getProduct(id);
    }

    /** GET /api/categories */
    @GetMapping("/categories")
    public List<CategoryDto> listCategories() {
        return service.getCategories();
    }
}
