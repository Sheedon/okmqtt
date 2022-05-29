# OkMqtt
```tex
An MQTT client for an Android client that performs an MQTT request subscription and distributes the 
response results on demand.
```
[中文文档](README_CN.md)

MQTT is a client-side server architecture publish/subscribe messaging protocol. The current library 
gives you a much easier way to schedule.

OkMqtt is a client for response message distribution and provides a client that binds requests to 
responses:
* Request management pool, which distributes response results based on request header configuration. 
  And the subscription behavior is decomposed into three types
    * Send a message, subscribe to a topic for a response, and finally get a response result.
    * Send a message without a response.
    * Subscribe to a set of topics without any messages happening.
* Topic filtering, avoid wildcard topics and common topics common subscription, resulting in the same message multiple responses.
* When the network goes wrong, OkMqtt is relentless and quietly reconnecting.

OkMqtt is easy. Its request/response API is designed to be a smooth builder and immutable, with call-back asynchronous calls.

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
    implementation 'com.github.Sheedon:okmqtt:2.1.0-alpha-2.1'
}
```



#### Step 3: Configure Client

The parameters that the mqtt client usually needs to configure are configured as follows. For other parameters, please refer to the source code.

```java
OkMqttClient okMqttClient = new OkMqttClient.Builder()
        // Configure mqtt connection basic parameters
        .clientInfo(clientContext, serverUri, clientId)
        // Topic subscribed by default, optional
        .subscribeBodies(topicsBodies.toArray(new Topics[0]))
        // Response information parser acting on keyword association, 
        // configure custom feedback as needed
        .keywordConverter(new CallbackNameConverter(new Gson()))
        .build();
```



#### Step 4：Build the request and listen for the result

##### Build a single request

Build the request object and send a message to the MQTT-Broker. The contents of the request object include: subscription topic, message quality, retention or not, and MQTT message payload, as shown in the following simple implementation.

```java
// 1.Build the request object
Request request = new Request.Builder()
        // Subscription topic, message quality, whether to retain
        .topic(topic, qos, retain)
        // mqtt message payload
        .data(message)
        .build();

// 2.Get Call through the configured client class
Call call = client.newCall(request);
// 3.Execute request
call.publish();
```



#### Single message subscription

Build the request object, and implement the MQTT subscription by adding subscription associated items. There are two ways to subscribe:

First, the use of MQTT topic subscription, the implementation of "Request.Builder" construction method "subscribeTopic", the subscription topic configuration into the Request object, subscription topic support wildcard.

```java
// 1.Construct the request object of the subscription message, mainly the subject of the feedback
Request request = new Request.Builder()
        // Subscribed topics
        .subscribeTopic(topic)
        .build();

// 2.Get Observable through the configured client class
Observable observable = okMqttClient.newObservable(request);
// 3.execute subscription
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



Second is to use keyword subscription, which is defined by the developer and needs to be configured with "keywordConverter" when the client is constructed.

Most commonly, multiple message types are defined within the message payload under the same subscription topic, so a single subscription topic is not sufficient, and using message types as keywords simplifies the logical implementation of message distribution in daily development. 

For example, if the alarm subject is 「sheedon/data/alarm」 and the MQTT message {"type":"lamp","status":false}, if only the alarms whose 「type is lamp」 need to be processed, keyword subscription is more convenient.

```java
// 1.Construct the request object of the subscription message, mainly the subject of the feedback
Request request = new Request.Builder()
        // Subscribed topics
        .subscribeTopic("sheedon/data/alarm")
        // subscribed keywords
        .keyword("lamp")
        .build();

// 2.Get Observable through the configured client class
Observable observable = okMqttClient.newObservable(request);
// 3.execute subscription
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



#### request-response mode

Combined with a single request and a single subscription, the request response pattern is combined. This binding only applies to scheduling behavior with low probability of request conflicts.

The binding logic is: within the timeout range, if the first **response result** matches the configuration **associated with of**  the request, it is considered to have completed the scheduling of a set of request responses.

```java
// 1.Construct the request object of the subscription message, mainly the subject of the feedback
Request request = new Request.Builder()
        // Subscription topic, message quality, whether to retain
        .topic(topic, qos, retain)
        // mqtt message payload
        .data(message)
        // Subscribed topics
        .subscribeTopic(topicStr)
        // subscribed keywords
        .keyword(keywordStr)
        .build();

// 2.Get Call through the configured client class
Call call = client.newCall(request);
// 3.Execute request response monitoring
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



#### Multi-Subscription Topic Mode

In everyday development use, you might want to subscribe to similar topic groups that you don't want to subscribe to via wildcards. Stand-by multiple subscription topics can support this subscription requirement. 

In the construction mode, there are three ways to add the subscription topic: 

the first is to directly set the subscription topic as follows: A; 

the second is to configure Relation, which essentially has the same configuration information as 1; 

the third is to configure Relation set object.

