package com.cgr.android.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Saurabh on 11/3/2016.
 */
public class AppUtils {

    public static void writeUserPrefs(Context context, String key, String value) {
        try {
            SharedPreferences settings;
            settings = context.getSharedPreferences("wepark.launcher.mobile.android.kioskmode", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(key, value);
            editor.commit();
        } catch (Exception e) {
        }
    }

    public static String readUserPrefs(Context context, String key) {
        String value = "";
        try {
            SharedPreferences settings;
            settings = context.getSharedPreferences("wepark.launcher.mobile.android.kioskmode", Context.MODE_PRIVATE);
            value = settings.getString(key, "");

        } catch (Exception e) {
        }
        return value;
    }


    public static void clearPreference(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().clear();
    }
}
