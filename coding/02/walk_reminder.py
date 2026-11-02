"""
散歩リマインダー通知

毎週月曜の朝に実行し、先週の散歩が少なかった犬の飼い主へプッシュ通知を送る。

拡張性を考慮し、通知ルール・通知チャネルを差し替え可能な構成にしている。
"""

from abc import ABC, abstractmethod
from datetime import date, timedelta
from enum import Enum

# =====================================================================
# 定数
# =====================================================================
TARGET_SMALL = 5000    # 小型犬の目安歩数（1日あたり）
TARGET_MEDIUM = 8000   # 中型犬の目安歩数（1日あたり）
TARGET_LARGE = 10000   # 大型犬の目安歩数（1日あたり）
RATIO = 0.6            # 目安の何割を下回ったら通知するか
MIN_DAYS = 4           # 判定に必要な最低日数


# =====================================================================
# 通知チャネル
# =====================================================================
class Channel(Enum):
    PUSH = "push"
    EMAIL = "email"
    SMS = "sms"


class NotificationChannel(ABC):
    """通知チャネルの抽象基底クラス"""

    @abstractmethod
    def send(self, owner_id: str, message: str) -> bool:
        pass


class PushChannel(NotificationChannel):
    """プッシュ通知チャネル"""

    def __init__(self, client):
        self.client = client

    def send(self, owner_id: str, message: str) -> bool:
        return self.client.push(owner_id, message)


class EmailChannel(NotificationChannel):
    """メール通知チャネル（将来対応予定）"""

    def send(self, owner_id: str, message: str) -> bool:
        raise NotImplementedError("メール通知は将来対応予定です")


class SmsChannel(NotificationChannel):
    """SMS 通知チャネル（将来対応予定）"""

    def send(self, owner_id: str, message: str) -> bool:
        raise NotImplementedError("SMS 通知は将来対応予定です")


class ChannelFactory:
    """通知チャネルを生成するファクトリ"""

    @staticmethod
    def create(channel: Channel, client=None) -> NotificationChannel:
        if channel == Channel.PUSH:
            return PushChannel(client)
        elif channel == Channel.EMAIL:
            return EmailChannel()
        elif channel == Channel.SMS:
            return SmsChannel()
        else:
            raise ValueError("未対応のチャネルです: " + str(channel))


# =====================================================================
# 通知ルール
# =====================================================================
class NotificationRule(ABC):
    """通知ルールの抽象基底クラス"""

    @abstractmethod
    def name(self) -> str:
        pass

    @abstractmethod
    def should_notify(self, pet: dict, records: list, today: date) -> bool:
        pass

    @abstractmethod
    def build_message(self, pet: dict) -> str:
        pass


class RuleRegistry:
    """通知ルールを登録・取得するレジストリ"""

    _rules = {}

    @classmethod
    def register(cls, rule_cls):
        cls._rules[rule_cls.__name__] = rule_cls
        return rule_cls

    @classmethod
    def get_all(cls) -> list:
        result = []
        for key in cls._rules:
            result.append(cls._rules[key]())
        return result


@RuleRegistry.register
class WalkShortageRule(NotificationRule):
    """散歩不足を判定するルール"""

    def name(self) -> str:
        return "walk_shortage"

    def should_notify(self, pet: dict, records: list, today: date) -> bool:
        # Step 1: 犬以外は対象外
        if pet["species"] != "dog":
            return False

        # Step 2: 持病ありの子は対象外
        if pet["has_condition"] == True:
            return False

        # Step 3: 直近7日間の日ごとの歩数を集計
        daily = []
        for i in range(7):
            d = today - timedelta(days=i)
            found = False
            total = 0
            for j in range(len(records)):
                if records[j]["pet_id"] == pet["id"]:
                    if records[j]["date"] == d.isoformat():
                        found = True
                        total = total + records[j]["steps"]
            if found == True:
                daily.append(total)
            else:
                daily.append(0)

        # Step 4: 記録がある日数をカウント
        days_with_data = 0
        for i in range(len(daily)):
            if daily[i] > 0:
                days_with_data = days_with_data + 1
        if days_with_data < MIN_DAYS:
            return False

        # Step 5: 1日あたりの平均歩数を計算
        s = 0
        for i in range(len(daily)):
            s = s + daily[i]
        avg = s / 7

        # Step 6: 犬のサイズごとに目安と比較
        if pet["size"] == "small":
            target = TARGET_SMALL
            if avg <= target * RATIO:
                return True
            else:
                return False
        elif pet["size"] == "medium":
            target = TARGET_MEDIUM
            if avg <= target * RATIO:
                return True
            else:
                return False
        elif pet["size"] == "large":
            target = TARGET_LARGE
            if avg <= target * RATIO:
                return True
            else:
                return False
        else:
            return False

    def build_message(self, pet: dict) -> str:
        return pet["name"] + "ちゃん、先週は少しお散歩が少なめでした。今週は少し長めのお散歩はいかがですか？"


# =====================================================================
# パイプライン
# =====================================================================
class NotificationPipeline:
    """登録された全ルールを評価し、通知を送信するパイプライン"""

    def __init__(self, channel: NotificationChannel, sent_log: set):
        self.channel = channel
        self.sent_log = sent_log  # 送信済みの (owner_id, 週の開始日) を保持
        self.rules = RuleRegistry.get_all()

    def run(self, pets: list, records: list, today: date) -> list:
        sent = []
        week_start = today - timedelta(days=today.weekday())
        for pet in pets:
            for rule in self.rules:
                if rule.should_notify(pet, records, today) == True:
                    key = (pet["owner_id"], week_start.isoformat())
                    if key in self.sent_log:
                        continue
                    message = rule.build_message(pet)
                    ok = self.channel.send(pet["owner_id"], message)
                    if ok == True:
                        self.sent_log.add(key)
                        sent.append(pet["id"])
        return sent
