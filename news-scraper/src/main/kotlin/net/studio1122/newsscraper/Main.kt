package net.studio1122.newsscraper

import net.studio1122.newsscraper.config.AppConfig
import net.studio1122.newsscraper.crawler.GeekNewsCrawler
import net.studio1122.newsscraper.crawler.TopicDetailCrawler
import net.studio1122.newsscraper.sender.ResultSender
import net.studio1122.newsscraper.summarizer.AISummarizer

fun main() {
    println("=== GeekNews 크롤러 시작 ===")
    val startTime = System.currentTimeMillis()

    // 1. 설정 로드
    val config = AppConfig.load()
    println("[INFO] 설정 로드 완료")
    println("  - GeekNews URL: ${config.geekNewsUrl}")
    println("  - OpenRouter model: ${config.openrouterModel}")
    println("  - Target API: ${config.targetApiUrl}")
    println("  - Auth API: ${config.authApiUrl} (user: ${config.username})")

    // 2. 메인 페이지 크롤링
    println("\n[STEP 1] 메인 페이지 크롤링...")
    val crawler = GeekNewsCrawler(config.geekNewsUrl)
    val newsItems = crawler.crawl()
    println("[INFO] ${newsItems.size}건의 뉴스 항목 발견")

    if (newsItems.isEmpty()) {
        println("[WARN] 크롤링된 뉴스가 없습니다. 종료합니다.")
        return
    }

    // 3. 상세 페이지에서 본문 추출
    println("\n[STEP 2] 상세 페이지 본문 추출...")
    val detailCrawler = TopicDetailCrawler()
    var contentCount = 0
    val itemsWithContent = newsItems.map { item ->
        print("  - ${item.title.take(50)}... ")
        val result = detailCrawler.fetchContent(item)
        if (result.content.isNotBlank()) {
            contentCount++
            println("OK (${result.content.length}자)")
        } else {
            println("본문 없음")
        }
        result
    }
    println("[INFO] 본문 추출 완료: ${contentCount}/${newsItems.size}건")

    // 4. OpenRouter로 요약 생성
    println("\n[STEP 3] OpenRouter 요약 생성...")
    val summarizer = AISummarizer(config.openrouterApiKey, config.openrouterModel)
    var summaryCount = 0
    val itemsWithSummary = itemsWithContent.map { item ->
        print("  - ${item.title.take(50)}... ")
        val result = summarizer.summarize(item)
        if (!result.summary.startsWith("(")) {
            summaryCount++
            println("OK")
        } else {
            println(result.summary)
        }
        result
    }
    println("[INFO] 요약 완료: ${summaryCount}/${newsItems.size}건")

    // 5. 인증 및 글 작성
    println("\n[STEP 4] 인증 및 글 작성...")
    val sender = ResultSender(config.targetApiUrl, config.authApiUrl, config.username, config.password)

    val cookieHeader = sender.authenticate()
    val sendSuccess = if (cookieHeader != null) {
        sender.createPost(itemsWithSummary, cookieHeader)
    } else {
        println("[ERROR] 인증 실패로 글 작성을 건너뜁니다.")
        false
    }

    // 6. 결과 요약
    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
    println("\n=== 처리 완료 ===")
    println("  총 뉴스: ${newsItems.size}건")
    println("  본문 추출: ${contentCount}건")
    println("  요약 성공: ${summaryCount}건")
    println("  글 작성: ${if (sendSuccess) "성공" else "실패"}")
    println("  소요 시간: ${"%.1f".format(elapsed)}초")
}
