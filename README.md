# Spring AI ChatModel using Official Java SDK

[![build](https://github.com/JavaAIDev/openai-chatmodel-standalone/actions/workflows/build.yaml/badge.svg)](https://github.com/JavaAIDev/openai-chatmodel-standalone/actions/workflows/build.yaml)
![Maven Central Version](https://img.shields.io/maven-central/v/com.javaaidev/openai-chatmodel-standalone)


Spring AI `ChatModel` implementation for OpenAI using
the [official SDK](https://github.com/openai/openai-java).

The motivation of this `ChatModel` implementation is to use Spring AI with Spring 5.

Supported features:

- Chat completions
- Function calling

## Use ChatModel

Add Maven dependency.

```xml

<dependency>
  <groupId>com.javaaidev</groupId>
  <artifactId>openai-chatmodel-standalone</artifactId>
  <version>0.2.0</version>
</dependency>
```

To use this `ChatModel`,

1. Create an `OpenAIClient`
2. Create an `OpenAIChatModel`
3. Create a Spring AI `ChatClient.Builder` with this `ChatModel`
4. Create a Spring AI `ChatClient` from `ChatClient.Builder`

See the code below:

```kotlin
val client = OpenAIOkHttpClient.fromEnv()
val chatModel = OpenAIChatModel(client)
val chatOptions = OpenAiChatOptions.builder()
    .model("gpt-3.5-turbo")
    .build()
val chatClient =
    ChatClient.builder(chatModel).defaultOptions(chatOptions).build()
val response = chatClient.prompt().user("tell me a joke")
    .call().content()
```
