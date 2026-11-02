"""
散歩レポート

首輪から届いた歩数の記録を日ごとに集計し、
「目標歩数を何日連続で達成しているか」を求める。

実行: python3 walk_report.py
"""

from dataclasses import dataclass
from datetime import date, datetime, timedelta, timezone

SECONDS_PER_DAY = 24 * 60 * 60
EPOCH = date(1970, 1, 1)


@dataclass(frozen=True)
class Record:
    """首輪から届く1件の記録。"""

    timestamp: int  # UNIX 時間（秒）
    steps: int  # その5分間の歩数


def daily_totals(records: list[Record]) -> dict[date, int]:
    """
    日本時間（JST）の日付ごとの合計歩数を返す（日付の昇順）。

    - 記録は届いた順に並んでおり、時刻順とは限らない
    - 通信が切れると、復帰後にまとめて再送される。
      同じ timestamp の記録は再送なので 1 回だけ数える
    - 歩数が負の記録はセンサーエラーなので無視する
    - 記録が 1 件も無い日は結果に含めない
    """
    totals: dict[date, int] = {}
    prev = None
    for r in records:
        # 再送による重複はスキップ
        if prev is not None and r.timestamp == prev.timestamp:
            continue
        prev = r

        # センサーエラーは無視
        if r.steps < 0:
            continue

        d = EPOCH + timedelta(days=r.timestamp // SECONDS_PER_DAY)
        totals[d] = totals.get(d, 0) + r.steps
    return dict(sorted(totals.items()))


def current_streak(totals: dict[date, int], goal: int, today: date) -> int:
    """
    昨日から遡って、目標歩数「以上」を何日連続で達成しているかを返す。

    - 今日はまだ終わっていないので数えない
    - 記録が 1 件も無い日は 0 歩として扱う
    """
    past = [steps for d, steps in sorted(totals.items()) if d < today]
    streak = 0
    for steps in reversed(past):
        if steps > goal:
            streak += 1
        else:
            break
    return streak


# ======================================================================
# テスト
# ======================================================================
def main():
    # ---- daily_totals ----
    check("daily_totals: サンプル（2日ぶん・順不同）",
          lambda: daily_totals([
              rec("2026-11-02T10:00", 300),
              rec("2026-11-01T18:30", 500),
              rec("2026-11-01T18:35", 200),
              rec("2026-11-02T10:05", 100),
          ]),
          totals(("2026-11-01", 700),
                 ("2026-11-02", 400)))

    check("daily_totals: 空の入力",
          lambda: daily_totals([]),
          totals())

    check("daily_totals: 再送された記録は 1 回だけ数える",
          lambda: daily_totals([
              rec("2026-11-01T10:00", 100),
              rec("2026-11-01T10:00", 100),
              rec("2026-11-01T10:05", 50),
          ]),
          totals(("2026-11-01", 150)))

    check("daily_totals: センサーエラー（負の歩数）は無視",
          lambda: daily_totals([
              rec("2026-11-01T12:00", 80),
              rec("2026-11-01T12:05", -1),
              rec("2026-11-01T12:10", 40),
          ]),
          totals(("2026-11-01", 120)))

    # ---- current_streak ----
    check("current_streak: 3日連続で達成",
          lambda: current_streak(totals(
              ("2026-10-30", 1500),
              ("2026-10-31", 2000),
              ("2026-11-01", 1800)), 1000, day("2026-11-02")),
          3)

    check("current_streak: 途中で未達成の日がある",
          lambda: current_streak(totals(
              ("2026-10-30", 1500),
              ("2026-10-31", 500),
              ("2026-11-01", 2000)), 1000, day("2026-11-02")),
          1)

    check("current_streak: 今日は数えない",
          lambda: current_streak(totals(
              ("2026-10-30", 1500),
              ("2026-10-31", 2000),
              ("2026-11-01", 1800),
              ("2026-11-02", 100)), 1000, day("2026-11-02")),
          3)

    check("current_streak: 記録なし",
          lambda: current_streak(totals(), 1000, day("2026-11-02")),
          0)

    print()
    print(f"{passed}/{total} passed")
    if passed != total:
        raise SystemExit(1)


# ======================================================================
# テスト用ヘルパー
# ======================================================================
JST = timezone(timedelta(hours=9))


def rec(jst_datetime: str, steps: int) -> Record:
    """日本時間の日時（例: "2026-11-01T10:00"）と歩数から記録を作る"""
    dt = datetime.fromisoformat(jst_datetime).replace(tzinfo=JST)
    return Record(int(dt.timestamp()), steps)


def day(s: str) -> date:
    return date.fromisoformat(s)


def totals(*date_and_steps: tuple[str, int]) -> dict[date, int]:
    """totals(("2026-11-01", 700), ("2026-11-02", 400)) のように書く"""
    return {day(d): steps for d, steps in date_and_steps}


def fmt(value) -> str:
    """{date(2026, 11, 1): 700} を {2026-11-01: 700} のように表示する"""
    if isinstance(value, dict):
        return "{" + ", ".join(f"{k}: {v}" for k, v in value.items()) + "}"
    return repr(value)


total = 0
passed = 0


def check(label, actual_fn, expected):
    global total, passed
    total += 1
    try:
        actual = actual_fn()
    except Exception as e:
        print(f"FAIL  {label}")
        print(f"      expected = {fmt(expected)}")
        print(f"      raised   = {e!r}")
        return
    if actual == expected:
        passed += 1
        print(f"PASS  {label}")
    else:
        print(f"FAIL  {label}")
        print(f"      expected = {fmt(expected)}")
        print(f"      actual   = {fmt(actual)}")


if __name__ == "__main__":
    main()
