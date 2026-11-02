"""
ペットの健康アラートを判定する日次バッチ。
デバイスからの取り込みと通知送信は別モジュールが担当しており、
このモジュールは「判定」だけを担当している。
"""

pet_names = {}
hourly_steps = {}             # 当日24時間ぶんの歩数
past_7days_steps = {}         # 過去7日の日次合計歩数
battery = {}                  # 最新のバッテリー残量(%)
hours_since_last_record = {}  # 最終受信からの経過時間


def check_all():
    results = []

    for pet_id in pet_names.keys():
        name = pet_names.get(pet_id)
        today = hourly_steps.get(pet_id)

        if today != None:
            total = 0
            for i in range(len(today)):
                total = total + today[i]

            past = past_7days_steps.get(pet_id)
            if past != None and len(past) > 0:
                s = 0
                for i in range(len(past)):
                    s = s + past[i]
                avg = s // 7
                if total < avg * 0.5:
                    if hours_since_last_record.get(pet_id) < 24:
                        results.append(pet_id + ":low_activity:" + name + "ちゃんの運動量が減っています")

            if battery.get(pet_id) != None:
                if battery.get(pet_id) < 15:
                    results.append(pet_id + ":low_battery:" + name + "ちゃんの首輪の充電が切れそうです")

            if hours_since_last_record.get(pet_id) >= 24:
                results.append(pet_id + ":no_data:" + name + "ちゃんの首輪からデータが届いていません")

            night_steps = 0
            for h in range(5):
                if h < len(today):
                    night_steps = night_steps + today[h]
            if night_steps > 200:
                if hours_since_last_record.get(pet_id) < 24:
                    results.append(pet_id + ":restless_night:" + name + "ちゃんが夜落ち着かないようです")

    return results


# ----------------------------------------------------------------------
# 動作確認用のサンプルデータ
# ----------------------------------------------------------------------
def register(id, name, today, past, bat, hours):
    pet_names[id] = name
    hourly_steps[id] = today
    past_7days_steps[id] = past
    battery[id] = bat
    hours_since_last_record[id] = hours


if __name__ == "__main__":
    register("P001", "モカ",
             [0, 0, 3, 0, 0, 120, 340, 210, 15, 8, 0, 0, 5, 2, 0, 60, 480, 390, 120, 30, 4, 0, 0, 1],
             [2800, 3000, 2700, 3100, 2900, 2850, 3050], 62.0, 1)

    register("P002", "レオ",
             [0, 0, 0, 0, 0, 40, 90, 60, 5, 0, 0, 0, 0, 0, 0, 20, 150, 110, 40, 10, 0, 0, 0, 0],
             [6800, 7100, 6500, 7000, 6900, 6700, 7200], 28.7, 1)

    register("P003", "ソラ",
             [0, 0, 5, 0, 0, 80, 200, 130, 10, 5, 0, 0, 0, 0, 0, 40, 300, 250, 80, 20, 0, 0, 0, 0],
             [1500, 1600, 1550, 1620, 1580, 1540, 1590], 11.2, 1)

    register("P005", "ムギ",
             [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
             [1100, 1200, 1150, 1180, 1120, 1160, 1140], 34.5, 63)

    register("P008", "ミケ",
             [90, 70, 60, 50, 40, 30, 120, 80, 10, 5, 0, 0, 0, 0, 0, 30, 200, 160, 60, 20, 0, 0, 0, 0],
             [1500, 1450, 1600, 1520, 1480, 1550, 1510], 48.7, 1)

    for r in check_all():
        print(r)
