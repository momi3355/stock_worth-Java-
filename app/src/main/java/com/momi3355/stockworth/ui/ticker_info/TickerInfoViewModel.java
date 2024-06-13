package com.momi3355.stockworth.ui.ticker_info;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TickerInfoViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public TickerInfoViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is ticker info fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}