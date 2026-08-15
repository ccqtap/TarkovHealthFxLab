# TarkovMod 健康系统审计（2026-08-13）

## 结论

需要把“完全取代 First Aid”拆成三个层次：

1. **运行时健康权威：已取代。** TarkovMod 5.0 自己拥有七部位 capability、伤害路由、治疗事务、NBT 持久化、owner-only 网络同步和原版血量兼容投影，并在启动时拒绝 `firstaid` 与 `tacz_firstaid_compat`。
2. **战斗中负面效果表现：尚未完成。** 当前只有健康页的文字/颜色状态卡，没有流血、骨折、疼痛的第一人称纹理、HUD 动画、镜头反馈或声音资源；本实验室正针对这一层。
3. **整合包内容与生命周期收尾：尚未完成。** V1.6.5 公测实例没有加载 First Aid，但仍可见旧 `firstaid:*` / `bleedingmod:*` 任务、配置、稀有度和声音引用；此外，定时出血致死与不死图腾的权威状态闭环仍需主模组单独修复。

## 代码证据

- `E:\Project\TarkovMod\src\main\java\com\qq\tarkovmod\health\api\HealthAuthorityCompatibility.java:9-27` 将 `firstaid` 和 `tacz_firstaid_compat` 列为硬冲突并抛出清晰错误。
- `...\health\damage\HealthDamageEvents.java:53-58,92-131,324-337` 在有 Tarkov capability 时取消原版伤害/治疗路径，改由 Tarkov 服务提交事务。
- `...\health\capability\TarkovHealthProvider.java:25-31,51-82,166-180` 是唯一持久化所有者，处理 NBT、clone、死亡重置和同步。
- `...\health\persistence\HealthNbtCodec.java:18-21` 只识别 Tarkov 自有 schema；First Aid NBT 不迁移，缺失数据按满状态初始化。
- `...\health\sync\SyncHealthStatePacket.java:21-35,271-296` 同步七部位当前/最大血量、流血等级、骨折、疼痛和止痛截止时间。
- `...\health\condition\ConditionRuntimeService.java:15-64` 每 20 tick 结算流血伤害和止痛药到期。
- `...\health\penalty\ConditionPenaltyService.java:23-58` 将腿骨折/疼痛映射到移动速度，将臂骨折/疼痛映射到攻击速度。
- `...\client\ClientNetworkPacketHandlers.java:110-143` 只有完整 owner-only 快照到达后才把客户端 capability 标记为权威。

## 当前状态模型

- 流血：`NONE / LIGHT / HEAVY`；默认阈值为 8 / 24 直接伤害，每秒每部位 0.15 / 0.45。
- 骨折：四肢布尔值，默认直接伤害阈值 18；没有自然愈合。
- 疼痛：每部位 0–100，默认增加量为直接伤害 × 1.5；止痛药只隐藏可见疼痛和惩罚，到期后原疼痛仍会出现。

## First Aid 边界

当前公测包 `E:\ALLPCL\pcl\.minecraft\versions\【We‘re Not Ready】未准备之时V1.6.5公开测试版本` 的 `mods` 目录没有 First Aid 或 TaCZ First Aid Compat，TarkovMod JAR SHA-256 为：

`EEC423AD0F7DC4E5C1CD1F950C17B7C211B1B30CDE952B250AF6E761662E9875`

仍需清理的旧内容引用包括任务奖励、初始物品、稀有度、PetiteInventory 边框和 Sound Physics 的 `firstaid:debuff.heartbeat`；这些是内容迁移问题，不代表 First Aid 运行时仍是健康权威。First Aid 的 GUI/HUD/声音没有被本实验室复制。

## 迁回主模组前的验收门槛

1. 用本实验室完成六个预设和真实 Tarkov 状态的视觉/音频验收。
2. 验证 `OFF`、`LOW`、`Reduce motion`、音效关闭和字幕行为。
3. 单独修复并测试出血致死、图腾恢复、旧任务/药品 ID 迁移。
4. 为主模组增加稳定的客户端只读 health-view API，再移除本实验室的反射桥。
