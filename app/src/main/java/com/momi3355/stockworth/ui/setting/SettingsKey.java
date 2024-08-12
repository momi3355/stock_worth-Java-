package com.momi3355.stockworth.ui.setting;

public enum SettingsKey {
    //알림 영역
    notifications("notifications"),
    notifications_vibrate("notifications_vibrate"),
    notifications_detailed("notifications_detailed"),
    notifications_foreground_firstName("notifications_foreground_firstName"),
    notifications_foreground_secondName("notifications_foreground_secondName");

    private final String key;

    SettingsKey(String key) {
        this.key = key;
    }
}
