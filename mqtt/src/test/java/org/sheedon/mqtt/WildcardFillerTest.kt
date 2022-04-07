package org.sheedon.mqtt

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.junit.Assert
import org.junit.Test

/**
 * java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/7 6:18 下午
 */
class WildcardFillerTest {

    private val filler = WildcardFiller()

    @Test
    fun subscribe() {

        // 添加一个无通配符主题 AA/BB/CC
        // 返回数据为该主题，无取消的反馈数据
        subscribe_AA_BB_CC()

        // 添加一个无通配符主题 AA/BB/DD
        // 返回数据为该主题，无取消的反馈数据
        subscribe_AA_BB_DD()

        // 添加一个有通配符主题，无关联 AA/BB/EE/#
        // 返回该主题，无取消的反馈数据
        `subscribe_AA_BB_EE_#`()

        subscribe_AA_BB_CC_again()

        // 添加一个有通配符主题，有关联 AA/BB/#
        // 返回该主题，取消主题为上面三个
        `subscribe_AA_BB_#`()

        // 添加一个有通配符主题，有关联 AA/BB/+
        // 无返回订阅，无返回取消
        `subscribe_AA_BB_+`()
    }

    /**
     * 添加一个无通配符主题 AA/BB/CC
     * 返回数据为该主题，无取消的反馈数据
     */
    private fun subscribe_AA_BB_CC() {
        val topic = Topics.build("AA/BB/CC", 0, true)
        val subscribe = filler.subscribe(topic) {
            // 不会进入
            Assert.assertTrue(false)
        }
        Assert.assertEquals(subscribe, topic)
    }

    /**
     * 添加一个无通配符主题 AA/BB/CC
     * 返回数据为该主题，无取消的反馈数据
     */
    private fun subscribe_AA_BB_CC_again() {
        val topic = Topics.build("AA/BB/CC", 0, true)
        val subscribe = filler.subscribe(topic) {
            // 不会进入
            Assert.assertTrue(false)
        }
        Assert.assertEquals(subscribe, null)
    }


    /**
     * 添加一个无通配符主题 AA/BB/DD
     * 返回数据为该主题，无取消的反馈数据
     */
    private fun subscribe_AA_BB_DD() {
        val topic = Topics.build("AA/BB/DD", 0, true)
        val subscribe = filler.subscribe(topic) {
            // 不会进入
            Assert.assertTrue(false)
        }
        Assert.assertEquals(subscribe, topic)
    }

    /**
     * 添加一个有通配符主题 AA/BB/EE/#
     * 返回数据为该主题，无取消的反馈数据
     */
    private fun `subscribe_AA_BB_EE_#`() {
        val topic = Topics.build("AA/BB/EE/#", 0, true)
        val subscribe = filler.subscribe(topic) {
            // 不会进入
            Assert.assertTrue(false)
        }
        Assert.assertEquals(subscribe, topic)
    }

    /**
     * 添加一个有通配符主题，有关联 AA/BB/#
     * 返回该主题，取消主题为上面三个
     */
    private fun `subscribe_AA_BB_#`() {
        val topic = Topics.build("AA/BB/#", 0, true)
        val subscribe = filler.subscribe(topic) {
            Assert.assertArrayEquals(
                (it as ArrayList).toArray(),
                arrayOf("AA/BB/CC", "AA/BB/DD", "AA/BB/EE/#")
            )
        }
        Assert.assertEquals(subscribe, topic)
    }

    /**
     * 添加一个有通配符主题，有关联 AA/BB/#
     * 返回该主题，取消主题为上面三个
     */
    private fun `subscribe_AA_BB_+`() {
        val topic = Topics.build("AA/BB/+", 0, true)
        val subscribe = filler.subscribe(topic) {
            // 不会进入
            Assert.assertTrue(false)
        }
        Assert.assertEquals(subscribe, null)
    }


    @Test
    fun subscribeArray() {
        // 需要订阅，无需取消
        subscribe_and_cannot_unsubscribe()

        // 需要订阅，需要取消
        subscribe_and_unsubscribe()

        cannot_subscribe_and_cannot_unsubscribe()
    }

    /**
     * 需要订阅，无需取消
     */
    fun subscribe_and_cannot_unsubscribe() {
        val topic = listOf(
            Topics.build("AA/BB/CC", 0, true),
            Topics.build("AA/BB/DD", 0, true),
            Topics.build("AA/BB/EE/#", 0, true),
        )
        val subscribe = filler.subscribe(topic) {
            // 不会进入
            Assert.assertTrue(false)
        }
        // 订阅项
        val topics = subscribe.first
        (topics as ArrayList).removeAll(arrayOf("AA/BB/CC", "AA/BB/DD", "AA/BB/EE/#"))
        Assert.assertTrue(topics.isEmpty())
    }

