-- Create table for stocks in portfolio
CREATE TABLE IF NOT EXISTS portfolio_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    symbol TEXT NOT NULL,
    quantity REAL NOT NULL,
    buy_price REAL NOT NULL,
    buy_date TEXT,
    sector TEXT
);

-- Simple index for symbol lookups
CREATE INDEX IF NOT EXISTS idx_symbol ON portfolio_stock(symbol);
