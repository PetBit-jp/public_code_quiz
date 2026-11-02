import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 散歩リマインダー通知
 *
 * 毎週月曜の朝に実行し、先週の散歩が少なかった犬の飼い主へプッシュ通知を送る。
 *
 * 拡張性を考慮し、通知ルール・通知チャネルを差し替え可能な構成にしている。
 */
public class WalkReminder {

    // =================================================================
    // 定数
    // =================================================================
    public static final int TARGET_SMALL = 5000;   // 小型犬の目安歩数（1日あたり）
    public static final int TARGET_MEDIUM = 8000;  // 中型犬の目安歩数（1日あたり）
    public static final int TARGET_LARGE = 10000;  // 大型犬の目安歩数（1日あたり）
    public static final double RATIO = 0.6;        // 目安の何割を下回ったら通知するか
    public static final int MIN_DAYS = 4;          // 判定に必要な最低日数
}

// =====================================================================
// データ
// =====================================================================
record Pet(String id, String name, String ownerId,
           String species,        // "dog" / "cat"
           String size,           // "small" / "medium" / "large"
           boolean hasCondition) {} // 持病あり

record StepRecord(String petId, LocalDate date, int steps) {}

// =====================================================================
// 通知チャネル
// =====================================================================
enum Channel { PUSH, EMAIL, SMS }

interface PushClient {
    boolean push(String ownerId, String message);
}

/** 通知チャネルの抽象基底クラス */
abstract class NotificationChannel {
    abstract boolean send(String ownerId, String message);
}

/** プッシュ通知チャネル */
class PushChannel extends NotificationChannel {
    private final PushClient client;

    PushChannel(PushClient client) {
        this.client = client;
    }

    @Override
    boolean send(String ownerId, String message) {
        return client.push(ownerId, message);
    }
}

/** メール通知チャネル（将来対応予定） */
class EmailChannel extends NotificationChannel {
    @Override
    boolean send(String ownerId, String message) {
        throw new UnsupportedOperationException("メール通知は将来対応予定です");
    }
}

/** SMS 通知チャネル（将来対応予定） */
class SmsChannel extends NotificationChannel {
    @Override
    boolean send(String ownerId, String message) {
        throw new UnsupportedOperationException("SMS 通知は将来対応予定です");
    }
}

/** 通知チャネルを生成するファクトリ */
class ChannelFactory {
    static NotificationChannel create(Channel channel, PushClient client) {
        if (channel == Channel.PUSH) {
            return new PushChannel(client);
        } else if (channel == Channel.EMAIL) {
            return new EmailChannel();
        } else if (channel == Channel.SMS) {
            return new SmsChannel();
        } else {
            throw new IllegalArgumentException("未対応のチャネルです: " + channel);
        }
    }
}

// =====================================================================
// 通知ルール
// =====================================================================

/** 通知ルールの抽象基底クラス */
abstract class NotificationRule {
    abstract String name();
    abstract boolean shouldNotify(Pet pet, List<StepRecord> records, LocalDate today);
    abstract String buildMessage(Pet pet);
}

/** 通知ルールを登録・取得するレジストリ */
class RuleRegistry {
    private static final Map<String, Supplier<NotificationRule>> rules = new LinkedHashMap<>();

    static {
        register("WalkShortageRule", WalkShortageRule::new);
    }

    static void register(String name, Supplier<NotificationRule> factory) {
        rules.put(name, factory);
    }

    static List<NotificationRule> getAll() {
        List<NotificationRule> result = new ArrayList<>();
        for (String key : rules.keySet()) {
            result.add(rules.get(key).get());
        }
        return result;
    }
}

/** 散歩不足を判定するルール */
class WalkShortageRule extends NotificationRule {

    @Override
    String name() {
        return "walk_shortage";
    }

    @Override
    boolean shouldNotify(Pet pet, List<StepRecord> records, LocalDate today) {
        // Step 1: 犬以外は対象外
        if (!pet.species().equals("dog")) {
            return false;
        }

        // Step 2: 持病ありの子は対象外
        if (pet.hasCondition() == true) {
            return false;
        }

        // Step 3: 直近7日間の日ごとの歩数を集計
        List<Integer> daily = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.minusDays(i);
            boolean found = false;
            int total = 0;
            for (int j = 0; j < records.size(); j++) {
                if (records.get(j).petId().equals(pet.id())) {
                    if (records.get(j).date().equals(d)) {
                        found = true;
                        total = total + records.get(j).steps();
                    }
                }
            }
            if (found == true) {
                daily.add(total);
            } else {
                daily.add(0);
            }
        }

        // Step 4: 記録がある日数をカウント
        int daysWithData = 0;
        for (int i = 0; i < daily.size(); i++) {
            if (daily.get(i) > 0) {
                daysWithData = daysWithData + 1;
            }
        }
        if (daysWithData < WalkReminder.MIN_DAYS) {
            return false;
        }

        // Step 5: 1日あたりの平均歩数を計算
        int s = 0;
        for (int i = 0; i < daily.size(); i++) {
            s = s + daily.get(i);
        }
        double avg = s / 7.0;

        // Step 6: 犬のサイズごとに目安と比較
        if (pet.size().equals("small")) {
            int target = WalkReminder.TARGET_SMALL;
            if (avg <= target * WalkReminder.RATIO) {
                return true;
            } else {
                return false;
            }
        } else if (pet.size().equals("medium")) {
            int target = WalkReminder.TARGET_MEDIUM;
            if (avg <= target * WalkReminder.RATIO) {
                return true;
            } else {
                return false;
            }
        } else if (pet.size().equals("large")) {
            int target = WalkReminder.TARGET_LARGE;
            if (avg <= target * WalkReminder.RATIO) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    String buildMessage(Pet pet) {
        return pet.name() + "ちゃん、先週は少しお散歩が少なめでした。今週は少し長めのお散歩はいかがですか？";
    }
}

// =====================================================================
// パイプライン
// =====================================================================

/** 登録された全ルールを評価し、通知を送信するパイプライン */
class NotificationPipeline {
    private final NotificationChannel channel;
    private final Set<String> sentLog; // 送信済みの "ownerId:週の開始日" を保持
    List<NotificationRule> rules;

    NotificationPipeline(NotificationChannel channel, Set<String> sentLog) {
        this.channel = channel;
        this.sentLog = sentLog;
        this.rules = RuleRegistry.getAll();
    }

    List<String> run(List<Pet> pets, List<StepRecord> records, LocalDate today) {
        List<String> sent = new ArrayList<>();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        for (Pet pet : pets) {
            for (NotificationRule rule : rules) {
                if (rule.shouldNotify(pet, records, today) == true) {
                    String key = pet.ownerId() + ":" + weekStart;
                    if (sentLog.contains(key)) {
                        continue;
                    }
                    String message = rule.buildMessage(pet);
                    boolean ok = channel.send(pet.ownerId(), message);
                    if (ok == true) {
                        sentLog.add(key);
                        sent.add(pet.id());
                    }
                }
            }
        }
        return sent;
    }
}
