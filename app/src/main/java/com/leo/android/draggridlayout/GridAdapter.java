package com.leo.android.draggridlayout;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created on 2018/5/30 上午9:44
 * weimain weimain@anve.com
 */
public abstract class GridAdapter {

    public abstract View getView(final int position, final ViewGroup parentView);

    public abstract int getCount();

    /**
     * itemView 尺寸，位置已经确定，
     *
     * @param itemView itemview
     * @param position 下标
     */
    public void handleItemView(View itemView, int position) {

    }

}
