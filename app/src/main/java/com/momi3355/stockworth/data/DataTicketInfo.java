package com.momi3355.stockworth.data;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.ArrayList;
import java.util.List;

public class DataTicketInfo {
    private final PyObject stockObject;
    final AppData data;

    public DataTicketInfo(Context context) {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(context));
        }
        Python py = Python.getInstance();
        data = AppData.getInstance();
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

    public ArrayList<String[]> getTickerInfo(String date1, String date2, String ticker_id) {
        PyObject result = stockObject.callAttr("getTickerInfo", date1, date2, ticker_id);
        String[] result_list = result.toString().split("\n");
        ArrayList<String[]> list = new ArrayList<>();
        for (int i = 2; i < result_list.length; i++) {
            Log.d("DataTicketInfo", result_list[i]);
            list.add(result_list[i].split("\\s+")); //"\\s+"는 하나 이상의 공백을 의미
        }

        return list;
    }
}
