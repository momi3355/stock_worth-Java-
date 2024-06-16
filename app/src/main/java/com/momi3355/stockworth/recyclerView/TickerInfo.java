package com.momi3355.stockworth.recyclerView;

/**
 * RecyclerView에서 사용되는 데이터를 저장하는 클래스
 * @see RecyclerViewAdapter
 */
public class TickerInfo {
    String item_icon;
    String item_name;
    double item_price;
    double item_rate;
    long item_volume;

    public TickerInfo(String name, double price, double rate, long volume) {
        item_name = name;
        item_price = price;
        item_rate = rate;
        item_volume = volume;

        item_icon = item_name.charAt(0)+""+item_name.charAt(1); //한글은 2바이트
    }
}
