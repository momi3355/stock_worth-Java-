package com.momi3355.stockworth.data;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.momi3355.stockworth.Server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataController {
    private final Context context;
    private final PyObject stockObject;
    private final HashMap<String, String> tickerMap = new HashMap<>();
    final AppData data;

    public DataController(Context context) {
        this.context = context;
        if (!Python.isStarted()) {
            //싱글톤 패턴이라서 아무곳이나 getInstance()를 가지고 오면 사용이 가능.
            Python.start(new AndroidPlatform(this.context));
        }
        Python py = Python.getInstance();
        stockObject = py.getModule("stock");
        data = AppData.getInstance();
    }

    @Deprecated
    public String getPreviousOpen() {
        return stockObject.callAttr("getPreviousOpen", "XKRX").toString();
    }

    public boolean isPreviousOpen() {
        return Boolean.parseBoolean(stockObject.callAttr("isRunMarket", "XKRX").toString());
    }

    private String getServerData(DataType dataType) throws IOException {
        /* [주식 정보 추출] */
        String url = Server.URL;
        String data_string = "";
        try {
            switch (dataType) {
                case stock_data:
                case market_data:
                    data_string = Jsoup.connect(url + dataType + ".json").ignoreContentType(true).execute().body();
                    break;
                default:
                    Log.e("DataController", "지금 파일포맷을 알 수 없습니다. (" + dataType + ")");
            }
        } catch (ConnectException e) {
            throw new IOException("504 error (서버가 오프라인이거나 올바르지 않는 'url' 입니다.)");
        }
        return data_string;
    }

    public void load() {
        for (int i = 0; i < DataType.getLength(); i++) {
            DataType dataType = DataType.values()[i];
            try {
                String json_data = getServerData(dataType);
                data.stockData[i] = new JSONObject(json_data);
                JSONArray array_data = data.stockData[dataType.getIndex()].getJSONArray("data");
                if (array_data.length() == 0) //정보가 없을 경우
                    throw new IOException("404 error");
                else { //정보가 없으면 정상적인 데이터가 아니기 때문에 하면 안된다.
                    try {
                        //백업 파일 생성(오프라인상태에서도 작동되게)
                        FileOutputStream outputStream = context.openFileOutput(dataType + ".json", Context.MODE_PRIVATE);
                        outputStream.write(json_data.getBytes());
                    } catch (IOException ex) {
                        Log.d("DataController", "백업 파일 생성하는 도중 파일을 쓸 수가 없습니다.");
                    }
                }
            } catch (Exception e) {
                /* [여기오는 경우] */
                // 1. 서버가 올바르지 않는 경우 or 서버가 오프라인인 경우
                // 2. JSON에서 data겍체를 찾을 수 없는 경우.
                // 3. 위에 있는 if (array_data.length() == 0) 에서 정보을 찾을 수 없는 경우.
                // 4. JSON파일이 손상된 경우.

                if (e instanceof IOException) {
                    Log.e("DataController", "server : " + e.getMessage());
                    //new Handler(context.getMainLooper()).post(()
                    //        -> Toast.makeText(context, "서버가 오프라인 입니다.", Toast.LENGTH_SHORT).show());
                } else { // JSONException
                    Log.e("DataController", "json file : error");
                }

                //오프라인 모드
                try {
                    FileInputStream inputStream = context.openFileInput(dataType + ".json");
                    data.stockData[i] = new JSONObject(getJsonString(inputStream));
                } catch (IOException ex) {
                    Log.d("DataController", "백업 파일을 불러오지 못하였습니다.");
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }

        // 즐겨찾기
        try {
            FileInputStream inputStream = context.openFileInput("favoriteData.json");
            JSONObject favorite = new JSONObject(getJsonString(inputStream));
            JSONArray array = favorite.getJSONArray("data");
            for (int j = 0; j < array.length(); j++)
                data.favoriteData.add(array.getString(j));
        } catch (FileNotFoundException e) {
            Log.d("DataController", "favoriteData.json 파일이 없습니다.");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        LocalTime now = LocalTime.now();
        //장시간이 아닐때에는 업데이트를 진행되지 않는다.
        if (now.getHour() < 9 || now.getHour() > 18) return;
        load();
//        for (int i = 0; i < DataType.getLength(); i++) {
//            DataType dataType = DataType.values()[i];
//            String input = getServerData(dataType);
//            data.stockData[dataType.getIndex()] = new JSONObject(input);
//        }
    }

    public void setTickerMap() {
        PyObject result = stockObject.callAttr("getTickers");
        Map<PyObject, PyObject> pythonList = result.asMap();
        // 각 PyObject를 Integer로 변환하여 ArrayList<Integer>를 구성
        for (PyObject key : pythonList.keySet()) {
            PyObject value = pythonList.get(key);
            if (value != null) {
                tickerMap.put(key.toJava(String.class), value.toJava(String.class));
            } else {
                Log.e("DataController", "setTickerMap: value가 없습니다.");
                return;
            }
        }
    }

    public HashMap<String, String> getTickerMap() {
        return tickerMap;
    }

    public static String getJsonString(InputStream is) {
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
