package com.momi3355.stockworth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;

public class LoadingActivity extends AppCompatActivity {
    private final DataController controller = new DataController(this);

    private final Runnable loadingProcess = () -> {
        try {
            controller.load();

            Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); //액티비티 종료
        } catch (IOException | JSONException e) {
            Toast.makeText(LoadingActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        Thread loading = new Thread(loadingProcess);
        loading.start();
    }
}