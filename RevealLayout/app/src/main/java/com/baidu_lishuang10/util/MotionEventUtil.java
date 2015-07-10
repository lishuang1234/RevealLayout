package com.baidu_lishuang10.util;

import android.view.MotionEvent;

/**
 * Created by baidu_lishuang10 on 15/7/10.
 */
public class MotionEventUtil {

    public static String getMotionState(MotionEvent event) {
        String action = "";
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                action = "ACTION_DOWN";
                break;
            case MotionEvent.ACTION_UP:
                action = "ACTION_UP";
                break;
            case MotionEvent.ACTION_CANCEL:
                action = "ACTION_CANCEL";
                break;
        }
        return action;
    }
}
