package com.stockwise.util;

import java.util.HashMap;
import java.util.Map;

public class YahooSymbolMapper {

    private static final Map<String, String> MAP = new HashMap<>();

    static {
        MAP.put("HDFC", "HDFCBANK.NS");
        MAP.put("ICICI", "ICICIBANK.NS");
        MAP.put("SBIN", "SBIN.NS");
        MAP.put("RELIANCE", "RELIANCE.NS");
        MAP.put("TCS", "TCS.NS");
        MAP.put("INFY", "INFY.NS");
        MAP.put("HCLTECH", "HCLTECH.NS");
        MAP.put("WIPRO", "WIPRO.NS");

        // Add as many as you want
    }

    public static String map(String symbol) {
        symbol = symbol.trim().toUpperCase();
        return MAP.getOrDefault(symbol, symbol + ".NS");
    }
}
