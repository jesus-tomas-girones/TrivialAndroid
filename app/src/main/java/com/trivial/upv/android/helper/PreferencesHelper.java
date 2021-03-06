/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trivial.upv.android.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.trivial.upv.android.model.Avatar;
import com.trivial.upv.android.model.Player;
import com.trivial.upv.android.model.Player;

import java.util.Random;

/**
 * Easy storage and retrieval of preferences.
 */
public class PreferencesHelper {

    private static final String PLAYER_PREFERENCES = "playerPreferences";
    private static final String PREFERENCE_FIRST_NAME = PLAYER_PREFERENCES + ".firstName";
    private static final String PREFERENCE_LAST_INITIAL = PLAYER_PREFERENCES + ".lastInitial";
    private static final String PREFERENCE_AVATAR = PLAYER_PREFERENCES + ".avatar";
    private static final String PREFERENCE_LAST_MODIFIED_CATEGORIES_JSON = "last_modified_categories_json";

    private PreferencesHelper() {
        //no instance
    }

    /**
     * Writes a {@link Player} to preferences.
     *
     * @param context The Context which to obtain the SharedPreferences from.
     * @param player  The {@link Player} to write.
     */
    public static void writeToPreferences(Context context, Player player) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(PREFERENCE_FIRST_NAME, player.getFirstName());
        editor.putString(PREFERENCE_LAST_INITIAL, player.getLastInitial());
        editor.putString(PREFERENCE_AVATAR, player.getAvatar().name());
        editor.apply();
    }

    //JVG.S
    public static void writeLastModifiedPreferences(Context context, long lastModified) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putLong(PREFERENCE_LAST_MODIFIED_CATEGORIES_JSON, lastModified);
        editor.apply();
    }
    //JVG.S
    public static long getLastModifiedPreferences(Context context) {
        SharedPreferences preferences = getSharedPreferences(context);
        long lastModified = preferences.getLong(PREFERENCE_LAST_MODIFIED_CATEGORIES_JSON, -1);
        return lastModified;
    }
    //JVG.E

    /**
     * Retrieves a {@link Player} from preferences.
     *
     * @param context The Context which to obtain the SharedPreferences from.
     * @return A previously saved player or <code>null</code> if none was saved previously.
     */
    public static Player getPlayer(Context context) {
        SharedPreferences preferences = getSharedPreferences(context);
        final String firstName = preferences.getString(PREFERENCE_FIRST_NAME, null);
        final String lastInitial = preferences.getString(PREFERENCE_LAST_INITIAL, null);
        final String avatarPreference = preferences.getString(PREFERENCE_AVATAR, null);
        final Avatar avatar;
        if (null != avatarPreference) {
            avatar = Avatar.valueOf(avatarPreference);
        } else {
            avatar = null;
        }

        if (null == firstName || null == lastInitial || null == avatar) {
            return null;
        }
        return new Player(firstName, lastInitial, avatar);
    }

    /**
     * Signs out a player by removing all it's data.
     *
     * @param context The context which to obtain the SharedPreferences from.
     */
    public static void signOut(Context context) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(PREFERENCE_FIRST_NAME);
        editor.remove(PREFERENCE_LAST_INITIAL);
        editor.remove(PREFERENCE_AVATAR);
        editor.apply();
    }

    /**
     * Checks whether a player is currently signed in.
     *
     * @param context The context to check this in.
     * @return <code>true</code> if login data exists, else <code>false</code>.
     */
    public static boolean isSignedIn(Context context) {
        final SharedPreferences preferences = getSharedPreferences(context);
        return preferences.contains(PREFERENCE_FIRST_NAME) &&
                preferences.contains(PREFERENCE_LAST_INITIAL) &&
                preferences.contains(PREFERENCE_AVATAR);
    }

    /**
     * Checks whether the player's input data is valid.
     *
     * @param firstName   The player's first name to be examined.
     * @param lastInitial The player's last initial to be examined.
     * @return <code>true</code> if both strings are not null nor 0-length, else <code>false</code>.
     */
    public static boolean isInputDataValid(CharSequence firstName, CharSequence lastInitial) {
        return !TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastInitial);
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences preferences = getSharedPreferences(context);
        return preferences.edit();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PLAYER_PREFERENCES, Context.MODE_PRIVATE);
    }

    // JVG.S
    public static Player getDummyPlayer(FragmentActivity activity) {
        final String firstName = "android";
        final String lastInitial = "curso.com";

        Random rand = new Random();
        int pos = rand.nextInt(16);
        final String avatarPreference = (Avatar.values())[pos].toString();

        final Avatar avatar;
        if (null != avatarPreference) {
            avatar = Avatar.valueOf(avatarPreference);
        } else {
            avatar = null;
        }

        if (null == firstName || null == lastInitial || null == avatar) {
            return null;
        }
        return new Player(firstName, lastInitial, avatar);
    }
    // JVG.E
}
