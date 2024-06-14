package com.momi3355.stockworth.ReclerView;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.momi3355.stockworth.R;

import java.util.List;
import java.util.Random;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;

    public List<TickerInfo> mItemList;
    public RecyclerViewAdapter(List<TickerInfo> itemList) {
        mItemList = itemList;
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
        return mItemList == null ? 0 : mItemList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItemList.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }


    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemIcon;
        TextView itemName;
        TextView itemPrice;
        TextView itemRate;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            itemIcon = itemView.findViewById(R.id.item_icon);
            itemName = itemView.findViewById(R.id.item_name);
            itemPrice = itemView.findViewById(R.id.item_price);
            itemRate = itemView.findViewById(R.id.item_rate);
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
        TickerInfo item = mItemList.get(position);
        String price_str = item.item_price + "원";
        String rate_str = item.item_rate + "%";
        if (item.item_rate >= 0) {
            if (!(item.item_rate == 0))
                rate_str = "+" + rate_str;
            viewHolder.itemRate.setTextColor(Color.RED);
        } else {
            viewHolder.itemRate.setTextColor(Color.BLUE);
        }

        viewHolder.itemIcon.setText(item.item_icon);
        changeIconColor(viewHolder.itemIcon);
        viewHolder.itemName.setText(item.item_name);
        viewHolder.itemPrice.setText(price_str);
        viewHolder.itemRate.setText(rate_str);
    }

    private void changeIconColor(TextView view) {
        // 랜덤 색상 생성
        Random rnd = new Random();
        int color = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        // TextView 배경색 변경
        view.setBackgroundColor(color);
    }
}
