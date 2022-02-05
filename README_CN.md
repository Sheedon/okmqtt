# MqttDispatcher
```tex
针对于不同 mqtt 主题消息，实现消息匹配的调度框架。
```
[English](README.md)

将客户端发送的请求消息，并提供反馈监听，借助 `Mqtt Dispatcher` 发送到 `mqtt server` 端。服务器接收请求数据后，反馈请求响应结果，再由 `Mqtt Dispatcher` 根据配置的 **反馈主题** ，得到反馈监听器，并发送反馈结果，从而实现一次完整的请求响应的调度。



## 使用方式

#### 第一步：将 JitPack 存储库添加到您的构建文件中

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```



#### 第二步：添加核心依赖

```groovy
dependencies {
    implementation 'com.github.Sheedon:MqttDispatcher:2.0-alpha'
}
```



#### 第三步：配置客户端

如下配置了mqtt客户端通常需要配置的参数，其他参数可参考源码。

```java
client = OkMqttClient.Builder()
  .clientInfo(context, // content: ApplicationContext
              serverUri, // serverUri:服务器地址 例如tcp://127.0.0.1:3883
              clientId) // clientId: 客户端ID
  .subscribeBodies(subscribeBodies = subscribeBodies.toTypedArray())// 需要订阅的主题
  .baseTopic("xxx") // 添加基础主题 非必填
  .addBackTopicConverter(CallbackNameConverter(Gson()))// 反馈主题转换器
  .build()
```



#### 第四步：构建请求，监听结果

##### 构建一个单一的请求

```kotlin
// 1.构建请求对象
val request = Request.Builder()
    .backTopic("get_manager_list")
    .data(jsonObject.toString())
    .build()

// 2.通过配置的客户端类，来得到Call
val call = client.newCall(request)
// 3.执行请求调度
call.enqueue(object :Callback{
    override fun onFailure(e: Throwable) {
        Log.v("TAG", "e:$e")
    }

    override fun onResponse(request: Request, response: Response) {
        Log.v("TAG", "response:${response.body()}")
    }
})
```

##### 构建一个消息的订阅

```kotlin
// 1.构建订阅消息的请求对象，主要是反馈的主题
val request: Request = Request.Builder()
    .backTopic("get_manager_list")
    .build()

// 2.通过配置的客户端类，来得到Observable
val observable = client.newObservable(request)
// 3.执行订阅
observable.subscribe(object : Callback {
    override fun onFailure(e: Throwable) {
        Log.v("TAG", "e:$e")
    }

    override fun onResponse(request: Request, response: Response) {
        Log.v("TAG", "response:${response.body()}")
    }

})
```





## [License](LICENSE)

```tex
Copyright 2022 Sheedon.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

