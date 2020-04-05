# MqttDispatcher

### Gradle

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

**Step 2.** Add the dependency

```
	dependencies {
	        implementation 'com.github.Sheedon:MqttDispatcher:Tag'
	}
```



### Maven

**Step 1.** Add the JitPack repository to your build file

```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

**Step 2.** Add the dependency

```
	<dependency>
	    <groupId>com.github.Sheedon</groupId>
	    <artifactId>MqttDispatcher</artifactId>
	    <version>Tag</version>
	</dependency>
```



### Create

```java
OkMqttClient mClient = new OkMqttClient.Builder()
        .clientInfo(App.getInstance(), serverUri, clientId)// 上下文，mqtt地址，客户端id 
        .subscribeBodies(subscribeBodies)// 订阅主题
        .baseTopic("")// 添加基础主题，用于请求
        .addConverterFactory(CallbackRuleConverterFactory.create())//反馈匹配规则转化工厂
        .callback(this)// 额外反馈监听
        .build();
```



