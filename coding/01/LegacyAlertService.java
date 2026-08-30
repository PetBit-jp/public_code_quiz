import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ペットの健康アラートを判定する日次バッチ。
 * デバイスからの取り込みと通知送信は別モジュールが担当しており、
 * このクラスは「判定」だけを担当している。
 */
public class LegacyAlertService {

    static Map<String, String> petNames = new HashMap<>();
    static Map<String, List<Integer>> hourlySteps = new HashMap<>();      // 当日24時間ぶんの歩数
    static Map<String, List<Integer>> past7DaysSteps = new HashMap<>();   // 過去7日の日次合計歩数
    static Map<String, Double> battery = new HashMap<>();                 // 最新のバッテリー残量(%)
    static Map<String, Integer> hoursSinceLastRecord = new HashMap<>();   // 最終受信からの経過時間

    public static List<String> checkAll() {
        List<String> results = new ArrayList<>();

        for (String petId : petNames.keySet()) {
            String name = petNames.get(petId);
            List<Integer> today = hourlySteps.get(petId);

            if (today != null) {
                int total = 0;
                for (int i = 0; i < today.size(); i++) {
                    total = total + today.get(i);
                }

                List<Integer> past = past7DaysSteps.get(petId);
                if (past != null && past.size() > 0) {
                    int s = 0;
                    for (int i = 0; i < past.size(); i++) {
                        s = s + past.get(i);
                    }
                    int avg = s / 7;
                    if (total < avg * 0.5) {
                        if (hoursSinceLastRecord.get(petId) < 24) {
                            results.add(petId + ":low_activity:" + name + "ちゃんの運動量が減っています");
                        }
                    }
                }

                if (battery.get(petId) != null) {
                    if (battery.get(petId) < 15) {
                        results.add(petId + ":low_battery:" + name + "ちゃんの首輪の充電が切れそうです");
                    }
                }

                if (hoursSinceLastRecord.get(petId) >= 24) {
                    results.add(petId + ":no_data:" + name + "ちゃんの首輪からデータが届いていません");
                }

                int nightSteps = 0;
                for (int h = 0; h < 5; h++) {
                    if (h < today.size()) {
                        nightSteps = nightSteps + today.get(h);
                    }
                }
                if (nightSteps > 200) {
                    if (hoursSinceLastRecord.get(petId) < 24) {
                        results.add(petId + ":restless_night:" + name + "ちゃんが夜落ち着かないようです");
                    }
                }
            }
        }

        return results;
    }

    // ------------------------------------------------------------------
    // 動作確認用のサンプルデータ
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        register("P001", "モカ",
                List.of(0, 0, 3, 0, 0, 120, 340, 210, 15, 8, 0, 0, 5, 2, 0, 60, 480, 390, 120, 30, 4, 0, 0, 1),
                List.of(2800, 3000, 2700, 3100, 2900, 2850, 3050), 62.0, 1);

        register("P002", "レオ",
                List.of(0, 0, 0, 0, 0, 40, 90, 60, 5, 0, 0, 0, 0, 0, 0, 20, 150, 110, 40, 10, 0, 0, 0, 0),
                List.of(6800, 7100, 6500, 7000, 6900, 6700, 7200), 28.7, 1);

        register("P003", "ソラ",
                List.of(0, 0, 5, 0, 0, 80, 200, 130, 10, 5, 0, 0, 0, 0, 0, 40, 300, 250, 80, 20, 0, 0, 0, 0),
                List.of(1500, 1600, 1550, 1620, 1580, 1540, 1590), 11.2, 1);

        register("P005", "ムギ",
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(1100, 1200, 1150, 1180, 1120, 1160, 1140), 34.5, 63);

        register("P008", "ミケ",
                List.of(90, 70, 60, 50, 40, 30, 120, 80, 10, 5, 0, 0, 0, 0, 0, 30, 200, 160, 60, 20, 0, 0, 0, 0),
                List.of(1500, 1450, 1600, 1520, 1480, 1550, 1510), 48.7, 1);

        for (String r : checkAll()) {
            System.out.println(r);
        }
    }

    static void register(String id, String name, List<Integer> today,
                         List<Integer> past, Double bat, int hours) {
        petNames.put(id, name);
        hourlySteps.put(id, today);
        past7DaysSteps.put(id, past);
        battery.put(id, bat);
        hoursSinceLastRecord.put(id, hours);
    }
}
