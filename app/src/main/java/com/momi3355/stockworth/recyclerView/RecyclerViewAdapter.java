package com.momi3355.stockworth.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.momi3355.stockworth.R;
import com.momi3355.stockworth.TickerInfoActivity;
import com.momi3355.stockworth.data.AppData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;

    private final List<TickerInfo> itemList;
    private SharedPreferences prefs;
    private final boolean isDarkMode;

    private int bg_toggle = 0;

    public RecyclerViewAdapter(List<TickerInfo> list, SharedPreferences prefs, boolean isDarkMode) {
        itemList = list;

        this.prefs = prefs;
        this.isDarkMode = isDarkMode;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_row, parent, false);
            return new ItemViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ItemViewHolder) {
            populateItemRows((ItemViewHolder) viewHolder, position);
        } else if (viewHolder instanceof LoadingViewHolder) {
            showLoadingView((LoadingViewHolder) viewHolder, position);
        }

    }

    @Override
    public int getItemCount() {
        return itemList == null ? 0 : itemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }


    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        Context context; //tickerInfoActivity로 이동하기 위함
        CardView itemRow; //이거는 배경을 바꾸기 위함

        TextView itemIcon;
        TextView itemName;
        TextView itemPrice;
        TextView itemRate;

        CheckBox itemFavorite;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            itemRow = itemView.findViewById(R.id.item_row);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemName = itemView.findViewById(R.id.item_name);
            itemPrice = itemView.findViewById(R.id.item_price);
            itemRate = itemView.findViewById(R.id.item_rate);

            itemFavorite = itemView.findViewById(R.id.item_favorite);
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.recyclerView_progressBar);
        }
    }

    private void showLoadingView(LoadingViewHolder viewHolder, int position) {
        //로딩될때 실행되는 함수.

    }

    private void populateItemRows(ItemViewHolder viewHolder, int position) {
        AppData appData = AppData.getInstance();
        TickerInfo item = itemList.get(position);

        String price_str = String.format(Locale.KOREA, "%,d원", item.item_price);
        String rate_str = item.item_rate + "%";
        Context context = viewHolder.context;
        if (item.item_rate >= 0) {
            if (!(item.item_rate == 0))
                rate_str = "+" + rate_str;
            viewHolder.itemRate.setTextColor(context.getColor(R.color.red));
        } else {
            viewHolder.itemRate.setTextColor(context.getColor(R.color.blue));
        }

        TextView itemIcon = viewHolder.itemIcon;
        GradientDrawable magnitudeCircle = (GradientDrawable)itemIcon.getBackground();
        magnitudeCircle.setColor(getRandomColor());

        itemIcon.setText(item.item_icon);
        viewHolder.itemName.setText(item.item_name);
        viewHolder.itemPrice.setText(price_str);
        viewHolder.itemRate.setText(rate_str);

        if (isDarkMode) {
            if (bg_toggle == 0) {
                viewHolder.itemRow.setBackgroundColor(Color.parseColor("#00071D")); //penn_blue
            } else { //짝수
                //홀수 보다 8감소
                viewHolder.itemRow.setBackgroundColor(Color.parseColor("#000715"));
            }
            bg_toggle = bg_toggle == 0 ? 1 : 0; //토글 스위치
        } else viewHolder.itemName.setTextColor(Color.BLACK);
        HashSet<String> favoriteData = appData.favoriteData;

        viewHolder.itemFavorite.setChecked(false); //기본값
        for (String temp : favoriteData) {
            if (temp.contentEquals(viewHolder.itemName.getText())) {
                viewHolder.itemFavorite.setChecked(true);
            }
        }
        viewHolder.itemFavorite.setOnClickListener(view -> {
            String name = prefs.getString("favoriteStock", "없음");
            CharSequence itemName = viewHolder.itemName.getText();
            boolean isChecked = viewHolder.itemFavorite.isChecked();
            Log.d("MarketInfoFragment", "populateItemRows: "+itemName+"_버튼 누름");
            if (isChecked) { //즐겨찾기 추가
                Toast.makeText(context, itemName+" 즐겨찾기 추가", Toast.LENGTH_SHORT).show();
                favoriteData.add((String)itemName);
            } else {
                Toast.makeText(context, itemName+" 즐겨찾기 취소", Toast.LENGTH_SHORT).show();
                if (name.equals((String)itemName)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("favoriteStock", "없음");
                    editor.apply();
                }
                favoriteData.remove((String)itemName);
            }

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("data", new JSONArray(favoriteData));

                Log.d("MarketInfoFragment", "json : "+jsonObject);

                FileOutputStream outputStream = context.openFileOutput("favoriteData.json", Context.MODE_PRIVATE);
                outputStream.write(jsonObject.toString().getBytes());
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        });
        viewHolder.itemRow.setClickable(true);
        viewHolder.itemRow.setOnClickListener(view -> {
            Intent intent = new Intent(context, TickerInfoActivity.class);
            intent.putExtra("ticker_name", viewHolder.itemName.getText());
            //Activity를 변경하기전에 메게변수를 전달한다.
            context.startActivity(intent);
        });
    }

    private static int getRandomColor() {
        // 랜덤 색상 생성
        Random rnd = new Random();
        return Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }
}
