package com.stockwise.service;

import com.stockwise.model.Portfolio;
import org.springframework.stereotype.Service;

@Service
public class PortfolioAnalysisService {

    // ---- DIRECTLY USE Portfolio'S OWN METHODS ----

    public double totalInvested(Portfolio p) {
        return p.getTotalInvested();
    }

    public double currentValue(Portfolio p) {
        return p.getCurrentValue();
    }

    public double unrealizedPL(Portfolio p) {
        return p.getUnrealizedPL();
    }

    public double unrealizedPLPercent(Portfolio p) {
        return p.getUnrealizedPLPercent();
    }

    public double volatilityScore(Portfolio p) {
        return p.getVolatilityScore();
    }

    public double diversificationIndex(Portfolio p) {
        return p.getDiversificationScore();
    }

    public String riskRating(Portfolio p) {
        return p.getRiskRating();
    }

    public java.util.Map<String, Double> allocationPercent(Portfolio p) {
        return p.getAllocationPercent();
    }

    public java.util.List<com.stockwise.model.Stock> topGainers(Portfolio p, int n) {
        return p.getTopGainers(n);
    }

    public java.util.List<com.stockwise.model.Stock> topLosers(Portfolio p, int n) {
        return p.getTopLosers(n);
    }
}
