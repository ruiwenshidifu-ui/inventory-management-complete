package com.inventory.management;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final Map<String, Inventory> inventoryDB = new HashMap<>();
    private final ProductController productController;

    public InventoryController(ProductController productController) {
        this.productController = productController;
        initializeInventoryData();
    }

    private void initializeInventoryData() {
        Map<String, Product> products = productController.getAllProductsInternal();
        
        for (Product product : products.values()) {
            int stock, warning;
            switch (product.getName()) {
                case "牛奶":
                    stock = 50;
                    warning = 10;
                    break;
                case "面包":
                    stock = 5;
                    warning = 20;
                    break;
                case "矿泉水":
                    stock = 100;
                    warning = 50;
                    break;
                default:
                    stock = 0;
                    warning = 10;
            }
            
            Inventory inv = new Inventory();
            inv.setProductId(product.getId());
            inv.setCurrentStock(stock);
            inv.setWarningLevel(warning);
            inv.setLocation("货架-" + product.getCategory());
            inventoryDB.put(product.getId(), inv);
        }
    }

    @PostMapping("/internal/create")
    public String createInventoryForProduct(@RequestBody Product product) {
        Inventory inv = new Inventory();
        inv.setProductId(product.getId());
        inv.setCurrentStock(0);
        inv.setWarningLevel(10);
        inv.setLocation("货架-" + product.getCategory());
        inventoryDB.put(product.getId(), inv);
        return "库存记录创建成功";
    }

    @PostMapping("/internal/delete")
    public String deleteInventoryForProduct(@RequestParam String productId) {
        inventoryDB.remove(productId);
        return "库存记录删除成功";
    }

    @PostMapping("/internal/update_warning")
    public String updateWarningLevel(@RequestParam String productId, @RequestParam int warningLevel) {
        Inventory inv = inventoryDB.get(productId);
        if (inv != null) {
            inv.setWarningLevel(warningLevel);
            return "预警值更新成功";
        }
        return "商品不存在";
    }

    private Inventory enrichInventoryItem(Inventory inv) {
        Map<String, Product> products = productController.getAllProductsInternal();
        Product product = products.get(inv.getProductId());
        
        if (product != null) {
            inv.setProductName(product.getName());
            inv.setCategory(product.getCategory());
        } else {
            inv.setProductName("未知商品");
            inv.setCategory("未知");
        }
        inv.setLowStock(inv.getCurrentStock() <= inv.getWarningLevel());
        return inv;
    }

    @GetMapping("/list")
    public List<Inventory> getInventoryList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock) {

        return inventoryDB.values().stream()
                .map(this::enrichInventoryItem)
                .filter(inv -> {
                    boolean matches = true;
                    if (name != null && !inv.getProductName().toLowerCase().contains(name.toLowerCase())) 
                        matches = false;
                    if (category != null && !inv.getCategory().equalsIgnoreCase(category)) 
                        matches = false;
                    if (minStock != null && inv.getCurrentStock() < minStock) 
                        matches = false;
                    if (maxStock != null && inv.getCurrentStock() > maxStock) 
                        matches = false;
                    return matches;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/alerts")
    public List<Inventory> getLowStockAlerts() {
        return inventoryDB.values().stream()
                .map(this::enrichInventoryItem)
                .filter(Inventory::isLowStock)
                .collect(Collectors.toList());
    }

    @GetMapping("/{productId}")
    public Inventory getProductStock(@PathVariable String productId) {
        Inventory inv = inventoryDB.get(productId);
        if (inv == null) {
            throw new InventoryNotFoundException("Inventory for product not found with ID: " + productId);
        }
        return enrichInventoryItem(inv);
    }

    @PostMapping("/update_stock")
    public Map<String, Object> updateStock(@RequestParam String productId, @RequestParam int quantityChange) {
        Inventory inv = inventoryDB.get(productId);
        if (inv == null) {
            throw new InventoryNotFoundException("Product not found in inventory with ID: " + productId);
        }

        int currentStock = inv.getCurrentStock();
        int newStock = currentStock + quantityChange;

        if (newStock < 0) {
            throw new InsufficientStockException("Insufficient stock for product ID: " + productId);
        }

        inv.setCurrentStock(newStock);
        inv.setLastUpdateTime(LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("newStock", newStock);
        response.put("message", "库存更新成功");
        return response;
    }

    @GetMapping("/stats")
    public Map<String, Object> getInventoryStats() {
        List<Inventory> allInventory = inventoryDB.values().stream()
                .map(this::enrichInventoryItem)
                .collect(Collectors.toList());
        
        long totalProducts = allInventory.size();
        long lowStockCount = allInventory.stream().filter(Inventory::isLowStock).count();
        int totalStockValue = allInventory.stream().mapToInt(Inventory::getCurrentStock).sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("lowStockCount", lowStockCount);
        stats.put("totalStockValue", totalStockValue);
        stats.put("healthyStockCount", totalProducts - lowStockCount);
        
        return stats;
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class InventoryNotFoundException extends RuntimeException {
    public InventoryNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}