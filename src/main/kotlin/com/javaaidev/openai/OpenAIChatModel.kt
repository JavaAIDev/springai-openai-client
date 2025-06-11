package com.javaaidev.openai

import com.openai.client.OpenAIClient
import com.openai.core.JsonObject
import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.chat.completions.*
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.ModelOptionsUtils
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.model.tool.ToolExecutionResult
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class OpenAIChatModel(
    private val openAIClient: OpenAIClient,
    manager: ToolCallingManager? = null,
    options: OpenAiChatOptions? = null,
) : ChatModel {
    private val defaultOptions = options ?: OpenAiChatOptions.builder().build()
    private val toolCallingManager = manager ?: ToolCallingManager.builder().build()
    private val toolExecutionEligibilityPredicate = DefaultToolExecutionEligibilityPredicate()
    private val chunkMerger = OpenAiStreamFunctionCallingHelper()

    override fun call(prompt: Prompt): ChatResponse {
        val requestPrompt = buildRequestPrompt(prompt)
        return internalCall(requestPrompt, null)
    }

    private fun internalCall(prompt: Prompt, previousChatResponse: ChatResponse?): ChatResponse {
        val completion = openAIClient.chat().completions().create(buildChatCompletionCreateParams(prompt))
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
        if (toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.options, response)) {
            val toolExecutionResult = toolCallingManager.executeToolCalls(prompt, response)
            if (toolExecutionResult.returnDirect()) {
                return ChatResponse.builder()
                    .from(response)
                    .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                    .build()
            } else {
                return this.internalCall(
                    Prompt(toolExecutionResult.conversationHistory(), prompt.options),
                    response
                )
            }
        }
        return response
    }

    override fun stream(prompt: Prompt): Flux<ChatResponse> {
        val requestPrompt = buildRequestPrompt(prompt)
        return internalStream(requestPrompt, null)
    }

    private fun internalStream(prompt: Prompt, previousChatResponse: ChatResponse?): Flux<ChatResponse> {
        val isInsideTool = AtomicBoolean(false)
        return Flux.fromStream(openAIClient.chat().completions().createStreaming(buildChatCompletionCreateParams(prompt)).stream())
            .map { chunk ->
                if (chunkMerger.isStreamingToolFunctionCall(chunk)) {
                    isInsideTool.set(true)
                }
                chunk
            }
            .windowUntil { chunk ->
                if (isInsideTool.get() && chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
                    isInsideTool.set(false)
                    true
                } else {
                    !isInsideTool.get()
                }
            }
            .concatMapIterable { window ->
                val monoChunk = window.reduce(
                    ChatCompletionChunk.builder().id("").choices(listOf()).created(0).model("").build()
                ) { previous, current ->
                    chunkMerger.merge(previous, current)
                }
                listOf(monoChunk)
            }
            .flatMap { it }
            .map { chunk ->
                val generations = chunk.choices().map { choice ->
                    buildGeneration(
                        choice, mapOf(
                            "id" to chunk.id(),
                            "index" to choice.index(),
                            "finishReason" to choice.finishReason().map { reason -> reason.value().name }.orElse("")
                        )
                    )
                }.toList()
                ChatResponse.builder().generations(generations).build()
            }.flatMap { response ->
                if (toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.options, response)) {
                    Flux.defer {
                        val toolExecutionResult = toolCallingManager.executeToolCalls(prompt, response)
                        if (toolExecutionResult.returnDirect()) {
                            Flux.just(
                                ChatResponse.builder()
                                    .from(response)
                                    .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                                    .build()
                            )
                        } else {
                            this.internalStream(
                                Prompt(toolExecutionResult.conversationHistory(), prompt.options),
                                response
                            )
                        }
                    }.subscribeOn(Schedulers.boundedElastic())
                } else {
                    Flux.just(response)
                }
            }
    }

    private fun buildRequestPrompt(prompt: Prompt): Prompt {
        var runtimeOptions: OpenAiChatOptions? = null
        if (prompt.options != null) {
            runtimeOptions = if (prompt.options is ToolCallingChatOptions) {
                ModelOptionsUtils.copyToTarget(
                    prompt.options as ToolCallingChatOptions,
                    ToolCallingChatOptions::class.java,
                    OpenAiChatOptions::class.java
                )
            } else {
                ModelOptionsUtils.copyToTarget(
                    prompt.options, ChatOptions::class.java,
                    OpenAiChatOptions::class.java
                )
            }
        }

        val requestOptions = ModelOptionsUtils.merge(
            runtimeOptions, this.defaultOptions,
            OpenAiChatOptions::class.java
        )

        if (runtimeOptions != null) {
            requestOptions.httpHeaders = mergeHttpHeaders(runtimeOptions.httpHeaders, this.defaultOptions.httpHeaders)
            requestOptions.internalToolExecutionEnabled = ModelOptionsUtils.mergeOption<Boolean>(
                runtimeOptions.internalToolExecutionEnabled,
                this.defaultOptions.internalToolExecutionEnabled
            )
            requestOptions.toolNames = ToolCallingChatOptions.mergeToolNames(
                runtimeOptions.toolNames,
                this.defaultOptions.toolNames
            )
            requestOptions.toolCallbacks = ToolCallingChatOptions.mergeToolCallbacks(
                runtimeOptions.toolCallbacks,
                this.defaultOptions.toolCallbacks
            )
            requestOptions.toolContext = ToolCallingChatOptions.mergeToolContext(
                runtimeOptions.toolContext,
                this.defaultOptions.toolContext
            )
        } else {
            requestOptions.httpHeaders = this.defaultOptions.httpHeaders
            requestOptions.internalToolExecutionEnabled = this.defaultOptions.internalToolExecutionEnabled
            requestOptions.toolNames = this.defaultOptions.toolNames
            requestOptions.toolCallbacks = this.defaultOptions.toolCallbacks
            requestOptions.toolContext = this.defaultOptions.toolContext
        }
        return prompt.mutate().chatOptions(requestOptions).build()
    }

    private fun buildChatCompletionCreateParams(prompt: Prompt): ChatCompletionCreateParams {
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
                    message.media.map { media ->
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
                    }.let {
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
                    val contentParts = mutableListOf(
                        ChatCompletionAssistantMessageParam.Content.ChatCompletionRequestAssistantMessageContentPart.ofText(
                            ChatCompletionContentPartText.builder().text(message.text).build()
                        )
                    )
                    message.toolCalls.map { toolCall ->
                        ChatCompletionMessageToolCall.builder()
                            .id(toolCall.id)
                            .function(
                                ChatCompletionMessageToolCall.Function.builder()
                                    .name(toolCall.name)
                                    .arguments(toolCall.arguments)
                                    .build()
                            )
                            .build()
                    }.let {
                        if (it.isNotEmpty()) {
                            messageParamBuilder.toolCalls(it)
                        }
                    }
                    messageParamBuilder.contentOfArrayOfContentParts(contentParts)
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

        if (prompt.options is ToolCallingChatOptions) {
            val tools = (prompt.options as ToolCallingChatOptions).let {
                toolCallingManager.resolveToolDefinitions(it).map { toolDefinition ->
                    val parametersMap =
                        ModelOptionsUtils.jsonToMap(toolDefinition.inputSchema())
                    val jsonValue = JsonValue.from(parametersMap)
                    ChatCompletionTool.builder()
                        .function(
                            FunctionDefinition.builder()
                                .name(toolDefinition.name())
                                .description(toolDefinition.description())
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
            if (tools.isNotEmpty()) {
                paramsBuilder.tools(tools)
            }
        }
        return paramsBuilder.build()
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

    private fun buildGeneration(
        choice: ChatCompletionChunk.Choice,
        metadata: Map<String, Any>
    ): Generation {
        val toolCalls = choice.delta().toolCalls().map { calls ->
            calls.filter { it.id().isPresent }
                .map { toolCall ->
                    AssistantMessage.ToolCall(
                        toolCall.id().orElse(""),
                        "function",
                        toolCall.function().flatMap { it.name() }.orElse(""),
                        toolCall.function().flatMap { it.arguments() }.orElse("")
                    )
                }
        }.orElse(listOf())
        val finishReason = choice.finishReason().map { it.value().name }.orElse("")
        val metadataBuilder = ChatGenerationMetadata.builder().finishReason(finishReason)
        val assistantMessage =
            AssistantMessage(choice.delta().content().orElse(""), metadata, toolCalls, listOf())
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

    private fun mergeHttpHeaders(
        runtimeHttpHeaders: Map<String, String>,
        defaultHttpHeaders: Map<String, String>
    ): Map<String, String> {
        val mergedHttpHeaders = HashMap(defaultHttpHeaders)
        mergedHttpHeaders.putAll(runtimeHttpHeaders)
        return mergedHttpHeaders
    }
}