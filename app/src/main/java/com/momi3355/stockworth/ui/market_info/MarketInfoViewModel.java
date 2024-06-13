package com.momi3355.stockworth.ui.market_info;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MarketInfoViewModel extends ViewModel {

    private final MutableLiveData<String> marketName = new MutableLiveData<>();

    public MarketInfoViewModel() {

    }

    public void setMarketName(String name) {
        marketName.setValue(name);
    }

    public LiveData<String> getMarketName() {
        return marketName;
    }
}