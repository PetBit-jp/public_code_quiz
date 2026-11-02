import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * 散歩レポート
 *
 * 首輪から届いた歩数の記録を日ごとに集計し、
 * 「目標歩数を何日連続で達成しているか」を求める。
 *
 * 実行: java WalkReport.java
 */
public class WalkReport {

    /**
     * 首輪から届く1件の記録。
     *
     * @param timestamp UNIX 時間（秒）
     * @param steps     その5分間の歩数
     */
    record Record(long timestamp, int steps) {}

    static final long SECONDS_PER_DAY = 24 * 60 * 60;

    /**
     * 日本時間（JST）の日付ごとの合計歩数を返す（日付の昇順）。
     *
     * - 記録は届いた順に並んでおり、時刻順とは限らない
     * - 通信が切れると、復帰後にまとめて再送される。
     *   同じ timestamp の記録は再送なので 1 回だけ数える
     * - 歩数が負の記録はセンサーエラーなので無視する
     * - 記録が 1 件も無い日は結果に含めない
     */
    public static SortedMap<LocalDate, Integer> dailyTotals(List<Record> records) {
        SortedMap<LocalDate, Integer> totals = new TreeMap<>();
        Record prev = null;
        for (Record r : records) {
            // 再送による重複はスキップ
            if (prev != null && r.timestamp() == prev.timestamp()) {
                continue;
            }
            prev = r;

            // センサーエラーは無視
            if (r.steps() < 0) {
                continue;
            }

            LocalDate date = LocalDate.ofEpochDay(r.timestamp() / SECONDS_PER_DAY);
            totals.merge(date, r.steps(), Integer::sum);
        }
        return totals;
    }

    /**
     * 昨日から遡って、目標歩数「以上」を何日連続で達成しているかを返す。
     *
     * - 今日はまだ終わっていないので数えない
     * - 記録が 1 件も無い日は 0 歩として扱う
     */
    public static int currentStreak(SortedMap<LocalDate, Integer> totals, int goal, LocalDate today) {
        List<Integer> past = new ArrayList<>(totals.headMap(today).values());
        int streak = 0;
        for (int i = past.size() - 1; i >= 0; i--) {
            if (past.get(i) > goal) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    // ==================================================================
    // テスト
    // ==================================================================
    public static void main(String[] args) {
        // ---- dailyTotals ----
        check("dailyTotals: サンプル（2日ぶん・順不同）",
                () -> dailyTotals(List.of(
                        rec("2026-11-02T10:00", 300),
                        rec("2026-11-01T18:30", 500),
                        rec("2026-11-01T18:35", 200),
                        rec("2026-11-02T10:05", 100))),
                totals(
                        "2026-11-01", 700,
                        "2026-11-02", 400));

        check("dailyTotals: 空の入力",
                () -> dailyTotals(List.of()),
                totals());

        check("dailyTotals: 再送された記録は 1 回だけ数える",
                () -> dailyTotals(List.of(
                        rec("2026-11-01T10:00", 100),
                        rec("2026-11-01T10:00", 100),
                        rec("2026-11-01T10:05", 50))),
                totals("2026-11-01", 150));

        check("dailyTotals: センサーエラー（負の歩数）は無視",
                () -> dailyTotals(List.of(
                        rec("2026-11-01T12:00", 80),
                        rec("2026-11-01T12:05", -1),
                        rec("2026-11-01T12:10", 40))),
                totals("2026-11-01", 120));

        // ---- currentStreak ----
        check("currentStreak: 3日連続で達成",
                () -> currentStreak(totals(
                        "2026-10-30", 1500,
                        "2026-10-31", 2000,
                        "2026-11-01", 1800), 1000, date("2026-11-02")),
                3);

        check("currentStreak: 途中で未達成の日がある",
                () -> currentStreak(totals(
                        "2026-10-30", 1500,
                        "2026-10-31", 500,
                        "2026-11-01", 2000), 1000, date("2026-11-02")),
                1);

        check("currentStreak: 今日は数えない",
                () -> currentStreak(totals(
                        "2026-10-30", 1500,
                        "2026-10-31", 2000,
                        "2026-11-01", 1800,
                        "2026-11-02", 100), 1000, date("2026-11-02")),
                3);

        check("currentStreak: 記録なし",
                () -> currentStreak(totals(), 1000, date("2026-11-02")),
                0);

        System.out.println();
        System.out.println(passed + "/" + total + " passed");
        if (passed != total) {
            System.exit(1);
        }
    }

    // ==================================================================
    // テスト用ヘルパー
    // ==================================================================
    static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    /** 日本時間の日時（例: "2026-11-01T10:00"）と歩数から記録を作る */
    static Record rec(String jstDateTime, int steps) {
        long epochSec = LocalDateTime.parse(jstDateTime).atZone(JST).toEpochSecond();
        return new Record(epochSec, steps);
    }

    static LocalDate date(String s) {
        return LocalDate.parse(s);
    }

    /** totals("2026-11-01", 700, "2026-11-02", 400) のように日付と歩数を交互に並べる */
    static SortedMap<LocalDate, Integer> totals(Object... dateAndSteps) {
        SortedMap<LocalDate, Integer> m = new TreeMap<>();
        for (int i = 0; i < dateAndSteps.length; i += 2) {
            m.put(date((String) dateAndSteps[i]), (Integer) dateAndSteps[i + 1]);
        }
        return m;
    }

    static int total = 0;
    static int passed = 0;

    static void check(String label, Supplier<Object> actualSupplier, Object expected) {
        total++;
        Object actual;
        try {
            actual = actualSupplier.get();
        } catch (Exception e) {
            System.out.println("FAIL  " + label);
            System.out.println("      expected = " + expected);
            System.out.println("      thrown   = " + e);
            return;
        }
        if (Objects.equals(actual, expected)) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            System.out.println("FAIL  " + label);
            System.out.println("      expected = " + expected);
            System.out.println("      actual   = " + actual);
        }
    }
}
