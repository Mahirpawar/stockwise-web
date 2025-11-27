package com.stockwise.service;

import com.stockwise.model.Portfolio;
import com.stockwise.model.Stock;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final PortfolioAnalysisService analysis;

    public ReportService(PortfolioAnalysisService analysis) {
        this.analysis = analysis;
    }

    public String generateTextReport(Portfolio p) {
        StringBuilder sb = new StringBuilder();
        sb.append("StockWise - Portfolio Report\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        sb.append(String.format("Total Invested: %.2f INR\n", analysis.totalInvested(p)));
        sb.append(String.format("Current Value: %.2f INR\n", analysis.currentValue(p)));
        sb.append(String.format("Unrealized P/L: %.2f INR (%.2f%%)\n\n", analysis.unrealizedPL(p),
                (analysis.totalInvested(p) == 0 ? 0.0 : (analysis.unrealizedPL(p) / analysis.totalInvested(p) * 100.0))));

        sb.append("Per-stock details:\n");
        for (Stock s : p.getStocks()) {
            sb.append(String.format("%s — Qty: %.2f, BuyPrice: %.2f, CurrPrice: %.2f, Invested: %.2f, CurrValue: %.2f, P/L: %.2f (%.2f%%)\n",
                    s.getSymbol(), s.getQuantity(), s.getBuyPrice(), s.getCurrentPrice(), s.investedAmount(), s.currentValue(), s.unrealizedPL(), s.unrealizedPLPercent()));
        }

        sb.append("\nAllocation (%):\n");
        for (Map.Entry<String, Double> e : analysis.allocationPercent(p).entrySet()) {
            sb.append(String.format("%s : %.2f%%\n", e.getKey(), e.getValue()));
        }

        sb.append("\nVolatility score: ").append(String.format("%.2f", analysis.volatilityScore(p))).append("\n");
        sb.append("Diversification index: ").append(String.format("%.2f", analysis.diversificationIndex(p))).append("\n");

        return sb.toString();
    }

    public void exportTxt(Portfolio p, String path) throws IOException {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write(generateTextReport(p));
        }
    }

    public void exportCsv(Portfolio p, String path) throws IOException {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write("symbol,quantity,buy_price,buy_date,current_price,invested,current_value,unrealized_pl,unrealized_pl_percent,sector\n");
            for (Stock s : p.getStocks()) {
                fw.write(String.format("%s,%.2f,%.2f,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                        s.getSymbol(), s.getQuantity(), s.getBuyPrice(), s.getBuyDate(), s.getCurrentPrice(),
                        s.investedAmount(), s.currentValue(), s.unrealizedPL(), s.unrealizedPLPercent(), s.getSector()));
            }
        }
    }
}
