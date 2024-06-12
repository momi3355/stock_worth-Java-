package com.momi3355.stockworth;

public enum DataType {
    stock_data("stock_data.json", 0),
    ticker_data("ticker_data.json", 1),
    market_data("market_data.json", 2);

    private final String fileName;
    private final int index;

    DataType(String fileName, int index) {
        this.fileName = fileName;
        this.index = index;
    }

    public String getFileName() {
        return fileName;
    }

    public int getIndex() {
        return index;
    }

    public static int getLength() {
        return DataType.values().length;
    }
}
