package com.momi3355.stockworth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.momi3355.stockworth.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private DataService dataService;
    private HandlerThread handlerThread;
    private Handler mainHandler;
    private Handler backgroundHandler;

    private boolean isService;

    ServiceConnection conn = new ServiceConnection() {
        //서비스와 연결되었을때 호출되는 메서드
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DataService.LocalBinder dataBinder = (DataService.LocalBinder)service;
            dataService = dataBinder.getService();
            isService = true;
            // 백그라운드 스레드에서 backgroundRunnable 실행
            backgroundHandler.post(backgroundRunnable);
        }
        // 서비스와 연결이 끊겼을 때 호출되는 메서드
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isService = false;
        }
    };

    Runnable backgroundRunnable = () -> {
        // 백그라운드 할거 없으면 삭제 요함.
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 백그라운드 스레드 핸들러 초기화
        handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        // navigation_name = ['home', 'market_info', 'ticker_info', 'notifications']
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_market_info,
                R.id.navigation_ticker_info, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(getApplicationContext(), DataService.class);
        try {
            startService(intent);
            bindService(intent, conn, Context.BIND_AUTO_CREATE);
            Toast.makeText(MainActivity.this, "Service start", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("MainActivity", "onCreate: "+e.getMessage());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isService) {
            unbindService(conn);
            isService = false;
        }

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