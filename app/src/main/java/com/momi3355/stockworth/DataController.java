package com.momi3355.stockworth;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

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

//이놈은 싱글톤 으로 . (2개이상 있으면 안되기 때문,)
public class DataController {

    private final Python py;
    private final Context context;

    private final JSONObject[] stockData = new JSONObject[DataType.getLength()];


    public DataController(Context context, Python py) {
        this.context = context;
        this.py = py;
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
        if (!Boolean.getBoolean(stockObject.callAttr("isRunMarket", "XKRX").toString())) {
            //이전 개장시간 추출;
            now = stockObject.callAttr("getPreviousOpen", "XKRX").toString();
        }
        /* [주식 정보 추출] */
        String data_string = "";
        switch (dateType) {
//            case stock_data:
//                data_string = stockObject.callAttr("getMarketInfo", now).toString();
//            case ticker_data:
//                data_string = stockObject.callAttr("getTickers", now).toString();
            case market_data:
                data_string = stockObject.callAttr("getMarket", now).toString();
            default:
                Log.e("DataController", "지금 파일포맷을 알 수 없습니다.");
        }
        if (data_string.equals("")) {
            //에러 표기 요함
        }
        Log.d("DataController", "getFileInputStream: "+data_string);
        /* [파일 쓰기] */
        output.write(data_string.getBytes(StandardCharsets.UTF_8));  //파일 저장
        output.close();
        return context.openFileInput(dateType.getFileName());
    }

    private FileInputStream getFileInputStream(DataType dateType) throws IOException {
        try {
            return context.openFileInput(dateType.getFileName());
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
            Log.d("DataController", "load: "+json.replace('\n', ' '));
        }
        return json;
    }
}
