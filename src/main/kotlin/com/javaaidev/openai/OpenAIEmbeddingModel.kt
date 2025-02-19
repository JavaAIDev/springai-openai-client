package com.javaaidev.openai

import com.openai.client.OpenAIClient
import com.openai.models.EmbeddingCreateParams
import org.springframework.ai.chat.metadata.EmptyUsage
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.*
import org.springframework.ai.model.ModelOptionsUtils

class OpenAIEmbeddingModel(
    private val openAIClient: OpenAIClient,
    private val defaultOptions: OpenAIEmbeddingOptions? = null,
) :
    AbstractEmbeddingModel() {
    override fun call(request: EmbeddingRequest): EmbeddingResponse {
        val paramsBuilder = EmbeddingCreateParams.builder()
            .inputOfArrayOfStrings(request.instructions)

        val options = mergeOptions(request.options)

        options.model?.let {
            paramsBuilder.model(it)
        }
        options.dimensions?.let {
            paramsBuilder.dimensions(it.toLong())
        }
        options.encodingFormat?.let {
            paramsBuilder.encodingFormat(EmbeddingCreateParams.EncodingFormat.of(it))
        }
        options.user?.let {
            paramsBuilder.user(it)
        }

        val response = openAIClient.embeddings().create(paramsBuilder.build())
        val embeddings = response.data().map { e ->
            Embedding(e.embedding().map { v -> v.toFloat() }.toFloatArray(), e.index().toInt())
        }
        return EmbeddingResponse(embeddings, EmbeddingResponseMetadata(response.model(), EmptyUsage()))
    }

    private fun mergeOptions(runtimeOptions: EmbeddingOptions?): OpenAIEmbeddingOptions {
        val defaultOptions = this.defaultOptions ?: OpenAIEmbeddingOptions.builder().build()
        return ModelOptionsUtils.copyToTarget(
            runtimeOptions, EmbeddingOptions::class.java,
            OpenAIEmbeddingOptions::class.java
        )?.let { options ->
            OpenAIEmbeddingOptions.builder()
                .model(ModelOptionsUtils.mergeOption(options.model, defaultOptions.model))
                .dimensions(
                    ModelOptionsUtils.mergeOption(
                        options.dimensions,
                        defaultOptions.dimensions
                    )
                )
                .encodingFormat(
                    ModelOptionsUtils.mergeOption(
                        options.encodingFormat,
                        defaultOptions.encodingFormat
                    )
                )
                .user(ModelOptionsUtils.mergeOption(options.user, defaultOptions.user))
                .build()
        } ?: defaultOptions
    }

    override fun embed(document: Document): FloatArray {
        return embed(document.formattedContent)
    }

}