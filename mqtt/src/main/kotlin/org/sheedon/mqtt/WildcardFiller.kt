package org.sheedon.mqtt

/**
 * 通配符过滤类
 *
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/25 5:58 下午
 */
internal class WildcardFiller(
    var subscribeBodies: MutableMap<String, SubscribeBody> = mutableMapOf()
) {

    /**
     * 订阅主题
     *
     * @param body 单项订阅内容
     */
    internal fun subscribe(body: SubscribeBody) {
        subscribeBodies
            .takeUnless { subscribeBodies.containsKey(body.convertKey()) }
            ?.let { it[body.convertKey()] = body }
    }

    /**
     * 订阅主题
     *
     * @param bodies 订阅内容集合
     * @param attachRecord 是否附加到记录中
     */
    internal fun subscribe(
        bodies: List<SubscribeBody>,
        attachRecord: Boolean = false,
    ): Pair<List<String>, List<Int>> {

        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()

        bodies.forEach { body ->
            subscribeBodies
                .also {
                    topic.add(body.topic!!)
                    qos.add(body.qos)
                }
                .takeIf { attachRecord && !subscribeBodies.containsKey(body.convertKey()) }
                ?.let {
                    it[body.convertKey()] = body
                }
        }

        return Pair(topic, qos)
    }


    /**
     * 取消订阅主题
     *
     * @param body 单项订阅内容
     */
    internal fun unsubscribe(
        body: SubscribeBody
    ) {
        subscribeBodies
            .takeIf { subscribeBodies.containsKey(body.convertKey()) }
            ?.remove(body.convertKey())
    }

    /**
     * 取消订阅主题
     *
     * @param bodies 订阅内容集合
     * @param attachRecord 是否附加到记录中
     */
    internal fun unsubscribe(
        bodies: List<SubscribeBody>,
        attachRecord: Boolean = false,
    ): List<String> {

        val topic = mutableListOf<String>()
        val qos = mutableListOf<Int>()

        bodies.forEach { body ->
            subscribeBodies
                .also {
                    topic.add(body.topic!!)
                    qos.add(body.qos)
                }
                .takeIf { attachRecord && subscribeBodies.containsKey(body.convertKey()) }
                ?.remove(body.convertKey())
        }

        return topic
    }

    /**
     * 获取订阅主题内容集合
     */
    internal fun getSubscribeBodyList(): List<SubscribeBody> {
        return subscribeBodies.values.toMutableList()
    }

    /**
     * 清空订阅
     */
    internal fun clear() {
        subscribeBodies.clear()
    }

}