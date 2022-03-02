package kr.co.yesco.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtil {

    private Context mContext;

    public PreferenceUtil(Context mContext) {
        this.mContext = mContext;
    }

    public void setStringPreferences(String key, String value) {
        SharedPreferences pref = mContext.getSharedPreferences(Constant.PRE_KEY, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public void setIntPreferences(String key, Integer value) {
        SharedPreferences pref = mContext.getSharedPreferences(Constant.PRE_KEY, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, value);
        editor.commit();
    }
    public String getStringPreferences(String key) {

        SharedPreferences pref = mContext.getSharedPreferences(Constant.PRE_KEY, mContext.MODE_PRIVATE);
        return pref.getString(key, "");
    }
    public int getIntPreferences(String key) {

        SharedPreferences pref = mContext.getSharedPreferences(Constant.PRE_KEY, mContext.MODE_PRIVATE);
        return pref.getInt(key,0);
    }
}

