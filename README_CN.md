# OkMqtt

```tex
一个用于Android客户端，执行MQTT请求订阅，并将响应结果按需分发的MQTT客户端。
```

[English](README.md)

MQTT是一个客户端服务端架构的发布/订阅模式的消息传输协议。当前库可以提供给您以更简便的调度方式。
OkMqtt是一个用于响应消息分发，并且提供将请求与响应相绑定的客户端：
* 请求管理池，根据请求头配置来分发响应结果。并且将订阅行为分解为三种类型
    * 发送一条消息，并且订阅一个响应所需的主题，最终得到一个响应结果。
    * 发送一条消息，无需响应。
    * 订阅一组主题，不发生任何消息。
* 主题过滤，避免带通配符主题与普通主题共同订阅，导致同一个消息多次响应。
* 当网络出现问题，OkMqtt会坚持不懈地默默重新连接。

使用OkMqtt很容易。它的请求/响应 API 设计有流畅的构建器和不变性，并且采用带回调的异步调用。



## 使用方式



### 第一步：将 JitPack 存储库添加到您的构建文件中

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

### 第二步：添加核心依赖

```groovy
dependencies {
    implementation 'com.github.Sheedon:okmqtt:2.1.0-alpha-1'
}
```

### 第三步：配置客户端

如下配置了mqtt客户端通常需要配置的参数，其他参数可参考源码。

```java
OkMqttClient okMqttClient = new OkMqttClient.Builder()
                // 配置mqtt连接基本参数
                .clientInfo(clientContext, serverUri, clientId)
  							// 默认订阅的主题，可不配置
                .subscribeBodies(topicsBodies.toArray(new Topics[0]))
                // 作用于关键字关联的响应信息解析器，按需配置自定义反馈
                .keywordConverter(new CallbackNameConverter(new Gson()))
                .build();
```

### 第四步：构建请求，监听结果

#### 单一请求

构建请求对象，往MQTT-Broker发送一条消息。请求对象的内容包括：订阅主题，消息质量，是否保留和mqtt消息有效载荷，如下是一个简单实现。

```java
// 1.构建请求对象
Request request = new Request.Builder()
        // 订阅主题，消息质量，是否保留
        .topic(topic, qos, retain)
        // mqtt消息有效载荷
        .data(message)
        .build();

// 2.通过配置的客户端类，来得到Call
Call call = client.newCall(request);
// 3.执行请求调度
call.publish();
```



#### 单一消息订阅

构建请求对象，通配添加订阅关联项，实现MQTT订阅。订阅方式有两种：

其一，采用mqtt主题订阅，执行「Request.Builder」构造的方法「backTopic」，将订阅主题配置入请求对象中，订阅主题支持通配符。

```java
// 1.构建订阅消息的请求对象，主要是反馈的主题
Request request = new Request.Builder()
        // 订阅的主题
        .backTopic(topic)
        .build();

// 2.通过配置的客户端类，来得到Observable
Observable observable = okMqttClient.newObservable(request);
// 3.执行订阅
observable.enqueue(new ObservableBack() {
        @Override
        public void onResponse(@NonNull Observable observable, @NonNull Response response) {
                Log.v("TAG", "response:${response.body()}")
        }

        @Override
        public void onFailure(@Nullable Throwable e) {
                Log.v("TAG", "e:$e")
        }
});
```

其二，采用关键字订阅，此订阅方式由开发者自行定义，且需要在客户端构造时配置「keywordConverter」来实现。最常见的是，同一个订阅主题下，消息有效载荷内定义了多种消息类型，那么单一订阅主题就无法满足使用，而将消息类型做为关键字，便简化了日常开发中的消息分发的逻辑实现。例如告警主题为「sheedon/data/alarm」，mqtt消息{"type":"lamp","status":false}，在此若只需要处理type为lamp的告警，则采用关键字订阅更加便捷。

