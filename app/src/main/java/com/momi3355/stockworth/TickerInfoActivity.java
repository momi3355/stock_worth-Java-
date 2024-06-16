package com.momi3355.stockworth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.DataType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TickerInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticker_info);

        JSONObject ticker = null;
        String tickerId = null;
        try {
            JSONArray appData = AppData.getInstance().stockData[DataType.stock_data.getIndex()].getJSONArray("data");
            Intent intent = getIntent();
            //Activity가 이동하기전에 전달받은 변수.
            String tickerName = intent.getStringExtra("ticker_name");
            Log.d("TickerInfoActivity", "onCreate: "+tickerName);
            // MainActivity에서 검색창으로 올 수 있으니깐 천체 검색한다.
            for (int i = 0; i < appData.length(); i++) {
                JSONArray market = appData.getJSONObject(i).getJSONArray("stock_data");
                for (int j = 0; j < market.length(); j++) {
                    ticker = market.getJSONObject(j);
                    if (tickerName.equals(ticker.getString("name")))
                        tickerId = ticker.getString("id"); //id를 검색
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!(tickerId == null)) {
            Log.d("TickerInfoActivity", "onCreate: " + tickerId);
            // TODO : 여기부터 진행(상세정보 웹상에서 불러오고 보여주기)
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("주식정보"); //엑선바에서 타이틀변경
            actionBar.setDisplayHomeAsUpEnabled(true); //뒤로가기 화살표 추가
        }
        //들어오는 애니메이션
        overridePendingTransition(R.anim.from_right_enter, R.anim.none);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //뒤로 가기를 눌렀을 때 발동하는 이벤트
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        //나가는 애니메이션
        overridePendingTransition(R.anim.none, R.anim.to_right_exit);
    }
}