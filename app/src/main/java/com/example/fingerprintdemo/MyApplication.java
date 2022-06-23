package com.example.fingerprintdemo;

import android.app.Application;
import android.content.Context;

/**
 * PACK com.example.fingerprintdemo
 * CREATE BY Shay
 * DATE BY 2022/6/21 16:50 星期二
 * <p>
 * DESCRIBE
 * <p>
 */
// TODO:2022/6/21 

public class MyApplication extends Application {
    static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext(){
        return context;
    }
}
