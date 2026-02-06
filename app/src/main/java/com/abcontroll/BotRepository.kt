package com.abcontroll

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BotRepository(
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun getUpdates(token: String, offset: Long?): List<TelegramUpdate> = withContext(Dispatchers.IO) {
        val baseUrl = "https://api.telegram.org/bot$token/getUpdates".toHttpUrl()
        val urlBuilder = baseUrl.newBuilder()
            .addQueryParameter("timeout", "0")

        if (offset != null) {
            urlBuilder.addQueryParameter("offset", offset.toString())
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext emptyList<TelegramUpdate>()
            }
            val parsed = json.decodeFromString<TelegramResponse<TelegramUpdate>>(body)
            parsed.result
        }
    }

    suspend fun sendMessage(
        token: String,
        chatId: Long,
        text: String,
        replyToMessageId: Long?
    ): Boolean = withContext(Dispatchers.IO) {
        val payload = buildString {
            append("{\"chat_id\":")
            append(chatId)
            append(",\"text\":")
            append(json.encodeToString(text))
            if (replyToMessageId != null) {
                append(",\"reply_to_message_id\":")
                append(replyToMessageId)
            }
            append("}")
        }

        val requestBody = payload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$token/sendMessage")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }
}
