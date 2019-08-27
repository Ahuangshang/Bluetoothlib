package com.baidu.test;

import android.annotation.SuppressLint;
import android.app.Application;

import com.baidu.tts.BTClient;

/**
 * Description:
 * Created on 2019/8/27 0027 11:45:41
 * Organization:华云
 * author:AHuangSHang
 */
public class app extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BTClient.init(this);
    }
}
