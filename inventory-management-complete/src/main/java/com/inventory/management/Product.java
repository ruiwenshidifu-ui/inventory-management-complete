package com.inventory.management;

import java.util.UUID;

public class Product {
    private String id;
    private String name;
    private String category;
    private String unit;
    private double salePrice;
    private double costPrice;
    private boolean isActive;

    public Product() {
        this.id = UUID.randomUUID().toString();
    }

    public Product(String name, String category, String unit, double salePrice, double costPrice) {
        this();
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.salePrice = salePrice;
        this.costPrice = costPrice;
        this.isActive = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getSalePrice() { return salePrice; }
    public void setSalePrice(double salePrice) { this.salePrice = salePrice; }
    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}