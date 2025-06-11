package com.javaaidev.openai

import com.openai.models.chat.completions.ChatCompletionChunk
import com.openai.models.chat.completions.ChatCompletionChunk.Choice
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta.ToolCall
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta.ToolCall.Function
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.FinishReason

class OpenAiStreamFunctionCallingHelper {
    fun merge(previous: ChatCompletionChunk?, current: ChatCompletionChunk): ChatCompletionChunk {
        if (previous == null) {
            return current
        }
        val serviceTier = current.serviceTier().or { previous.serviceTier() }
        val previousChoice = previous.choices().firstOrNull()
        val currentChoice = current.choices().firstOrNull()
        val chunkChoices: List<Choice>
        if (currentChoice == null) {
            chunkChoices = listOf()
        } else {
            val choice = merge(previousChoice, currentChoice)
            chunkChoices = listOf(choice)
        }
        return ChatCompletionChunk.builder()
            .id(current.id())
            .choices(chunkChoices)
            .created(current.created())
            .model(current.model())
            .serviceTier(serviceTier)
            .build()
    }

    private fun merge(previous: Choice?, current: Choice): Choice {
        if (previous == null) {
            return current
        }
        val finishReason = current.finishReason()
        val index = current.index()
        val delta = merge(previous.delta(), current.delta())
        val logprobs = current.logprobs().or { previous.logprobs() }
        return Choice.builder().delta(delta).finishReason(finishReason).index(index).logprobs(logprobs).build()
    }

    private fun merge(previous: Delta, current: Delta): Delta {
        val content = current.content().or { previous.content() }.orElse("")
        val role = current.role().or { previous.role() }.orElse(Delta.Role.ASSISTANT)
        val refusal = current.refusal().or { previous.refusal() }.orElse("")
        val toolCalls = mutableListOf<ToolCall>()
        var lastPreviousToolCall: ToolCall? = null
        if (previous.toolCalls().isPresent) {
            val previousToolCalls = previous.toolCalls().get()
            lastPreviousToolCall = previousToolCalls.last()
            if (previousToolCalls.size > 1) {
                toolCalls.addAll(previousToolCalls.subList(0, previousToolCalls.size - 1))
            }
        }
        if (current.toolCalls().isPresent) {
            val currentToolCalls = current.toolCalls().get()
            if (currentToolCalls.size > 1) {
                throw IllegalStateException("Currently only one tool call is supported per message!")
            }
            val currentToolCall = currentToolCalls.first()
            if (currentToolCall.id().isPresent) {
                if (lastPreviousToolCall != null) {
                    toolCalls.add(lastPreviousToolCall)
                }
                toolCalls.add(currentToolCall)
            } else {
                toolCalls.add(merge(lastPreviousToolCall, currentToolCall))
            }
        } else {
            if (lastPreviousToolCall != null) {
                toolCalls.add(lastPreviousToolCall)
            }
        }
        return Delta.builder().content(content).role(role).refusal(refusal).toolCalls(toolCalls).build()
    }

    private fun merge(previous: ToolCall?, current: ToolCall): ToolCall {
        if (previous == null) {
            return current
        }
        val id = current.id().or { previous.id() }.orElse("")
        val type = current.type().or { previous.type() }.orElse(ToolCall.Type.FUNCTION)
        val function = merge(previous.function().orElse(null), current.function().get())
        return ToolCall.builder().id(id).type(type).index(current.index()).function(function).build()
    }

    private fun merge(previous: Function?, current: Function): Function {
        if (previous == null) {
            return current
        }
        val name = current.name().or { previous.name() }.orElse("")
        val arguments = StringBuilder()
        previous.arguments().ifPresent { arguments.append(it) }
        current.arguments().ifPresent { arguments.append(it) }
        return Function.builder()
            .name(name)
            .arguments(arguments.toString())
            .build()
    }

    fun isStreamingToolFunctionCall(chunk: ChatCompletionChunk?): Boolean {
        if (chunk == null || chunk.choices().isEmpty()) {
            return false
        }
        val choice = chunk.choices().firstOrNull() ?: return false
        return choice.delta().toolCalls().let {
            it.isPresent && it.get().isNotEmpty()
        }
    }

    fun isStreamingToolFunctionCallFinish(chunk: ChatCompletionChunk?): Boolean {
        if (chunk == null || chunk.choices().isEmpty()) {
            return false
        }
        val choice = chunk.choices().firstOrNull() ?: return false
        return choice.finishReason().let {
            it.isPresent && it.get() == FinishReason.TOOL_CALLS
        }
    }
}