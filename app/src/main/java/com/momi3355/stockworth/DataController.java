package com.momi3355.stockworth;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DataController {
    private final Python py;
    private final Context context;
    final AppData data;

    public DataController(Context context) {
        this.context = context;
        if (!Python.isStarted()) {
            //싱글톤 패턴이라서 아무곳이나 getInstance()를 가지고 오면 사용이 가능.
            Python.start(new AndroidPlatform(this.context));
        }
        py = Python.getInstance();
        data = AppData.getInstance();
    }

    public String getPreviousOpen() {
        PyObject stockObject = py.getModule("stock");
        return stockObject.callAttr("getPreviousOpen", "XKRX").toString();
    }

    private FileInputStream newFile(DataType dataType) throws IOException {
        FileOutputStream output = context.openFileOutput(dataType.getFileName(), Context.MODE_PRIVATE);
        PyObject stockObject = py.getModule("stock");
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        /* [휴장시간확인] */
        if (!Boolean.parseBoolean(stockObject.callAttr("isRunMarket", "XKRX").toString())) {
            //이전 개장시간 추출;
            now = stockObject.callAttr("getPreviousOpen", "XKRX").toString();
        }
        /* [주식 정보 추출] */
        String data_string = "";
        switch (dataType) {
            case stock_data:
                data_string = stockObject.callAttr("getMarketInfo", now).toString(); break;
            case ticker_data:
                data_string = stockObject.callAttr("getTickers", now).toString(); break;
            case market_data:
                data_string = stockObject.callAttr("getMarket", now).toString(); break;
            default:
                Log.e("DataController", "지금 파일포맷을 알 수 없습니다. ("+dataType+")");
        }
        if (data_string.equals("")) {
            //에러 표기 요함
        }
        /* [파일 쓰기] */
        output.write(data_string.getBytes(StandardCharsets.UTF_8));  //파일 저장
        output.close();
        return context.openFileInput(dataType.getFileName());
    }

    private FileInputStream getFileInputStream(DataType dataType) throws IOException {
        try {
            FileInputStream file = context.openFileInput(dataType.getFileName());
            if (file.available() == 0) //isEmpty();
                throw new FileNotFoundException(dataType.getFileName());
            return file;
        } catch (FileNotFoundException e) {
            Log.e("DataController", "getFileInputStream: "+e.getMessage());
            return newFile(dataType);
        }
    }

    public void load() throws IOException, JSONException {
        for (int i = 0; i < DataType.getLength(); i++) {
            DataType dataType = DataType.values()[i];

            FileInputStream fileInput = getFileInputStream(dataType);
            try {
                data.stockData[i] = new JSONObject(DataController.getJsonString(fileInput));
                JSONArray array_data = data.stockData[dataType.getIndex()].getJSONArray("data");
                if (array_data.length() == 0) //정보가 없을 경우
                    throw new FileNotFoundException(dataType.getFileName());
            } catch (NullPointerException | JSONException e) {
                /* 여기오는 경우 */
                // 1. JSON에서 data겍체를 찾을 수 없는 경우.
                // 2. 위에 있는 if (array_data.length() == 0) 에서 정보을 찾을 수 없는 경우.
                // 3. JSON파일이 손상된 경우.
                Log.w("DataController", "load: "+e.getMessage());
                fileInput.close(); // 파일 닫고, 다시 열기
                fileInput = newFile(dataType);
            } finally {
                update(data.stockData[i], dataType); //일단 로딩한다음 업데이트 확인.
//                Log.d("DataController", "update_time: "+data.stockData[i].getString("update_time"));
//                JSONArray array_data = data.stockData[i].getJSONArray("data");
//                for (int j = 0; j < array_data.length(); j++) {
//                    JSONObject item = array_data.getJSONObject(j);
//                    Log.d("DataController", "data("+j+")_market_name: "+item.getString("market_name"));
//                    Log.d("DataController", "data("+j+")_rate: "+item.getDouble("rate"));
//                    Log.d("DataController", "data("+j+")_price: "+item.getDouble("price"));
//                }
                fileInput.close();
            }
        }
    }

    public void update(JSONObject json, DataType dataType) throws JSONException, IOException {
        String updateTime = json.getString("update_time");
        String previousOpen = getPreviousOpen();

        Log.d("DataController", "time: "+updateTime+" - "+previousOpen);
        if (!updateTime.equals(previousOpen)) { //'업데이트 시간'과 '최근 개장일'를 비교
            int diff = Integer.parseInt(updateTime) - Integer.parseInt(previousOpen);
            if (diff == 1 && LocalTime.now().getHour() < 9) return; //이른 아침
            FileInputStream input = newFile(dataType);
            data.stockData[dataType.getIndex()] = new JSONObject(DataController.getJsonString(input));
            input.close();
        }
    }

    public void update() throws IOException, JSONException {
        for (int i = 0; i < DataType.getLength(); i++) {
            DataType dataType = DataType.values()[i];
            FileInputStream input = newFile(dataType);
            data.stockData[dataType.getIndex()] = new JSONObject(DataController.getJsonString(input));
            input.close();
        }
    }

    private static String getJsonString(InputStream is) {
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
