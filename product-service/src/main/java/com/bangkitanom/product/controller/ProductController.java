package com.bangkitanom.product.controller;

import com.bangkitanom.product.dto.ProductRequest;
import com.bangkitanom.product.dto.ProductResponse;
import com.bangkitanom.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void save(@RequestBody ProductRequest productRequest) {
        productService.save(productRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductResponse> getAll() {
        return productService.getAll();
    }

}
