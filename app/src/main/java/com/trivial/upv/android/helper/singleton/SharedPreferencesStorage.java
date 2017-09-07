package com.trivial.upv.android.helper.singleton;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.trivial.upv.android.R;

/**
 * Created by jvg63 on 22/01/2017.
 */

public class SharedPreferencesStorage  {


    public static final String PREF_URL_CATEGORIES = "url_categories";

    public static final String PREF_URL_MODE_ONE_PLAYER_NUM_QUIZZES = "mode_one_player_quizzes";
    public static final String PREF_URL_MODE_ONE_PLAYER_TOTAL_TIME = "mode_one_player_total_time";

    public static final String PREF_URL_MODE_ONLINE_NUM_PLAYERS="mode_online_num_players";
    public static final String PREF_URL_MODE_ONLINE_NUM_QUIZZES ="mode_online_quizzes";
    public static final String PREF_URL_MODE_ONLINE_TOTAL_TIME= "mode_online_total_time";

    private Context context;

//
private SharedPreferencesStorage(Context context) {
    this.context = context;
}

    private static SharedPreferencesStorage instance;

    public static SharedPreferencesStorage getInstance(Context context) {
        if (instance == null) {
            synchronized (SharedPreferencesStorage.class) {
                if (instance == null)
                    instance = new SharedPreferencesStorage(context);
            }
        }
        return instance;
    }

    public boolean firstTime() {
        return (!getPreference().contains(PREF_URL_CATEGORIES));

    }

    private SharedPreferences getPreference() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }


    public  void writeStringPreference(String key, String value) {
        SharedPreferences.Editor editor = getPreference().edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void writeIntPreference(String key, int value ) {
        SharedPreferences.Editor editor = getPreference().edit();
        editor.putInt(key, value);
        editor.commit();
    }


    public int readIntPreference(String key, int def) {
        return getPreference().getInt(key, def);
    }
    public String readStringPreference(String key) {
        return getPreference().getString(key, "0");
    }

    public void createDefaultValues() {
//        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
//        firstTime
        if (firstTime()) {

            writeStringPreference(PREF_URL_CATEGORIES, context.getString(R.string.url_categories));

            writeIntPreference(PREF_URL_MODE_ONE_PLAYER_NUM_QUIZZES, 10);
            writeIntPreference(PREF_URL_MODE_ONE_PLAYER_TOTAL_TIME, 250);

            writeIntPreference(PREF_URL_MODE_ONLINE_NUM_PLAYERS, 2);
            writeIntPreference(PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10);
            writeIntPreference(PREF_URL_MODE_ONLINE_TOTAL_TIME, 250);
        }
    }
//
//    private void writeIntPreference(String key, int value) {
//        SharedPreferences.Editor editor = getPreference().edit();
//        editor.putInt(key, value);
//        editor.commit();
//    }
//
//    private void removeStringPreference(String key) {
//        SharedPreferences.Editor editor = getPreference().edit();
//        editor.remove(key);
//        editor.commit();
//    }
//

}