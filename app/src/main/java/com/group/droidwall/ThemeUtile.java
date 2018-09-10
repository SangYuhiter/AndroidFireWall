package com.group.droidwall;

import android.app.Activity;

public class ThemeUtile {
    public ThemeUtile()
    {
    }
    public static boolean night = false;
    public static void setTheme(Activity activity){
        boolean isLight=ThemeUtile.night;
        activity.setTheme(R.style.ThemeLight);
    }
    public static void changeTheme(Activity activity){
        if (ThemeUtile.night){
            activity.setTheme(R.style.ThemeDark);
        }else{
            activity.setTheme(R.style.AppTheme);
        }
    }
}
