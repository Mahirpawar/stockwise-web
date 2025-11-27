package com.stockwise.controller;

import com.stockwise.model.Portfolio;
import com.stockwise.model.Stock;
import com.stockwise.repository.PortfolioRepository;
import com.stockwise.service.MarketDataService;
import com.stockwise.service.PortfolioAnalysisService;
import com.stockwise.service.ReportService;
import com.stockwise.service.SuggestionService;
import com.stockwise.util.CSVUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class WebController {

    @Autowired private PortfolioRepository repo;
    @Autowired private MarketDataService market;
    @Autowired private PortfolioAnalysisService analysis;
    @Autowired private SuggestionService suggestionService;
    @Autowired private ReportService reportService;

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/manual-add")
    public String manualAddPage() { return "index"; }

    // ---------------- VIEW PORTFOLIO -------------------
    @GetMapping("/portfolio")
    public String viewPortfolio(Model m) {
        try {
            List<Stock> stocks;
            try {
                stocks = repo.findAll();
            } catch (SQLException e) {
                e.printStackTrace();
                m.addAttribute("error","DB error: "+e.getMessage());
                return "index";
            }

            // If DB is empty -> load sample CSV from classpath and persist it.
            // This ensures sample appears only when DB empty (first run).
            if (stocks == null || stocks.isEmpty()) {
                try (InputStream is = getClass().getResourceAsStream("/sample/sample.csv")) {
                    if (is != null) {
                        List<Stock> sample = CSVUtil.parsePortfolioCSV(is);
                        if (sample != null && !sample.isEmpty()) {
                            try {
                                repo.replaceAll(sample); // write sample into DB
                                stocks = repo.findAll(); // reload from DB
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                                // fall back to sample in-memory (do not overwrite DB)
                                stocks = sample;
                            }
                        }
                    }
                } catch (Exception ex) {
                    // if anything fails, keep stocks empty and continue (no crash)
                    ex.printStackTrace();
                }
            }

            fillCurrentPricesSafely(stocks);

            Portfolio p = new Portfolio();
            p.setStocks(stocks);

            m.addAttribute("portfolio", p);
            m.addAttribute("totalInvested", analysis.totalInvested(p));
            m.addAttribute("currentValue", analysis.currentValue(p));
            m.addAttribute("unrealized", analysis.unrealizedPL(p));
            m.addAttribute("volatility", analysis.volatilityScore(p));
            m.addAttribute("diversification", analysis.diversificationIndex(p));
            m.addAttribute("alloc", analysis.allocationPercent(p));
            m.addAttribute("suggestions", suggestionService.generateSuggestions(p));

            return "portfolio";

        } catch (Exception e) {
            e.printStackTrace();
            m.addAttribute("error","Error: "+e.getMessage());
            return "index";
        }
    }

    // ---------------- CSV UPLOAD -------------------
    @PostMapping("/upload")
    public String uploadCSV(@RequestParam("file") MultipartFile file, Model m) {
        try {
            if (file.isEmpty()) {
                m.addAttribute("message","File is empty");
                return "index";
            }

            List<Stock> parsed = CSVUtil.parsePortfolioCSV(file.getInputStream());

            try {
                repo.replaceAll(parsed); // overwrite DB with uploaded CSV
            } catch (SQLException e) {
                e.printStackTrace();
                m.addAttribute("message","DB error: "+e.getMessage());
                return "index";
            }

            // IMPORTANT: redirect to dashboard so uploaded data is shown immediately
            return "redirect:/portfolio";

        } catch (Exception ex) {
            ex.printStackTrace();
            m.addAttribute("message","Upload failed: "+ex.getMessage());
            return "index";
        }
    }

    // ---------------- MANUAL ADD -------------------
    @PostMapping("/manual-add")
    public String manualAdd(@RequestParam("symbol") String symbol,
                            @RequestParam("quantity") double quantity,
                            @RequestParam("averagePrice") double averagePrice) {

        try {
            Stock s = new Stock(
                    symbol.toUpperCase(),
                    quantity,
                    averagePrice,
                    java.time.LocalDate.now().toString(),
                    "Unknown"
            );

            try {
                repo.saveOrUpdate(s);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "redirect:/portfolio";
    }

    // ---------------- DELETE STOCK -------------------
    @PostMapping("/delete")
    public String delete(@RequestParam("symbol") String symbol) {
        try {
            repo.deleteBySymbol(symbol);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "redirect:/portfolio";
    }


    // ===========================================================
    //   API ENDPOINTS
    // ===========================================================
    @GetMapping("/api/portfolio")
    @ResponseBody
    public Portfolio apiPortfolio() {
        List<Stock> stocks = new ArrayList<>();
        try {
            stocks = repo.findAll();
        } catch (SQLException e) { e.printStackTrace(); }

        fillCurrentPricesSafely(stocks);

        Portfolio p = new Portfolio();
        p.setStocks(stocks);
        return p;
    }


    @GetMapping("/api/summary")
    @ResponseBody
    public Map<String,Object> apiSummary() {
        List<Stock> stocks = new ArrayList<>();
        try {
            stocks = repo.findAll();
        } catch (SQLException e) { e.printStackTrace(); }

        fillCurrentPricesSafely(stocks);

        Portfolio p = new Portfolio();
        p.setStocks(stocks);

        double totalInvested = analysis.totalInvested(p);
        double currentValue = analysis.currentValue(p);
        double unrealized = analysis.unrealizedPL(p);

        Map<String,Object> out = new HashMap<>();
        out.put("totalInvested", totalInvested);
        out.put("currentValue", currentValue);
        out.put("unrealized", unrealized);
        out.put("unrealizedPercent", totalInvested==0?0:(unrealized/totalInvested)*100);
        out.put("volatility", analysis.volatilityScore(p));
        out.put("diversification", analysis.diversificationIndex(p));
        out.put("allocation", analysis.allocationPercent(p));
        out.put("suggestions", suggestionService.generateSuggestions(p));
        out.put("riskRating", computeRiskRating(analysis.volatilityScore(p)));

        return out;
    }

    @GetMapping("/api/top")
    @ResponseBody
    public Map<String,List<Stock>> apiTop(@RequestParam(defaultValue="5") int limit) {

        List<Stock> stocks = new ArrayList<>();
        try {
            stocks = repo.findAll();
        } catch (SQLException e) { e.printStackTrace(); }

        fillCurrentPricesSafely(stocks);

        List<Stock> filtered =
                stocks.stream().filter(s -> s.investedAmount()!=0).collect(Collectors.toList());

        List<Stock> gainers = filtered.stream()
                .sorted(Comparator.comparingDouble(Stock::unrealizedPLPercent).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        List<Stock> losers = filtered.stream()
                .sorted(Comparator.comparingDouble(Stock::unrealizedPLPercent))
                .limit(limit)
                .collect(Collectors.toList());

        Map<String,List<Stock>> out = new HashMap<>();
        out.put("gainers", gainers);
        out.put("losers", losers);
        return out;
    }


    // ===========================================================
    //   EXPORT
    // ===========================================================
    @GetMapping("/export/report")
    public ResponseEntity<InputStreamResource> downloadReport() throws Exception {

        List<Stock> stocks = new ArrayList<>();
        try {
            stocks = repo.findAll();
        } catch (SQLException e) { e.printStackTrace(); }

        fillCurrentPricesSafely(stocks);

        Portfolio p = new Portfolio();
        p.setStocks(stocks);

        String txt = reportService.generateTextReport(p);
        byte[] bytes = txt.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=portfolio_report.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(new InputStreamResource(new ByteArrayInputStream(bytes)));
    }


    @GetMapping("/export/csv")
    public ResponseEntity<InputStreamResource> downloadCSV() throws Exception {

        List<Stock> stocks = new ArrayList<>();
        try {
            stocks = repo.findAll();
        } catch (SQLException e) { e.printStackTrace(); }

        fillCurrentPricesSafely(stocks);

        Portfolio p = new Portfolio();
        p.setStocks(stocks);

        StringBuilder sb = new StringBuilder();
        sb.append("symbol,quantity,buy_price,buy_date,current_price,invested,current_value,unrealized_pl,unrealized_pl_percent,sector\n");

        for (Stock s : p.getStocks()) {
            sb.append(String.format(Locale.US,
                    "%s,%.2f,%.2f,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                    s.getSymbol(), s.getQuantity(), s.getBuyPrice(), s.getBuyDate(),
                    s.getCurrentPrice(), s.investedAmount(), s.currentValue(),
                    s.unrealizedPL(), s.unrealizedPLPercent(), s.getSector()
            ));
        }

        byte[] bytes = sb.toString().getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment;filename=portfolio_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(new ByteArrayInputStream(bytes)));
    }


    // ===========================================================
    //   UTILITIES
    // ===========================================================
    private void fillCurrentPricesSafely(List<Stock> stocks) {
        try {
            String[] syms = stocks.stream().map(Stock::getSymbol).toArray(String[]::new);
            Map<String,Double> prices = market.fetchPrices(syms);

            for (Stock s : stocks) {
                s.setCurrentPrice(prices.getOrDefault(s.getSymbol(), 0.0));
            }

        } catch (Exception e) {
            for (Stock s : stocks) s.setCurrentPrice(0.0);
        }
    }

    private String computeRiskRating(double volatility) {
        if (Double.isNaN(volatility)) return "Unknown";
        if (volatility < 30) return "Low";
        if (volatility < 60) return "Medium";
        return "High";
    }
}
