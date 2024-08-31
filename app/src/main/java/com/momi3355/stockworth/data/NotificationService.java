package com.momi3355.stockworth.data;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.github.mikephil.charting.utils.Utils;
import com.momi3355.stockworth.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
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
    public final DataTickerInfo tickerInfo = new DataTickerInfo(this);
    private final HashMap<String, Pair<Boolean, Boolean>> favoriteMap = new HashMap<>();

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
        if (controller.isPreviousOpen()) { //장 시간 중 이면
            Set<String> notifi_type = prefs.getStringSet("notification_detailed", null);
            Map<String, String> tickerMap = controller.getTickerMap(); //name to id
            if (notifi_type.contains("foreground_notification")) {
                final String title = "주식정보";
                String date = tickerInfo.getPreviousOpen(1).get(0);
                String[] marketInfo = tickerInfo.getMarketInfo(date); //직접 가지고 온다.
                double rate = Double.parseDouble(marketInfo[2]);
                String price = Utils.formatNumber(Integer.parseInt(marketInfo[1]), 0, true);

                String rate_arrow = (rate >= 0.00) ? "↑" : "↓";
                String market = "코스피 " + price + "원 " + rate_arrow + rate + "%";
                String favorite = "";
                if (!data.favoriteStock.equals("없음")) {
                    String name = data.favoriteStock;
                    String[] favoriteInfo = tickerInfo.getTickerInfo(date, date, tickerMap.get(name)).get(0);
                    rate = Double.parseDouble(favoriteInfo[7]);
                    price = Utils.formatNumber(Integer.parseInt(favoriteInfo[4]), 0, true);

                    rate_arrow = (rate >= 0.00) ? "↑" : "↓";
                    favorite = "\n" + name + " " + price + "원 " + rate_arrow + rate + "%";
                }
                String main_str = market + favorite;

                NotificationType type = NotificationType.foreground;
                NotificationCompat.Builder notification = getStockNotification(type.getFileName(), title,
                        main_str, true);
                notificationManager.notify(type.getIndex(), notification.build());
            }
            if (notifi_type.contains("favorites_notification")) {
                boolean enableNotifi = false;
                final String title = "즐겨찾기 정보";

                int index = 1;
                NotificationType type = NotificationType.favorites;
                for (String favorite : data.favoriteData) { //즐겨찾기 한 목록
                    if (index % 10 == 0)
                        scheduler.schedule((Runnable)this, 10, TimeUnit.MILLISECONDS); //10ms 대기
                    String date = tickerInfo.getPreviousOpen(1).get(0);
                    String id = tickerMap.get(favorite);
                    String[] stockInfo = tickerInfo.getTickerInfo(date, date, id).get(0);

                    double rate = Double.parseDouble(stockInfo[7]);
                    String rate_arrow = (rate >= 0.00) ? "↑" : "↓";
                    String price = Utils.formatNumber(Integer.parseInt(stockInfo[4]), 0, true);
                    if (rate < -5.00) {
                        String text = favorite+" "+price+"("+rate_arrow+rate+"%) 5% 이하에 도달했습니다.";
                        if (favoriteMap.containsKey(favorite)) {
                            Pair<Boolean, Boolean> value = favoriteMap.get(favorite);
                            if (value != null)
                                if (value.first) continue; //값이 이미 있음.
                                else favoriteMap.put(id, new Pair<>(true, value.second));
                        } else favoriteMap.put(id, new Pair<>(true, false));

                        NotificationCompat.Builder notification = getStockNotification(type.getFileName(), title,
                                text, false);
                        notificationManager.notify(LocalTime.now().getNano(), notification.build());
                        enableNotifi = true;
                    } else if (rate > 5.00) {
                        String text = favorite+" "+price+"("+rate_arrow+rate+"%) 5% 이상에 도달했습니다.";
                        if (favoriteMap.containsKey(favorite)) {
                            Pair<Boolean, Boolean> value = favoriteMap.get(favorite);
                            if (value != null)
                                if (value.second) continue; //값이 이미 있음.
                                else favoriteMap.put(id, new Pair<>(value.first, true));
                        } else favoriteMap.put(id, new Pair<>(false, true));

                        NotificationCompat.Builder notification = getStockNotification(type.getFileName(), title,
                                text, false);
                        notificationManager.notify(LocalTime.now().getNano(), notification.build());
                        enableNotifi = true;
                    }
                    index++;
                }

                //그룹 본체
                if (enableNotifi) {
                    notificationManager.notify(
                            type.getIndex(),
                            getSummaryNotification(type.getFileName(), title).build());
                }
            }
        } else {
            //알림은 날마다 초기화 된다.
            if (!favoriteMap.isEmpty()) {
                favoriteMap.clear();
            }
        }
    };

    Runnable market = () -> {
        Log.d("NotificationService", "market: 알림 실행");
        boolean enableNotifi = false;
        Set<String> notifi_type = prefs.getStringSet("notification_detailed", null);
        if (notifi_type.contains("market_notification")) {
            final String title = "시장알림";
            LocalTime now = LocalTime.now(); //현재 시간

            NotificationType type = NotificationType.market;
            if (controller.isPreviousOpen()) {
                if (now.getHour() == 9 && now.getMinute() == 0) {
                    notificationManager.notify(now.getNano(),
                            getStockNotification(type.getFileName(), title,
                                    "주식시장이 시작되었습니다.", false).build());
                    enableNotifi = true;
                } else if (now.getHour() == 18 && now.getMinute() == 0) {
                    notificationManager.notify(now.getNano(),
                            getStockNotification(type.getFileName(), title,
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
                        notificationManager.notify(now.getNano(),
                                getStockNotification(type.getFileName(), title,
                                        "오늘은 휴장 시간입니다.", false).build());
                        enableNotifi = true;
                    }
                }
            }
            //그룹 본체
            if (enableNotifi) {
                notificationManager.notify(type.getIndex(),
                        getSummaryNotification(type.getFileName(), title).build());
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 알람이 2개.
        //  1. 포그라운드 - 실시간 주식정보
        //  2. 백그라운드 - 마켓정보 및 즐겨찾는 주가 %알림.
        if (prefs.getBoolean("notifications", true)) {
            Set<String> notifi_type = prefs.getStringSet("notification_detailed", null);
            if (notifi_type.contains("foreground_notification")) {
                final String title = "주식정보";
                try {
                    JSONArray stockData = data.stockData[DataType.market_data.getIndex()]
                            .getJSONArray("data");
                    JSONObject item = stockData.getJSONObject(0);
                    double rate = item.getDouble("rate");
                    String price = Utils.formatNumber((int)item.getDouble("price"), 0, true);

                    String rate_arrow = (rate >= 0.00) ? "↑" : "↓";
                    String market = "코스피 " + price + "원 " + rate_arrow + rate + "%";
                    String favorite = "";
                    if (!data.favoriteStock.equals("없음")) {
                        JSONArray allData = data.stockData[DataType.stock_data.getIndex()]
                                .getJSONArray("data");
                        String name = data.favoriteStock;
                        for (int i = 0; i < allData.length(); i++) {
                            JSONArray stock_item = allData.getJSONObject(i).getJSONArray("stock_data");
                            for (int j = 0; j < stock_item.length(); j++) {
                                item = stock_item.getJSONObject(j);
                                Log.d("NotificationService", "onStartCommand: "+item.getString("name"));
                                if (item.getString("name").equals(name)) {
                                    rate = item.getDouble("rate");
                                    price = Utils.formatNumber((int)item.getDouble("price"), 0, true);
                                    rate_arrow = (rate >= 0.00) ? "↑" : "↓";
                                    favorite = "\n" + name + " " + price + "원 " + rate_arrow + rate + "%";
                                    break;
                                }
                            }
                        }
                    }
                    String main_str = market + favorite;

                    NotificationType type = NotificationType.foreground;
                    NotificationCompat.Builder notification = getStockNotification(type.getFileName(), title,
                            main_str, true);

                    notificationManager.notify(type.getIndex(), notification.build());
                    startForeground(type.getIndex(), notification.build());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(foreground, 1, 15, TimeUnit.MINUTES);
            scheduler.scheduleAtFixedRate(market, 0, 5, TimeUnit.MINUTES);
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
}
