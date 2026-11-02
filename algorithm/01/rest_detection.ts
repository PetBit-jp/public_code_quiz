/**
 * 問題1: 休息時間の検出
 *
 * 1時間ごとの歩数から、最も長く続いた休息（歩数 10 未満）の
 * 開始時刻と長さを求める。
 *
 * 実行: node rest_detection.ts   （Node.js 22.18 以上）
 * 下のテストが全部 PASS すれば完成です。
 */

/** この値未満の歩数の時間帯を「休息中」とみなす */
const REST_THRESHOLD = 10;

/**
 * 最も長く続いた休息の [開始時刻, 長さ(時間)] を返す。
 *
 * - 休息が1つも無い場合は [-1, 0]
 * - 最長の休息が複数ある場合は、最も早い時刻のもの
 */
export function longestRest(steps: number[]): [number, number] {
  // TODO
  return null as unknown as [number, number];
}

// ====================================================================
// テスト（ここから下は編集不要です）
// ====================================================================
let total = 0;
let passed = 0;

function check(label: string, steps: number[], expected: [number, number]): void {
  total++;
  let actual: unknown;
  try {
    actual = longestRest([...steps]);
  } catch (e) {
    console.log(`FAIL  ${label}`);
    console.log(`      input    = ${JSON.stringify(steps)}`);
    console.log(`      expected = ${JSON.stringify(expected)}`);
    console.log(`      thrown   = ${e}`);
    return;
  }
  if (JSON.stringify(actual) === JSON.stringify(expected)) {
    passed++;
    console.log(`PASS  ${label}`);
  } else {
    console.log(`FAIL  ${label}`);
    console.log(`      input    = ${JSON.stringify(steps)}`);
    console.log(`      expected = ${JSON.stringify(expected)}`);
    console.log(`      actual   = ${JSON.stringify(actual)}`);
  }
}

check("サンプル（1日ぶん）",
  [0, 0, 3, 0, 0, 120, 340, 210, 15, 8, 0, 0,
   5, 2, 0, 60, 480, 390, 120, 30, 4, 0, 0, 1],
  [9, 6]);

check("空の配列",
  [],
  [-1, 0]);

check("全時間が休息",
  [0, 0, 0, 0],
  [0, 4]);

check("休息が一度も無い",
  [50, 60, 70, 80],
  [-1, 0]);

check("最長が複数 → 早いほうを返す",
  [0, 0, 100, 0, 0, 100],
  [0, 2]);

check("末尾まで休息が続く",
  [100, 0, 0, 0],
  [1, 3]);

check("境界値（10は休息ではない / 9は休息）",
  [10, 9],
  [1, 1]);

console.log();
console.log(`${passed}/${total} passed`);
if (passed !== total) {
  process.exit(1);
}
