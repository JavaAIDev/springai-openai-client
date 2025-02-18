package com.javaaidev.openai

import com.openai.client.okhttp.OpenAIOkHttpClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest

class OpenAIEmbeddingModelTest {
    private val embeddingModel: EmbeddingModel

    init {
        val client = OpenAIOkHttpClient.fromEnv()
        embeddingModel = OpenAIEmbeddingModel(client)
    }

    @Test
    @DisplayName("simple embedding")
    fun testEmbedding() {
        val response = embeddingModel.call(
            EmbeddingRequest(
                listOf("hello", "world"),
                OpenAIEmbeddingOptions.builder()
                    .model("text-embedding-3-small")
                    .build()
            )
        )
        assertNotNull(response)
        assertTrue(response.results.isNotEmpty())
    }
}