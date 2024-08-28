package com.momi3355.stockworth.data;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.momi3355.stockworth.LoadingActivity;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataService extends Service {
    /** 데이터를 불러오고 저장하는 멤버변수
     * <p>데이터를 불러오는거는 'LoadingActivity'에서 진행된다.</p>
     * <p>데이터가 업데이트는 이 곳 'onStartCommand'에서 진행된다.</p>
     * @see LoadingActivity
     * @see AppData */
    public final DataController controller = new DataController(this);
    /** 주식의 데이터를 관리하는 멤버변수
     * @see DataController */
    private final LocalBinder binder = new LocalBinder();

    private ScheduledExecutorService scheduler; //데이터가 업데이트가 진행되는 스케줄러

    // 바인터 필요없는거 같기도 하고(AppData로 불러오면 되기 때문에/)
    public class LocalBinder extends Binder {
        public DataService getService() {
            return DataService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // controller.load(); //로드는 'LoadingActivity' 에서 진행된다.
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(controller::update, 10, 10, TimeUnit.MINUTES);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}