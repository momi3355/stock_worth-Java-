package com.momi3355.stockworth.ui.setting;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import android.util.Log;

import com.momi3355.stockworth.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    SharedPreferences prefs;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, null);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        initSummary(getPreferenceScreen());
    }

    SharedPreferences.OnSharedPreferenceChangeListener prefListener =
        //onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        (sharedPreferences, key) -> {
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