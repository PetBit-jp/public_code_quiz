/**
 * 散歩レポート
 *
 * 首輪から届いた歩数の記録を日ごとに集計し、
 * 「目標歩数を何日連続で達成しているか」を求める。
 *
 * 実行: node walk_report.ts   （Node.js 22.18 以上）
 */

/** 首輪から届く1件の記録 */
export type WalkRecord = {
  timestamp: number; // UNIX 時間（秒）
  steps: number;     // その5分間の歩数
};

/** 日付は "YYYY-MM-DD" の文字列で表す */
export type DailyTotals = Map<string, number>;

/**
 * 日本時間（JST）の日付ごとの合計歩数を返す（日付の昇順）。
 *
 * - 記録は届いた順に並んでおり、時刻順とは限らない
 * - 通信が切れると、復帰後にまとめて再送される。
 *   同じ timestamp の記録は再送なので 1 回だけ数える
 * - 歩数が負の記録はセンサーエラーなので無視する
 * - 記録が 1 件も無い日は結果に含めない
 */
export function dailyTotals(records: WalkRecord[]): DailyTotals {
  const totals: DailyTotals = new Map();
  let prev: WalkRecord | null = null;
  for (const r of records) {
    // 再送による重複はスキップ
    if (prev !== null && r.timestamp === prev.timestamp) {
      continue;
    }
    prev = r;

    // センサーエラーは無視
    if (r.steps < 0) {
      continue;
    }

    const date = new Date(r.timestamp * 1000).toISOString().slice(0, 10);
    totals.set(date, (totals.get(date) ?? 0) + r.steps);
  }
  return new Map([...totals].sort(([a], [b]) => a.localeCompare(b)));
}

/**
 * 昨日から遡って、目標歩数「以上」を何日連続で達成しているかを返す。
 *
 * - 今日はまだ終わっていないので数えない
 * - 記録が 1 件も無い日は 0 歩として扱う
 */
export function currentStreak(totals: DailyTotals, goal: number, today: string): number {
  const past = [...totals]
    .filter(([date]) => date < today)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([, steps]) => steps);
  let streak = 0;
  for (let i = past.length - 1; i >= 0; i--) {
    if (past[i] > goal) {
      streak++;
    } else {
      break;
    }
  }
  return streak;
}

// ====================================================================
// テスト用ヘルパー
// ====================================================================

/** 日本時間の日時（例: "2026-11-01T10:00"）と歩数から記録を作る */
function rec(jstDateTime: string, steps: number): WalkRecord {
  const epochMs = Date.parse(`${jstDateTime}:00+09:00`);
  return { timestamp: epochMs / 1000, steps };
}

/** totals(["2026-11-01", 700], ["2026-11-02", 400]) のように書く */
function totals(...dateAndSteps: [string, number][]): DailyTotals {
  return new Map(dateAndSteps);
}

function fmt(value: unknown): string {
  if (value instanceof Map) {
    return "{" + [...value].map(([k, v]) => `${k}: ${v}`).join(", ") + "}";
  }
  return JSON.stringify(value);
}

function equals(a: unknown, b: unknown): boolean {
  if (a instanceof Map && b instanceof Map) {
    if (a.size !== b.size) return false;
    for (const [k, v] of a) {
      if (b.get(k) !== v) return false;
    }
    return true;
  }
  return a === b;
}

let total = 0;
let passed = 0;

function check(label: string, actualFn: () => unknown, expected: unknown): void {
  total++;
  let actual: unknown;
  try {
    actual = actualFn();
  } catch (e) {
    console.log(`FAIL  ${label}`);
    console.log(`      expected = ${fmt(expected)}`);
    console.log(`      thrown   = ${e}`);
    return;
  }
  if (equals(actual, expected)) {
    passed++;
    console.log(`PASS  ${label}`);
  } else {
    console.log(`FAIL  ${label}`);
    console.log(`      expected = ${fmt(expected)}`);
    console.log(`      actual   = ${fmt(actual)}`);
  }
}

// ====================================================================
// テスト
// ====================================================================

// ---- dailyTotals ----
check("dailyTotals: サンプル（2日ぶん・順不同）",
  () => dailyTotals([
    rec("2026-11-02T10:00", 300),
    rec("2026-11-01T18:30", 500),
    rec("2026-11-01T18:35", 200),
    rec("2026-11-02T10:05", 100),
  ]),
  totals(
    ["2026-11-01", 700],
    ["2026-11-02", 400]));

check("dailyTotals: 空の入力",
  () => dailyTotals([]),
  totals());

check("dailyTotals: 再送された記録は 1 回だけ数える",
  () => dailyTotals([
    rec("2026-11-01T10:00", 100),
    rec("2026-11-01T10:00", 100),
    rec("2026-11-01T10:05", 50),
  ]),
  totals(["2026-11-01", 150]));

check("dailyTotals: センサーエラー（負の歩数）は無視",
  () => dailyTotals([
    rec("2026-11-01T12:00", 80),
    rec("2026-11-01T12:05", -1),
    rec("2026-11-01T12:10", 40),
  ]),
  totals(["2026-11-01", 120]));

// ---- currentStreak ----
check("currentStreak: 3日連続で達成",
  () => currentStreak(totals(
    ["2026-10-30", 1500],
    ["2026-10-31", 2000],
    ["2026-11-01", 1800]), 1000, "2026-11-02"),
  3);

check("currentStreak: 途中で未達成の日がある",
  () => currentStreak(totals(
    ["2026-10-30", 1500],
    ["2026-10-31", 500],
    ["2026-11-01", 2000]), 1000, "2026-11-02"),
  1);

check("currentStreak: 今日は数えない",
  () => currentStreak(totals(
    ["2026-10-30", 1500],
    ["2026-10-31", 2000],
    ["2026-11-01", 1800],
    ["2026-11-02", 100]), 1000, "2026-11-02"),
  3);

check("currentStreak: 記録なし",
  () => currentStreak(totals(), 1000, "2026-11-02"),
  0);

console.log();
console.log(`${passed}/${total} passed`);
if (passed !== total) {
  process.exit(1);
}
