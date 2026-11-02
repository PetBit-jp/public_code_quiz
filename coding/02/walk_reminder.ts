/**
 * 散歩リマインダー通知
 *
 * 毎週月曜の朝に実行し、先週の散歩が少なかった犬の飼い主へプッシュ通知を送る。
 *
 * 拡張性を考慮し、通知ルール・通知チャネルを差し替え可能な構成にしている。
 */

// =====================================================================
// 型
// =====================================================================
export type Pet = {
  id: string;
  name: string;
  ownerId: string;
  species: string;      // "dog" | "cat"
  size: string;         // "small" | "medium" | "large"
  hasCondition: boolean; // 持病あり
};

export type StepRecord = {
  petId: string;
  date: string;  // "YYYY-MM-DD"
  steps: number; // その日の合計歩数
};

// =====================================================================
// 定数
// =====================================================================
export const TARGET_SMALL = 5000;   // 小型犬の目安歩数（1日あたり）
export const TARGET_MEDIUM = 8000;  // 中型犬の目安歩数（1日あたり）
export const TARGET_LARGE = 10000;  // 大型犬の目安歩数（1日あたり）
export const RATIO = 0.6;           // 目安の何割を下回ったら通知するか
export const MIN_DAYS = 4;          // 判定に必要な最低日数

// =====================================================================
// 日付ユーティリティ
// =====================================================================
export function addDays(date: string, days: number): string {
  const d = new Date(date + "T00:00:00Z");
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** 0 = 月曜 ... 6 = 日曜 */
export function weekday(date: string): number {
  const d = new Date(date + "T00:00:00Z");
  return (d.getUTCDay() + 6) % 7;
}

// =====================================================================
// 通知チャネル
// =====================================================================
export const Channel = {
  PUSH: "push",
  EMAIL: "email",
  SMS: "sms",
} as const;
export type ChannelType = (typeof Channel)[keyof typeof Channel];

export interface PushClient {
  push(ownerId: string, message: string): boolean;
}

/** 通知チャネルの抽象基底クラス */
export abstract class NotificationChannel {
  abstract send(ownerId: string, message: string): boolean;
}

/** プッシュ通知チャネル */
export class PushChannel extends NotificationChannel {
  client: PushClient;

  constructor(client: PushClient) {
    super();
    this.client = client;
  }

  send(ownerId: string, message: string): boolean {
    return this.client.push(ownerId, message);
  }
}

/** メール通知チャネル（将来対応予定） */
export class EmailChannel extends NotificationChannel {
  send(ownerId: string, message: string): boolean {
    throw new Error("メール通知は将来対応予定です");
  }
}

/** SMS 通知チャネル（将来対応予定） */
export class SmsChannel extends NotificationChannel {
  send(ownerId: string, message: string): boolean {
    throw new Error("SMS 通知は将来対応予定です");
  }
}

/** 通知チャネルを生成するファクトリ */
export class ChannelFactory {
  static create(channel: ChannelType, client?: PushClient): NotificationChannel {
    if (channel === Channel.PUSH) {
      return new PushChannel(client as PushClient);
    } else if (channel === Channel.EMAIL) {
      return new EmailChannel();
    } else if (channel === Channel.SMS) {
      return new SmsChannel();
    } else {
      throw new Error("未対応のチャネルです: " + channel);
    }
  }
}

// =====================================================================
// 通知ルール
// =====================================================================

/** 通知ルールの抽象基底クラス */
export abstract class NotificationRule {
  abstract name(): string;
  abstract shouldNotify(pet: Pet, records: StepRecord[], today: string): boolean;
  abstract buildMessage(pet: Pet): string;
}

/** 通知ルールを登録・取得するレジストリ */
export class RuleRegistry {
  static rules: { [name: string]: new () => NotificationRule } = {};

  static register(name: string, ruleClass: new () => NotificationRule): void {
    RuleRegistry.rules[name] = ruleClass;
  }

  static getAll(): NotificationRule[] {
    const result: NotificationRule[] = [];
    for (const key in RuleRegistry.rules) {
      result.push(new RuleRegistry.rules[key]());
    }
    return result;
  }
}

/** 散歩不足を判定するルール */
export class WalkShortageRule extends NotificationRule {
  name(): string {
    return "walk_shortage";
  }

  shouldNotify(pet: Pet, records: StepRecord[], today: string): boolean {
    // Step 1: 犬以外は対象外
    if (pet.species !== "dog") {
      return false;
    }

    // Step 2: 持病ありの子は対象外
    if (pet.hasCondition === true) {
      return false;
    }

    // Step 3: 直近7日間の日ごとの歩数を集計
    const daily: number[] = [];
    for (let i = 0; i < 7; i++) {
      const d = addDays(today, -i);
      let found = false;
      let total = 0;
      for (let j = 0; j < records.length; j++) {
        if (records[j].petId === pet.id) {
          if (records[j].date === d) {
            found = true;
            total = total + records[j].steps;
          }
        }
      }
      if (found === true) {
        daily.push(total);
      } else {
        daily.push(0);
      }
    }

    // Step 4: 記録がある日数をカウント
    let daysWithData = 0;
    for (let i = 0; i < daily.length; i++) {
      if (daily[i] > 0) {
        daysWithData = daysWithData + 1;
      }
    }
    if (daysWithData < MIN_DAYS) {
      return false;
    }

    // Step 5: 1日あたりの平均歩数を計算
    let s = 0;
    for (let i = 0; i < daily.length; i++) {
      s = s + daily[i];
    }
    const avg = s / 7;

    // Step 6: 犬のサイズごとに目安と比較
    if (pet.size === "small") {
      const target = TARGET_SMALL;
      if (avg <= target * RATIO) {
        return true;
      } else {
        return false;
      }
    } else if (pet.size === "medium") {
      const target = TARGET_MEDIUM;
      if (avg <= target * RATIO) {
        return true;
      } else {
        return false;
      }
    } else if (pet.size === "large") {
      const target = TARGET_LARGE;
      if (avg <= target * RATIO) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  buildMessage(pet: Pet): string {
    return pet.name + "ちゃん、先週は少しお散歩が少なめでした。今週は少し長めのお散歩はいかがですか？";
  }
}

RuleRegistry.register("WalkShortageRule", WalkShortageRule);

// =====================================================================
// パイプライン
// =====================================================================

/** 登録された全ルールを評価し、通知を送信するパイプライン */
export class NotificationPipeline {
  channel: NotificationChannel;
  sentLog: Set<string>; // 送信済みの "ownerId:週の開始日" を保持
  rules: NotificationRule[];

  constructor(channel: NotificationChannel, sentLog: Set<string>) {
    this.channel = channel;
    this.sentLog = sentLog;
    this.rules = RuleRegistry.getAll();
  }

  run(pets: Pet[], records: StepRecord[], today: string): string[] {
    const sent: string[] = [];
    const weekStart = addDays(today, -weekday(today));
    for (const pet of pets) {
      for (const rule of this.rules) {
        if (rule.shouldNotify(pet, records, today) === true) {
          const key = pet.ownerId + ":" + weekStart;
          if (this.sentLog.has(key)) {
            continue;
          }
          const message = rule.buildMessage(pet);
          const ok = this.channel.send(pet.ownerId, message);
          if (ok === true) {
            this.sentLog.add(key);
            sent.push(pet.id);
          }
        }
      }
    }
    return sent;
  }
}
