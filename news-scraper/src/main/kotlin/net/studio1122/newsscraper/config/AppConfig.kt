package net.studio1122.newsscraper.config

import java.util.Properties

data class AppConfig(
    val geekNewsUrl: String,
    val ollamaUrl: String,
    val ollamaModel: String,
    val targetApiUrl: String,
    val authApiUrl: String,
    val username: String,
    val password: String
) {
    companion object {
        fun load(): AppConfig {
            val props = Properties()
            val stream = AppConfig::class.java.classLoader.getResourceAsStream("application.properties")
            if (stream != null) {
                props.load(stream)
            }

            return AppConfig(
                geekNewsUrl = env("GEEKNEWS_URL") ?: props.getProperty("geeknews.url", "https://news.hada.io"),
                ollamaUrl = env("OLLAMA_URL") ?: props.getProperty("ollama.url", "http://localhost:11434"),
                ollamaModel = env("OLLAMA_MODEL") ?: props.getProperty("ollama.model", "gpt-oss:20b"),
                targetApiUrl = env("TARGET_API_URL") ?: props.getProperty("target.api.url", "http://localhost:8080/api/news"),
                authApiUrl = env("AUTH_API_URL") ?: props.getProperty("auth.api.url", "https://tux.studio1122.net/api/auth"),
                username = env("NEWS_USERNAME") ?: props.getProperty("auth.username", "newsbot"),
                password = env("NEWS_PASSWORD") ?: props.getProperty("auth.password", "")
            )
        }

        private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
    }
}
