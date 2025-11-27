package com.stockwise.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Mock fallback provider. Does NOT have @Primary.
 */
@Service
public class MockMarketDataService implements MarketDataService {

    private final Random rnd = new Random();
    private final Map<String, Double> lastPrice = new HashMap<>();

    @Override
    public Map<String, Double> fetchPrices(String[] symbols) {
        Map<String, Double> out = new LinkedHashMap<>();

        for (String sym : symbols) {
            if (sym == null) continue;
            String s = sym.toUpperCase().trim();

            double base = deterministicBase(s);
            double last = lastPrice.getOrDefault(s, base);

            double pct = (rnd.nextDouble() - 0.5) * 0.04;
            double next = last * (1 + pct);

            next = Math.round(next * 100.0) / 100.0;

            lastPrice.put(s, next);
            out.put(s, next);
        }
        return out;
    }

    private double deterministicBase(String sym) {
        int sum = 0;
        for (char c: sym.toCharArray()) sum += c;
        double base = (sum % 3000) + 100;
        return Math.round(base * 100.0) / 100.0;
    }
}
