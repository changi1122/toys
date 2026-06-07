from __future__ import annotations

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from kbo_crawler.models import TeamStats
from kbo_crawler.parser import extract_rows_text, parse_table_rows

KBO_URL = "https://www.koreabaseball.com/Record/Team/Hitter/Basic1.aspx"
TABLE_CSS = "table.tData"
WAIT_TIMEOUT = 15


def make_driver(headless: bool = True) -> webdriver.Chrome:
    """Chrome WebDriver를 생성한다. Selenium Manager가 chromedriver를 자동으로 관리한다."""
    options = Options()
    if headless:
        options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    return webdriver.Chrome(options=options)


def fetch_team_stats(driver: webdriver.Chrome | None = None) -> list[TeamStats]:
    """KBO 팀 타격 기록을 크롤링하여 반환한다.

    driver를 인자로 넘기면 해당 드라이버를 사용하고 종료하지 않는다.
    driver가 None이면 내부에서 생성하고 완료 후 종료한다.
    """
    owns_driver = driver is None
    if owns_driver:
        driver = make_driver()

    try:
        driver.get(KBO_URL)
        table = WebDriverWait(driver, WAIT_TIMEOUT).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, TABLE_CSS))
        )
        rows_text = extract_rows_text(table)
        return parse_table_rows(rows_text)
    finally:
        if owns_driver:
            driver.quit()


def debug_raw_rows() -> None:
    """크롤링한 raw 행 데이터를 출력한다. 컬럼 구조 확인용."""
    owns_driver = True
    driver = make_driver()
    try:
        driver.get(KBO_URL)
        table = WebDriverWait(driver, WAIT_TIMEOUT).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, TABLE_CSS))
        )
        rows_text = extract_rows_text(table)
        for i, row in enumerate(rows_text):
            print(f"[{i}] ({len(row)} cells) {row}")
    finally:
        driver.quit()


if __name__ == "__main__":
    import sys
    if "--debug" in sys.argv:
        debug_raw_rows()
    else:
        stats = fetch_team_stats()
        for team in stats:
            print(f"{team.rank}위 {team.team_name}: 타율 {team.batting_avg}, 홈런 {team.home_runs}, 타점 {team.rbi}")
