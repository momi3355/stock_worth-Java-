package com.momi3355.stockworth;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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
    //market_data("market_data.json", 2);
    market_data("market_data.json", 0);

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

public class DataController {
    private final Python py;
    private final Context context;

    private final JSONObject[] stockData = new JSONObject[DataType.getLength()];

    public DataController(Context context) {
        this.context = context;
        if (!Python.isStarted()) {
            //싱글톤 패턴이라서 아무곳이나 getInstance()를 가지고 오면 사용이 가능.
            Python.start(new AndroidPlatform(this.context));
        }
        py = Python.getInstance();
    }

    public String getPreviousOpen() {
        PyObject stockObject = py.getModule("stock");
        return stockObject.callAttr("getPreviousOpen", "XKRX").toString();
    }

    private FileInputStream newFile(DataType dateType) throws IOException {
        FileOutputStream output = context.openFileOutput(dateType.getFileName(), Context.MODE_PRIVATE);
        PyObject stockObject = py.getModule("stock");
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        /* [휴장시간확인] */
        if (!Boolean.parseBoolean(stockObject.callAttr("isRunMarket", "XKRX").toString())) {
            //이전 개장시간 추출;
            now = stockObject.callAttr("getPreviousOpen", "XKRX").toString();
        }
        /* [주식 정보 추출] */
        String data_string = "";
        switch (dateType) {
//            case stock_data:
//                data_string = stockObject.callAttr("getMarketInfo", now).toString(); break;
//            case ticker_data:
//                data_string = stockObject.callAttr("getTickers", now).toString(); break;
            case market_data:
                data_string = stockObject.callAttr("getMarket", now).toString(); break;
            default:
                Log.e("DataController", "지금 파일포맷을 알 수 없습니다. ("+dateType+")");
        }
        if (data_string.equals("")) {
            //에러 표기 요함
        }
        /* [파일 쓰기] */
        output.write(data_string.getBytes(StandardCharsets.UTF_8));  //파일 저장
        output.close();
        return context.openFileInput(dateType.getFileName());
    }

    private FileInputStream getFileInputStream(DataType dateType) throws IOException {
        try {
            FileInputStream file = context.openFileInput(dateType.getFileName());
            if (file.available() == 0) //isEmpty();
                throw new FileNotFoundException(dateType.getFileName());
            return file;
        } catch (FileNotFoundException e) {
            return newFile(dateType);
        }
    }

    public void load() throws IOException, JSONException {
        for (int i = 0; i < DataType.getLength(); i++) {
            DataType dataType = DataType.values()[i];

            FileInputStream fileInput = getFileInputStream(dataType);
            stockData[i] = new JSONObject(getJsonString(fileInput));
            update(stockData[i], dataType); //일단 로딩한다음 업데이트 확인.
//            Log.d("DataController", "update_time: "+stockData[i].getString("update_time"));
//            JSONArray array_data = stockData[i].getJSONArray("data");
//            for (int j = 0; j < array_data.length(); j++) {
//                JSONObject data = array_data.getJSONObject(j);
//                Log.d("DataController", "data("+j+")_market_name: "+data.getString("market_name"));
//                Log.d("DataController", "data("+j+")_rate: "+data.getDouble("rate"));
//                Log.d("DataController", "data("+j+")_price: "+data.getDouble("price"));
//            }
            fileInput.close();
        }
    }

    public void update(JSONObject json, DataType dataType) throws JSONException, IOException {
        String updateTime = json.getString("update_time");
        String previousOpen = getPreviousOpen();

        Log.d("DataController", "time: "+updateTime+" - "+previousOpen);
        if (!updateTime.equals(previousOpen)) { //'업데이트 시간'과 '최근 개장일'를 비교
            FileInputStream input = newFile(dataType);
            stockData[dataType.getIndex()] = new JSONObject(getJsonString(input));
            input.close();
        }
    }

    public void update() throws IOException, JSONException {
        for (int i = 0; i < DataType.getLength(); i++) {
            DataType dataType = DataType.values()[i];
            //TODO : 이제 thread를 통해서 2분마다 업데이트를 하는 로직 필요.
            FileInputStream input = newFile(dataType);
            stockData[dataType.getIndex()] = new JSONObject(getJsonString(input));
            input.close();
        }
    }

    private String getJsonString(InputStream is) {
        String json = "";
        try {
            int fileSize = is.available();
            byte[] buffer = new byte[fileSize];
            is.read(buffer, 0, fileSize);

            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e("DataController", "getJsonString: "+e.getMessage());
        } finally {
            Log.d("DataController", "getJsonString(load): "+json.replace('\n', ' '));
        }
        return json;
    }
}
