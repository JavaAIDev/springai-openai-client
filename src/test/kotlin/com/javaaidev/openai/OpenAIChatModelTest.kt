package com.javaaidev.openai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.openai.client.okhttp.OpenAIOkHttpClient
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.model.function.FunctionCallback
import org.springframework.ai.model.function.FunctionCallbackResolver
import java.util.function.Function
import kotlin.test.assertNotNull

class OpenAIChatModelTest {
    private val chatClient: ChatClient
    private val objectMapper = jacksonObjectMapper()

    init {
        val client = OpenAIOkHttpClient.fromEnv()
        val chatModel = OpenAIChatModel(client, CustomFunctionCallbackResolver(objectMapper))
        val chatOptions = OpenAIChatOptions.builder()
            .model("gpt-3.5-turbo")
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
    @DisplayName("Function calling")
    fun testFunctionCalling() {
        val response = chatClient.prompt()
            .functions("toUppercase")
            .user("what's the uppercase of Hello")
            .call().content()
        assertNotNull(response)
    }

    class ToUppercaseRequest(var input: String? = null)

    class ToUppercaseResponse(var output: String? = null)

    class ToUppercase : Function<ToUppercaseRequest, ToUppercaseResponse> {
        override fun apply(t: ToUppercaseRequest): ToUppercaseResponse {
            return ToUppercaseResponse(t.input?.uppercase())
        }

    }

    private class CustomFunctionCallbackResolver(private val objectMapper: ObjectMapper) :
        FunctionCallbackResolver {
        override fun resolve(name: String): FunctionCallback {

            return FunctionCallback.builder()
                .function("toUppercase", ToUppercase())
                .description("Convert a string to its uppercase")
                .inputType(ToUppercaseRequest::class.java)
                .objectMapper(objectMapper)
                .schemaType(FunctionCallback.SchemaType.JSON_SCHEMA)
                .build()
        }

    }
}