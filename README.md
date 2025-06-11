# SpringAI OpenAI Client using Official Java SDK

[![build](https://github.com/JavaAIDev/springai-openai-client/actions/workflows/build.yaml/badge.svg)](https://github.com/JavaAIDev/springai-openai-client/actions/workflows/build.yaml)
![Maven Central Version](https://img.shields.io/maven-central/v/com.javaaidev/springai-openai-client)
[![javadoc](https://javadoc.io/badge2/com.javaaidev/springai-openai-client/javadoc.svg)](https://javadoc.io/doc/com.javaaidev/springai-openai-client)

Spring AI `ChatModel` and `EmbeddingModel` implementations for OpenAI using
the [official SDK](https://github.com/openai/openai-java).

The motivation of this `ChatModel` and `EmbeddingModel` implementations is to use Spring AI with
Spring 5.

Add Maven dependency of the latest version.

```xml

<dependency>
  <groupId>com.javaaidev</groupId>
  <artifactId>springai-openai-client</artifactId>
  <version>${VERSION}</version>
</dependency>
```

## ChatModel

Supported features:

- Chat completions
- Function calling
- Streaming mode

### Use ChatModel

To use this `ChatModel`,

1. Create an `OpenAIClient`,
2. Create an `OpenAIChatModel`,
3. Create a Spring AI `ChatClient.Builder` with this `ChatModel`,
4. Create a Spring AI `ChatClient` from `ChatClient.Builder`.

See the code below:

```kotlin
val client = OpenAIOkHttpClient.fromEnv()
val chatModel = OpenAIChatModel(client, 
    DefaultToolCallingManager.builder().toolCallbackResolver(CustomToolCallbackResolver()).build())
val chatOptions = OpenAiChatOptions.builder()
    .model("gpt-4o-mini")
    .build()
chatClient =
    ChatClient.builder(chatModel).defaultOptions(chatOptions).build()
val response = chatClient.prompt().user("tell me a joke")
    .call().content()
```

### Streaming

Streaming mode is also supported.

```kotlin
val builder = StringBuilder()
chatClient.prompt().user("tell me a joke")
    .stream().chatResponse().doOnNext {
        builder.append(it.result.output.text)
    }.blockLast()
val result = builder.toString()
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