    /**
     * 需要订阅，需要取消
     */
    fun subscribe_and_unsubscribe() {
        val topic = listOf(
            Topics.build("AA/BB/#", 0, true),
            Topics.build("AA/BB/FF/#", 0, true)
        )
        val subscribe = filler.subscribe(topic) {
            (it as ArrayList).removeAll(arrayOf("AA/BB/CC", "AA/BB/DD", "AA/BB/EE/#"))
            Assert.assertTrue(it.isEmpty())
        }
        // 订阅项
        val topics = subscribe.first
        Assert.assertArrayEquals(topics.toTypedArray(), arrayOf("AA/BB/#"))
    }


    /**
     * 不需要订阅，不需要取消
     */
    fun cannot_subscribe_and_cannot_unsubscribe() {
        val topic = listOf(
            Topics.build("AA/BB/+", 0, true),
            Topics.build("AA/BB/FF/+", 0, true)
        )
        val subscribe = filler.subscribe(topic) {
            Assert.assertTrue(false)
        }
        // 订阅项
        val topics = subscribe.first
        Assert.assertTrue(topics.isEmpty())
    }

    /**
     * 单项取消订阅
     */
    @Test
    fun unsubscribe() {
        subscribe_AA_BB_CC()
        unsubscribe_AA_BB_CC()

        subscribe_and_cannot_unsubscribe()
        subscribe_and_unsubscribe()

        cannot_unsubscribe_and_cannot_subscribe()
    }

    /**
     * 有取消，无订阅
     */
    private fun unsubscribe_AA_BB_CC() {
        val topic = Topics.build("AA/BB/CC", 0, true)
        val subscribe = filler.unsubscribe(topic) { topics, qos ->
            // 不会进入
            Assert.assertTrue(false)
        }
        Assert.assertEquals(subscribe, topic)
    }

    /**
     * 有取消，有订阅
     */
    private fun unsubscribe_and_subscribe() {
        val topic = Topics.build("AA/BB/#", 0, true)
        val subscribe = filler.unsubscribe(topic) { topics, qos ->
            Assert.assertEquals(topics.toTypedArray(), listOf("AA/BB/EE/#", "AA/BB/CC", "AA/BB/DD"))
        }
        Assert.assertEquals(subscribe, topic)
    }


    /**
     * 不需要订阅，不需要取消
     */
    fun cannot_unsubscribe_and_cannot_subscribe() {
        val topic = Topics.build("AA/BB/FF/+", 0, true)
        val subscribe = filler.subscribe(topic) {
            Assert.assertTrue(false)
        }
        // 订阅项
        Assert.assertEquals(subscribe, null)
    }


    /**
     * 多项取消订阅
     */
    @Test
    fun unsubscribeArray() {
        // 有取消，无订阅
        subscribe_and_cannot_unsubscribe()
//        unsubscribeAndNotSubscribe()

//        // 需要订阅，需要取消
        subscribe_and_unsubscribe()
//        unsubscribeAndSubscribe()
//
//        cannot_subscribe_and_cannot_unsubscribe()
        cannotUnSubscribeAndCannotSubscribe()
    }

    /**
     * 有取消，无订阅
     */
    fun unsubscribeAndNotSubscribe() {
        val topic = listOf(
            Topics.build("AA/BB/CC", 0, true),
            Topics.build("AA/BB/DD", 0, true)
        )
        val subscribe = filler.unsubscribe(topic) { topics, qos ->
            // 不会进入
            Assert.assertTrue(false)
        }
        // 订阅项
        Assert.assertArrayEquals(subscribe.toTypedArray(), arrayOf("AA/BB/CC", "AA/BB/DD"))
    }

    /**
     * 有订阅，有取消
     */
    fun unsubscribeAndSubscribe() {
        val topic = listOf(
            Topics.build("AA/BB/#", 0, true),
            Topics.build("AA/BB/FF/#", 0, true)
        )
        val subscribe = filler.unsubscribe(topic) { topics, qos ->
            Assert.assertArrayEquals(
                topics.toTypedArray(),
                arrayOf("AA/BB/EE/#", "AA/BB/CC", "AA/BB/DD")
            )
        }
        // 订阅项
        Assert.assertArrayEquals(subscribe.toTypedArray(), arrayOf("AA/BB/#", "AA/BB/FF/#"))
    }

    /**
     * 无订阅，无取消
     */
    fun cannotUnSubscribeAndCannotSubscribe() {
        val topic = listOf(
            Topics.build("AA/BB/CC", 0, true),
            Topics.build("AA/BB/DD", 0, true)
        )
        val subscribe = filler.unsubscribe(topic) { topics, qos ->
            // 不会进入
            Assert.assertTrue(false)
        }
        // 订阅项
        Assert.assertArrayEquals(subscribe.toTypedArray(), arrayOf())
    }
}