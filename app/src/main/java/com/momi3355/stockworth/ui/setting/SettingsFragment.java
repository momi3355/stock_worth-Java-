package com.momi3355.stockworth.ui.setting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.widget.Toast;

import com.momi3355.stockworth.R;
import com.momi3355.stockworth.data.AppData;
import com.momi3355.stockworth.data.NotificationService;

import java.util.HashSet;
import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat {
    SharedPreferences prefs;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
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

        //테마 변경 이벤트
        setPreference("theme", new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if ("light".equals(newValue)) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                } else if ("dark".equals(newValue)) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else if ("device".equals(newValue)) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                }
                return true;
            }
        });

        //알림 번경 이벤트
        setPreference("notifications", new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (newValue instanceof Boolean) {
                    Activity activity = requireActivity();
                    Intent intent = new Intent(activity, NotificationService.class);
                    if ((Boolean)newValue) {
                        activity.startService(intent);
                    } else {
                        activity.stopService(intent);
                    }
                } else {
                    Log.e("SettingsFragment", "알림설정이 'Boolean'이 아닙니다.");
                }
                return true;
            }
        });

        //알림_상세 번경 이벤트
        setPreference("notification_detailed", new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (newValue instanceof Set) {
                    Activity activity = requireActivity();
                    Intent intent = new Intent(activity, NotificationService.class);
                    if (((Set<?>)newValue).contains("foreground_notification")) {
                        activity.startService(intent);
                    } else {
                        //재시작
                        activity.stopService(intent);
                        activity.startService(intent);
                    }
                } else {
                    Log.e("SettingsFragment", "상세 알림 설정이 'set'이 아닙니다.");
                }
                return true;
            }
        });

        //초기화 버튼 이벤트
        setPreference("reset", new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireActivity());
                dialog.setTitle("초기화"); //제목
                dialog.setMessage("즐겨찾기를 제외한 환경설정을 초기화 하시겠습니까?"); // 메시지
                //버튼 클릭시 동작
                dialog.setNegativeButton("아니요", null); //취소는 아무것도 안함
                dialog.setPositiveButton("예", (dialog1, which) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear().apply();
                    PreferenceManager.setDefaultValues(requireContext(), R.xml.settings_preference, true);
                    requireActivity().recreate(); //다시 생성
                    Toast.makeText(requireContext(), "설정이 초기화 되었습니다.", Toast.LENGTH_SHORT).show();
                });
                dialog.show();
                return true;
            }
        });

        //즐겨찾기_초기화 버튼 이벤트
        setPreference("favorite_reset" , new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireActivity());
                dialog.setTitle("즐겨찾기 초기화"); //제목
                dialog.setMessage("지금까지 저장된 즐겨찾기를 초기화 하시겠습니까?"); // 메시지
                //버튼 클릭시 동작
                dialog.setNegativeButton("아니요", null); //취소는 아무것도 안함
                dialog.setPositiveButton("예", (dialog1, which) -> {
                    HashSet<String> favoriteData = AppData.getInstance().favoriteData;
                    favoriteData.clear();
                    Toast.makeText(requireContext(), "즐겨찾기가 초기화 되었습니다.", Toast.LENGTH_SHORT).show();
                });
                dialog.show();
                return true;
            }
        });
    }

    void setPreference(String key, Object listener) {
        Preference preference = findPreference(key);
        if (preference != null) {
            if (listener instanceof Preference.OnPreferenceClickListener) { //버튼 클릭
                preference.setOnPreferenceClickListener((Preference.OnPreferenceClickListener)listener);
            }
            if (listener instanceof Preference.OnPreferenceChangeListener) { //값 변경
                preference.setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener)listener);
            }
        } else {
            Log.e("SettingsFragment", key+": 이라는 설정이 없습니다.");
        }
    }
}