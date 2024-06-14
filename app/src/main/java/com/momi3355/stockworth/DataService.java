package com.momi3355.stockworth;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataService extends Service {
    private static final int NOTIFICATION_ID = 1; //포그라운드 알람_ID
    private static final String CHANNEL_ID = "실시간 주식 정보";
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

    // 바인터 필요없는거 같기도 하고(AppData로 불러오면 되기 때문에/)
    class LocalBinder extends Binder {
        DataService getService() {
            return DataService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //채널 생성(오래오버전 부터는 필수)
        createNotificationChannel(CHANNEL_ID, NotificationManager.IMPORTANCE_LOW, //체널, 우선순위
                getString(R.string.channel_name), getString(R.string.channel_description)); //이름, 설명부여
        NotificationCompat.Builder stockBuilder = getStockNotification("주식정보 로딩 중....");
        // TODO : 팝업창 띄어서 허용을 해야한다.

        // Foreground Service로 실행
        startForeground(NOTIFICATION_ID, stockBuilder.build());
        // Background 서비스도 실행 요함. (설정으로 변경 가능)

        // controller.load(); //로드는 'LoadingActivity' 에서 진행된다.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DataService", "onStartCommand : ");
        scheduler = Executors.newSingleThreadScheduledExecutor();

        LocalTime now = LocalTime.now(); //현재 시간
        //시계의 0분, 20분, 40분 마다 업데이트
        LocalTime nextTwentyMinutes = now.plusMinutes(now.getMinute() >= 40 ? 60 - now.getMinute() : 20 - now.getMinute());
        Duration initialDelay = Duration.between(now, nextTwentyMinutes); //20분 될때까지 대기
        // 여기에서 다른 알람도 호출 가능하다.
        //manager.notify() 를 이용하면 알람 내용변경 가능하다.
        //notificationManager.notify(NOTIFICATION_ID, getStockNotification("변경").build());

        // 1. 포그라운드 : 실시간으로 주식정보를 manager.notify()이용해서 text를 수정.
        // 20분 마다 업데이트
        // [보류]
        // 2. 백그라운드 : 메시지가 변경이 아니라 겹처서 알림발송
        //     . 장시작, 장종료시간 알림.
        //     . 즐겨찾는 종목 변동%가 설정만큼 올라가거나, 내려갔을 경우 알림.
        scheduler.scheduleAtFixedRate(() -> {
            //포그라운드.
            try {
                controller.update();
                // TODO : 성공하면 알람내용 - 코스피의 주가 수정, 날짜-시간 출력. (우선순위 낮음)
            } catch (IOException | JSONException e) {
                Log.e("DataService", "onStartCommand: "+e.getMessage());
            }
            //TODO : 메시지도 변경 요함
            //notificationManager.notify(NOTIFICATION_ID, getStockNotification("변경").build());
        }, initialDelay.toMillis(), 20, TimeUnit.MINUTES); //이건 시간의 20분.
        //}, 0, 20, TimeUnit.MINUTES); //이게 20분 마다 실행
        return START_NOT_STICKY; //서비스가 강제 종료되어도 재시작하지 않음.
    }

    private void createNotificationChannel(final String id, int importance, String name, String description) {
        notificationManager = getSystemService(NotificationManager.class); //알림 메니져 생성
        if (notificationManager.getNotificationChannel(id) == null) { //생성한 적이 없으면
            NotificationChannel channel = new NotificationChannel(id, name, importance); //채널 생성
            if (importance < NotificationManager.IMPORTANCE_DEFAULT) {
                channel.setSound(null, null); //알람음 없음.
                channel.enableVibration(false); //진동 없음.
            }
            channel.setDescription(description); //설명부여
            notificationManager.createNotificationChannel(channel);
        }
    }

    @NonNull
    private NotificationCompat.Builder getStockNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp) //아이콘
                .setContentTitle("Worth") // 제목
                .setContentText(text)    // 내용
                .setOngoing(true);      // 사용자가 끄지못하도록하는것
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler!= null) {
            scheduler.shutdown();
        }
    }
}