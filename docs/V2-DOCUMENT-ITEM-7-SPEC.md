# Tarkov Health FX Lab v2：文档第 7 条实施规格

状态：**standalone Lab 实现完成；自动化构建通过，完整整合包游戏内回归仍按验收矩阵执行。**  
来源：`C:\Users\QQ\Downloads\优化及bug.docx` 第 7 条“肢体血量效果”。  
基线：v1 已封存在 `archive/v1/`，v2 不覆盖、删除或重写 v1 JAR、源码 ZIP 与校验清单。

## 1. 实现目标与权威边界

v2 把原先仅用于视觉预览的实验室扩展为独立的双端功能验收模组。它可以验证七部位 HP、流血、疼痛、骨折、四类黑肢、治疗、MobEffect、移动惩罚、第一人称表现，以及选定第三方模组的 API 兼容。

当前 standalone Lab 的唯一服务端真源是实验室自己的 `PlayerInjuryStore`。MobEffect 是命令入口、兼容投影和表现载体，不是第二份真源。客户端只接收 owner 状态快照；Mock 则完全留在客户端。

这不等于已经替换 TarkovMod 的健康 authority：

- 实验室没有接入 TarkovMod 的真实子弹、近战、爆炸、护甲后部位伤害路径。
- `/healthfx server damage` 和头部转移只修改 Lab 七部位状态，不造成真实 Tarkov/原版实体伤害。
- Lab 流血 tick 只扣 Lab 部位 HP；standalone 阶段没有把它连接到 Tarkov 死亡判定。
- `Tarkov live` 只反射读取 TarkovMod 的客户端健康快照，用于表现对照，不写入 Tarkov capability。
- 移植时必须用一个单一 authority adapter 将这些入口连接到 Tarkov 健康服务，并在该适配器启用时停用 Lab 存储和重复结算。

## 2. 第 7 条功能实现

### 2.1 七部位真源与同步

已实现 `HEAD / THORAX / STOMACH / LEFT_ARM / RIGHT_ARM / LEFT_LEG / RIGHT_LEG` 七个部位，每个部位独立保存：

- 当前 HP 与最大 HP；
- `NONE / LIGHT / HEAVY` 流血；
- `0–100` 疼痛；
- 四肢骨折；
- 四肢黑肢；
- 最近受影响部位。

状态保存在玩家持久数据中，在登录、换维度、重生等节点同步。死亡 clone 会清除 Lab 伤情；非死亡 clone 保留。客户端 `LAB_SERVER` 来源把完整七部位快照映射到视觉状态，不用单个 MobEffect 反推真源。

### 2.2 MobEffect 与 `/effect`

已注册：

- `tarkov_health_fx_lab:light_bleeding`
- `tarkov_health_fx_lab:heavy_bleeding`
- `tarkov_health_fx_lab:pain`
- `tarkov_health_fx_lab:fracture`
- `tarkov_health_fx_lab:blackened_left_arm`
- `tarkov_health_fx_lab:blackened_right_arm`
- `tarkov_health_fx_lab:blackened_left_leg`
- `tarkov_health_fx_lab:blackened_right_leg`
- `tarkov_health_fx_lab:repair`
- `tarkov_health_fx_lab:analgesia`

另兼容原版 `minecraft:regeneration`。外部有时限的 `/effect` 实例可以写入 Lab 真源；由真源生成的无限隐藏实例带有 ownership 标记，移除、到期和重投影不会互相误判。全局的流血、疼痛、骨折效果在没有部位参数时使用最近受影响部位；需要精确部位时使用 `/healthfx server set ...`。

Lab 真源只结算一次流血：每个轻度流血部位每 2 秒扣 `0.25` Lab HP，每个重度流血部位每秒扣 `0.5` Lab HP。MobEffect 本身不再额外扣一次。

### 2.3 头部伤害随机转移

已实现持久化、默认关闭的服务端开关：

```text
/healthfx server head_redirect status|on|off
/healthfx server head_redirect test <damage> [seed]
```

