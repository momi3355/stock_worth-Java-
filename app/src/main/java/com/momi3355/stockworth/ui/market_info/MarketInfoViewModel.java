package com.momi3355.stockworth.ui.market_info;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.DataType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MarketInfoViewModel extends ViewModel {
    private final MutableLiveData<CharSequence> marketName = new MutableLiveData<>();
    private final MutableLiveData<JSONArray> marketInfo = new MutableLiveData<>();

    public MarketInfoViewModel() throws JSONException {
        JSONArray stock_array = AppData.getInstance().stockData[DataType.stock_data.getIndex()].getJSONArray("data");

        JSONObject market = stock_array.getJSONObject(0);
        String market_name = market.getString("market_name");
        JSONArray market_info = market.getJSONArray("stock_data");

        setMarketName(market_name);
        setMarketInfo(market_info);
    }

    public void setMarketName(CharSequence name) {
        //marketName.postValue(name);
        marketName.postValue("KOSPI"); //지금 코스피 밖에 없어서.
    }

    public void setMarketInfo(JSONArray info) {
        marketInfo.postValue(info);
    }

    public LiveData<CharSequence> getMarketName() {
        return marketName;
    }

    public LiveData<JSONArray> getMarketInfo() {
        return marketInfo;
    }
}