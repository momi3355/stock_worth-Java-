package com.momi3355.stockworth;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "실시간 주식 정보";
    /** 주식의 데이터를 관리하는 멤버변수
     * @see DataController */
    private final DataController controller = new DataController(this);

    private NotificationManager notificationManager; //알람 메니져

    @Override
    public void onCreate() {
        super.onCreate();
        //채널 생성(오래오버전 부터는 필수)
        createNotificationChannel(CHANNEL_ID, NotificationManager.IMPORTANCE_LOW, //체널, 우선순위
                getString(R.string.channel_name), getString(R.string.channel_description)); //이름, 설명부여
        NotificationCompat.Builder stockBuilder = getStockNotification("주식정보 로딩 중....");
        /* TODO : 팝업창 띄어서 허용을 해야한다. */

        // Foreground Service로 실행
        startForeground(NOTIFICATION_ID, stockBuilder.build());

        try {
            controller.load();
        } catch (IOException e) {
            Log.e("DataService", "onCreate: IO에러("+e.getMessage()+")");
        } catch (JSONException e) {
            Log.e("DataService", "onCreate: JSON에러("+e.getMessage()+")");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: 이걸로 백그라운드 서비스(Activity에서 구현부도 필요하다.)
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DataService", "onStartCommand : ");
        // TODO : 여기에서 포그라운드 서비스.
        //manager.notify() 를 이용하면 알람 내용변경 가능하다.
        //notificationManager.notify(NOTIFICATION_ID, getStockNotification("변경").build());

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
            .setContentTitle("Worth") //제목
            .setContentText(text) //내용
            .setOngoing(true); //사용자가 끄지못하도록하는것
    }

    @Override
    public void onDestroy() {
        Log.d("DataService","onDestroy()");
        super.onDestroy();
    }
}