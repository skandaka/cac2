package com.example.cac3.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for OpenAI API
 */
interface OpenAIService {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
