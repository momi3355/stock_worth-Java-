package com.momi3355.stockworth.data;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.momi3355.stockworth.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService extends Service {
    private enum NotificationType {
        foreground("실시간 주식 정보", 1),
        market("주식시장의 정보", 2),
        favorites("즐겨찾기에 관한 정보", 3);

        private final String fileName;
        private final int index;

        NotificationType(String fileName, int index) {
            this.fileName = fileName;
            this.index = index;
        }

        public String getFileName() {
            return fileName;
        }

        public int getIndex() {
            return index;
        }

        public static String[] getAllNames() {
            return new String[] {
                    foreground.getFileName(),
                    market.getFileName(),
                    favorites.getFileName(),
            };
        }
    }

    public final DataController controller = new DataController(this);

    private NotificationManager notificationManager; //알람 메니져
    private ScheduledExecutorService scheduler; //데이터가 업데이트가 진행되는 스케줄러
    private SharedPreferences prefs;
    private AppData data;

    @Override
    public void onCreate() {
        super.onCreate();

        data = AppData.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        notificationManager = getSystemService(NotificationManager.class);

        //채널 생성(오래오버전 부터는 필수)
        for (String channel : NotificationType.getAllNames()) { // TODO : 나중에 각각 설명을 따로 부여할 필요가 있다.
            createNotificationChannel(channel, getString(R.string.channel_description)); //이름, 설명부여
        }
    }

    Runnable foreground = () -> {
        Set<String> notifi_type = prefs.getStringSet("notification_detailed", null);
        if (notifi_type.contains("foreground_notification")) {
            final String title = "주식정보";
            try {
                JSONArray stockData = data.stockData[DataType.market_data.getIndex()]
                        .getJSONArray("data");
                JSONObject item = stockData.getJSONObject(0);
                double rate = item.getDouble("rate");
                int price = (int) item.getDouble("price");
                String rate_arrow = (rate >= 0.00) ? "↑" : "↓";
                String main_str = "코스피 " + price + "원 " + rate_arrow + rate + "%";

                NotificationType type = NotificationType.foreground;
                NotificationCompat.Builder notification = getStockNotification(type.getFileName(), title,
                        main_str, true);

                notificationManager.notify(type.getIndex(), notification.build());
                startForeground(type.getIndex(), notification.build());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    Runnable market = () -> {
        boolean enableNotifi = false;
        Set<String> notifi_type = prefs.getStringSet("notification_detailed", null);
        if (notifi_type.contains("market_notification")) {
            final String title = "시장알림";
            LocalTime now = LocalTime.now(); //현재 시간

            NotificationType type = NotificationType.market;
            String channel_id = type.getFileName();

            if (controller.isPreviousOpen()) {
                if (now.getHour() == 9 && now.getMinute() == 0) {
                    notificationManager.notify(
                            now.getNano(),
                            getStockNotification(channel_id, title,
                                    "주식시장이 시작되었습니다.", false).build());
                    enableNotifi = true;
                } else if (now.getHour() == 18 && now.getMinute() == 0) {
                    notificationManager.notify(
                            now.getNano(),
                            getStockNotification(channel_id, title,
                                    "주식시장이 종료되었습니다.", false).build());
                    enableNotifi = true;
                }
            } else {
                //오전 12시, 오전 9시, 오후 12시
                if ((now.getHour() == 0  || now.getHour() == 9 || now.getHour() == 12)
                        && now.getMinute() == 0 /* [정각] */) {
                    //주말이 아니면
                    if (!(LocalDate.now().getDayOfWeek().equals(DayOfWeek.SUNDAY) ||
                            LocalDate.now().getDayOfWeek().equals(DayOfWeek.SATURDAY))) {
                        notificationManager.notify(
                                now.getNano(),
                                getStockNotification(channel_id, title,
                                        "오늘은 휴장 시간입니다.", false).build());
                        enableNotifi = true;
                    }
                }
            }
            //그룹 본체
            if (enableNotifi) {
                notificationManager.notify(
                        type.getIndex(),
                        getSummaryNotification(channel_id, title).build());
            }
        }
        // TODO : 즐겨찾는 주가 표기도 요함.
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 알람이 2개.
        //  1. 포그라운드 - 실시간 주식정보
        //  2. 백그라운드 - 마켓정보 및 즐겨찾는 주가 %알림.
        if (prefs.getBoolean("notifications", true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(foreground, 0, 15, TimeUnit.MINUTES);
            scheduler.scheduleAtFixedRate(market, 0, 1, TimeUnit.MINUTES);
        }
        return START_STICKY;
    }

    private void createNotificationChannel(final String id, String description) {
        int importance = NotificationManager.IMPORTANCE_HIGH;
        if (notificationManager.getNotificationChannel(id) == null) { //생성한 적이 없으면
            if (id.equals(NotificationType.foreground.getFileName())) {
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
        stopForeground(true);
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
