package com.momi3355.stockworth.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>();

    public void setSearchQuery(String searchQuery) {
        this.searchQuery.setValue(searchQuery);
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }
}