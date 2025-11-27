package com.stockwise.service;

import com.stockwise.model.Portfolio;
import com.stockwise.model.Stock;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SuggestionService {

    private final PortfolioAnalysisService analysisService;

    public SuggestionService(PortfolioAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public List<String> generateSuggestions(Portfolio p) {
        List<String> out = new ArrayList<>();
        Map<String, Double> alloc = analysisService.allocationPercent(p);
        double vol = analysisService.volatilityScore(p);

        // overweight
        for (Map.Entry<String, Double> e : alloc.entrySet()) {
            if (e.getValue() > 25.0) {
                out.add(e.getKey() + " is Overweight (" + String.format("%.2f", e.getValue()) + "%)");
            } else if (e.getValue() < 1.0) {
                out.add(e.getKey() + " is Underweight (" + String.format("%.2f", e.getValue()) + "%)");
            }
        }

        // volatility threshold rules
        if (vol > 30.0) out.add("Portfolio volatility is High (" + String.format("%.2f", vol) + ")");
        else if (vol > 15.0) out.add("Portfolio volatility is Medium (" + String.format("%.2f", vol) + ")");
        else out.add("Portfolio volatility is Low (" + String.format("%.2f", vol) + ")");

        // per-stock P/L
        for (Stock s : p.getStocks()) {
            double pl = s.unrealizedPLPercent();
            if (pl > 20.0) out.add(s.getSymbol() + ": P/L " + String.format("%.2f", pl) + "% — Consider booking profits");
            if (pl < -10.0) out.add(s.getSymbol() + ": P/L " + String.format("%.2f", pl) + "% — Review holding");
        }

        // sector concentration
        Map<String, Double> sectorAlloc = new HashMap<>();
        double total = analysisService.currentValue(p);
        if (total > 0) {
            for (Stock s : p.getStocks()) sectorAlloc.put(s.getSector(), sectorAlloc.getOrDefault(s.getSector(), 0.0) + s.currentValue());
            for (Map.Entry<String, Double> e : sectorAlloc.entrySet()) {
                double pct = (e.getValue() / total) * 100.0;
                if (pct > 60.0) out.add("Sector concentration: " + e.getKey() + " at " + String.format("%.2f", pct) + "% — Diversify");
            }
        }

        if (out.isEmpty()) out.add("No strong suggestions: portfolio appears balanced.");
        return out;
    }
}
