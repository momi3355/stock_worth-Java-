package com.momi3355.stockworth.ui.home;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.DataType;
import com.momi3355.stockworth.R;
import com.momi3355.stockworth.databinding.FragmentHomeBinding;
import com.momi3355.stockworth.ui.market_info.MarketInfoViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    private final View.OnClickListener item_ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            //[layout 버튼 이벤트]
            // 1. Viewport를 [상세검색]이동.
            // 2. 해당 종목을 검색

            TextView textView = (TextView) ((LinearLayout) view).getChildAt(0); //마켓 이름.
            if (textView != null) {
                String text = textView.getText().toString();
                MarketInfoViewModel marketInfoVM = new ViewModelProvider(requireActivity()).get(MarketInfoViewModel.class);
                marketInfoVM.setMarketName(text);
            }
            // [view fragment 이동]
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_home, true) // 이전 목적지부터 시작하여 백스택에서 제거
                    .build();
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.navigation_market_info, null, navOptions);
        }
    };

    private final Observer<JSONArray> marketdata_observer = new Observer<JSONArray>() {
        @Override
        public void onChanged(JSONArray array) {
            //데이터가 수정되면 실행
            //사실 필요 없을 수도? (우선순위 낮음.)
            //UI 수정
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        JSONObject[] appData = AppData.getInstance().stockData;
        //HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        //homeViewModel.getMarket_data().observe(getViewLifecycleOwner(), marketdata_observer);
        
        TableLayout market_tableLayout = binding.allMarketTableLayout;

        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (isDarkMode) {
            View line = binding.line1;
            line.setBackgroundColor(Color.GRAY);
        }

        try {
            JSONArray data = appData[DataType.market_data.getIndex()].getJSONArray("data");

            TableRow tableRow = new TableRow(getActivity());
            int c = 0; //지수 총합이 Row가 2기때문에(1 ~ 2지정)
            for (int i = 0; i < data.length(); i++) {
                if (c >= 2) {
                    c = 0;
                    if (!(i == data.length() - 1)) {
                        market_tableLayout.addView(tableRow);
                        tableRow = new TableRow(getActivity());
                    }
                }
                LinearLayout item_layout = new LinearLayout(getActivity());
                //리플효과 추가(버튼 누르는 것 처럼)
                item_layout.setBackgroundResource(R.drawable.ripple_unbounded);
                item_layout.setOrientation(LinearLayout.VERTICAL);
                item_layout.setClickable(true);
                item_layout.setOnClickListener(item_ClickListener);
                JSONObject item = data.getJSONObject(i);
                String[] temp = new String[] {
                        item.getString("market_name"),
                        item.getDouble("rate") + "%",
                        item.getDouble("price") + "원"
                };
                for (int j = 0; j < temp.length; j++) {
                    TextView textView = new TextView(getActivity());
                    textView.setText(temp[j]);
                    if (j == 0) { //종목 이름
                        textView.setTextSize(21); //21pt
                        if (isDarkMode) {
                            textView.setTextColor(Color.WHITE);
                        } else textView.setTextColor(Color.BLACK);
                    } else {
                        textView.setTextSize(16); //16pt
                    }
                    item_layout.addView(textView);
                }

                tableRow.addView(item_layout);
                c++;
            }
            market_tableLayout.addView(tableRow);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}