package com.stockwise.repository;

import com.stockwise.model.Stock;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class PortfolioRepository {

    private final String url = "jdbc:sqlite:data/stockwise.db";

    // Save or update a stock
    public void saveOrUpdate(Stock s) throws SQLException {
        try (Connection c = DriverManager.getConnection(url)) {
            String selectSql = "SELECT id, quantity, buy_price FROM portfolio_stock WHERE symbol = ?";
            PreparedStatement ps = c.prepareStatement(selectSql);
            ps.setString(1, s.getSymbol());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                double oldQty = rs.getDouble("quantity");
                double oldBuy = rs.getDouble("buy_price");

                double newQty = oldQty + s.getQuantity();
                double newAvg = ((oldQty * oldBuy) + (s.getQuantity() * s.getBuyPrice())) / newQty;

                String updateSql = "UPDATE portfolio_stock SET quantity = ?, buy_price = ?, buy_date = ?, sector = ? WHERE id = ?";
                PreparedStatement ups = c.prepareStatement(updateSql);
                ups.setDouble(1, newQty);
                ups.setDouble(2, newAvg);
                ups.setString(3, s.getBuyDate());
                ups.setString(4, s.getSector());
                ups.setInt(5, id);
                ups.executeUpdate();
                ups.close();
            } else {
                String insertSql = "INSERT INTO portfolio_stock(symbol, quantity, buy_price, buy_date, sector) VALUES(?,?,?,?,?)";
                PreparedStatement ins = c.prepareStatement(insertSql);
                ins.setString(1, s.getSymbol());
                ins.setDouble(2, s.getQuantity());
                ins.setDouble(3, s.getBuyPrice());
                ins.setString(4, s.getBuyDate());
                ins.setString(5, s.getSector());
                ins.executeUpdate();
                ins.close();
            }

            rs.close();
            ps.close();
        }
    }

    public void replaceAll(List<Stock> stocks) throws SQLException {
        try (Connection c = DriverManager.getConnection(url)) {
            c.setAutoCommit(false);
            Statement st = c.createStatement();
            st.executeUpdate("DELETE FROM portfolio_stock");
            st.close();

            String ins = "INSERT INTO portfolio_stock(symbol,quantity,buy_price,buy_date,sector) VALUES(?,?,?,?,?)";
            PreparedStatement ps = c.prepareStatement(ins);

            for (Stock s : stocks) {
                ps.setString(1, s.getSymbol());
                ps.setDouble(2, s.getQuantity());
                ps.setDouble(3, s.getBuyPrice());
                ps.setString(4, s.getBuyDate());
                ps.setString(5, s.getSector());
                ps.addBatch();
            }

            ps.executeBatch();
            c.commit();
            ps.close();
        }
    }

    public List<Stock> findAll() throws SQLException {
        List<Stock> out = new ArrayList<>();

        try (Connection c = DriverManager.getConnection(url)) {
            String sql = "SELECT id, symbol, quantity, buy_price, buy_date, sector FROM portfolio_stock ORDER BY symbol";
            PreparedStatement ps = c.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Stock s = new Stock();
                s.setId(rs.getInt("id"));
                s.setSymbol(rs.getString("symbol"));
                // FIX: quantity must be double (do not cast to int)
                s.setQuantity(rs.getDouble("quantity"));
                s.setBuyPrice(rs.getDouble("buy_price"));
                s.setBuyDate(rs.getString("buy_date"));
                s.setSector(rs.getString("sector"));
                out.add(s);
            }

            rs.close();
            ps.close();
        }

        return out;
    }

    public void deleteBySymbol(String symbol) throws SQLException {
        try (Connection c = DriverManager.getConnection(url)) {
            String sql = "DELETE FROM portfolio_stock WHERE symbol = ?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, symbol.toUpperCase());
            ps.executeUpdate();
            ps.close();
        }
    }
}
