package com.stockwise.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockwise.util.YahooSymbolMapper;


import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.*;

/**
 * Fetches REAL stock prices from Yahoo Finance.
 * This class is @Primary so Spring will use this FIRST.
 */
@Service
@Primary
public class YahooMarketDataService implements MarketDataService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, CacheEntry> cache = new HashMap<>();
    private final long CACHE_TTL_MS = 4000; // 4 seconds

    @Override
    public Map<String, Double> fetchPrices(String[] symbols) {
        Map<String, Double> out = new LinkedHashMap<>();

        for (String sym : symbols) {
            if (sym == null) continue;

            String clean = sym.toUpperCase().trim();

            // 1) Check cache
            CacheEntry ce = cache.get(clean);
            if (ce != null && (Instant.now().toEpochMilli() - ce.ts) < CACHE_TTL_MS) {
                out.put(clean, ce.price);
                continue;
            }

            // 2) Fetch from Yahoo
            double price = 0;
            try {
                price = fetchPrice(clean);
            } catch (Exception ex) {
                price = 0.0;
            }

            cache.put(clean, new CacheEntry(price, Instant.now().toEpochMilli()));
            out.put(clean, price);
        }
        return out;
    }

    private double fetchPrice(String symbol) throws Exception {

        // If no exchange is provided → assume Indian NSE → append .NS
        String yahooSymbol = YahooSymbolMapper.map(symbol);


        String urlStr =
                "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol +
                        "?range=1d&interval=1m";

        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setConnectTimeout(4000);
        con.setReadTimeout(4000);

        try (InputStream is = con.getInputStream()) {
            JsonNode root = mapper.readTree(is);

            JsonNode result = root.path("chart").path("result");
            if (!result.isArray() || result.isEmpty()) return 0;

            JsonNode meta = result.get(0).path("meta");

            // 1) Try regularMarketPrice
            if (meta.has("regularMarketPrice")) {
                return meta.get("regularMarketPrice").asDouble();
            }

            // 2) Try last close
            JsonNode indicators = result.get(0).path("indicators").path("quote");
            if (indicators.isArray() && indicators.size() > 0) {
                JsonNode closeArr = indicators.get(0).path("close");
                if (closeArr.isArray()) {
                    for (int i = closeArr.size() - 1; i >= 0; i--) {
                        JsonNode val = closeArr.get(i);
                        if (val != null && val.isNumber()) {
                            return val.asDouble();
                        }
                    }
                }
            }
        }

        return 0.0;
    }

    // Cache entry
    private static class CacheEntry {
        double price;
        long ts;
        CacheEntry(double price, long ts) {
            this.price = price;
            this.ts = ts;
        }
    }
}
