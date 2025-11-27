package com.stockwise.model;

import java.util.*;
import java.util.stream.Collectors;

public class Portfolio {

    private List<Stock> stocks = new ArrayList<>();

    public Portfolio() {}

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks != null ? stocks : new ArrayList<>();
    }

    public void add(Stock s) {
        if (s != null) stocks.add(s);
    }

    /* -----------------------------------------
       PORTFOLIO CALCULATIONS (NEW)
    ----------------------------------------- */

    // Total amount invested
    public double getTotalInvested() {
        return stocks.stream().mapToDouble(Stock::investedAmount).sum();
    }

    // Current portfolio value
    public double getCurrentValue() {
        return stocks.stream().mapToDouble(Stock::currentValue).sum();
    }

    // Unrealized P/L
    public double getUnrealizedPL() {
        return getCurrentValue() - getTotalInvested();
    }

    // P/L percentage
    public double getUnrealizedPLPercent() {
        double invested = getTotalInvested();
        if (invested <= 0) return 0;
        return (getUnrealizedPL() / invested) * 100;
    }

    /* -----------------------------------------
       ALLOCATION MAP  (Stock % of portfolio)
    ----------------------------------------- */
    public Map<String, Double> getAllocationPercent() {
        double total = getCurrentValue();

        Map<String, Double> alloc = new LinkedHashMap<>();
        for (Stock s : stocks) {
            double value = s.currentValue();
            double pct = (total > 0) ? (value / total * 100) : 0;
            alloc.put(s.getSymbol(), Math.round(pct * 100.0) / 100.0);
        }
        return alloc;
    }

    /* -----------------------------------------
       TOP GAINERS / LOSERS
    ----------------------------------------- */
    public List<Stock> getTopGainers(int limit) {
        return stocks.stream()
                .sorted(Comparator.comparingDouble(Stock::unrealizedPLPercent).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Stock> getTopLosers(int limit) {
        return stocks.stream()
                .sorted(Comparator.comparingDouble(Stock::unrealizedPLPercent))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /* -----------------------------------------
       SECTOR DIVERSIFICATION
    ----------------------------------------- */
    public Map<String, Double> getSectorWeights() {
        double total = getCurrentValue();

        Map<String, Double> sectorMap = new LinkedHashMap<>();

        for (Stock s : stocks) {
            double v = s.currentValue();
            String sector = s.getSector() != null ? s.getSector() : "Unknown";

            sectorMap.put(sector,
                    sectorMap.getOrDefault(sector, 0.0) + v);
        }

        // Convert to %
        for (String key : sectorMap.keySet()) {
            double val = sectorMap.get(key);
            sectorMap.put(key, total > 0 ? (val / total * 100) : 0);
        }

        return sectorMap;
    }

    /* -----------------------------------------
       RISK RATING (Simplified heuristic)
    ----------------------------------------- */
    public String getRiskRating() {
        double vol = getVolatilityScore();

        if (vol < 10) return "Low";
        if (vol < 25) return "Medium";
        return "High";
    }

    /* -----------------------------------------
       VOLATILITY (simple dispersion measure)
    ----------------------------------------- */
    public double getVolatilityScore() {
        if (stocks.isEmpty()) return 0;

        List<Double> returns = stocks.stream()
                .map(Stock::unrealizedPLPercent)
                .toList();

        double mean = returns.stream().mapToDouble(v -> v).average().orElse(0);

        double variance = returns.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance); // std-dev % (volatility)
    }

    /* -----------------------------------------
       DIVERSIFICATION SCORE (0–100)
    ----------------------------------------- */
    public double getDiversificationScore() {
        Map<String, Double> sectors = getSectorWeights();
        if (sectors.isEmpty()) return 0;

        // Perfect diversification = equal weights
        double ideal = 100.0 / sectors.size();

        double score = 0;
        for (double w : sectors.values()) {
            score += (100 - Math.abs(w - ideal));
        }

        return Math.min(score / sectors.size(), 100); // normalized
    }
}
