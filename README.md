# MqttDispatcher
```tex
For different mqtt topic messages, implement a scheduling framework for message matching.
```
[中文文档](README_CN.md)

Send the request message sent by the client and provide feedback monitoring, and send it to the `mqtt server` side with the help of `Mqtt Dispatcher`. After the server receives the request data, it feeds back the request response result, and then `Mqtt Dispatcher` obtains the feedback listener according to the configured feedback topic, and sends the feedback result, so as to realize a complete scheduling of the request response.


## How to use

#### Step 1: Add the JitPack repository to your build file

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```



#### Step 2: Add core dependencies

```groovy
dependencies {
    implementation 'com.github.Sheedon:MqttDispatcher:2.0-alpha'
}
```



#### Step 3: Configure Client

The parameters that the mqtt client usually needs to configure are configured as follows. For other parameters, please refer to the source code.

```java
client = OkMqttClient.Builder()
  .clientInfo(context, // content: ApplicationContext
              serverUri, // serverUri: example tcp://127.0.0.1:3883
              clientId) // clientId: Client ID
  .subscribeBodies(subscribeBodies = subscribeBodies.toTypedArray())// Topic to subscribe to
  .baseTopic("xxx") // Add base subject, Not required
  .addBackTopicConverter(CallbackNameConverter(Gson()))// Callback Topic Changer
  .build()
```



#### Step 4：Build the request and listen for the result

##### Build a single request

```kotlin
// 1.Build the request object
val request = Request.Builder()
    .backTopic("get_manager_list")
    .data(jsonObject.toString())
    .build()

// 2.Get Call through the configured client class
val call = client.newCall(request)
// 3.Execute request scheduling
call.enqueue(object :Callback{
    override fun onFailure(e: Throwable) {
        Log.v("TAG", "e:$e")
    }

    override fun onResponse(request: Request, response: Response) {
        Log.v("TAG", "response:${response.body()}")
    }
})
```

##### Build a message subscription

```kotlin
// 1.Construct the request object of the subscription message, mainly the subject of the feedback
val request: Request = Request.Builder()
    .backTopic("get_manager_list")
    .build()

// 2.Get Observable through the configured client class
val observable = client.newObservable(request)
// 3.execute subscription
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

