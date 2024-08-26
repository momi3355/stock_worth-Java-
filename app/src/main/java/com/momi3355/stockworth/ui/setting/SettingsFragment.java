package com.momi3355.stockworth.ui.setting;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.widget.Toast;

import com.momi3355.stockworth.R;
import com.momi3355.stockworth.data.AppData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {
    SharedPreferences prefs;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        initSummary(getPreferenceScreen());

        //초기화 버튼 이벤트
        Preference reset_btn = findPreference("reset");
        assert reset_btn != null;
        reset_btn.setOnPreferenceClickListener(preference -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear().apply();
            setPreferencesFromResource(R.xml.settings_preference, rootKey); //다시 표기
            initSummary(getPreferenceScreen());
            Toast.makeText(requireContext(), "설정이 초기화 되었습니다.", Toast.LENGTH_SHORT).show();
            return true;
        });

        //즐겨찾기_초기화 버튼 이벤트
        Preference fareset_btn = findPreference("favorite_reset");
        assert fareset_btn != null;
        fareset_btn.setOnPreferenceClickListener(preference -> {
            HashSet<String> favoriteData = AppData.getInstance().favoriteData;
            favoriteData.clear();
            Toast.makeText(requireContext(), "즐겨찾기가 초기화 되었습니다.", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    SharedPreferences.OnSharedPreferenceChangeListener prefListener =
        //onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        (sharedPreferences, key) -> {
            if (key != null)
                updatePrefSummary(findPreference(key));
        };

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (String.valueOf(p.getTitle()).toLowerCase().contains("password")) {
                p.setSummary("******");
            } else {
//                if (String.valueOf(p.getSummary()).equalsIgnoreCase("null")
//                        || String.valueOf(p.getSummary()).equals("")
//                        || String.valueOf(p.getSummary()).equals("無")) {
//                    p.setSummary("없음");
//                } else
                p.setSummary(editTextPref.getText());
            }
        }

    }
}