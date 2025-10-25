package com.example.cac3.ai

import com.google.gson.annotations.SerializedName

/**
 * OpenAI API Request/Response Models
 */

data class ChatCompletionRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null
)

data class ChatMessage(
    val role: String, // "system", "user", or "assistant"
    val content: String
)

data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: ChatMessage,
    @SerializedName("finish_reason")
    val finishReason: String
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * Error response from OpenAI API
 */
data class OpenAIErrorResponse(
    val error: OpenAIError
)

data class OpenAIError(
    val message: String,
    val type: String,
    val param: String?,
    val code: String?
)
