package com.momi3355.stockworth.ui.market_info;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.momi3355.stockworth.AppData;
import com.momi3355.stockworth.DataType;
import com.momi3355.stockworth.R;
import com.momi3355.stockworth.databinding.FragmentMarketInfoBinding;
import com.momi3355.stockworth.databinding.FragmentNotificationsBinding;

import org.jetbrains.annotations.ApiStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MarketInfoFragment extends Fragment {
    private FragmentMarketInfoBinding binding;
    private TextView market_textView;

    private final Observer<String> MarketName_observer = new Observer<String>() {
        @Override
        public void onChanged(String marketName) {
            View root = binding.getRoot();
            // TODO : 여기부터 진행
            Toast.makeText(getActivity(), marketName, Toast.LENGTH_SHORT).show();

            market_textView = root.findViewById(R.id.market_name);
            Log.d("MarketInfoFragment", "onViewCreated: "+(market_textView == null));
//            if (!market_textView.getText().equals(marketName)) { //이름이 같지 않으면
//                market_textView.setText(marketName);
//                ListView list_view = root.findViewById(R.id.market_info_list);
//            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        MarketInfoViewModel marketInfoViewModel = new ViewModelProvider(this).get(MarketInfoViewModel.class);

        binding = FragmentMarketInfoBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        marketInfoViewModel.getMarketName().observe(getViewLifecycleOwner(), MarketName_observer);
        try {
            //1. 'Appdata'의 인스턴스를 가지고 온다.
            //2. 'stockData'의 배열에서 'DataType.stock_data'의 인덱스를 추출
            //3. 'stockData[]'의 데이터에서 JSONArray(data)를 추출.
            //4. 'JSONArray'에서 0번째의 'market_name'이라는 데이터를 추출
            JSONArray stock_array = AppData.getInstance().stockData[DataType.stock_data.getIndex()].getJSONArray("data");
            String market_name = stock_array.getJSONObject(0).getString("market_name");
            marketInfoViewModel.setMarketName(market_name);
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