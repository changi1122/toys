import pytest

from kbo_crawler.parser import parse_table_rows

HEADER_ROW = ["순위", "팀명", "AVG", "G", "PA", "AB", "R", "H", "2B", "3B", "HR", "TB", "RBI", "SAC", "SF"]
DATA_ROW_KT = ["1", "KT", "0.284", "59", "2411", "2069", "335", "588", "94", "12", "42", "832", "317", "34", "14"]
DATA_ROW_HH = ["2", "한화", "0.282", "58", "2396", "2072", "367", "585", "95", "12", "63", "893", "341", "32", "17"]
TOTAL_ROW = ["합계", "0.266", "295", "23342", "20267", "2959", "5383", "917", "87", "531", "8067", "2794", "258", "193"]


def test_skips_header_row():
    result = parse_table_rows([HEADER_ROW, DATA_ROW_KT])
    assert len(result) == 1
    assert result[0].team_name == "KT"


def test_skips_total_row():
    result = parse_table_rows([HEADER_ROW, DATA_ROW_KT, TOTAL_ROW])
    assert len(result) == 1


def test_parses_multiple_rows():
    result = parse_table_rows([HEADER_ROW, DATA_ROW_KT, DATA_ROW_HH])
    assert len(result) == 2
    assert result[0].team_name == "KT"
    assert result[1].team_name == "한화"


def test_skips_empty_rows():
    result = parse_table_rows([[], HEADER_ROW, DATA_ROW_KT])
    assert len(result) == 1


def test_returns_empty_list_for_only_header():
    result = parse_table_rows([HEADER_ROW])
    assert result == []


def test_returns_empty_list_for_empty_input():
    result = parse_table_rows([])
    assert result == []


def test_raises_on_malformed_row():
    with pytest.raises(ValueError):
        parse_table_rows([["1", "KT", "NOT_A_FLOAT"]])


def test_parsed_stats_values():
    result = parse_table_rows([HEADER_ROW, DATA_ROW_KT])
    team = result[0]
    assert team.rank == 1
    assert team.games == 59
    assert team.home_runs == 42
    assert team.batting_avg == pytest.approx(0.284)
