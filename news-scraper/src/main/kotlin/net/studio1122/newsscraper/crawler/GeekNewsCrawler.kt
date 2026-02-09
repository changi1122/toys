package net.studio1122.newsscraper.crawler

import net.studio1122.newsscraper.model.NewsItem
import org.jsoup.Jsoup

class GeekNewsCrawler(private val baseUrl: String) {

    fun crawl(): List<NewsItem> {
        val doc = Jsoup.connect(baseUrl)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 Edg/127.0.0.0")
            .timeout(10_000)
            .get()

        val items = mutableListOf<NewsItem>()

        // 각 뉴스 아이템은 div.topic_row
        val rows = doc.select("div.topic_row")
        for (row in rows) {
            // 제목: .topictitle h1 텍스트 + .topicurl 텍스트
            val h1 = row.selectFirst("div.topictitle h1") ?: continue
            val topicUrlSpan = row.selectFirst("span.topicurl")
            val title = h1.text().trim() +
                (if (topicUrlSpan != null) " ${topicUrlSpan.text().trim()}" else "")
            if (title.isBlank()) continue

            // 원문 링크: h1을 감싸는 a 태그의 href
            val titleLink = row.selectFirst("div.topictitle a") ?: continue
            val href = titleLink.attr("href")
            val link = if (href.startsWith("http")) href else "$baseUrl/$href"

            // GeekNews 상세 페이지: .topicdesc 안의 a 태그 href
            val descLink = row.selectFirst("div.topicdesc a")
            val topicUrl = if (descLink != null) {
                val topicHref = descLink.attr("href")
                if (topicHref.startsWith("http")) topicHref
                else "$baseUrl/$topicHref"
            } else {
                continue
            }

            items.add(NewsItem(title = title, link = link, topicUrl = topicUrl))
            if (items.size >= 10) break
        }

        return items
    }
}
