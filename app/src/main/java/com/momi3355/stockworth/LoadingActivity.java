package com.momi3355.stockworth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ProgressBar;

import com.momi3355.stockworth.data.DataController;

public class LoadingActivity extends AppCompatActivity {
    private final DataController controller = new DataController(this);

    /**
     * 쓰레드로하는 이유는 <b>onCreate()</b>에서 하면<br>
     * <b>Activity</b>를 생성하지도 않고 오래걸려서,<br>
     * 프로그램의 <b>'응답없음'</b>이 되기 때문에<br>
     * 일을 <b>Thread</b>에 넣어서 따로 처리해준다.<br>
     *
     * <p>※ 작업시간이 긴 작업은 <b>Activity</b>에서 하는 것은 권장되지 않는다.</p>
     */
    private final Runnable loadingProcess = () -> {
        controller.load();

        Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
        //loading -> main으로 이동하는 intent
        startActivity(intent);
        finish(); //액티비티 종료
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        //설정파일 초기화 (파일 없을 때만)
        PreferenceManager.setDefaultValues(this, R.xml.settings_preference, false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme", "device");
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "device":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (isDarkMode) {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            //로고 변경
            progressBar.setIndeterminateDrawable(AppCompatResources.getDrawable(this, R.drawable.logo_white));
        }

        Thread loading = new Thread(loadingProcess);
        loading.start();
    }
}