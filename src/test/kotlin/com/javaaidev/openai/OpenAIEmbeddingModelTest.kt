package com.javaaidev.openai

import com.openai.client.okhttp.OpenAIOkHttpClient
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest

class OpenAIEmbeddingModelTest {
    private val embeddingModel: EmbeddingModel

    init {
        val client = OpenAIOkHttpClient.fromEnv()
        embeddingModel = OpenAIEmbeddingModel(
            client,
            OpenAIEmbeddingOptions.builder().model("text-embedding-3-small").build()
        )
    }

    @Test
    @DisplayName("simple embedding")
    fun testEmbedding() {
        val response = embeddingModel.embed(
            listOf("hello", "world")
        )
        assertNotNull(response)
        assertTrue(response.isNotEmpty())
    }

    @Test
    @DisplayName("embedding with options")
    fun testEmbeddingWithOptions() {
        val model = "text-embedding-3-large"
        val response = embeddingModel.call(
            EmbeddingRequest(
                listOf("hello", "world"),
                OpenAIEmbeddingOptions.builder()
                    .model(model)
                    .build()
            )
        )
        assertNotNull(response)
        assertEquals(model, response.metadata.model)
        assertTrue(response.results.isNotEmpty())
    }
}