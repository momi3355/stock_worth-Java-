package com.momi3355.stockworth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class DataService extends Service {
    private DataController controller;

    public DataService(DataController controller) {
        this.controller = controller;
    }

    @Override
    public void onCreate() {
        Log.d("DataService", "onCreate: ?");
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("DataService","onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("DataService","onDestroy()");
        super.onDestroy();
    }
}