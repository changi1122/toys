from __future__ import annotations

from kbo_crawler.models import TeamStats


def parse_table_rows(rows_text: list[list[str]]) -> list[TeamStats]:
    """헤더 및 빈 행을 제외하고, 각 행을 TeamStats로 파싱하여 반환한다."""
    result = []
    for row in rows_text:
        if not row or row[0] in ("순위", "합계", ""):
            continue
        try:
            result.append(TeamStats.from_row(row))
        except (ValueError, IndexError) as exc:
            raise ValueError(f"행 파싱 실패 {row!r}: {exc}") from exc
    return result


def extract_rows_text(table_element) -> list[list[str]]:
    """Selenium WebElement(<table>)로부터 각 행의 셀 텍스트 리스트를 추출한다."""
    rows = table_element.find_elements("tag name", "tr")
    result = []
    for row in rows:
        cells = row.find_elements("tag name", "td")
        if not cells:
            cells = row.find_elements("tag name", "th")
        result.append([cell.text.strip() for cell in cells])
    return result
