# SpringAI OpenAI Client using Official Java SDK

[![build](https://github.com/JavaAIDev/springai-openai-client/actions/workflows/build.yaml/badge.svg)](https://github.com/JavaAIDev/springai-openai-client/actions/workflows/build.yaml)
![Maven Central Version](https://img.shields.io/maven-central/v/com.javaaidev/springai-openai-client)

Spring AI `ChatModel` and `EmbeddingModel` implementations for OpenAI using
the [official SDK](https://github.com/openai/openai-java).

The motivation of this `ChatModel` and `EmbeddingModel` implementations is to use Spring AI with
Spring 5.

Add Maven dependency.

```xml

<dependency>
  <groupId>com.javaaidev</groupId>
  <artifactId>springai-openai-client</artifactId>
  <version>0.4.1</version>
</dependency>
```

## ChatModel

Supported features:

- Chat completions
- Function calling

### Use ChatModel

To use this `ChatModel`,

1. Create an `OpenAIClient`,
2. Create an `OpenAIChatModel`,
3. Create a Spring AI `ChatClient.Builder` with this `ChatModel`,
4. Create a Spring AI `ChatClient` from `ChatClient.Builder`.

See the code below:

```kotlin
val client = OpenAIOkHttpClient.fromEnv()
val chatModel = OpenAIChatModel(client)
val chatOptions = OpenAiChatOptions.builder()
    .model("gpt-4o-mini")
    .build()
val chatClient =
    ChatClient.builder(chatModel).defaultOptions(chatOptions).build()
val response = chatClient.prompt().user("tell me a joke")
    .call().content()
```

## EmbeddingModel

To use this `EmbeddingModel`,

1. Create an `OpenAIClient`,
2. Create an `OpenAIEmbeddingModel`

See the code below:

```kotlin
val client = OpenAIOkHttpClient.fromEnv()
val embeddingModel = OpenAIEmbeddingModel(client)
val response = embeddingModel.call(
    EmbeddingRequest(
        listOf("hello", "world"),
        OpenAIEmbeddingOptions.builder()
            .model("text-embedding-3-small")
            .build()
    )
)
```
