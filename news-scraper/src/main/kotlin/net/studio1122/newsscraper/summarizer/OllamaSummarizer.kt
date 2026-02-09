package net.studio1122.newsscraper.summarizer

import com.google.gson.Gson
import com.google.gson.JsonParser
import net.studio1122.newsscraper.model.NewsItem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OllamaSummarizer(
    private val ollamaUrl: String,
    private val model: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun summarize(item: NewsItem): NewsItem {
        if (item.content.isBlank()) {
            return item.copy(summary = "(본문 없음)")
        }

        return try {
            val prompt = "다음 글을 한국어로 2-3문장으로 요약해줘:\n\n${item.content}"
            val body = gson.toJson(
                mapOf(
                    "model" to model,
                    "prompt" to prompt,
                    "stream" to false
                )
            )

            val request = Request.Builder()
                .url("$ollamaUrl/api/generate")
                .post(body.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("  [WARN] Ollama 요약 실패 (HTTP ${response.code}): ${item.title}")
                    return item.copy(summary = "(요약 실패: HTTP ${response.code})")
                }

                val responseBody = response.body?.string() ?: ""
                val json = JsonParser.parseString(responseBody).asJsonObject
                val summary = json.get("response")?.asString?.trim() ?: "(응답 파싱 실패)"
                item.copy(summary = summary)
            }
        } catch (e: Exception) {
            println("  [WARN] Ollama 요약 오류: ${item.title} - ${e.message}")
            item.copy(summary = "(요약 오류: ${e.message})")
        }
    }
}
