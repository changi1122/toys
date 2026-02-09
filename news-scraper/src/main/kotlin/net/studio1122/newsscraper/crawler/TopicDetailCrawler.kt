package net.studio1122.newsscraper.crawler

import net.studio1122.newsscraper.model.NewsItem
import org.jsoup.Jsoup

class TopicDetailCrawler {

    fun fetchContent(item: NewsItem): NewsItem {
        return try {
            val doc = Jsoup.connect(item.topicUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 Edg/127.0.0.0")
                .timeout(10_000)
                .get()

            val topicContents = doc.selectFirst(".topic_contents")
            if (topicContents != null) {
                val text = topicContents.text().trim()
                if (text.isNotBlank()) {
                    return item.copy(content = text)
                }
            }

            item
        } catch (e: Exception) {
            println("  [WARN] 본문 추출 실패: ${item.topicUrl} - ${e.message}")
            item
        }
    }
}
