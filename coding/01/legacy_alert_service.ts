/**
 * ペットの健康アラートを判定する日次バッチ。
 * デバイスからの取り込みと通知送信は別モジュールが担当しており、
 * このファイルは「判定」だけを担当している。
 */

export const petNames: any = {};
export const hourlySteps: any = {};          // 当日24時間ぶんの歩数
export const past7DaysSteps: any = {};       // 過去7日の日次合計歩数
export const battery: any = {};              // 最新のバッテリー残量(%)
export const hoursSinceLastRecord: any = {}; // 最終受信からの経過時間

export function checkAll(): string[] {
  const results: string[] = [];

  for (const petId of Object.keys(petNames)) {
    const name = petNames[petId];
    const today = hourlySteps[petId];

    if (today != null) {
      let total = 0;
      for (let i = 0; i < today.length; i++) {
        total = total + today[i];
      }

      const past = past7DaysSteps[petId];
      if (past != null && past.length > 0) {
        let s = 0;
        for (let i = 0; i < past.length; i++) {
          s = s + past[i];
        }
        const avg = Math.floor(s / 7);
        if (total < avg * 0.5) {
          if (hoursSinceLastRecord[petId] < 24) {
            results.push(petId + ":low_activity:" + name + "ちゃんの運動量が減っています");
          }
        }
      }

      if (battery[petId] != null) {
        if (battery[petId] < 15) {
          results.push(petId + ":low_battery:" + name + "ちゃんの首輪の充電が切れそうです");
        }
      }

      if (hoursSinceLastRecord[petId] >= 24) {
        results.push(petId + ":no_data:" + name + "ちゃんの首輪からデータが届いていません");
      }

      let nightSteps = 0;
      for (let h = 0; h < 5; h++) {
        if (h < today.length) {
          nightSteps = nightSteps + today[h];
        }
      }
      if (nightSteps > 200) {
        if (hoursSinceLastRecord[petId] < 24) {
          results.push(petId + ":restless_night:" + name + "ちゃんが夜落ち着かないようです");
        }
      }
    }
  }

  return results;
}

// ------------------------------------------------------------------
// 動作確認用のサンプルデータ
// ------------------------------------------------------------------
function register(id: string, name: string, today: number[],
                  past: number[], bat: number, hours: number): void {
  petNames[id] = name;
  hourlySteps[id] = today;
  past7DaysSteps[id] = past;
  battery[id] = bat;
  hoursSinceLastRecord[id] = hours;
}

register("P001", "モカ",
  [0, 0, 3, 0, 0, 120, 340, 210, 15, 8, 0, 0, 5, 2, 0, 60, 480, 390, 120, 30, 4, 0, 0, 1],
  [2800, 3000, 2700, 3100, 2900, 2850, 3050], 62.0, 1);

register("P002", "レオ",
  [0, 0, 0, 0, 0, 40, 90, 60, 5, 0, 0, 0, 0, 0, 0, 20, 150, 110, 40, 10, 0, 0, 0, 0],
  [6800, 7100, 6500, 7000, 6900, 6700, 7200], 28.7, 1);

register("P003", "ソラ",
  [0, 0, 5, 0, 0, 80, 200, 130, 10, 5, 0, 0, 0, 0, 0, 40, 300, 250, 80, 20, 0, 0, 0, 0],
  [1500, 1600, 1550, 1620, 1580, 1540, 1590], 11.2, 1);

register("P005", "ムギ",
  [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  [1100, 1200, 1150, 1180, 1120, 1160, 1140], 34.5, 63);

register("P008", "ミケ",
  [90, 70, 60, 50, 40, 30, 120, 80, 10, 5, 0, 0, 0, 0, 0, 30, 200, 160, 60, 20, 0, 0, 0, 0],
  [1500, 1450, 1600, 1520, 1480, 1550, 1510], 48.7, 1);

for (const r of checkAll()) {
  console.log(r);
}
