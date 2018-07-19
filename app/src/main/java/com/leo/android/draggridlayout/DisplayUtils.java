package com.leo.android.draggridlayout;

import android.content.Context;

/**
 * Created on 2018/7/16 下午1:42
 * weimian weimian@anve.com
 */
final class DisplayUtils {

    private DisplayUtils() {
    }

    public static int getScreenHeight(Context context){
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getScreenWdith(Context context){
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
