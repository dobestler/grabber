package com.home.grabber;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class StateStorage {

    public static final String FILE_NAME = "TaskStateStorage";
    private static final String TAG = StateStorage.class.getSimpleName();

    public int getRunningTasks(final Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        return sharedPref.getAll().size();
    }

    public void putRunningTask(final Context context, final int taskId) {
        String key = String.valueOf(taskId);
        SharedPreferences sharedPref = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        if (sharedPref.contains(key)) {
            Log.d(TAG, "putRunningTask | Already contains task " + key);
        } else {
            Log.d(TAG, "putRunningTask | Adding new task " + key);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(String.valueOf(taskId), 1);
            editor.commit();
        }
    }

    public void removeRunningTask(final Context context, final int taskId) {
        String key = String.valueOf(taskId);
        SharedPreferences sharedPref = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        if (sharedPref.contains(key)) {
            Log.d(TAG, "removeRunningTask | Removing task " + key);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(String.valueOf(taskId));
            editor.commit();
        } else {
            Log.d(TAG, "removeRunningTask | Did not contain task " + key);
        }
    }
}
