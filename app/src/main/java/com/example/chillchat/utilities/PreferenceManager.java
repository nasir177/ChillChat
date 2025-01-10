package com.example.chillchat.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences("chillchat_preferences", Context.MODE_PRIVATE);
    }

    // Store a boolean value in shared preferences
    public void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();  // Apply asynchronously for better performance
    }

    // Retrieve a boolean value from shared preferences, default to false if not found
    public boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);  // Return false by default
    }

    // Store a string value in shared preferences
    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    // Retrieve a string value from shared preferences, return null if not found
    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    // Store an integer value in shared preferences
    public void putInt(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    // Retrieve an integer value from shared preferences, default to 0 if not found
    public int getInt(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    // Store a long value in shared preferences
    public void putLong(String key, long value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    // Retrieve a long value from shared preferences, default to 0L if not found
    public long getLong(String key) {
        return sharedPreferences.getLong(key, 0L);
    }

    // Store a float value in shared preferences
    public void putFloat(String key, float value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    // Retrieve a float value from shared preferences, default to 0.0f if not found
    public float getFloat(String key) {
        return sharedPreferences.getFloat(key, 0.0f);
    }

    // Remove a specific value from shared preferences
    public void remove(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    // Clear all shared preferences
    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();  // Clear all preferences
    }
}
