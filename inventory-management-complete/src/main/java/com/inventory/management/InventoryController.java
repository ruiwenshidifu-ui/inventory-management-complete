package com.inventory.management;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final Map<String, Inventory> inventoryDB = new HashMap<>();
    private final ProductController productController;
    private int globalWarningLevel = 10; // 全局预警值

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
        inv.setWarningLevel(globalWarningLevel);
        inv.setLocation("货架-" + product.getCategory());
        inventoryDB.put(product.getId(), inv);
        return "库存记录创建成功";
    }

    @PostMapping("/internal/delete")
    public String deleteInventoryForProduct(@RequestParam String productId) {
        inventoryDB.remove(productId);
        return "库存记录删除成功";
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

    // ===== 新增功能：预警设置 =====
    @PostMapping("/warning")
    public String updateWarningLevel(@RequestParam String productId, @RequestParam int warningLevel) {
        Inventory inv = inventoryDB.get(productId);
        if (inv == null) {
            throw new InventoryNotFoundException("Product not found with ID: " + productId);
        }
        inv.setWarningLevel(warningLevel);
        return "预警值更新成功：商品 " + productId + " 的预警值设置为 " + warningLevel;
    }

    @GetMapping("/warning/{productId}")
    public int getWarningLevel(@PathVariable String productId) {
        Inventory inv = inventoryDB.get(productId);
        if (inv == null) {
            throw new InventoryNotFoundException("Product not found with ID: " + productId);
        }
        return inv.getWarningLevel();
    }

    @PostMapping("/warning/global")
    public String setGlobalWarningLevel(@RequestParam int warningLevel) {
        this.globalWarningLevel = warningLevel;
        
        // 更新所有商品的预警值
        for (Inventory inv : inventoryDB.values()) {
            inv.setWarningLevel(warningLevel);
        }
        
        return "全局预警值已更新为：" + warningLevel;
    }

    @GetMapping("/warning/global")
    public int getGlobalWarningLevel() {
        return globalWarningLevel;
    }

    // ===== 新增功能：数据报表 =====
    @GetMapping("/report/daily")
    public Map<String, Object> getDailyReport() {
        List<Inventory> allInventory = inventoryDB.values().stream()
                .map(this::enrichInventoryItem)
                .collect(Collectors.toList());
        
        long totalProducts = allInventory.size();
        long lowStockCount = allInventory.stream().filter(Inventory::isLowStock).count();
        int totalStockValue = allInventory.stream().mapToInt(Inventory::getCurrentStock).sum();
        
        // 按分类统计
        Map<String, Long> categoryStats = allInventory.stream()
                .collect(Collectors.groupingBy(Inventory::getCategory, Collectors.counting()));
        
        Map<String, Object> report = new HashMap<>();
        report.put("reportDate", LocalDateTime.now().toString());
        report.put("totalProducts", totalProducts);
        report.put("lowStockCount", lowStockCount);
        report.put("totalStockValue", totalStockValue);
        report.put("healthyStockCount", totalProducts - lowStockCount);
        report.put("categoryDistribution", categoryStats);
        report.put("lowStockProducts", allInventory.stream()
                .filter(Inventory::isLowStock)
                .map(inv -> Map.of(
                    "productName", inv.getProductName(),
                    "currentStock", inv.getCurrentStock(),
                    "warningLevel", inv.getWarningLevel()
                ))
                .collect(Collectors.toList()));
        
        return report;
    }

    // ===== 新增功能：导出库存 =====
    @GetMapping("/export")
    public String exportInventory() {
        List<Inventory> allInventory = inventoryDB.values().stream()
                .map(this::enrichInventoryItem)
                .collect(Collectors.toList());
        
        StringBuilder csv = new StringBuilder();
        // CSV 头部
        csv.append("商品名称,分类,当前库存,预警值,位置,状态,最后更新时间\n");
        
        // CSV 数据行
        for (Inventory inv : allInventory) {
            String status = inv.isLowStock() ? "低库存" : "正常";
            String lastUpdate = inv.getLastUpdateTime() != null ? 
                inv.getLastUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A";
            
            csv.append(String.format("\"%s\",\"%s\",%d,%d,\"%s\",\"%s\",\"%s\"\n",
                inv.getProductName(), inv.getCategory(), inv.getCurrentStock(),
                inv.getWarningLevel(), inv.getLocation(), status, lastUpdate));
        }
        
        return csv.toString();
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