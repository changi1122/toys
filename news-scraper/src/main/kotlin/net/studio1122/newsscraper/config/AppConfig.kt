package net.studio1122.newsscraper.config

import java.util.Properties

data class AppConfig(
    val geekNewsUrl: String,
    val openrouterModel: String,
    val openrouterApiKey: String,
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
                openrouterModel = env("OPENROUTER_MODEL") ?: props.getProperty("openrouter.model", "qwen/qwen3-vl-30b-a3b-thinking"),
                openrouterApiKey = env("OPENROUTER_API_KEY") ?: props.getProperty("openrouter.api.key", ""),
                targetApiUrl = env("TARGET_API_URL") ?: props.getProperty("target.api.url", "http://localhost:8080/api/news"),
                authApiUrl = env("AUTH_API_URL") ?: props.getProperty("auth.api.url", "https://tux.studio1122.net/api/auth"),
                username = env("NEWS_USERNAME") ?: props.getProperty("auth.username", "newsbot"),
                password = env("NEWS_PASSWORD") ?: props.getProperty("auth.password", "")
            )
        }

        private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
    }
}