```java
// 1.Construct a subscription object that subscribes to a topic group
Subscribe subscribe = new Subscribe.Builder()
        // A.Standard configuration, subject, keywords, message quality. You can choose either topic 
        // or keyword, or you can add both.
        .add(topic, keyword，qos)
        // Configured by Relation
        .add(relation)
        // Add Relation collection
        .addAll(relations)
        .build();

// 2.Get Observable through the configured client class
Observable observable = okMqttClient.newObservable(request);
// 3.Execute request response monitoring
observable.enqueue(new FullCallback() {
        @Override
		public void onResponse(@NonNull Observable observable, @NonNull Response response) {
		    Log.v("TAG", "response:${response.body()}")
		}

  	    @Override
   	    public void onResponse(@Nullable MqttWireMessage response) {
            // Subscribe to the situation
            Log.v("TAG", "response:${response.body()}")
        }
  
		@Override
		public void onFailure(@Nullable Throwable e) {
				Log.v("TAG", "e:$e")
		}
});
```



## methods and fields

### 1. OkMqttClient Construct configuration method

As mqtt-client should be shared, and use `new OkMqttClient.Builder()` to create a shared instance with custom settings:

| method                                                       | description                                                  | necessity |
| ------------------------------------------------------------ | ------------------------------------------------------------ | :-------- |
| charsetName(String charsetName)                              | MQTT message payload type. <br /> If [RequestBody.autoencode] is true and [RequestBody.charset] is null, the load encoding type of MQTTMessage is set with this value. The default charsetName is' UTF-8 '. | false     |
| messageTimeout(int timeout)                                  | This value, in seconds, defines the maximum interval at which a request will wait for a network callback until the response to an MQTT message is established. The default timeout is 5 seconds. | false     |
| keywordConverter(DataConverter<ResponseBody, String> keywordConverter) | Configure keyword matching logic, as required by the developer. The purpose is to extract the subject and message content of the MQTT message as a "transformed adapter class" implementation key to match the keywords added to the request, and if so feed back to the listener for the request or subscription. <br /> This configuration item cannot be missing when keyword subscriptions are used. | -         |



### 2. Request Constructional configuration method

Request，Is the bearer that sends MQTT messages and subscribes to an MQTT topic. The configuration information also consists of the responsibilities of these two parts.

| field                    | type             | description                                                  |
| ------------------------ | ---------------- | ------------------------------------------------------------ |
| body                     | RequestBody      | As the request content for constructing mqTT-Topic and MqttMessage. MQTT messages contain the application payload and options to specify how the message is delivered. The message consists of a 'payload' (the body of the message) represented as a byte []. |
| body.topic               | String           | The subject to which an MQTT message is sent, and cannot be empty. |
| body.data                | String           | MQTT messages, internally converted to bytes of the specified encoding type, can be null. |
| body.qos                 | Integer          | Quality of service for MQTT messages, 0, 1, or 2. Default quality of service: 0. |
| body.retained            | boolean          | Whether the messaging engine should keep publishing messages. <br /> sends a message that is reserved to 'true' and uses an empty byte array as the payload, such as' new Byte [0] 'to clear reserved messages from the server. <br /> Defaults to false. |
| body.charset             | String           | When sending the payload of a message, the receiver may only receive a data set in a specified character format, so the sender needs to change the character format so that the receiver can process the readable content. If charset is empty, the encoding format configured in the Client is used. |
| body.autoEncode          | boolean          | Whether to use the encoding format configured in Client. If body.charset is not empty, false is mandatory. Otherwise, if the manual configuration is set to false, the encoding format configured in the Client is not used. <br /> Is true by default. |
|                          |                  |                                                              |
| relation                 | Relation         | The configuration that associates the request object with the key data of the response. The associated information is' subject 'and' keyword '. <br />1. If both are configured, the keywords under the subscription topic are used. <br />2. Configure only subscribed topics, and verify only specified topics. <br />3. If only keywords are configured, the keywords of all topics are matched. |
| relation.topics          | Topics           | As part of an MQTT request or subscription data source, it is intended to configure the associated subscription topic as a feedback message. |
| topics.topic             | String           | Topics to which MQTT subscribed                              |
| topics.qos               | Integer          | Quality of service for an MQTT subscription, 0, 1, or 2. Default quality of service: 0. |
| topics.userContext       | Object           | Optional object used to pass the context to the callback. If not, use NULL. The default value is null. |
| topics.headers           | Headers          | Topic header information, which records the configuration information of the current subscribed topic, whether to reserve the subscription, and the subscription mode. |
| headers.attachRecord     | boolean          | Whether to keep a subscription record for automatic subscription to the current topic after MQTT reconnection. |
| headers.subscriptionType | SubscriptionType | Subscription type, including 'REMOTE' and 'LOCAL'. <br />**REMOTE** : REMOTE subscription, which requires the actual implementation of MQTT subscription <br />**LOCAL** : LOCAL subscription, which requires the LOCAL addition of the topic corresponding to the listener. <br />For example, when we do not want to subscribe remotely to wildcard topics, but do not want to re-subscribe locally to unsubscribe, we can subscribe locally to wildcard topics to meet the needs of a single topic subscription and prevent MQTT subscribes to wildcard topics from receiving invalid information. |
| relation.keyword         | String           | Is associated as a matching field of the response message.   |
| relation.timeout         | Integer          | Defines the maximum interval at which a request will wait for a network callback until an MQTT message response is established. |



## Thanks

I just encapsulated the mqTT-Client library developed by the @Eclipse Foundation, and borrowed the idea from @Square's OKHTTP, which demonstrates a free MQTT-Broker that connects to @EMQ






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

