package com.javaaidev.openai

import com.openai.client.OpenAIClient
import com.openai.core.JsonObject
import com.openai.core.JsonValue
import com.openai.models.*
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.model.AbstractToolCallSupport
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.ModelOptionsUtils
import org.springframework.ai.model.function.FunctionCallbackResolver
import org.springframework.ai.model.function.FunctionCallingOptions
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import java.util.*

class OpenAIChatModel(
    private val openAIClient: OpenAIClient,
    functionCallbackResolver: FunctionCallbackResolver? = null
) : AbstractToolCallSupport(functionCallbackResolver), ChatModel {

    override fun call(prompt: Prompt): ChatResponse {
        return internalCall(prompt, null)
    }

    private fun internalCall(prompt: Prompt, previousChatResponse: ChatResponse?): ChatResponse {
        val paramsBuilder = ChatCompletionCreateParams.builder()

        prompt.instructions.forEach { message ->
            when (message) {
                is UserMessage -> {
                    val messageParamBuilder = ChatCompletionUserMessageParam.builder()
                    val contentParts = mutableListOf(
                        ChatCompletionContentPart.ofText(
                            ChatCompletionContentPartText.builder().text(message.text).build()
                        )
                    )
                    message.media?.map { media ->
                        when (media.mimeType) {
                            MimeTypeUtils.parseMimeType("audio/mp3") -> ChatCompletionContentPart.ofInputAudio(
                                ChatCompletionContentPartInputAudio.builder()
                                    .inputAudio(
                                        ChatCompletionContentPartInputAudio.InputAudio.builder()
                                            .data(fromAudioData(media.data))
                                            .format(ChatCompletionContentPartInputAudio.InputAudio.Format.MP3)
                                            .build()
                                    )
                                    .build()
                            )

                            MimeTypeUtils.parseMimeType("audio/wav") -> ChatCompletionContentPart.ofInputAudio(
                                ChatCompletionContentPartInputAudio.builder()
                                    .inputAudio(
                                        ChatCompletionContentPartInputAudio.InputAudio.builder()
                                            .data(fromAudioData(media.data))
                                            .format(ChatCompletionContentPartInputAudio.InputAudio.Format.WAV)
                                            .build()
                                    )
                                    .build()
                            )

                            else -> ChatCompletionContentPart.ofImageUrl(
                                ChatCompletionContentPartImage.builder()
                                    .imageUrl(
                                        ChatCompletionContentPartImage.ImageUrl.builder()
                                            .url(fromMediaData(media.mimeType, media.data))
                                            .build()
                                    )
                                    .build()
                            )
                        }
                    }?.let {
                        contentParts.addAll(it)
                    }
                    paramsBuilder.addMessage(
                        messageParamBuilder.contentOfArrayOfContentParts(
                            contentParts
                        ).build()
                    )
                }

                is SystemMessage -> paramsBuilder.addSystemMessage(message.text)
                is AssistantMessage -> {
                    val messageParamBuilder = ChatCompletionAssistantMessageParam.builder()
                    message.toolCalls?.map { toolCall ->
                        ChatCompletionMessageToolCall.builder()
                            .id(toolCall.id)
                            .function(
                                ChatCompletionMessageToolCall.Function.builder()
                                    .name(toolCall.name)
                                    .arguments(toolCall.arguments)
                                    .build()
                            )
                            .build()
                    }?.let {
                        messageParamBuilder.toolCalls(it)
                    }
                    paramsBuilder.addMessage(messageParamBuilder.build())
                }

                is ToolResponseMessage -> {
                    message.responses.forEach { toolResponse ->
                        paramsBuilder.addMessage(
                            ChatCompletionToolMessageParam.builder()
                                .toolCallId(toolResponse.id)
                                .content(toolResponse.responseData)
                                .build()
                        )
                    }
                }
            }
        }
        prompt.options?.model?.let {
            paramsBuilder.model(it)
        }
        prompt.options?.temperature?.let {
            paramsBuilder.temperature(it)
        }

        if (prompt.options is FunctionCallingOptions) {
            val tools = (prompt.options as FunctionCallingOptions).functions?.let {
                resolveFunctionCallbacks(it).map { functionCallback ->
                    val parametersMap =
                        ModelOptionsUtils.jsonToMap(functionCallback.inputTypeSchema)
                    val jsonValue = JsonValue.from(parametersMap)
                    ChatCompletionTool.builder()
                        .function(
                            FunctionDefinition.builder()
                                .name(functionCallback.name)
                                .description(functionCallback.description)
                                .parameters(
                                    FunctionParameters.builder()
                                        .putAllAdditionalProperties((jsonValue as JsonObject).values)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                }
            }
            if (tools?.isNotEmpty() == true) {
                paramsBuilder.tools(tools)
            }
        }

        val completion = openAIClient.chat().completions().create(paramsBuilder.build())
        val generations = completion.choices().map { choice ->
            buildGeneration(
                choice, mapOf(
                    "id" to completion.id(),
                    "index" to choice.index(),
                    "finishReason" to choice.finishReason().value().name
                )
            )
        }
        val response = ChatResponse.builder().generations(generations).build()
        if (isToolCall(response, setOf("TOOL_CALLS", "STOP"))) {
            val toolCallConversation = handleToolCalls(prompt, response)
            return this.internalCall(Prompt(toolCallConversation, prompt.options), response)
        }
        return response
    }

    private fun buildGeneration(
        choice: ChatCompletion.Choice,
        metadata: Map<String, Any>
    ): Generation {
        val toolCalls = choice.message().toolCalls().map { calls ->
            calls.map { toolCall ->
                AssistantMessage.ToolCall(
                    toolCall.id(),
                    "function",
                    toolCall.function().name(),
                    toolCall.function().arguments()
                )
            }
        }.orElse(listOf())
        val finishReason = choice.finishReason().value().name
        val metadataBuilder = ChatGenerationMetadata.builder().finishReason(finishReason)
        val assistantMessage =
            AssistantMessage(choice.message().content().orElse(""), metadata, toolCalls, listOf())
        return Generation(assistantMessage, metadataBuilder.build())
    }

    private fun fromAudioData(audioData: Any): String {
        return if (audioData is ByteArray) {
            Base64.getEncoder().encodeToString(audioData)
        } else throw IllegalArgumentException("Unsupported audio data type: " + audioData.javaClass.simpleName)
    }

    private fun fromMediaData(mimeType: MimeType, mediaContentData: Any): String {
        return when (mediaContentData) {
            is ByteArray -> {
                String.format(
                    "data:%s;base64,%s",
                    mimeType.toString(),
                    Base64.getEncoder().encodeToString(mediaContentData)
                )
            }

            is String -> {
                mediaContentData
            }

            else -> {
                throw IllegalArgumentException(
                    "Unsupported media data type: " + mediaContentData.javaClass.simpleName
                )
            }
        }
    }
}