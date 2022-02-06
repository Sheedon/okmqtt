/*
 * Copyright (C) 2022 Sheedon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt

import org.sheedon.rr.core.DispatchAdapter
import org.sheedon.rr.core.IRequest
import org.sheedon.rr.dispatcher.AbstractClient
import org.sheedon.rr.dispatcher.DefaultEventManager
import org.sheedon.rr.timeout.android.TimeOutHandler
import java.lang.IllegalStateException

/**
 * RequestResponseBinder Decorated client class for request-response binding
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/30 12:58 下午
 */
class MqttRRBinderClient constructor(
    builder: Builder
) : AbstractClient<String/*backTopic*/,
        String/*message ID*/,
        RequestBody/*request format*/,
        ResponseBody/*response body*/>(
    builder
) {

    internal val switchMediator: DispatchAdapter<RequestBody, ResponseBody> =
        builder.loadDispatchAdapter()

    /**
     * Create a call for a request-response
     *
     * @param request request object
     * @return Call The action used to perform the enqueue submit request
     */
    override fun newCall(request: IRequest<String, RequestBody>): Call {
        return RealCall.newCall(this, request as Request)
    }

    /**
     * An observer Observable that creates information
     *
     * @param request request object
     * @return Observable Subscribe to a topic and listen for messages from that topic
     */
    override fun newObservable(request: IRequest<String, RequestBody>): Observable {
        return RealObserver.newObservable(this, request as Request)
    }


    class Builder :
        AbstractClient.Builder<MqttRRBinderClient, String, String, RequestBody, ResponseBody>() {

        // Character set encoding type
        internal var charsetName: String = "GBK"


        internal fun loadDispatchAdapter(): DispatchAdapter<RequestBody, ResponseBody> {
            return dispatchAdapter!!
        }

        /**
         * Set the character set encoding type and convert it to a string of the specified format when receiving data
         *
         * @param charsetName Character set encoding type
         * @return Builder builder
         */
        fun charsetName(charsetName: String) = apply {
            this.charsetName = charsetName
        }

        /**
         * Check and bind builds
         */
        override fun checkAndBind() {
            if (behaviorServices.isEmpty()) {
                behaviorServices.add(MqttEventBehaviorService())
            }
            if (eventManagerPool.isEmpty()) {
                eventManagerPool.add(DefaultEventManager())
            }
            if (timeoutManager == null) {
                timeoutManager = TimeOutHandler()
            }
            if (dispatchAdapter == null) {
                dispatchAdapter = SwitchMediator(charsetName, requestAdapter)
            }
            if (backTopicConverters.isEmpty()) {
                throw IllegalStateException("backTopicConverter is null.")
            }
            if (responseAdapter == null) {
                responseAdapter = MqttResponseAdapter(charsetName)
            }
        }

        override fun builder(): MqttRRBinderClient {
            return MqttRRBinderClient(this)
        }


    }
}