开启后，Lab 的 `/healthfx server damage ... head ...` 会把头部伤害均匀转移到六个非头部部位之一，伤害量不变。结果记录 seed、原部位、目标部位、伤害量和是否转移；F8 可以查询、切换、用固定 seed 测试并解析最近回执。

此开关尚未拦截 TarkovMod 的真实伤害事件；真实 damage router 接线属于后续移植。

### 2.4 疼痛与镇痛

- 流血、骨折、黑肢和部位伤害会保证相应部位存在最低疼痛值；手动命令可调到 `0–100`。
- 疼痛表现包括外围压暗/波纹、低幅镜头偏移和受控音频；关闭屏幕效果或减少动态后不会留下镜头偏移。
- 镇痛抑制表现，但保留原始疼痛；结束后视觉从仍存在的值恢复。
- 对锁定版本 Explosion Overhaul，镇痛期间兼容桥会停止其模糊、脑震荡镜头、耳鸣/低通并清理由该版本排队的晃动。
- F8 临时镇痛命令默认 `60` 秒；LR `ibuprofen` 适配默认 `120` 秒。

第三方表现优先由 EnhancedVisuals、Camera Overhaul、Explosion Overhaul 的外部资源和接口提供；依赖缺失或桥失败时使用 Lab 自有 v1 fallback。没有把第三方贴图或声音复制进 v2。

### 2.5 骨折、黑肢与移动

- 四肢骨折独立保存；腿部骨折每只产生 `-15%` 移动速度乘区惩罚。
- 四肢 HP 降到 `0` 时进入黑肢；黑腿每只产生 `-25%` 移动惩罚。
- 总移动惩罚上限为 `-65%`。镇痛把“骨折腿”部分缩放到 `35%`，但不削弱黑腿部分，也不清除伤情。
- 黑肢不能被普通 Lab 治疗恢复；必须由修复或原版再生先解锁。
- 状态卡提供稳定形状与文字；颜色不是唯一通道。黑肢会单独计数，不依赖骨折图标代替。

### 2.6 TaCZ 后坐力

`integrated` JAR 的 TaCZ 1.1.8 适配器监听公开附件属性缓存事件，并按本地玩家的权威快照重建 `GunProperties.RECOIL`：

- 每只仅骨折手臂：水平超额 `+15%`、垂直超额 `+25%`；
- 每只黑臂：水平超额 `+30%`、垂直超额 `+45%`；
- 黑臂与骨折计数互斥；
- 镇痛只把伤情产生的超额乘以 `0.5`；
- 总倍率最大 `3.0x`。

适配器不写枪械 NBT、枪包 JSON 或旧缓存。伤情、主手枪械或附件状态变化时，从 TaCZ 的当前属性重建；遇到无法安全保留的非线性 Lua 修正时保留原始后坐力并记录警告。

### 2.7 修复、再生与 LR Tactical

- **修复**：优先永久清除一个骨折；没有骨折时解锁一个黑肢，并额外清除一个区域疼痛。原文“修复直到效果结束”的歧义在 v2 中采用一次合法治疗永久修复的语义。
- **再生**：每次新的 `minecraft:regeneration` 应用最多解锁一个黑肢；同一持续实例不会每 tick 重复解锁。
- **LR blood pack**：在 LR 原有动画合法完成后的 `ConsumableUseEvent` 请求 `REPAIR`。
- **LR ibuprofen**：在合法完成事件后请求 `ANALGESIA`。

LR 会在事件前执行 `remove_effects: ["@harmful"]`，因此适配器从不把投影效果是否仍存在当作真源。事件完成后按 Lab truth 执行治疗，再重建仍应存在的权威投影；取消动画不会发送治疗请求。

## 3. 复用优先架构

| 通道 | 锁定接口/版本 | v2 行为 | 失败路径 |
| --- | --- | --- | --- |
| 视觉与心跳/耳鸣 | EnhancedVisuals 1.8.2 | 调用其 VisualManager、已有视觉和声音 | Lab v1 fallback |
| 疼痛/腿伤镜头 | Camera Overhaul 1.1-1.20.1 | 公开 camera transform callback | Forge camera fallback |
| 爆炸镇痛兼容 | Explosion Overhaul 0.2.3.0-forge | 版本隔离停止/清理 | 标记不可用，不操作第三方状态 |
| 枪械后坐力 | TaCZ 1.1.8-hotfix | integrated 直接 API 适配器 | 保留 TaCZ 原始后坐力 |
| 医疗物品/动画 | LR Tactical 0.4.1 | integrated 完成事件适配器 | LR 正常使用，Lab 不追加治疗 |

