package com.momi3355.stockworth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.DataTicketInfo;
import com.momi3355.stockworth.data.DataType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TickerInfoActivity extends AppCompatActivity {
    private final DataTicketInfo controller = new DataTicketInfo(this);
    private LoadingDialog loadingDialog;
    private HandlerThread createThread;
    private Handler createHandler;
    private Timer background;

    private final Runnable backgroundRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("TickerInfoActivity", "background : 백그라운드 실행 중");
            String ticker_id = (String)((TextView)findViewById(R.id.ticker_id)).getText();

            List<String> date = controller.getPreviousOpen(3);
            //UI를 변경하기위해서 사용하는 Thread
            runOnUiThread(() -> {
                String month = date.get(0).substring(4, 6);
                String day = date.get(0).substring(6, 8);
                String date_str = month+"월"+day+"일";
                ((TextView)findViewById(R.id.day)).setText(date_str);
            });

            final ArrayList<String[]> tickerInfo = controller.getTickerInfo(date.get(1), date.get(0), ticker_id);

            runOnUiThread(() -> {
                TextView ticker_change_price = findViewById(R.id.ticker_change_price);
                TextView ticker_now_price = findViewById(R.id.ticker_now_price);
                TextView ticker_rate = findViewById(R.id.ticker_rate);

                Integer afterPrice = Integer.valueOf(tickerInfo.get(1)[4]);
                Integer beforePrice = Integer.valueOf(tickerInfo.get(0)[4]);
                double tickerRate = Double.parseDouble(tickerInfo.get(1)[7]);
                String tickerNowPrice_str = "";
                if (tickerRate >= 0) {
                    ticker_change_price.setText("+");
                    ticker_change_price.setTextColor(getColor(R.color.red));
                    ticker_rate.setTextColor(getColor(R.color.red));

                } else {
                    ticker_change_price.setText("");
                    ticker_change_price.setTextColor(getColor(R.color.blue));
                    ticker_rate.setTextColor(getColor(R.color.blue));
                }

                String changePrice = ticker_change_price.getText() //부호 포함
                        + String.format(Locale.KOREA, "%,d원", (afterPrice - beforePrice));
                ticker_change_price.setText(changePrice);

                String tickerRate_str = "("+tickerRate+"%)";
                tickerNowPrice_str += String.format(Locale.KOREA, "%,d원", afterPrice);
                ticker_rate.setText(tickerRate_str);
                ticker_now_price.setText(tickerNowPrice_str);
            });

            runOnUiThread(() -> {
                String lowest = String.format(Locale.KOREA, "%,d원",
                        Integer.valueOf(tickerInfo.get(1)[3]));
                ((TextView) findViewById(R.id.day_range_lowest)).setText(lowest);

                String highest = String.format(Locale.KOREA, "%,d원",
                        Integer.valueOf(tickerInfo.get(1)[2]));
                ((TextView) findViewById(R.id.day_range_highest)).setText(highest);

                SeekBar day_range = findViewById(R.id.day_range);
                int range_max = Integer.parseInt(tickerInfo.get(1)[2]) - Integer.parseInt(tickerInfo.get(1)[3]);
                int range_progress = Integer.parseInt(tickerInfo.get(1)[4]) - Integer.parseInt(tickerInfo.get(1)[3]);
                day_range.setMax(range_max);
                day_range.setProgress(range_progress);  // 현재 값
                day_range.setEnabled(false);
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
                //Log.d("TickerInfoActivity", String.format(Locale.KOREA, "대금 %,d원", Long.valueOf(tradingValue)));
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


            runOnUiThread(() -> {
                DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate before = LocalDate.now().minus(12, ChronoUnit.MONTHS);
                ArrayList<String[]> tickerYearInfo = controller.getTickerInfo(before.format(dateFormat), date.get(0), "m", ticker_id);

                int year_max = Integer.MIN_VALUE;
                int year_min = Integer.MAX_VALUE;
                for (String[] item : tickerYearInfo) {
                    int min = Integer.parseInt(item[3]);
                    int max = Integer.parseInt(item[2]);

                    if (year_min > min) year_min = min;
                    if (year_max < max) year_max = max;
                }

                String lowest = String.format(Locale.KOREA, "%,d원", year_min);
                ((TextView) findViewById(R.id.year_range_lowest)).setText(lowest);

                String highest = String.format(Locale.KOREA, "%,d원", year_max);
                ((TextView) findViewById(R.id.year_range_highest)).setText(highest);

                SeekBar year_range = findViewById(R.id.year_range);
                int range_max = year_max - year_min;
                int range_progress = Integer.parseInt(tickerInfo.get(1)[4]) - year_min;
                year_range.setMax(range_max);
                year_range.setProgress(range_progress);  // 현재 값
                year_range.setEnabled(false);
            });

            //loadingDialog.dismiss();
        }
    };

    private final Runnable lineChartRunnable = new Runnable() {
        @Override
        public void run() {
            DataTicketInfo ticketInfo = new DataTicketInfo(getBaseContext());
            String ticker_id = (String)((TextView)findViewById(R.id.ticker_id)).getText();

            List<String[]> stockData = ticketInfo.getTickerChartInfo(ticker_id);
            List<CandleEntry> entries = new ArrayList<>();
            for (int i = 0; i < stockData.size(); i++) {
                String[] temp = stockData.get(i);
                float open = Float.parseFloat(temp[1]);
                float shadowH = Float.parseFloat(temp[2]);
                float shadowL = Float.parseFloat(temp[3]);
                float close = Float.parseFloat(temp[4]);
                entries.add(new CandleEntry(i, shadowH, shadowL, open, close, temp[0]));
            }

            //샘플 데이터
            //entries.add(new CandleEntry(0, 225.0f, 219.84f, 224.94f, 221.07f));
            //entries.add(new CandleEntry(1, 228.35f, 222.57f, 223.52f, 226.41f));
            //entries.add(new CandleEntry(2, 226.84f,  222.52f, 225.75f, 223.84f));
            //entries.add(new CandleEntry(3, 222.95f, 217.27f, 222.15f, 217.88f));

            CandleStickChart chart = findViewById(R.id.ticker_chart);
            chart.setScaleEnabled(false);  //터치를 통한 확대/축소 비활성화
            chart.setPinchZoom(false);     //핀치 줌 비활성화
            CandleDataSet dataSet = new CandleDataSet(entries, "Stock Data"); //데이터 삽입

            //X축 날짜 포맷터 설정
            XAxis xAxis = chart.getXAxis();
            xAxis.setEnabled(false);

            YAxis rightAxis = chart.getAxisRight();
            rightAxis.setEnabled(false); //오른쪽 Y축 레이블 숨기기

            Legend legend = chart.getLegend();
            legend.setEnabled(false); //범례 숨기기

            Description description = new Description();
            description.setText("Select a data point");
            chart.setDescription(description); //라벨 설정

            //다크 모드 감지
            boolean isDarkMode = (chart.getContext().getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;

            if (isDarkMode) {
                //다크 모드 색상 설정
                dataSet.setShadowColor(Color.WHITE);                 //차트 봉 색상
                dataSet.setValueTextColor(Color.WHITE);              //데이터 값 텍스트 색상
                chart.getXAxis().setTextColor(Color.LTGRAY);         //X축 레이블 색상
                chart.getAxisLeft().setTextColor(Color.LTGRAY);      //Y축 레이블 색상
                chart.getAxisRight().setTextColor(Color.LTGRAY);     //Y축 레이블 색상
                chart.getLegend().setTextColor(Color.LTGRAY);        //범례 텍스트 색상
                chart.getDescription().setTextColor(Color.WHITE);    //설명 텍스트 색상
                dataSet.setIncreasingColor(Color.GREEN);             //양봉 색상
                dataSet.setDecreasingColor(Color.RED);               //음봉 색상
                dataSet.setDecreasingPaintStyle(Paint.Style.STROKE); //음봉 스타일
            } else {
                //라이트 모드 색상 설정
                dataSet.setShadowColor(Color.BLACK);
                dataSet.setValueTextColor(Color.BLACK);
                chart.getXAxis().setTextColor(Color.DKGRAY);
                chart.getAxisLeft().setTextColor(Color.DKGRAY);
                chart.getAxisRight().setTextColor(Color.DKGRAY);
                chart.getLegend().setTextColor(Color.DKGRAY);
                chart.getDescription().setTextColor(Color.BLACK);
                dataSet.setIncreasingColor(Color.rgb(224, 45, 35)); //red
                dataSet.setDecreasingColor(Color.rgb(55, 124, 229)); //blue
                dataSet.setDecreasingPaintStyle(Paint.Style.FILL);
            }
            dataSet.setShadowWidth(1.3f); //차트 봉 두께
            dataSet.setIncreasingPaintStyle(Paint.Style.FILL); //양봉_스타일
            dataSet.setNeutralColor(dataSet.getIncreasingColor()); //기본봉 색상

            dataSet.setValueFormatter(new ValueFormatter() { //최고점만 표기
                @Override
                public String getFormattedValue(float value) {
                    float highest = Float.MIN_VALUE;

                    for (CandleEntry entry : entries)
                        if (entry.getHigh() > highest) highest = entry.getHigh();

                    if (value == highest) {
                        return Utils.formatNumber(value, 0, true);
                    } else return ""; //미표기
                }
            });

            chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    //선택된 CandleEntry로 Description 업데이트
                    if (e instanceof CandleEntry) {
                        CandleEntry candleEntry = (CandleEntry) e;
                        String open = Utils.formatNumber((int)candleEntry.getOpen(), 0, true);
                        String close = Utils.formatNumber((int)candleEntry.getClose(), 0, true);
                        String date = String.valueOf(candleEntry.getData());
                        description.setText(date+" : "+open+" -> "+close);
                        chart.setDescription(description);
                        chart.invalidate();  //차트 갱신
                    }
                }

                @Override
                public void onNothingSelected() {
                    //아무 것도 선택되지 않았을 때
                    description.setText("Select a data point");
                    chart.setDescription(description);
                    chart.invalidate();  // 차트 갱신
                }
            });

            CandleData candleData = new CandleData(dataSet);
            chart.setData(candleData);

            chart.invalidate(); //차트를 갱신

            loadingDialog.dismiss();
        }
    };

    //날짜 문자열을 float 값으로 변환
    private float dateToFloat(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        try {
            Date date = sdf.parse(dateString);
            if (date != null) {
                return (float) (date.getTime() / 1000L); //초 단위로 변환
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticker_info);

        String tickerId = null;
        try {
            JSONArray appData = AppData.getInstance().stockData[DataType.stock_data.getIndex()].getJSONArray("data");
            Intent intent = getIntent();
            //Activity가 이동하기전에 전달받은 변수.
            String tickerName = intent.getStringExtra("ticker_name");
            //Log.d("TickerInfoActivity", "onCreate: "+tickerName);
            // MainActivity에서 검색창으로 올 수 있으니깐 천체 검색한다.
            for (int i = 0; i < appData.length(); i++) {
                JSONArray market = appData.getJSONObject(i).getJSONArray("stock_data");
                for (int j = 0; j < market.length(); j++) {
                    JSONObject ticker = market.getJSONObject(j);
                    if (tickerName.equals(ticker.getString("name"))) {
                        tickerId = ticker.getString("id"); //id를 검색
                        break;
                    }
                }
            }

            if (!(tickerId == null)) {
                TextView ticker_name = findViewById(R.id.ticker_name);
                TextView ticker_id = findViewById(R.id.ticker_id);

                ticker_id.setText(tickerId);
                ticker_name.setText(tickerName);
            }
//            else {
//                /* [에러 출력 요함] */
//            }
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
        createThread = new HandlerThread("CreateThread");
        createThread.start();
        createHandler = new Handler(createThread.getLooper());

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                createHandler.post(backgroundRunnable);
                createHandler.post(lineChartRunnable);
            }
        };
        background.scheduleAtFixedRate(timerTask, 0, 180000); //3분마다 실행.
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //뒤로 가기를 눌렀을 때 발동하는 이벤트
        if (item.getItemId() == android.R.id.home) {
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
        if (createHandler!= null) {
            createHandler.removeCallbacksAndMessages(null);
            if (createThread!= null) {
                createThread.quitSafely();
            }
        }

        if (background!= null) {
            background.cancel();
        }
    }
}