package com.momi3355.stockworth.data;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.momi3355.stockworth.LoadingActivity;
import com.momi3355.stockworth.MainActivity;
import com.momi3355.stockworth.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataService extends Service {
    private static final int NOTIFICATION_ID = 1; //포그라운드 알람_ID
    private static final int NOTIFICATION_MARKET_ID = 2;
    private static final int NOTIFICATION_FAVORITES_ID = 3;

    private static final String CHANNEL_FOREGROUND_ID = "실시간 주식 정보";
    private static final String CHANNEL_FAVORITES_ID = "즐겨찾기에 관한 정보";
    private static final String CHANNEL_MARKET_ID = "주식시장의 정보";
    /** 데이터를 불러오고 저장하는 멤버변수
     * <p>데이터를 불러오는거는 'LoadingActivity'에서 진행된다.</p>
     * <p>데이터가 업데이트는 이 곳 'onStartCommand'에서 진행된다.</p>
     * @see LoadingActivity
     * @see AppData */
    public final DataController controller = new DataController(this);
    /** 주식의 데이터를 관리하는 멤버변수
     * @see DataController */
    private final LocalBinder binder = new LocalBinder();

    private NotificationManager notificationManager; //알람 메니져
    private ScheduledExecutorService scheduler; //데이터가 업데이트가 진행되는 스케줄러
    private SharedPreferences prefs;



    // 바인터 필요없는거 같기도 하고(AppData로 불러오면 되기 때문에/)
    public class LocalBinder extends Binder {
        public DataService getService() {
            return DataService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //채널 생성(오래오버전 부터는 필수)
        ArrayList<String> chenal_id = new ArrayList<>();
        chenal_id.add("실시간 주식 정보"); chenal_id.add("즐겨찾기에 관한 정보"); chenal_id.add("주식시장의 정보");
        for (String chenel : chenal_id) { // TODO : 나중에 각각 설명을 따로 부여할 필요가 있다.
            createNotificationChannel(chenel, getString(R.string.channel_description)); //이름, 설명부여
        }
        NotificationCompat.Builder stockBuilder =
                getStockNotification(CHANNEL_FOREGROUND_ID, "Worth", "주식정보 로딩 중....", true);
        // TODO : 팝업창 띄어서 허용을 해야한다.
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Toast.makeText(getApplicationContext(), "Service start", Toast.LENGTH_SHORT).show();
        // Foreground Service로 실행

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("update", true)) {
            startForeground(NOTIFICATION_ID, stockBuilder.build());
            // Background 서비스도 실행 요함. (설정으로 변경 가능)

        }
        // controller.load(); //로드는 'LoadingActivity' 에서 진행된다.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // TODO : 업데이트 타입이 market_time이면 9시까지 대기.

        //LocalTime now = LocalTime.now(); //현재 시간
        //시계의 0분, 20분, 40분 마다 업데이트
        //LocalTime nextTwentyMinutes = now.plusMinutes(now.getMinute() >= 40 ? 60 - now.getMinute() : 20 - now.getMinute());
        //Duration initialDelay = Duration.between(now, nextTwentyMinutes); //20분 될때까지 대기

        // 1. 포그라운드 : 실시간으로 주식정보를 manager.notify()이용해서 text를 수정.
        // 20분 마다 업데이트
        // [보류]
        // 2. 백그라운드 : 메시지가 변경이 아니라 겹처서 알림발송
        //     . 장시작, 장종료시간 알림.
        //     . 즐겨찾는 종목 변동%가 설정만큼 올라가거나, 내려갔을 경우 알림.
        scheduler.scheduleAtFixedRate(() -> {
            controller.update();

            AppData data = AppData.getInstance();
            if (prefs.getBoolean("notifications", true)) {
                boolean enableNotifi = false;
                Set<String> notifi_type = prefs.getStringSet("notification_detailed", null);
                if (notifi_type.contains("market_notification")) {
                    final String title = "시장알림";
                    LocalTime now = LocalTime.now(); //현재 시간
                    if (controller.isPreviousOpen()) {
                        if (now.getHour() == 9) {
                            notificationManager.notify(
                                    now.getNano(),
                                    getStockNotification(CHANNEL_MARKET_ID, title,
                                            "주식시장이 시작되었습니다.", false).build());
                            enableNotifi = true;
                        } else if (now.getHour() == 18) {
                            notificationManager.notify(
                                    now.getNano(),
                                    getStockNotification(CHANNEL_MARKET_ID, title,
                                            "주식시장이 종료되었습니다.", false).build());
                            enableNotifi = true;
                        }
                    } else {
                        //오전 12시, 오전 9시, 오후 12시
                        if (now.getHour() == 0 || now.getHour() == 9 || now.getHour() == 12) {
                            //주말이 아니면
                            if (!(LocalDate.now().getDayOfWeek().equals(DayOfWeek.SUNDAY) ||
                                    LocalDate.now().getDayOfWeek().equals(DayOfWeek.SATURDAY))) {
                                notificationManager.notify(
                                        now.getNano(),
                                        getStockNotification(CHANNEL_MARKET_ID, title,
                                                "오늘은 휴장 시간입니다.", false).build());
                                enableNotifi = true;
                            }
                        }
                    }
                    //그룹 본체
                    if (enableNotifi) {
                        notificationManager.notify(
                                NOTIFICATION_MARKET_ID,
                                getSummaryNotification(CHANNEL_MARKET_ID, title).build());
                    }
                }
                if (notifi_type.contains("foreground_notification")) {
                    final String title = "주식정보";
                    try {
                        JSONArray stockData = data.stockData[DataType.market_data.getIndex()]
                                .getJSONArray("data");
                        JSONObject item = stockData.getJSONObject(0);
                        double rate = item.getDouble("rate");
                        int price = (int)item.getDouble("price");
                        String rate_arrow = (rate >= 0.00) ? "↑" : "↓";
                        String main_str = "코스피 "+price+"원 "+rate_arrow+rate+"%";
                        notificationManager.notify(NOTIFICATION_ID,
                                getStockNotification(CHANNEL_FOREGROUND_ID, title,
                                        main_str, true).build());
                        // TODO : 즐겨찾는 주가 표기도 요함.
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
//                if (notifi_type.contains("favorites_notification")) {
//                    notificationManager.notify(
//                            NOTIFICATION_MARKET_ID,
//                            getSummaryNotification(CHANNEL_MARKET_ID, title).build());
//                }
            }
        //}, initialDelay.toMillis(), 20, TimeUnit.MINUTES); //이건 시간의 20분.
        }, 0, 1, TimeUnit.MINUTES); //이게 10분 마다 실행
        return START_NOT_STICKY;
    }

    private void createNotificationChannel(final String id, String description) {
        int importance = NotificationManager.IMPORTANCE_HIGH;
        notificationManager = getSystemService(NotificationManager.class); //알림 메니져 생성
        if (notificationManager.getNotificationChannel(id) == null) { //생성한 적이 없으면
            if (id.equals(CHANNEL_FOREGROUND_ID)) {
                importance = NotificationManager.IMPORTANCE_LOW;
            }
            NotificationChannel channel = new NotificationChannel(id, id, importance); //채널 생성
            if (importance < NotificationManager.IMPORTANCE_DEFAULT) {
                channel.setSound(null, null); //알람음 없음.
                channel.enableVibration(false); //진동 없음.
            }
            channel.setDescription(description); //설명부여
            notificationManager.createNotificationChannel(channel);
        }
    }

    @NonNull
    private NotificationCompat.Builder getStockNotification(String CHANNEL_ID, String title, String text, boolean ongoing) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_white) //아이콘
            .setContentTitle(title)  // 제목
            .setContentText(text)   // 내용
            .setOngoing(ongoing)  // 사용자가 끄지못하도록하는것
            .setGroup(CHANNEL_ID);
    }

    @NonNull
    private NotificationCompat.Builder getSummaryNotification(String CHANNEL_ID, String title) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_white)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setGroup(CHANNEL_ID)
            .setGroupSummary(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        String update_type = prefs.getString("update_type", "app_running");
        if (update_type.equals("app_running")) {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        }
    }
}