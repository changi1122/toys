import pytest

from kbo_crawler.models import TeamStats

SAMPLE_ROW = [
    "1", "KT", "0.284", "59", "2411", "2069",
    "335", "588", "94", "12", "42", "832", "317", "34", "14",
]


def test_from_row_creates_correct_instance():
    stats = TeamStats.from_row(SAMPLE_ROW)
    assert stats.rank == 1
    assert stats.team_name == "KT"
    assert stats.batting_avg == pytest.approx(0.284)
    assert stats.games == 59
    assert stats.plate_appearances == 2411
    assert stats.at_bats == 2069
    assert stats.runs == 335
    assert stats.hits == 588
    assert stats.doubles == 94
    assert stats.triples == 12
    assert stats.home_runs == 42
    assert stats.total_bases == 832
    assert stats.rbi == 317
    assert stats.sacrifice_hits == 34
    assert stats.sacrifice_flies == 14


def test_from_row_returns_team_stats_instance():
    stats = TeamStats.from_row(SAMPLE_ROW)
    assert isinstance(stats, TeamStats)


def test_from_row_raises_on_bad_int():
    bad_row = SAMPLE_ROW.copy()
    bad_row[0] = "NOT_A_NUMBER"
    with pytest.raises(ValueError):
        TeamStats.from_row(bad_row)


def test_from_row_raises_on_bad_float():
    bad_row = SAMPLE_ROW.copy()
    bad_row[2] = "NOT_A_FLOAT"
    with pytest.raises(ValueError):
        TeamStats.from_row(bad_row)


def test_from_row_raises_on_short_row():
    with pytest.raises((ValueError, IndexError)):
        TeamStats.from_row(["1", "KT"])
