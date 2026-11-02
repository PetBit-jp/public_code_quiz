"""
問題1: 休息時間の検出

1時間ごとの歩数から、最も長く続いた休息（歩数 10 未満）の
開始時刻と長さを求める。

実行: python3 rest_detection.py
下のテストが全部 PASS すれば完成です。
"""

# この値未満の歩数の時間帯を「休息中」とみなす
REST_THRESHOLD = 10


def longest_rest(steps: list[int]) -> tuple[int, int]:
    """
    最も長く続いた休息の (開始時刻, 長さ(時間)) を返す。

    - 休息が1つも無い場合は (-1, 0)
    - 最長の休息が複数ある場合は、最も早い時刻のもの
    """
    # TODO
    return None


# ======================================================================
# テスト（ここから下は編集不要です）
# ======================================================================
def main():
    check("サンプル（1日ぶん）",
          [0, 0, 3, 0, 0, 120, 340, 210, 15, 8, 0, 0,
           5, 2, 0, 60, 480, 390, 120, 30, 4, 0, 0, 1],
          (9, 6))

    check("空の配列",
          [],
          (-1, 0))

    check("全時間が休息",
          [0, 0, 0, 0],
          (0, 4))

    check("休息が一度も無い",
          [50, 60, 70, 80],
          (-1, 0))

    check("最長が複数 → 早いほうを返す",
          [0, 0, 100, 0, 0, 100],
          (0, 2))

    check("末尾まで休息が続く",
          [100, 0, 0, 0],
          (1, 3))

    check("境界値（10は休息ではない / 9は休息）",
          [10, 9],
          (1, 1))

    print()
    print(f"{passed}/{total} passed")
    if passed != total:
        raise SystemExit(1)


total = 0
passed = 0


def check(label, steps, expected):
    global total, passed
    total += 1
    try:
        actual = longest_rest(list(steps))
    except Exception as e:
        print(f"FAIL  {label}")
        print(f"      input    = {steps}")
        print(f"      expected = {expected}")
        print(f"      raised   = {e!r}")
        return
    if actual is not None and tuple(actual) == expected:
        passed += 1
        print(f"PASS  {label}")
    else:
        print(f"FAIL  {label}")
        print(f"      input    = {steps}")
        print(f"      expected = {expected}")
        print(f"      actual   = {actual}")


if __name__ == "__main__":
    main()