```java
// 1.构建订阅消息的请求对象，主要是反馈的主题
Request request = new Request.Builder()
        // 订阅的主题
        .backTopic("sheedon/data/alarm")
        // 订阅的关键字
        .keyword("lamp")
        .build();

// 2.通过配置的客户端类，来得到Observable
Observable observable = okMqttClient.newObservable(request);
// 3.执行订阅
observable.enqueue(new ObservableBack() {
        @Override
        public void onResponse(@NonNull Observable observable, @NonNull Response response) {
                Log.v("TAG", "response:${response.body()}")
        }

        @Override
        public void onFailure(@Nullable Throwable e) {
                Log.v("TAG", "e:$e")
        }
});
```



#### 请求响应模式

结合单一请求和单一订阅，就组合而成请求响应模式，该绑定只针对请求冲突概率低的调度行为。

绑定逻辑为：在超时范围内，第一个**响应结果**符合请求所**关联**的配置，则认为是完成一组请求响应的调度。

```java
// 1.构建订阅消息的请求对象，主要是反馈的主题
Request request = new Request.Builder()
        // 订阅主题，消息质量，是否保留
        .topic(topic, qos, retain)
        // mqtt消息有效载荷
        .data(message)
        // 订阅的主题
        .backTopic(topicStr)
        // 订阅的关键字
        .keyword(keywordStr)
        .build();

// 2.通过配置的客户端类，来得到Call
Call call = client.newCall(request);
// 3.执行请求响应监听
call.enqueue(new Callback() {
        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
                Log.v("TAG", "response:${response.body()}")
        }

        @Override
        public void onFailure(@Nullable Throwable e) {
                Log.v("TAG", "e:$e")
        }
});
```



#### 多订阅主题模式

在日常开发使用中，可能存在不希望通过通配符订阅，却需要订阅类似的主题组。待用多订阅主题即可支持该订阅需求。构造模式下提供三种添加订阅主题的方式：其一直接设置订阅主题如下a，其二是通过配置Relation，本质上配置信息同1，其三则是配置Relation集合对象。

```java
// 1.构建订阅主题组的订阅对象
Subscribe subscribe = new Subscribe.Builder()
        // a.标准配置，主题，关键字，消息质量。主题和关键字二选一，也可都添加。
        .add(topic, keyword，qos)
        // 通过Relation配置
        .add(relation)
        // 添加Relation集合
        .addAll(relations)
        .build();

// 2.通过配置的客户端类，来得到Observable
Observable observable = okMqttClient.newObservable(request);
// 3.执行请求响应监听
observable.enqueue(new FullCallback() {
        @Override
		public void onResponse(@NonNull Observable observable, @NonNull Response response) {
		    Log.v("TAG", "response:${response.body()}")
		}

  	    @Override
   	    public void onResponse(@Nullable MqttWireMessage response) {
            // 订阅情况
            Log.v("TAG", "response:${response.body()}")
        }
  
		@Override
		public void onFailure(@Nullable Throwable e) {
				Log.v("TAG", "e:$e")
		}
});
```



## 方法与字段

### 1. OkMqttClient 构造配置方法

作为mqtt-client应该被共享，且使用 `new OkMqttClient.Builder()` 创建具有自定义设置的共享实例：

| 方法                                                         | 说明                                                         | 必要性                                      |
| ------------------------------------------------------------ | ------------------------------------------------------------ | :------------------------------------------ |
| charsetName(String charsetName)                              | mqtt消息有效载荷类型。<br />如果 [RequestBody.autoEncode] 为 true 且 [RequestBody.charset] 为 null，则通过该值设置 MQTTMessage 的负载编码类型。默认的 charsetName 是 `UTF-8`。 | 否                                          |
| messageTimeout(int timeout)                                  | 此值（以秒为单位）定义了请求将等待网络回调到 MQTT 消息响应建立的最大时间间隔。默认超时为 5 秒。 | 否                                          |
| keywordConverter(DataConverter<ResponseBody, String> keywordConverter) | 配置关键字匹配逻辑，由开发者按需配置。目的是将mqtt消息的主题和消息内容按「转换的适配器类」实现关键字提取，从而与请求所添加的关键字匹配，一致则反馈到请求或订阅的监听对象中。<br />在使用关键字订阅情况下，该配置项不可欠缺。 | 采用关键字：是                 不使用则：否 |



