package com.momi3355.stockworth.ui.home;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chaquo.python.Python;
import com.momi3355.stockworth.AppData;
import com.momi3355.stockworth.DataController;
import com.momi3355.stockworth.DataType;
import com.momi3355.stockworth.R;
import com.momi3355.stockworth.databinding.FragmentHomeBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        JSONObject[] appData = AppData.getInstance().stockData;
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TableLayout market_tableLayout = root.findViewById(R.id.allMarket_tableLayout);
        try {
            JSONArray data = appData[DataType.market_data.getIndex()].getJSONArray("data");
            homeViewModel.setMarket_data(data);
            homeViewModel.getMarket_data().observe(getViewLifecycleOwner(), newArray -> { //데이터가 수정되면 실행
                //사실 필요 없을 수도? (우선순위 낮음.)
                //UI 수정
            });
            /* [지수 총합] */
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
                // TODO : 여기부터 진행(item_layout . Event)
                item_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //[layout 버튼 이벤트]
                        // 1. Viewport를 [상세검색]이동.
                        // 2. 해당 종목을 검색

                        // [상세 ViewModel에서 값 변경] (나중에)
                        // 1. 상세검색의 market_name을 변경 (사실 '코스피'밖에 없기 때문에 안해도 될지도?)
                        // 2. viewModel .observe를 발생시겨 값이 변경 된다.
                        //
                        LinearLayout linearLayout = (LinearLayout)v;
                        View child = linearLayout.getChildAt(0);
                        if (child instanceof TextView) {
                            TextView textView = (TextView)child;
                            Log.d("HomeFragment", "onClick: "+textView.getText());
                        }
                        // [view fragment 이동]
                        // FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        // HomeFragment homeFragment = new HomeFragment();
                        // //main_layout에 homeFragment로 transaction 한다.
                        // transaction.replace(R.id.main_layout, homeFragment);
                        // //꼭 commit을 해줘야 바뀐다.
                        // transaction.commit();
                        Log.d("HomeFragment", "onClick: "+v.getId());
                    }
                });
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
                        textView.setTextSize(21);
                        textView.setTextColor(Color.BLACK);
                    } else {
                        textView.setTextSize(16);
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