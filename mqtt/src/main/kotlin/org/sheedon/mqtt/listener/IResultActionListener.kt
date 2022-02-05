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
package org.sheedon.mqtt.listener

/**
 * connect/disconnect/subscribe/unsubscribe 's action listener
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/1/28 8:14 下午
 */
interface IResultActionListener {
    /**
     * This method is invoked when an action has completed successfully.
     */
    fun onSuccess()

    /**
     * This method is invoked when an action fails.
     *
     * @param exception thrown by the action that has failed
     */
    fun onFailure(exception: Throwable?)
}