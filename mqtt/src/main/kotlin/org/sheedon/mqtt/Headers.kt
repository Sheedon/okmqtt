package org.sheedon.mqtt

/**
 * mqtt调度库的头文件
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/3/7 10:52 下午
 */
class Headers private constructor(
    private val namesAndValues: Array<String>
) : Iterable<Pair<String, String>>{

    /** Returns the last value corresponding to the specified field, or null. */
    operator fun get(name: String): String? = get(namesAndValues, name)

    @get:JvmName("size") val size: Int
        get() = namesAndValues.size / 2


    override fun iterator(): Iterator<Pair<String, String>> {
        TODO("Not yet implemented")
    }
}