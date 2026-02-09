package net.studio1122.newsscraper.sender

import com.google.gson.Gson
import net.studio1122.newsscraper.model.NewsItem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ResultSender(
    private val targetApiUrl: String,
    private val authApiUrl: String,
    private val username: String,
    private val password: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 인증 API에 로그인하여 액세스 토큰 쿠키를 반환한다.
     * 실패 시 null을 반환한다.
     */
    fun authenticate(): String? {
        val body = gson.toJson(mapOf("username" to username, "password" to password))

        val request = Request.Builder()
            .url(authApiUrl)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("[ERROR] 인증 실패 (HTTP ${response.code}): ${response.body?.string()}")
                    return null
                }

                // Set-Cookie 헤더에서 액세스 토큰 쿠키 추출
                val cookies = response.headers("Set-Cookie")
                val cookieHeader = cookies.joinToString("; ") { it.substringBefore(";") }

                if (cookieHeader.isBlank()) {
                    println("[ERROR] 인증 응답에 쿠키가 없습니다")
                    return null
                }

                println("[INFO] 인증 성공 (user: $username)")
                cookieHeader
            }
        } catch (e: Exception) {
            println("[ERROR] 인증 오류: ${e.message}")
            null
        }
    }

    /**
     * 크롤링 결과를 커뮤니티 글 작성 API로 전송한다.
     * POST /api/community?type=FREE
     */
    fun createPost(items: List<NewsItem>, cookieHeader: String): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val title = "$today GeekNews 요약"
        val htmlBody = buildHtmlBody(items)

        val payload = mapOf(
            "title" to title,
            "body" to htmlBody,
            "editorVersion" to 1
        )

        val body = gson.toJson(payload)
        val url = "$targetApiUrl/api/community?type=FREE"

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(jsonMediaType))
            .header("Cookie", cookieHeader)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    println("[INFO] 글 작성 성공 (HTTP ${response.code})")
                    true
                } else {
                    println("[ERROR] 글 작성 실패 (HTTP ${response.code}): ${response.body?.string()}")
                    false
                }
            }
        } catch (e: Exception) {
            println("[ERROR] 글 작성 오류: ${e.message}")
            false
        }
    }

    private fun buildHtmlBody(items: List<NewsItem>): String {
        val sb = StringBuilder()
        sb.append("<h2>GeekNews 오늘의 뉴스 요약</h2>")
        sb.append("<p>총 <strong>${items.size}</strong>건의 뉴스를 요약했습니다.</p>")
        sb.append("<br>")
        sb.append("<hr>")

        for ((index, item) in items.withIndex()) {
            sb.append("<div style=\"margin-bottom:24px;\">")
            sb.append("<h3 style=\"margin-bottom:4px;\">${index + 1}. <a href=\"${escapeHtml(item.link)}\" target=\"_blank\">${escapeHtml(item.title)}</a></h3>")

            if (item.summary.isNotBlank() && !item.summary.startsWith("(")) {
                sb.append("<p style=\"color:#666;font-size:15px;\">${escapeHtml(item.summary)}</p>")
            }

            sb.append("<ul style=\"margin-top:4px;\">")
            sb.append("<li><a href=\"${escapeHtml(item.topicUrl)}\" target=\"_blank\">GeekNews 토론</a></li>")
            sb.append("</ul>")
            sb.append("</div>")
        }

        sb.append("<hr>")
        sb.append("<p style=\"color:#999;font-size:12px;\">본 게시글은 배치 프로그램 작성 및 로컬 LLM 활용 학습을 위해 크롤링하였습니다. 매일 최대 1회까지만 크롤링을 실행합니다.</p>")
        return sb.toString()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
