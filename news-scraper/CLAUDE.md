# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GeekNews(https://news.hada.io) 첫 페이지의 뉴스 상위 10건을 크롤링하고, 각 글의 상세 페이지에서 본문을 추출한 뒤, Ollama 로컬 모델로 요약하여 커뮤니티 REST API에 글을 작성하는 Kotlin 배치 프로그램.

## Build & Test Commands

- **Build:** `./gradlew build` (use `gradlew.bat build` on Windows)
- **Run:** `./gradlew run`
- **Run tests:** `./gradlew test`
- **Run a single test class:** `./gradlew test --tests "fully.qualified.TestClassName"`
- **Run a single test method:** `./gradlew test --tests "fully.qualified.TestClassName.methodName"`
- **Clean build:** `./gradlew clean build`
- **배포용 빌드:** `./gradlew installDist` → `build/install/news-scraper/bin/news-scraper.bat`

## Tech Stack

- **Language:** Kotlin (JVM toolchain 17, code style: official)
- **Build:** Gradle 8.14 with Kotlin plugin 2.2.21, application plugin
- **Dependencies:** Jsoup 1.18.3 (HTML 파싱), OkHttp 4.12.0 (HTTP 클라이언트), Gson 2.11.0 (JSON)
- **Testing:** JUnit Platform (via `kotlin-test`)
- **Group ID:** `net.studio1122`

## Project Structure

```
src/main/kotlin/net/studio1122/newsscraper/
├── Main.kt                      # 진입점, 전체 흐름 오케스트레이션
├── config/AppConfig.kt          # 설정값 로드 (properties/환경변수)
├── crawler/GeekNewsCrawler.kt   # 메인 페이지 크롤링 (상위 10건)
├── crawler/TopicDetailCrawler.kt # 상세 페이지 .topic_contents 본문 추출
├── model/NewsItem.kt            # 데이터 클래스
├── summarizer/OllamaSummarizer.kt # Ollama API로 본문 요약
└── sender/ResultSender.kt       # 인증 + 커뮤니티 글 작성
src/main/resources/application.properties  # 설정 파일
```

## 설정 (환경변수 / application.properties)

| 환경변수 | properties key | 기본값 | 설명 |
|---|---|---|---|
| `GEEKNEWS_URL` | `geeknews.url` | `https://news.hada.io` | GeekNews URL |
| `OLLAMA_URL` | `ollama.url` | `http://localhost:11434` | Ollama 서버 URL |
| `OLLAMA_MODEL` | `ollama.model` | `gpt-oss:20b` | Ollama 모델명 |
| `TARGET_API_URL` | `target.api.url` | `https://tux.studio1122.net` | 대상 API 베이스 URL |
| `AUTH_API_URL` | `auth.api.url` | `https://tux.studio1122.net/api/auth` | 인증 API URL |
| `NEWS_USERNAME` | `auth.username` | `newsbot` | 인증 사용자명 |
| `NEWS_PASSWORD` | `auth.password` | (없음) | 인증 비밀번호 |

## 실행 흐름

1. AppConfig 로드
2. GeekNewsCrawler로 메인 페이지 크롤링 (상위 10건)
3. TopicDetailCrawler로 각 뉴스 상세 페이지 `.topic_contents` 본문 추출
4. OllamaSummarizer로 Ollama API 호출하여 한국어 2-3문장 요약
5. ResultSender로 인증(POST /api/auth → 쿠키 획득) 후 글 작성(POST /api/community?type=FREE)

## GeekNews HTML 구조

- 메인 페이지: `div.topic_row` 안에 `div.topictitle h1`(제목), `div.topictitle a`(원문 링크), `div.topicdesc a`(상세 페이지 링크)
- 상세 페이지: `.topic_contents`가 요약 대상 본문
