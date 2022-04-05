package org.sheedon.app.factory

import com.google.gson.Gson
import org.sheedon.app.RspModel
import org.sheedon.mqtt.ResponseBody
import org.sheedon.mqtt.internal.DataConverter
import java.lang.Exception

/**
 * java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/1 10:58 上午
 */
class CallbackNameConverter(
    val gson: Gson
) : DataConverter<ResponseBody, String> {

    override fun convert(value: ResponseBody): String {
        var rspModel: RspModel<*>? = null
        try {
            rspModel = gson.fromJson(value.data, RspModel::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rspModel?.type ?: ""
    }
}