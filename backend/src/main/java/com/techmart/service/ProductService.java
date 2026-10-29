package com.techmart.service;

import com.techmart.dto.CategoryDto;
import com.techmart.dto.ProductDto;
import com.techmart.exception.BadRequestException;
import com.techmart.exception.ResourceNotFoundException;
import com.techmart.repository.CategoryRepository;
import com.techmart.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public ProductService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public List<ProductDto> findProducts(String search, Integer categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        if (categoryId != null && categoryId <= 0) {
            throw new BadRequestException("categoryId must be a positive number");
        }
        if (minPrice != null && minPrice.signum() < 0) {
            throw new BadRequestException("minPrice cannot be negative");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new BadRequestException("maxPrice cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }

        String term = (search == null || search.isBlank()) ? null : search.trim();

        return products.search(term, categoryId, minPrice, maxPrice, toSort(sort))
                .stream()
                .map(ProductDto::from)
                .toList();
    }

    public ProductDto getProduct(int id) {
        if (id <= 0) {
            throw new BadRequestException("Product id must be a positive number");
        }
        return products.findById(id)
                .map(ProductDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));
    }

    public List<CategoryDto> getCategories() {
        return categories.findAll(Sort.by("name")).stream().map(CategoryDto::from).toList();
    }

    private Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("id");
        }
        return switch (sort) {
            case "name" -> Sort.by("name");
            case "price_asc" -> Sort.by("price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> throw new BadRequestException(
                    "Invalid sort '" + sort + "'. Use one of: name, price_asc, price_desc");
        };
    }
}
