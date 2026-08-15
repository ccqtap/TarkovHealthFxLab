# v2 TaCZ / LR Tactical 可选兼容

## 边界

- 编译目标固定为 TaCZ `1.1.8-hotfix` 与 LR Tactical `0.4.1`。
- 第三方 JAR 只进入 `compileOnly`；成品不复制其代码、动画、模型、贴图、声音或数据。
- 默认构建不编译 `src/integration/java`，因此不要求安装任何第三方 JAR。
- 集成构建仍可在 TaCZ 或 LR Tactical 缺失时启动：主代码只保存适配类的字符串名称，检测到对应 mod 后才反射加载。

## 构建

默认使用当前整合包的稳定副本目录：

```powershell
.\gradlew.bat clean build -PwithIntegrations=true
```

也可显式指定仅用于编译的 mod 目录：

```powershell
.\gradlew.bat clean build -PwithIntegrations=true -PintegrationModsDir='E:/path/to/mods'
```

目录中必须各有且只有一个：

- `tacz-1.20.1-1.1.8-hotfix.jar`
- `*lrtactical-1.20.1-0.4.1.jar`

## 接线契约

主健康系统安装 `ThirdPartyCompatBootstrap` 时提供两个小接口：

- `InjuryCompatState`：从已经同步的 Tarkov 健康真源返回骨折臂数量、黑臂数量和镇痛状态。黑臂与骨折计数互斥。
- `MedicalActionSink`：在 LR 动画合法完成后的服务端事件中处理 `REPAIR` 或 `ANALGESIA`，随后同步权威状态并重建 MobEffect 投影。

LR Tactical 会先执行 `remove_effects: ["@harmful"]`，后发布 `ConsumableUseEvent`。因此 sink 绝不能把 MobEffect 是否尚存作为治疗条件；事件请求显式标记 `lrEffectsAlreadyApplied=true`。

## TaCZ 行为

TaCZ `AttachmentPropertyEvent` 不包含射手。客户端适配器监视本地玩家的权威伤势快照、主手物品和枪械 NBT；变化时调用公开的 `AttachmentPropertyManager.postChangeEvent` 重建缓存。事件处理只修改这一次新建的 `GunProperties.RECOIL` 缓存：

- 每只骨折臂：水平超额 `+15%`、垂直超额 `+25%`。
- 每只黑臂：水平超额 `+30%`、垂直超额 `+45%`。
- 镇痛：只把上述超额减半，不清除伤势。
- 伤势解除：再次从枪械与附件数据重建基础缓存，不对旧倍率做除法，也不写枪械 NBT/枪包 JSON。

适配器用公开 `ParameterizedCache.eval` 的三个采样点保留普通线性附件修正。遇到非线性 Lua 修正时保持原始后坐力并记录一次警告，避免破坏特殊枪械脚本。

## LR 映射

| Consumable ID | 完成动作 |
| --- | --- |
| `lrtactical:blood_pack` | `REPAIR` |
| `lrtactical:ibuprofen` | `ANALGESIA` |

只按 `ConsumableUseEvent.getConsumableId()` 精确匹配，不检查显示名或通用基础物品 `lrtactical:consumable`，因此直接复用 LR 的完成时机与第一人称动画。

## 仍需游戏内验证

- 装/拆制退器、握把后，伤势倍率与附件倍率可同时生效。
- 治愈、镇痛开始/结束、切枪、死亡/重生后无倍率残留。
- 特殊 Lua 后坐力脚本触发降级时枪械原始行为不变。
- LR 动画取消时不发治疗；合法完成时只发一次。
- `@harmful` 投影被 LR 先移除后，权威骨折/黑肢不会丢失，且同步后投影正确恢复。
