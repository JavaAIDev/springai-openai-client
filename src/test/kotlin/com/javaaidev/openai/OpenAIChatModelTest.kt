package com.javaaidev.openai

import com.openai.client.okhttp.OpenAIOkHttpClient
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.model.tool.DefaultToolCallingManager
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.function.FunctionToolCallback
import org.springframework.ai.tool.resolution.ToolCallbackResolver
import java.util.function.Function
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenAIChatModelTest {
    private val chatClient: ChatClient

    init {
        val client = OpenAIOkHttpClient.fromEnv()
        val chatModel = OpenAIChatModel(
            client,
            DefaultToolCallingManager.builder().toolCallbackResolver(CustomToolCallbackResolver()).build()
        )
        val chatOptions = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .build()
        chatClient =
            ChatClient.builder(chatModel).defaultOptions(chatOptions).build()
    }

    @Test
    @DisplayName("Simple text completion")
    fun testChatCompletion() {
        val response = chatClient.prompt().user("tell me a joke")
            .call().content()
        assertNotNull(response)
    }

    @Test
    @DisplayName("Simple streaming completion")
    fun testStreamCompletion() {
        val builder = StringBuilder()
        chatClient.prompt().user("tell me a joke")
            .stream().chatResponse().doOnNext {
                builder.append(it.result.output.text)
            }.blockLast()
        val result = builder.toString()
        assertTrue { result.isNotEmpty() }
    }

    @Test
    @DisplayName("Tool calling")
    fun testToolCalling() {
        val response = chatClient.prompt()
            .toolNames("toUppercase")
            .user("what's the uppercase of Hello")
            .call().content()
        assertNotNull(response)
    }

    @Test
    @DisplayName("Stream tool calling")
    fun testStreamToolCalling() {
        val builder = StringBuilder()
        chatClient.prompt().toolNames("toUppercase")
            .user("what's the uppercase of Hello")
            .stream().chatResponse().doOnNext {
                builder.append(it.result.output.text)
            }.blockLast()
        val result = builder.toString()
        assertTrue { result.isNotEmpty() }
    }

    class ToUppercaseRequest(var input: String? = null)

    class ToUppercaseResponse(var output: String? = null)

    class ToUppercase : Function<ToUppercaseRequest, ToUppercaseResponse> {
        override fun apply(t: ToUppercaseRequest): ToUppercaseResponse {
            return ToUppercaseResponse(t.input?.uppercase())
        }

    }

    private class CustomToolCallbackResolver :
        ToolCallbackResolver {
        override fun resolve(name: String): ToolCallback {

            return FunctionToolCallback.builder("toUppercase", ToUppercase())
                .description("Convert a string to its uppercase")
                .inputType(ToUppercaseRequest::class.java)
                .build()
        }

    }
}