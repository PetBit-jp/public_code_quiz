import java.util.Arrays;

/**
 * 問題1: 休息時間の検出
 *
 * 1時間ごとの歩数から、最も長く続いた休息（歩数 10 未満）の
 * 開始時刻と長さを求める。
 *
 * 実行: java RestDetection.java
 * 下のテストが全部 PASS すれば完成です。
 */
public class RestDetection {

    /** この値未満の歩数の時間帯を「休息中」とみなす */
    static final int REST_THRESHOLD = 10;

    /**
     * 最も長く続いた休息の {開始時刻, 長さ(時間)} を返す。
     *
     * - 休息が1つも無い場合は {-1, 0}
     * - 最長の休息が複数ある場合は、最も早い時刻のもの
     */
    public static int[] longestRest(int[] steps) {
        // TODO
        return null;
    }

    // ==================================================================
    // テスト（ここから下は編集不要です）
    // ==================================================================
    public static void main(String[] args) {
        check("サンプル（1日ぶん）",
                new int[]{0, 0, 3, 0, 0, 120, 340, 210, 15, 8, 0, 0,
                          5, 2, 0, 60, 480, 390, 120, 30, 4, 0, 0, 1},
                new int[]{9, 6});

        check("空の配列",
                new int[]{},
                new int[]{-1, 0});

        check("全時間が休息",
                new int[]{0, 0, 0, 0},
                new int[]{0, 4});

        check("休息が一度も無い",
                new int[]{50, 60, 70, 80},
                new int[]{-1, 0});

        check("最長が複数 → 早いほうを返す",
                new int[]{0, 0, 100, 0, 0, 100},
                new int[]{0, 2});

        check("末尾まで休息が続く",
                new int[]{100, 0, 0, 0},
                new int[]{1, 3});

        check("境界値（10は休息ではない / 9は休息）",
                new int[]{10, 9},
                new int[]{1, 1});

        System.out.println();
        System.out.println(passed + "/" + total + " passed");
        if (passed != total) {
            System.exit(1);
        }
    }

    static int total = 0;
    static int passed = 0;

    static void check(String label, int[] input, int[] expected) {
        total++;
        int[] actual;
        try {
            actual = longestRest(input);
        } catch (Exception e) {
            System.out.println("FAIL  " + label);
            System.out.println("      input    = " + Arrays.toString(input));
            System.out.println("      expected = " + Arrays.toString(expected));
            System.out.println("      thrown   = " + e);
            return;
        }
        if (Arrays.equals(actual, expected)) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            System.out.println("FAIL  " + label);
            System.out.println("      input    = " + Arrays.toString(input));
            System.out.println("      expected = " + Arrays.toString(expected));
            System.out.println("      actual   = " + Arrays.toString(actual));
        }
    }
}
