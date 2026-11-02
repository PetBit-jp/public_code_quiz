# 問題1: 休息時間の検出

首輪型デバイスが1時間ごとに歩数を記録しています。ある1日分（24時間）の歩数が配列で与えられます。

```
steps = [0, 0, 3, 0, 0, 120, 340, 210, 15, 8, 0, 0,
         5, 2, 0, 60, 480, 390, 120, 30, 4, 0, 0, 1]
#        ↑ index が時刻（0時〜23時）
```

歩数が **10未満** の時間帯を「休息中」とみなします。

## (1)

**最も長く続いた休息の「開始時刻」と「長さ（時間）」** を返す関数を書いてください。

| 言語 | ファイル | 実装する関数 |
|---|---|---|
| Java | [`RestDetection.java`](RestDetection.java) | `int[] longestRest(int[] steps)` |
| Python | [`rest_detection.py`](rest_detection.py) | `longest_rest(steps: list[int]) -> tuple[int, int]` |
| TypeScript | [`rest_detection.ts`](rest_detection.ts) | `longestRest(steps: number[]): [number, number]` |

好きな言語を 1 つ選んでください。

上の例では `(9, 6)` — 9時から6時間連続で休息していた、が答えになります。

仕様は以下のとおりです。

- 戻り値は `(開始時刻, 長さ)` の2要素
- **休息が1つも無い場合は `(-1, 0)`** を返す
- **最長の休息が複数ある場合は、最も早い時刻のもの** を返す

## (2)

各ファイルの末尾にテストが7件入っています。**全部 PASS すれば完了**です。

```sh
java RestDetection.java      # Java 17 以上
python3 rest_detection.py    # Python 3.9 以上
node rest_detection.ts       # Node.js 22.18 以上
```

```
PASS  サンプル（1日ぶん）
PASS  空の配列
...
7/7 passed
```

テストには以下が含まれます。手を動かす前に、それぞれ何を返すべきか考えてみてください。

- 配列が空のとき
- 全時間が休息だったとき
- 休息が一度も無かったとき
- 最長の休息が複数あるとき
- 末尾まで休息が続くとき

関数の中身だけ実装してください。テスト部分は編集不要です。
