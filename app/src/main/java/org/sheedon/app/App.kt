package org.sheedon.app

import android.app.Application

/**
 * java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/1 10:44 上午
 */
class App : Application() {


    companion object {

        lateinit var instance: Application
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}