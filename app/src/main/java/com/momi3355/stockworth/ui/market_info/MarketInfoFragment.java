package com.momi3355.stockworth.ui.market_info;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.momi3355.stockworth.MainActivity;
import com.momi3355.stockworth.R;
import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.DataType;
import com.momi3355.stockworth.recyclerView.RecyclerViewAdapter;
import com.momi3355.stockworth.recyclerView.TickerInfo;
import com.momi3355.stockworth.databinding.FragmentMarketInfoBinding;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class MarketInfoFragment extends Fragment {
    private static final int NEXT_LIMIT = 50;

    private final ArrayList<TickerInfo> rowsArrayList = new ArrayList<>();

    private FragmentMarketInfoBinding binding;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;

    private JSONArray ticker_data;

    private boolean isLoading = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketInfoBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView marketName_textView = binding.marketName;
        MarketInfoViewModel marketInfoViewModel = new ViewModelProvider(requireActivity()).get(MarketInfoViewModel.class);
        marketInfoViewModel.getMarketName().observe(getViewLifecycleOwner(), marketName_textView::setText);

        rowsArrayList.clear(); //데이터 초기화

        // [rowsArrayList 초기화]
        try {
            ticker_data = marketInfoViewModel.getMarketInfo().getValue();
            if (ticker_data == null) {
                //초기화가 되어있지 않으면 기본값 불러오기
                ticker_data = AppData.getInstance().stockData[DataType.stock_data.getIndex()]
                        .getJSONArray("data").getJSONObject(0).getJSONArray("stock_data");
            }
            for (int j = 0; j < NEXT_LIMIT; j++) {
                TickerInfo tickerInfo = getTickerInfo(ticker_data, j);
                rowsArrayList.add(tickerInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // [recyclerView 설정]
        recyclerView = binding.marketInfoList;

        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        // [recyclerViewAdapter 초기화]
        recyclerViewAdapter = new RecyclerViewAdapter(rowsArrayList, isDarkMode);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(recyclerViewAdapter);

        // [recyclerView 리스너 장착]
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == rowsArrayList.size() - 1) {
                        //bottom of list!
                        loadMore();
                        isLoading = true;
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 현제 스크롤을 최대치를 도달했을 때 실행하는 메소드
     */
    private void loadMore() {
        rowsArrayList.add(null);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                rowsArrayList.remove(rowsArrayList.size() - 1);
                int scrollPosition = rowsArrayList.size();
                recyclerViewAdapter.notifyItemRemoved(scrollPosition);
                int currentSize = scrollPosition;
                int nextLimit = currentSize + NEXT_LIMIT;
                //스크롤 맥스치를 변경한 후에 데이터 투입
                while (currentSize - 1 < nextLimit) {
                    try {
                        TickerInfo tickerInfo = getTickerInfo(ticker_data, currentSize - 1);
                        rowsArrayList.add(tickerInfo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    currentSize++;
                }
                isLoading = false;
            }
        }, 2000); // 2초
    }

    @NonNull
    private static TickerInfo getTickerInfo(JSONArray data, int i) throws JSONException {
        String name = data.getJSONObject(i).getString("name");
        double price = data.getJSONObject(i).getDouble("price");
        double rate = data.getJSONObject(i).getDouble("rate");
        long volume = data.getJSONObject(i).getLong("volume");

        return new TickerInfo(name, price, rate, volume);
    }
}