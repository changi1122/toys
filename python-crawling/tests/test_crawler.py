from unittest.mock import MagicMock, patch

import pytest

from kbo_crawler.crawler import KBO_URL, fetch_team_stats

HEADER = ["순위", "팀명", "AVG", "G", "PA", "AB", "R", "H", "2B", "3B", "HR", "TB", "RBI", "SAC", "SF"]
DATA_ROW = ["1", "KT", "0.284", "59", "2411", "2069", "335", "588", "94", "12", "42", "832", "317", "34", "14"]


def _make_mock_cell(text: str) -> MagicMock:
    cell = MagicMock()
    cell.text = text
    return cell


def _make_mock_row(cell_texts: list[str]) -> MagicMock:
    row = MagicMock()
    cells = [_make_mock_cell(t) for t in cell_texts]

    def find_elements_side_effect(by, value):
        if value == "td":
            return cells
        return []

    row.find_elements.side_effect = find_elements_side_effect
    return row


def _make_mock_table(rows_data: list[list[str]]) -> MagicMock:
    table = MagicMock()
    mock_rows = [_make_mock_row(row) for row in rows_data]
    table.find_elements.return_value = mock_rows
    return table


def test_fetch_team_stats_uses_injected_driver():
    """주입된 driver는 fetch 완료 후 quit되지 않는다."""
    mock_driver = MagicMock()
    mock_table = _make_mock_table([HEADER, DATA_ROW])

    with patch("kbo_crawler.crawler.WebDriverWait") as mock_wait:
        mock_wait.return_value.until.return_value = mock_table
        result = fetch_team_stats(driver=mock_driver)

    mock_driver.get.assert_called_once_with(KBO_URL)
    mock_driver.quit.assert_not_called()
    assert len(result) == 1
    assert result[0].team_name == "KT"


def test_fetch_team_stats_creates_and_quits_own_driver():
    """driver 인자 없이 호출하면 내부에서 생성하고 완료 후 quit한다."""
    mock_driver = MagicMock()
    mock_table = _make_mock_table([HEADER, DATA_ROW])

    with patch("kbo_crawler.crawler.make_driver", return_value=mock_driver), \
         patch("kbo_crawler.crawler.WebDriverWait") as mock_wait:
        mock_wait.return_value.until.return_value = mock_table
        result = fetch_team_stats()

    mock_driver.quit.assert_called_once()
    assert len(result) == 1


def test_fetch_team_stats_quits_driver_on_exception():
    """예외 발생 시에도 자체 생성한 driver는 반드시 quit된다."""
    mock_driver = MagicMock()

    with patch("kbo_crawler.crawler.make_driver", return_value=mock_driver), \
         patch("kbo_crawler.crawler.WebDriverWait") as mock_wait:
        mock_wait.return_value.until.side_effect = Exception("Timeout")
        with pytest.raises(Exception, match="Timeout"):
            fetch_team_stats()

    mock_driver.quit.assert_called_once()


def test_fetch_team_stats_does_not_quit_injected_driver_on_exception():
    """예외 발생 시 주입된 driver는 quit되지 않는다."""
    mock_driver = MagicMock()

    with patch("kbo_crawler.crawler.WebDriverWait") as mock_wait:
        mock_wait.return_value.until.side_effect = Exception("Timeout")
        with pytest.raises(Exception, match="Timeout"):
            fetch_team_stats(driver=mock_driver)

    mock_driver.quit.assert_not_called()


def test_fetch_team_stats_returns_multiple_teams():
    """여러 팀 데이터가 있을 때 모두 파싱하여 반환한다."""
    mock_driver = MagicMock()
    data_row_2 = ["2", "한화", "0.282", "58", "2396", "2072", "367", "585", "95", "12", "63", "893", "341", "32", "17"]
    mock_table = _make_mock_table([HEADER, DATA_ROW, data_row_2])

    with patch("kbo_crawler.crawler.WebDriverWait") as mock_wait:
        mock_wait.return_value.until.return_value = mock_table
        result = fetch_team_stats(driver=mock_driver)

    assert len(result) == 2
    assert result[1].team_name == "한화"
