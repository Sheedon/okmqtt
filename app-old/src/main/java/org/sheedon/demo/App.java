package org.sheedon.demo;

import android.app.Application;

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/4 12:40
 */
public class App extends Application {

    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

    }

    /**
     * 外部获取单例
     *
     * @return Application
     */
    public static Application getInstance() {
        return instance;
    }
}
