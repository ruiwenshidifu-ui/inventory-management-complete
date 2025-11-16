package com.inventory.management;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final Map<String, Product> productDB = new HashMap<>();

    public ProductController() {
        initializeSampleData();
    }

    private void initializeSampleData() {
        Product p1 = new Product("牛奶", "乳制品", "盒", 5.50, 3.00);
        Product p2 = new Product("面包", "烘焙", "个", 8.00, 4.50);
        Product p3 = new Product("矿泉水", "饮品", "瓶", 2.00, 1.00);
        productDB.put(p1.getId(), p1);
        productDB.put(p2.getId(), p2);
        productDB.put(p3.getId(), p3);
        
        // 为新商品创建库存
        createInventoryForNewProduct(p1);
        createInventoryForNewProduct(p2);
        createInventoryForNewProduct(p3);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody Product product) {
        product.setId(UUID.randomUUID().toString());
        productDB.put(product.getId(), product);
        
        // 创建对应的库存记录
        createInventoryForNewProduct(product);
        
        return product;
    }

    @GetMapping
    public Collection<Product> getAllProducts() {
        return productDB.values();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable String id) {
        Product product = productDB.get(id);
        if (product == null) {
            throw new ProductNotFoundException("Product not found with ID: " + id);
        }
        return product;
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable String id, @RequestBody Product updatedProduct) {
        if (!productDB.containsKey(id)) {
            throw new ProductNotFoundException("Product not found with ID: " + id);
        }
        updatedProduct.setId(id);
        productDB.put(id, updatedProduct);
        return updatedProduct;
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable String id) {
        if (!productDB.containsKey(id)) {
            throw new ProductNotFoundException("Product not found with ID: " + id);
        }
        productDB.remove(id);
        
        // 删除对应的库存记录
        deleteInventoryForProduct(id);
        
        return "Product " + id + " deleted successfully";
    }

    @GetMapping("/internal/all")
    public Map<String, Product> getAllProductsInternal() {
        return productDB;
    }

    private void createInventoryForNewProduct(Product product) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8080/inventory/internal/create";
            restTemplate.postForObject(url, product, String.class);
        } catch (Exception e) {
            System.err.println("创建库存记录失败: " + e.getMessage());
        }
    }

    private void deleteInventoryForProduct(String productId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8080/inventory/internal/delete?productId=" + productId;
            restTemplate.postForObject(url, null, String.class);
        } catch (Exception e) {
            System.err.println("删除库存记录失败: " + e.getMessage());
        }
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}