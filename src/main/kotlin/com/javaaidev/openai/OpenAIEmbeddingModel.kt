package com.javaaidev.openai

import com.openai.client.OpenAIClient
import com.openai.models.EmbeddingCreateParams
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.AbstractEmbeddingModel
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse

class OpenAIEmbeddingModel(private val openAIClient: OpenAIClient) : AbstractEmbeddingModel() {
    override fun call(request: EmbeddingRequest): EmbeddingResponse {
        val paramsBuilder = EmbeddingCreateParams.builder()
            .inputOfArrayOfStrings(request.instructions)
        request.options.model?.let {
            paramsBuilder.model(it)
        }
        request.options.dimensions?.let {
            paramsBuilder.dimensions(it.toLong())
        }
        val response = openAIClient.embeddings().create(paramsBuilder.build())
        val embeddings = response.data().map { e ->
            Embedding(e.embedding().map { v -> v.toFloat() }.toFloatArray(), e.index().toInt())
        }
        return EmbeddingResponse(embeddings)
    }

    override fun embed(document: Document): FloatArray {
        return embed(document.formattedContent)
    }

}