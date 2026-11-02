"""
散歩リマインダー通知のテスト

実行: python3 test_walk_reminder.py
"""

from datetime import date, timedelta

from walk_reminder import (
    MIN_DAYS,
    RATIO,
    TARGET_LARGE,
    TARGET_MEDIUM,
    TARGET_SMALL,
    Channel,
    ChannelFactory,
    EmailChannel,
    NotificationPipeline,
    NotificationRule,
    PushChannel,
    RuleRegistry,
    SmsChannel,
    WalkShortageRule,
)

MONDAY = date(2026, 11, 2)  # バッチを実行する月曜日


class FakePushClient:
    def __init__(self):
        self.pushed = []

    def push(self, owner_id, message):
        self.pushed.append((owner_id, message))
        return True


class AlwaysNotifyRule(NotificationRule):
    def name(self):
        return "always"

    def should_notify(self, pet, records, today):
        return True

    def build_message(self, pet):
        return "test"


def make_pet(**overrides):
    pet = {
        "id": "P001",
        "name": "モカ",
        "owner_id": "U001",
        "species": "dog",
        "size": "small",
        "has_condition": False,
    }
    pet.update(overrides)
    return pet


def make_records(pet_id, steps_per_day):
    records = []
    for i in range(7):
        d = MONDAY - timedelta(days=i)
        records.append({"pet_id": pet_id, "date": d.isoformat(), "steps": steps_per_day})
    return records


# =====================================================================
# テスト
# =====================================================================
def test_constants():
    assert TARGET_SMALL == 5000
    assert TARGET_MEDIUM == 8000
    assert TARGET_LARGE == 10000
    assert RATIO == 0.6
    assert MIN_DAYS == 4


def test_rule_is_registered():
    rules = RuleRegistry.get_all()
    assert any(isinstance(r, WalkShortageRule) for r in rules)


def test_rule_name():
    assert WalkShortageRule().name() == "walk_shortage"


def test_channel_factory_push():
    channel = ChannelFactory.create(Channel.PUSH, FakePushClient())
    assert isinstance(channel, PushChannel)


def test_channel_factory_email():
    assert isinstance(ChannelFactory.create(Channel.EMAIL), EmailChannel)


def test_channel_factory_sms():
    assert isinstance(ChannelFactory.create(Channel.SMS), SmsChannel)


def test_email_not_implemented():
    try:
        EmailChannel().send("U001", "hello")
        assert False
    except NotImplementedError:
        pass


def test_should_notify_returns_bool():
    result = WalkShortageRule().should_notify(make_pet(), make_records("P001", 1000), MONDAY)
    assert result is not None
    assert isinstance(result, bool)


def test_small_dog_short_walk():
    assert WalkShortageRule().should_notify(make_pet(), make_records("P001", 1000), MONDAY) == True


def test_small_dog_enough_walk():
    assert WalkShortageRule().should_notify(make_pet(), make_records("P001", 6000), MONDAY) == False


def test_cat_is_skipped():
    pet = make_pet(species="cat")
    assert WalkShortageRule().should_notify(pet, make_records("P001", 1000), MONDAY) == False


def test_build_message():
    message = WalkShortageRule().build_message(make_pet())
    assert "モカ" in message


def test_pipeline_sends_notification():
    client = FakePushClient()
    pipeline = NotificationPipeline(PushChannel(client), set())
    pipeline.rules = [AlwaysNotifyRule()]
    pipeline.run([make_pet()], [], MONDAY)
    assert len(client.pushed) == 1


def test_pipeline_returns_list():
    client = FakePushClient()
    pipeline = NotificationPipeline(PushChannel(client), set())
    result = pipeline.run([], [], MONDAY)
    assert isinstance(result, list)


# =====================================================================
# テストランナー
# =====================================================================
if __name__ == "__main__":
    tests = [(name, fn) for name, fn in list(globals().items())
             if name.startswith("test_") and callable(fn)]
    passed = 0
    for name, fn in tests:
        try:
            fn()
            passed += 1
            print(f"PASS  {name}")
        except Exception as e:
            print(f"FAIL  {name}  ({e!r})")
    print()
    print(f"{passed}/{len(tests)} passed")
    if passed != len(tests):
        raise SystemExit(1)
