import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 散歩リマインダー通知のテスト
 *
 * 実行: javac -d out *.java && java -cp out WalkReminderTest
 */
public class WalkReminderTest {

    static final LocalDate MONDAY = LocalDate.of(2026, 11, 2); // バッチを実行する月曜日

    static class FakePushClient implements PushClient {
        List<String> pushed = new ArrayList<>();

        @Override
        public boolean push(String ownerId, String message) {
            pushed.add(ownerId + ":" + message);
            return true;
        }
    }

    static class AlwaysNotifyRule extends NotificationRule {
        @Override String name() { return "always"; }
        @Override boolean shouldNotify(Pet pet, List<StepRecord> records, LocalDate today) { return true; }
        @Override String buildMessage(Pet pet) { return "test"; }
    }

    static Pet makePet() {
        return new Pet("P001", "モカ", "U001", "dog", "small", false);
    }

    static List<StepRecord> makeRecords(String petId, int stepsPerDay) {
        List<StepRecord> records = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            records.add(new StepRecord(petId, MONDAY.minusDays(i), stepsPerDay));
        }
        return records;
    }

    static void check(boolean condition) {
        if (!condition) {
            throw new AssertionError("assertion failed");
        }
    }

    // =================================================================
    // テスト
    // =================================================================
    static void testConstants() {
        check(WalkReminder.TARGET_SMALL == 5000);
        check(WalkReminder.TARGET_MEDIUM == 8000);
        check(WalkReminder.TARGET_LARGE == 10000);
        check(WalkReminder.RATIO == 0.6);
        check(WalkReminder.MIN_DAYS == 4);
    }

    static void testRuleIsRegistered() {
        boolean found = false;
        for (NotificationRule r : RuleRegistry.getAll()) {
            if (r instanceof WalkShortageRule) {
                found = true;
            }
        }
        check(found);
    }

    static void testRuleName() {
        check(new WalkShortageRule().name().equals("walk_shortage"));
    }

    static void testChannelFactoryPush() {
        check(ChannelFactory.create(Channel.PUSH, new FakePushClient()) instanceof PushChannel);
    }

    static void testChannelFactoryEmail() {
        check(ChannelFactory.create(Channel.EMAIL, null) instanceof EmailChannel);
    }

    static void testChannelFactorySms() {
        check(ChannelFactory.create(Channel.SMS, null) instanceof SmsChannel);
    }

    static void testEmailNotImplemented() {
        boolean thrown = false;
        try {
            new EmailChannel().send("U001", "hello");
        } catch (UnsupportedOperationException e) {
            thrown = true;
        }
        check(thrown);
    }

    static void testShouldNotifyReturnsBoolean() {
        Object result = new WalkShortageRule().shouldNotify(makePet(), makeRecords("P001", 1000), MONDAY);
        check(result != null);
        check(result instanceof Boolean);
    }

    static void testSmallDogShortWalk() {
        check(new WalkShortageRule().shouldNotify(makePet(), makeRecords("P001", 1000), MONDAY) == true);
    }

    static void testSmallDogEnoughWalk() {
        check(new WalkShortageRule().shouldNotify(makePet(), makeRecords("P001", 6000), MONDAY) == false);
    }

    static void testCatIsSkipped() {
        Pet cat = new Pet("P001", "モカ", "U001", "cat", "small", false);
        check(new WalkShortageRule().shouldNotify(cat, makeRecords("P001", 1000), MONDAY) == false);
    }

    static void testBuildMessage() {
        check(new WalkShortageRule().buildMessage(makePet()).contains("モカ"));
    }

    static void testPipelineSendsNotification() {
        FakePushClient client = new FakePushClient();
        NotificationPipeline pipeline = new NotificationPipeline(new PushChannel(client), new HashSet<>());
        pipeline.rules = List.of(new AlwaysNotifyRule());
        pipeline.run(List.of(makePet()), List.of(), MONDAY);
        check(client.pushed.size() == 1);
    }

    static void testPipelineReturnsList() {
        FakePushClient client = new FakePushClient();
        NotificationPipeline pipeline = new NotificationPipeline(new PushChannel(client), new HashSet<>());
        check(pipeline.run(List.of(), List.of(), MONDAY) != null);
    }

    // =================================================================
    // テストランナー
    // =================================================================
    public static void main(String[] args) {
        Map<String, Runnable> tests = new LinkedHashMap<>();
        tests.put("testConstants", WalkReminderTest::testConstants);
        tests.put("testRuleIsRegistered", WalkReminderTest::testRuleIsRegistered);
        tests.put("testRuleName", WalkReminderTest::testRuleName);
        tests.put("testChannelFactoryPush", WalkReminderTest::testChannelFactoryPush);
        tests.put("testChannelFactoryEmail", WalkReminderTest::testChannelFactoryEmail);
        tests.put("testChannelFactorySms", WalkReminderTest::testChannelFactorySms);
        tests.put("testEmailNotImplemented", WalkReminderTest::testEmailNotImplemented);
        tests.put("testShouldNotifyReturnsBoolean", WalkReminderTest::testShouldNotifyReturnsBoolean);
        tests.put("testSmallDogShortWalk", WalkReminderTest::testSmallDogShortWalk);
        tests.put("testSmallDogEnoughWalk", WalkReminderTest::testSmallDogEnoughWalk);
        tests.put("testCatIsSkipped", WalkReminderTest::testCatIsSkipped);
        tests.put("testBuildMessage", WalkReminderTest::testBuildMessage);
        tests.put("testPipelineSendsNotification", WalkReminderTest::testPipelineSendsNotification);
        tests.put("testPipelineReturnsList", WalkReminderTest::testPipelineReturnsList);

        int passed = 0;
        for (Map.Entry<String, Runnable> t : tests.entrySet()) {
            try {
                t.getValue().run();
                passed++;
                System.out.println("PASS  " + t.getKey());
            } catch (Throwable e) {
                System.out.println("FAIL  " + t.getKey() + "  (" + e + ")");
            }
        }
        System.out.println();
        System.out.println(passed + "/" + tests.size() + " passed");
        if (passed != tests.size()) {
            System.exit(1);
        }
    }
}