所有兼容代码都以外部依赖为边界：没有复制第三方 JAR 内 PNG、OGG、模型、动画、数据或反编译源码。F8 Provider 行区分适配器激活、fallback、未安装、版本不支持、适配器缺失/失败。

## 4. Core 与 Integrated

- `tarkov_health_fx_lab-v2-core.jar`：包含完整 Lab 规则、F8、命令、MobEffect、fallback 和三个版本隔离客户端桥；不编译 TaCZ/LR 直接适配类。
- `tarkov_health_fx_lab-v2-integrated.jar`：在 core 上增加 `TaCZClientCompat` 与 `LrTacticalCompat`。

完整集成的八项可选运行依赖固定为：EnhancedVisuals `1.8.2`、CreativeCore `2.12.35`、Camera Overhaul `1.1-1.20.1`、Explosion Overhaul `0.2.3.0-forge`、Cloth Config `11.1.136`、GeckoLib `4.8.3`、TaCZ `1.1.8-hotfix`、LR Tactical `0.4.1`。

这些依赖都不打入任一 Lab JAR，也不是 `mods.toml` 强制依赖。TarkovMod/PetiteInventory/MCEF 属于单独的 Tarkov live 开发对照 profile，不计入这八项。

## 5. 验收入口

F8 分为三页：

1. **伤情与场景**：七部位、HP/最大 HP、轻/重流血、疼痛、骨折、黑肢、Mock 与 Lab server 场景。
2. **治疗与头部转移**：修复、镇痛、再生、清除、转移开关、固定 seed 测试和最近记录。
3. **表现与无障碍**：屏幕效果、音频、减少动态、色散、心跳、高对比状态卡、总强度、镜头强度和独立耳鸣音量。

来源规则：Mock 只改本地；Lab server 通过权限等级 2 的 `/healthfx server` 写 Lab truth；Tarkov live 只读。场景按钮不会静默把 Lab/Tarkov 来源切回 Mock：Lab 场景生成确定的服务端命令序列，Tarkov live 场景按钮禁用。

完整命令表见项目 [`README.md`](../README.md)。

## 6. 验收状态

自动化已覆盖：

- 七部位状态、编解码、投影 ownership、治疗与再生边沿；
- 流血、移动惩罚、黑肢恢复锁、头部转移确定性；
- Lab 客户端映射、三来源轮换、场景命令计划和头部回执解析；
- EnhancedVisuals / Camera / Explosion 版本策略；
- TaCZ 后坐力纯算法、LR consumable 映射和 adapter availability 状态；
- 资源存在性与基础格式。

构建和剩余手工矩阵见 [`V2-VERIFICATION.md`](V2-VERIFICATION.md)。自动化通过不替代真实整合包中的视觉手感、TaCZ 附件/Lua、LR 动画取消和五个上层提供者逐项启停验收。

## 7. 后续移植要求

验收确认后，迁回 TarkovMod 的独立变更必须：

1. 定义一个 Tarkov authority adapter，将命令、MobEffect、治疗与表现读写映射到 Tarkov capability/健康服务。
2. 把真实伤害路由在护甲/命中部位确定后只提交一次，头部转移不得重复结算护甲、疼痛或流血概率。
3. 在 Tarkov authority 工作时禁用 `PlayerInjuryStore`、Lab 出血 tick 和重复 attribute 结算。
4. 保持 owner-only 同步、死亡/重生生命周期和 First Aid 冲突策略。
5. 以依赖/API 方式继续复用第三方资源，不把外部模组资源迁入 TarkovMod。

因此，v2 的完成含义是“第 7 条 standalone Lab 功能和集成适配已实现”，不是“真实 Tarkov 健康 authority 已完成移植”。
