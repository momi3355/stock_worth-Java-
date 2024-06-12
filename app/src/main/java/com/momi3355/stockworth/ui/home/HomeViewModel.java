package com.momi3355.stockworth.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.momi3355.stockworth.AppData;
import com.momi3355.stockworth.DataType;

import org.json.JSONArray;
import org.json.JSONException;

public class HomeViewModel extends ViewModel {
    private MutableLiveData<JSONArray> market_data = new MutableLiveData<>();

    public HomeViewModel() {

    }

    public void setMarket_data(JSONArray jsonArray) {
        market_data.postValue(jsonArray);
    }

    public LiveData<JSONArray> getMarket_data() {
        return market_data;
    }
}