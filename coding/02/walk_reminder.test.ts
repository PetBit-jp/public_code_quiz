/**
 * 散歩リマインダー通知のテスト
 *
 * 実行: node walk_reminder.test.ts   （Node.js 22.18 以上）
 */

import {
  addDays,
  Channel,
  ChannelFactory,
  EmailChannel,
  MIN_DAYS,
  NotificationPipeline,
  NotificationRule,
  PushChannel,
  RATIO,
  RuleRegistry,
  SmsChannel,
  TARGET_LARGE,
  TARGET_MEDIUM,
  TARGET_SMALL,
  WalkShortageRule,
} from "./walk_reminder.ts";
import type { Pet, PushClient, StepRecord } from "./walk_reminder.ts";

const MONDAY = "2026-11-02"; // バッチを実行する月曜日

class FakePushClient implements PushClient {
  pushed: [string, string][] = [];

  push(ownerId: string, message: string): boolean {
    this.pushed.push([ownerId, message]);
    return true;
  }
}

class AlwaysNotifyRule extends NotificationRule {
  name(): string {
    return "always";
  }
  shouldNotify(): boolean {
    return true;
  }
  buildMessage(): string {
    return "test";
  }
}

function makePet(overrides: Partial<Pet> = {}): Pet {
  return {
    id: "P001",
    name: "モカ",
    ownerId: "U001",
    species: "dog",
    size: "small",
    hasCondition: false,
    ...overrides,
  };
}

function makeRecords(petId: string, stepsPerDay: number): StepRecord[] {
  const records: StepRecord[] = [];
  for (let i = 0; i < 7; i++) {
    records.push({ petId, date: addDays(MONDAY, -i), steps: stepsPerDay });
  }
  return records;
}

function assert(condition: boolean): void {
  if (!condition) {
    throw new Error("assertion failed");
  }
}

// =====================================================================
// テスト
// =====================================================================
const tests: { [name: string]: () => void } = {
  constants() {
    assert(TARGET_SMALL === 5000);
    assert(TARGET_MEDIUM === 8000);
    assert(TARGET_LARGE === 10000);
    assert(RATIO === 0.6);
    assert(MIN_DAYS === 4);
  },

  ruleIsRegistered() {
    const rules = RuleRegistry.getAll();
    assert(rules.some((r) => r instanceof WalkShortageRule));
  },

  ruleName() {
    assert(new WalkShortageRule().name() === "walk_shortage");
  },

  channelFactoryPush() {
    assert(ChannelFactory.create(Channel.PUSH, new FakePushClient()) instanceof PushChannel);
  },

  channelFactoryEmail() {
    assert(ChannelFactory.create(Channel.EMAIL) instanceof EmailChannel);
  },

  channelFactorySms() {
    assert(ChannelFactory.create(Channel.SMS) instanceof SmsChannel);
  },

  emailNotImplemented() {
    let thrown = false;
    try {
      new EmailChannel().send("U001", "hello");
    } catch (e) {
      thrown = true;
    }
    assert(thrown);
  },

  shouldNotifyReturnsBoolean() {
    const result = new WalkShortageRule().shouldNotify(makePet(), makeRecords("P001", 1000), MONDAY);
    assert(result !== undefined && result !== null);
    assert(typeof result === "boolean");
  },

  smallDogShortWalk() {
    assert(new WalkShortageRule().shouldNotify(makePet(), makeRecords("P001", 1000), MONDAY) === true);
  },

  smallDogEnoughWalk() {
    assert(new WalkShortageRule().shouldNotify(makePet(), makeRecords("P001", 6000), MONDAY) === false);
  },

  catIsSkipped() {
    const pet = makePet({ species: "cat" });
    assert(new WalkShortageRule().shouldNotify(pet, makeRecords("P001", 1000), MONDAY) === false);
  },

  buildMessage() {
    assert(new WalkShortageRule().buildMessage(makePet()).includes("モカ"));
  },

  pipelineSendsNotification() {
    const client = new FakePushClient();
    const pipeline = new NotificationPipeline(new PushChannel(client), new Set());
    pipeline.rules = [new AlwaysNotifyRule()];
    pipeline.run([makePet()], [], MONDAY);
    assert(client.pushed.length === 1);
  },

  pipelineReturnsArray() {
    const client = new FakePushClient();
    const pipeline = new NotificationPipeline(new PushChannel(client), new Set());
    assert(Array.isArray(pipeline.run([], [], MONDAY)));
  },
};

// =====================================================================
// テストランナー
// =====================================================================
let passed = 0;
const names = Object.keys(tests);
for (const name of names) {
  try {
    tests[name]();
    passed++;
    console.log(`PASS  ${name}`);
  } catch (e) {
    console.log(`FAIL  ${name}  (${e})`);
  }
}
console.log();
console.log(`${passed}/${names.length} passed`);
if (passed !== names.length) {
  process.exit(1);
}
