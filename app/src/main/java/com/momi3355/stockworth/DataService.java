package com.momi3355.stockworth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.chaquo.python.Python;

public class DataService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "data_service_channel";
    /** 주식의 데이터를 관리하는 멤버변수
     * @see DataController */
    private final DataController controller = new DataController(this);

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(); //채널 생성(오래오버전 부터는 필수)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Worth") //제목
            .setContentText("Service is running...") //내용
            .setSmallIcon(R.drawable.ic_notifications_black_24dp) //아이콘
            .setOngoing(true); //사용자가 끄지못하도록하는것

        // Foreground Service로 실행
        startForeground(NOTIFICATION_ID, builder.build());
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
        return START_NOT_STICKY; //서비스가 강제 종료되어도 재시작하지 않음.
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //버전확인
            CharSequence name = getString(R.string.channel_name); //제목
            String description = getString(R.string.channel_description); //내용
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance); //채널 생성
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class); //알림 메니져 생성
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        Log.d("DataService","onDestroy()");
        super.onDestroy();
    }
}