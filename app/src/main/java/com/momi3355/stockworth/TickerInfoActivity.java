package com.momi3355.stockworth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.DataTicketInfo;
import com.momi3355.stockworth.data.DataType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TickerInfoActivity extends AppCompatActivity {
    private final DataTicketInfo controller = new DataTicketInfo(this);
    private LoadingDialog loadingDialog;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;

    private final Runnable backgroundRunnable = new Runnable() {
        @Override
        public void run() {
            String ticker_id = (String)((TextView)findViewById(R.id.ticker_id)).getText();

            List<String> date = controller.getPreviousOpen(1);
            //UI를 변경하기위해서 사용하는 Thread
            runOnUiThread(() -> {
                String month = date.get(0).substring(4, 6);
                String day = date.get(0).substring(6, 8);
                String date_str = month+"월"+day+"일";
                ((TextView)findViewById(R.id.day)).setText(date_str);
            });

            ArrayList<String[]> tickerInfo = controller.getTickerInfo(date.get(1), date.get(0), ticker_id);
            runOnUiThread(() -> {
                TextView ticker_change_price = findViewById(R.id.ticker_change_price);
                Integer afterPrice = Integer.valueOf(tickerInfo.get(1)[4]);
                Integer beforePrice = Integer.valueOf(tickerInfo.get(0)[4]);
                String changePrice = ticker_change_price.getText() //부호 포함
                        + String.format(Locale.KOREA, "%,d원", (afterPrice - beforePrice));
                ticker_change_price.setText(changePrice);
            });

            runOnUiThread(() -> {
                String startPrice = String.format(Locale.KOREA, "%,d원",
                        Integer.valueOf(tickerInfo.get(1)[1]));
                ((TextView) findViewById(R.id.ticker_price_start)).setText(startPrice);

                String endPrice = String.format(Locale.KOREA, "%,d원",
                        Integer.valueOf(tickerInfo.get(1)[4]));
                ((TextView)findViewById(R.id.ticker_price_end)).setText(endPrice);

                String volume = String.format(Locale.KOREA, "%,d주",
                        Long.valueOf(tickerInfo.get(1)[5]));
                ((TextView)findViewById(R.id.ticker_volume)).setText(volume);

                String tradingValue = tickerInfo.get(1)[6];
                String tradingValue_str = tradingValue;
                Log.d("TickerInfoActivity", String.format(Locale.KOREA, "대금 %,d원", Long.valueOf(tradingValue)));
                if (tradingValue.length() >= 13) { //조원
                    String group = tradingValue.substring(0, tradingValue.length() - 12); //조단위 추출
                    tradingValue_str = String.format(Locale.KOREA, "%,d조%,d억원",
                            Long.valueOf(group), //조
                            Long.valueOf(tradingValue.substring(group.length(), tradingValue.length() - 8))); //억
                } else if (tradingValue.length() >= 9) { //억원
                    tradingValue_str = String.format(Locale.KOREA, "%,d억원",
                            Long.valueOf(tradingValue.substring(0, tradingValue.length() - 8)));
                } else if (tradingValue.length() >= 7) { //백만
                    tradingValue_str = String.format(Locale.KOREA, "%,d백만",
                            Long.valueOf(tradingValue.substring(0, tradingValue.length() - 6)));
                } else { //원
                    tradingValue_str = String.format(Locale.KOREA, "%,d원",
                            Long.valueOf(tradingValue));
                }
                ((TextView)findViewById(R.id.ticker_tradingValue)).setText(tradingValue_str);
            });

            //day_range (현재 1일 최저가, 1일 최고가 위치)
            //day_range_lowest (1일 최저가)
            //day_range_highest (1일 최고가)
            //year_range (현재 1년 최저가, 1년 최고가 위치)
            //year_range_lowest (1년 최저가)
            //year_range_highest (1년 최고가)

            loadingDialog.dismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticker_info);

        JSONObject ticker_object = null;
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
                    JSONObject ticker = market.getJSONObject(j);
                    if (tickerName.equals(ticker.getString("name"))) {
                        tickerId = ticker.getString("id"); //id를 검색
                        ticker_object = ticker;
                        break;
                    }
                }
            }

            if (!(tickerId == null)) {
                TextView ticker_id = findViewById(R.id.ticker_id);
                TextView ticker_name = findViewById(R.id.ticker_name);
                TextView ticker_now_price = findViewById(R.id.ticker_now_price);
                TextView ticker_change_price = findViewById(R.id.ticker_change_price);
                TextView ticker_rate = findViewById(R.id.ticker_rate);

                ticker_id.setText(tickerId);
                ticker_name.setText(tickerName);

                String tickerNowPrice_str = "";
                int tickerNowPrice = ticker_object.getInt("price");
                double tickerRate = ticker_object.getDouble("rate");
                if (tickerRate >= 0) {
                    ticker_change_price.setText("+");
                    ticker_change_price.setTextColor(getColor(R.color.red));
                    ticker_rate.setTextColor(getColor(R.color.red));

                } else {
                    ticker_change_price.setText("");
                    ticker_change_price.setTextColor(getColor(R.color.blue));
                    ticker_rate.setTextColor(getColor(R.color.blue));
                }
                String tickerRate_str = "("+tickerRate+"%)";
                Log.d("TickerInfoActivity", "onCreate: "+tickerNowPrice);
                tickerNowPrice_str += String.format(Locale.KOREA, "%,d원", tickerNowPrice);
                ticker_rate.setText(tickerRate_str);
                ticker_now_price.setText(tickerNowPrice_str);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("주식정보"); //엑선바에서 타이틀변경
            actionBar.setDisplayHomeAsUpEnabled(true); //뒤로가기 화살표 추가
        }
        //들어오는 애니메이션
        overridePendingTransition(R.anim.from_right_enter, R.anim.none);
        //로딩 다이얼로그 실행
        loadingDialog = new LoadingDialog(this);
        loadingDialog.show();

        //thread 설정
        handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        //로딩 루틴 실행
        backgroundHandler.post(backgroundRunnable);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundHandler!= null) {
            backgroundHandler.removeCallbacksAndMessages(null);
            if (handlerThread!= null) {
                handlerThread.quitSafely();
            }
        }
    }
}