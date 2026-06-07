# KBO 팀 타격 기록 크롤러

KBO 공식 사이트에서 팀 타격 기록을 크롤링하는 Python 프로젝트.

- **크롤링**: Selenium (Chrome WebDriver)
- **의존성 관리**: uv
- **테스트**: pytest + unittest.mock

---

## 요구사항

- Python 3.11+
- [uv](https://docs.astral.sh/uv/) 설치
- Google Chrome 설치 (chromedriver는 Selenium Manager가 자동 관리)

---

## 명령어

### 의존성 설치

```bash
# 런타임 + 개발 의존성 전체 설치
uv sync

# 런타임 의존성만 설치 (개발 의존성 제외)
uv sync --no-dev
```

### 테스트

```bash
# 전체 테스트 실행
uv run pytest

# 상세 출력
uv run pytest -v

# 특정 테스트 파일만 실행
uv run pytest tests/test_models.py
uv run pytest tests/test_parser.py
uv run pytest tests/test_crawler.py
```

### 크롤링 실행

```bash
# KBO 팀 타격 기록 크롤링 (headless Chrome)
uv run python -m kbo_crawler.crawler
```

### 패키징

```bash
# 배포 패키지 빌드 (wheel + sdist) → dist/ 에 생성됨
uv build

# 빌드 결과물 확인
ls dist/
# dist/kbo_crawler-0.1.0-py3-none-any.whl
# dist/kbo_crawler-0.1.0.tar.gz
```

#### 빌드된 패키지 설치 및 실행

```bash
# 다른 환경(또는 현재 환경)에 wheel 직접 설치
uv pip install dist/kbo_crawler-0.1.0-py3-none-any.whl

# 설치 후 실행
python -m kbo_crawler.crawler

# 또는 소스 배포본(tar.gz)으로 설치
pip install dist/kbo_crawler-0.1.0.tar.gz
```

#### 코드 수정 후 로컬 재설치

```bash
# 1. pyproject.toml에서 버전 올리기
#    version = "0.1.1"

# 2. dist/ 정리 (선택 사항, 이전 버전 파일 누적 방지)
rm -rf dist/

# 3. 재빌드
uv build

# 4. 강제 재설치 (기존 버전 덮어쓰기)
uv pip install --force-reinstall dist/kbo_crawler-0.1.1-py3-none-any.whl
```

#### PyPI 배포 후 설치 및 실행

```bash
# PyPI에 배포
uv publish

# 테스트 PyPI에 배포
uv publish --publish-url https://test.pypi.org/legacy/

# PyPI에서 설치
pip install kbo-crawler

# 실행
python -m kbo_crawler.crawler
```

### 의존성 관리

```bash
# 런타임 의존성 추가
uv add <패키지명>

# 개발 의존성 추가
uv add --dev <패키지명>

# 의존성 제거
uv remove <패키지명>

# 설치된 패키지 목록 확인
uv pip list
```

---

## 프로젝트 구조

```
python-crawling/
├── pyproject.toml
├── kbo_crawler/
│   ├── models.py       # TeamStats dataclass
│   ├── parser.py       # HTML 테이블 파싱 로직
│   └── crawler.py      # Selenium WebDriver 크롤링
└── tests/
    ├── test_models.py
    ├── test_parser.py
    └── test_crawler.py
```

---

## 크롤링 대상

**URL**: https://www.koreabaseball.com/Record/Team/Hitter/Basic1.aspx

수집 항목: 팀명, 경기, 타수, 득점, 안타, 2루타, 3루타, 홈런, 타점, 도루, 볼넷, 삼진, 타율, 출루율, 장타율, OPS
