from dataclasses import dataclass


@dataclass
class TeamStats:
    rank: int               # 순위
    team_name: str          # 팀명
    batting_avg: float      # AVG (타율)
    games: int              # G (경기)
    plate_appearances: int  # PA (타석)
    at_bats: int            # AB (타수)
    runs: int               # R (득점)
    hits: int               # H (안타)
    doubles: int            # 2B (2루타)
    triples: int            # 3B (3루타)
    home_runs: int          # HR (홈런)
    total_bases: int        # TB (루타)
    rbi: int                # RBI (타점)
    sacrifice_hits: int     # SAC (희생번트)
    sacrifice_flies: int    # SF (희생플라이)

    @classmethod
    def from_row(cls, cells: list[str]) -> "TeamStats":
        """파싱된 셀 텍스트 리스트로부터 TeamStats 인스턴스를 생성한다."""
        return cls(
            rank=int(cells[0]),
            team_name=cells[1],
            batting_avg=float(cells[2]),
            games=int(cells[3]),
            plate_appearances=int(cells[4]),
            at_bats=int(cells[5]),
            runs=int(cells[6]),
            hits=int(cells[7]),
            doubles=int(cells[8]),
            triples=int(cells[9]),
            home_runs=int(cells[10]),
            total_bases=int(cells[11]),
            rbi=int(cells[12]),
            sacrifice_hits=int(cells[13]),
            sacrifice_flies=int(cells[14]),
        )
