package net.studio1122.newsscraper.model

data class NewsItem(
    val title: String,
    val link: String,
    val topicUrl: String,
    val content: String = "",
    val summary: String = ""
)
