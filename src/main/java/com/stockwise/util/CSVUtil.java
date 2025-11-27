package com.stockwise.util;

import com.stockwise.model.Stock;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Flexible CSV parser:
 * - Accepts ANY column order
 * - Accepts any header casing
 * - Ignores unknown columns
 * - buy_date optional → auto today
 * - sector optional → Unknown
 */
public class CSVUtil {

    public static List<Stock> parsePortfolioCSV(InputStream in) throws Exception {

        Reader reader = new InputStreamReader(in);

        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .parse(reader);

        List<Stock> list = new ArrayList<>();
        int line = 1;

        for (CSVRecord rec : records) {
            line++;

            try {
                String symbol = safe(rec, "symbol");
                if (symbol.isEmpty())
                    throw new Exception("missing symbol");

                double qty = parseDoubleSafe(rec, "quantity", 0);
                double bp = parseDoubleSafe(rec, "buy_price", 0);

                String date = safe(rec, "buy_date");
                if (date.isEmpty()) {
                    date = LocalDate.now().toString(); // auto date
                } else {
                    try {
                        LocalDate.parse(date); // validate
                    } catch (Exception e) {
                        date = LocalDate.now().toString(); // fix invalid date
                    }
                }

                String sector = safe(rec, "sector");
                if (sector.isEmpty()) sector = "Unknown";

                Stock s = new Stock(symbol.toUpperCase(), qty, bp, date, sector);
                list.add(s);

            } catch (Exception ex) {
                throw new Exception("CSV error at line " + line + ": " + ex.getMessage());
            }
        }

        return list;
    }

    // safely get value from CSV
    private static String safe(CSVRecord rec, String key) {
        try {
            if (rec.isMapped(key)) return rec.get(key).trim();
        } catch (Exception ignored) {}
        return "";
    }

    // safely parse numbers
    private static double parseDoubleSafe(CSVRecord rec, String key, double def) {
        try {
            if (rec.isMapped(key)) return Double.parseDouble(rec.get(key).trim());
        } catch (Exception ignored) {}
        return def;
    }
}