### 2. Request 构造配置方法

Request，是作为发送mqtt消息，订阅一条mqtt主题的承载者。配置信息也是由这两部分的职责组成。

| 字段                 | 类型             | 说明                                                         |
| -------------------- | ---------------- | ------------------------------------------------------------ |
| body                 | RequestBody      | 作为构造 MQTT-Topic 和 MqttMessage 的请求内容。 MQTT 消息包含应用程序有效负载和指定如何传递消息的选项。该消息包括一个`有效负载`（消息的主体），表示为一个字节 []。 |
| body.topic           | String           | 发送一条 mqtt 消息的主题，不能为空。                         |
| body.data            | String           | mqtt消息，内部会转换为制定编码类型的byte，可为空。           |
| body.qos             | Integer          | mqtt消息的服务质量，0、1 或 2。默认服务质量：0。             |
| body.retained        | boolean          | 消息传递引擎是否应保留发布消息。<br />发送一条保留设置为 `true` 并使用空字节数组作为有效负载的消息，例如`new byte[0]`将从服务器清除保留的消息。<br />默认值为false。 |
| body.charset         | String           | 在发送消息的有效载荷时，接收方可能只能接收到指定字符格式的数据集，所以发送方需要改变字符格式，以便接收方可以处理可读的内容。如果 charset 为空，则采用Client中配置的编码格式。 |
| body.autoEncode      | boolean          | 是否采用Client中配置的编码格式，若body.charset 不为空，则强制为false。否则，若手动配置配置为false，则不使用Client中配置的编码格式。<br />默认中为true。 |
|                      |                  |                                                              |
| relation             | Relation         | 将请求对象与响应的关键数据实现关联的配置。<br />关联的信息是 `主题` 和 `关键字`。<br />1. 如果两者都配置，则使用该订阅主题下的关键字。<br />2. 只配置订阅主题，则只核实指定主题。<br />3. 只配置关键字，则匹配所有主题下的关键字。 |
| relation.topics      | Topics           | 作为 MQTT 请求或订阅数据源的一部分，它旨在将关联的订阅主题配置为反馈消息。 |
| topics.topic         | String           | MQTT订阅的主题                                               |
| topics.qos           | Integer          | MQTT订阅的服务质量，0、1 或 2。默认服务质量：0。             |
| topics.userContext   | Object           | 用于将上下文传递给回调的可选对象。如果不需要，请使用 null。默认值为空。 |
| topics.headers       | Headers          | 主题头信息，记录当前订阅主题的配置信息，是否保留订阅和订阅方式。 |
| headers.attachRecord | boolean          | 是否保留订阅记录，用于mqtt重连后，自动订阅当前主题。         |
| headers.attachRecord | SubscriptionType | 订阅类型，由 `REMOTE` 和 `LOCAL` 。<br />**REMOTE**：远程订阅，代表该订阅需要真实实现MQTT订阅<br />**LOCAL**：本地订阅，代表该订阅只需要在本地添加主题所对应的监听即可。<br />例如我们在不希望远程订阅带通配符的主题，但本地又不希望重复订阅取消订阅相关主题是，可在本地订阅带通配符的主题，即满足单一主题订阅的需要，又防止MQTT订阅通配符主题导致接收到无效信息。 |
| relation.keyword     | String           | 作为响应消息的匹配字段进行关联。                             |
| relation.timeout     | Integer          | 定义了请求将等待网络回调到 MQTT 消息响应建立的最大时间间隔。 |



## 感谢

我只是在@Eclipse Foundation 开发的mqtt-client库上做了封装，思路上的借鉴@Square的okhttp，其中演示连接@EMQ的免费MQTT-Broker





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

