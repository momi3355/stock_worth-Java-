package com.momi3355.stockworth;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

enum DataType {
    //stock_data("stock_data.json", 0),
    //ticker_data("ticker_data.json", 1),
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

//이놈은 싱글톤 으로 .
public class DataController {

    private final Python py;
    private final Context context;

    private final JSONObject[] stockData = new JSONObject[DataType.getLength()];


    public DataController(Context context, Python py) {
        this.context = context;
        this.py = py;
    }

    public String newFile(String fileName) {
        PyObject stockObject = py.getModule("stock");
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        now += "08";
        switch (fileName) {
            case "stock_data.json":
                return stockObject.callAttr("getMarketInfo", now).toString();
            case "ticker_data.json":
                return stockObject.callAttr("getTickers", now).toString();
            case "market_data.json":
                return stockObject.callAttr("getMarket", now).toString();
            default:
        }
        return "";
    }

    public FileInputStream getFileInputStream(String fileName) throws IOException {
        try {
            return context.openFileInput(fileName);
        } catch (FileNotFoundException e) {
            FileOutputStream output = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            String file = newFile(fileName);
            Log.d("DataController", "getFileInputStream: "+file);
            output.write(file.getBytes(StandardCharsets.UTF_8));  //파일 저장
            output.close();
            return context.openFileInput(fileName);
        }
    }

    public void load() throws IOException, JSONException {
        for (int i = 0; i < DataType.getLength(); i++) {
            FileInputStream fileInput = getFileInputStream(DataType.values()[i].getFileName());
            String temp = getJsonString(fileInput);
            Log.d("DataController", "load: "+temp);
            //stockData[i] = new JSONObject(getJsonString(fileInput));
            fileInput.close();
        }
    }

    public void save() {
        //stockData[DataType.stock_data.getIndex()].get("dd");
        //openFileOutput()
    }

    private String getJsonString(InputStream is) {
        String json = "";
        try {
            int fileSize = is.available();
            byte[] buffer = new byte[fileSize];
            is.read(buffer, 0, fileSize);
            is.close();

            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e("DataController", "getJsonString: "+e.getMessage());
        }
        return json;
    }
}
