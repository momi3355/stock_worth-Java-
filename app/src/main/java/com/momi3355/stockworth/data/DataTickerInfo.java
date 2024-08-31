package com.momi3355.stockworth.data;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DataTickerInfo {
    private final PyObject stockObject;

    public DataTickerInfo(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }
        Python py = Python.getInstance();
        stockObject = py.getModule("stock");
    }

    public List<String> getPreviousOpen(int count) {
        PyObject result = stockObject.callAttr("getPreviousOpen_count", "XKRX", count);
        List<PyObject> pythonList = result.asList();
        // 각 PyObject를 Integer로 변환하여 ArrayList<Integer>를 구성
        List<String> list = new ArrayList<>();
        for (PyObject item : pythonList) {
            list.add(item.toJava(String.class));
        }
        return list;
    }

    public String[] getMarketInfo(String date) {
        PyObject result = stockObject.callAttr("getMarketInfo", date, "KOSPI");
        return result.toString().split("\\s+");
    }

    public ArrayList<String[]> getTickerInfo(String date1, String date2, String ticker_id) {
        return getTickerInfo(date1, date2, "d", ticker_id);
    }

    public ArrayList<String[]> getTickerInfo(String date1, String date2, String format, String ticker_id) {
        PyObject result = stockObject.callAttr("getTickerInfo", date1, date2, format, ticker_id);
        String[] result_list = result.toString().split("\n");
        ArrayList<String[]> list = new ArrayList<>();
        for (int i = 2; i < result_list.length; i++) {
            //Log.d("DataTicketInfo", result_list[i]);
            list.add(result_list[i].split("\\s+")); //"\\s+"는 하나 이상의 공백을 의미
        }
        return list;
    }

    public ArrayList<String[]> getTickerChartInfo(String ticker_id) {
        final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

        LocalDate now = LocalDate.now();
        LocalDate before = now.minus(2, ChronoUnit.MONTHS);
        return getTickerInfo(before.format(dateFormat), now.format(dateFormat), ticker_id);
    }
}
