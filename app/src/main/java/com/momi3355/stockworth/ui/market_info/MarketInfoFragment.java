package com.momi3355.stockworth.ui.market_info;

import androidx.lifecycle.ViewModelProvider;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.DataType;
import com.momi3355.stockworth.recyclerView.RecyclerViewAdapter;
import com.momi3355.stockworth.recyclerView.TickerInfo;
import com.momi3355.stockworth.databinding.FragmentMarketInfoBinding;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

public class MarketInfoFragment extends Fragment {
    private static final int NEXT_LIMIT = 50;

    private final ArrayList<TickerInfo> rowsArrayList = new ArrayList<>();
    private final ArrayList<TickerInfo> searchList = new ArrayList<>();

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
            if (searchList.size() <= 0) {
                ticker_data = marketInfoViewModel.getMarketInfo().getValue();
                if (ticker_data == null) {
                    //초기화가 되어있지 않으면 기본값 불러오기
                    ticker_data = AppData.getInstance().stockData[DataType.stock_data.getIndex()]
                            .getJSONArray("data").getJSONObject(0).getJSONArray("stock_data");
                }
                for (int i = 0; i < NEXT_LIMIT; i++) {
                    TickerInfo tickerInfo = getTickerInfo(ticker_data, i);
                    rowsArrayList.add(tickerInfo);
                }
            } else { //찾은 데이터가 있으면
                for (int i = 0; i < NEXT_LIMIT; i++) {
                    rowsArrayList.add(searchList.get(i));
                }
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

        SearchView searchView = binding.searchView;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() { //검색리스너 장착
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(requireActivity(), "검색 완료", Toast.LENGTH_SHORT).show();
                if (searchList.isEmpty() && rowsArrayList.isEmpty()) { //데이터가 비여있으면
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
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchList.clear(); //기존데이터 삭제
                try {
                    JSONArray data = AppData.getInstance().stockData[DataType.stock_data.getIndex()]
                            .getJSONArray("data").getJSONObject(0).getJSONArray("stock_data");
                    for (int i = 0; i < data.length(); i++) {
                        String name = data.getJSONObject(i).getString("name");
                        String id = data.getJSONObject(i).getString("id");
                        if (name.contains(newText)) { //이름
                            searchList.add(getTickerInfo(data, i));
                        } else if (id.contains(newText)) { //id
                            searchList.add(getTickerInfo(data, i));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                rowsArrayList.clear(); //데이터 초기화
                if (searchList.size() > 0)
                    for (int i = 0; i < NEXT_LIMIT; i++)
                        if (searchList.size() > i)
                            rowsArrayList.add(searchList.get(i));

                boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

                // [recyclerViewAdapter 초기화]
                recyclerViewAdapter = new RecyclerViewAdapter(rowsArrayList, isDarkMode);
                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                recyclerView.setAdapter(recyclerViewAdapter);
                return true;
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
                if (rowsArrayList.isEmpty()) return; //없으면 종료
                rowsArrayList.remove(rowsArrayList.size() - 1);
                int scrollPosition = rowsArrayList.size();
                recyclerViewAdapter.notifyItemRemoved(scrollPosition);
                int currentSize = scrollPosition;
                int nextLimit = currentSize + NEXT_LIMIT;
                //스크롤 맥스치를 변경한 후에 데이터 투입
                while (currentSize - 1 < nextLimit) {
                    try {
                        TickerInfo tickerInfo;
                        if (searchList.size() <= 0) {
                            tickerInfo = getTickerInfo(ticker_data, currentSize - 1);
                        } else { //찾은 데이터가 있으면
                            if (searchList.size() > nextLimit && searchList.size() > (currentSize - 1))
                                tickerInfo = searchList.get(currentSize - 1);
                            else break;
                        }
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
        int price = data.getJSONObject(i).getInt("price");
        double rate = data.getJSONObject(i).getDouble("rate");
        long volume = data.getJSONObject(i).getLong("volume");

        return new TickerInfo(name, price, rate, volume);
    }
}