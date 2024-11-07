package com.capstone.tansiri.map;

import android.content.Context;
import android.provider.Settings;

public class Util {
    public static String getDeviceID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}