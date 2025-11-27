package com.stockwise.model;

public class Stock {

    private int id;
    private String symbol;
    private double quantity;
    private double buyPrice;
    private String buyDate;
    private double currentPrice;
    private String sector;

    public Stock() {}

    public Stock(String symbol, double quantity, double buyPrice, String buyDate, String sector) {
        this.symbol = normalize(symbol);
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.buyDate = buyDate;
        this.sector = normalizeSector(sector);
    }

    // Constructor used for manual add
    public Stock(String symbol, double quantity, double buyPrice, String buyDate) {
        this.symbol = normalize(symbol);
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.buyDate = buyDate;
        this.sector = "Unknown";
    }

    /* ------------------------
       Getters and Setters
    ------------------------ */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = normalize(symbol); }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public double getBuyPrice() { return buyPrice; }
    public void setBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public String getBuyDate() { return buyDate; }
    public void setBuyDate(String buyDate) { this.buyDate = buyDate; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = normalizeSector(sector); }

    /* ------------------------
       Core Calculations
    ------------------------ */

    public double investedAmount() {
        return buyPrice * quantity;
    }

    public double currentValue() {
        return currentPrice * quantity;
    }

    public double getCurrentValue() { 
        return currentValue(); 
    }

    public double unrealizedPL() {
        return currentValue() - investedAmount();
    }

    public double unrealizedPLPercent() {
        double invested = investedAmount();
        if (invested <= 0) return 0;
        return (unrealizedPL() / invested) * 100;
    }

    /* ------------------------
       Helper Methods (New)
    ------------------------ */

    // Normalize symbol input
    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }

    // Clean sector text (optional but good practice)
    private String normalizeSector(String s) {
        if (s == null || s.isEmpty()) return "Unknown";
        return s.trim();
    }

    // ✔ UI Helper — green if profit, red if loss
    public boolean isProfitable() {
        return unrealizedPL() > 0;
    }

    // ✔ Formatted values for UI if needed
    public String formatMoney(double value) {
        return String.format("₹%.2f", value);
    }

    public String getFormattedPLPercent() {
        return String.format("%.2f%%", unrealizedPLPercent());
    }
}
