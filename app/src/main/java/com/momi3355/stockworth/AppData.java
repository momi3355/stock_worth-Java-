package com.momi3355.stockworth;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 어플에 대한 데이터를 저장하는 클래스.
 * <p>어느 곳이나 파일을 읽어야되기 때문에 싱글톤 패턴을 사용.</p>
 * 
 * @see JSONObject 데이터가 저장되는 형식.
 */
public class AppData {
    public final JSONObject[] stockData = new JSONObject[DataType.getLength()];
    private static AppData instance;

    private AppData() { }

    public static synchronized AppData getInstance() {
        if (instance == null)
            instance = new AppData();
        return instance;
    }
}