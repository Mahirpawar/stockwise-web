package com.stockwise.service;

import java.util.Map;

public interface MarketDataService {
    // return map symbol -> price (INR)
    Map<String, Double> fetchPrices(String[] symbols);
}