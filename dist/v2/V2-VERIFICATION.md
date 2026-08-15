# Tarkov Health FX Lab v2 验证记录

记录日期：2026-08-14  
结论：**standalone Lab 功能、两种构建 profile 与完整八依赖客户端启动冒烟均已通过；进入世界后的完整组合回归仍需按下方手工矩阵执行。**

## 1. 验证范围

本记录验证的是 `E:\Project\TarkovHealthFxLab` 自身：

- 七部位 Lab truth、MobEffect、流血 tick、移动惩罚、治疗和同步；
- F8 三页、Mock / Lab server / Tarkov live 来源边界；
- EnhancedVisuals、Camera Overhaul、Explosion Overhaul 的版本隔离桥；
- integrated profile 中的 TaCZ 与 LR Tactical 直接 API 适配器；
- v1 封存和第三方资源未复制边界。

它不证明真实 Tarkov damage/authority 已接线。`Tarkov live` 是只读对照，`/healthfx server` 写入的是 Lab 自己的 `PlayerInjuryStore`。

## 2. 构建产物

| Profile | 产物名 | 已验证内容 |
| --- | --- | --- |
| 默认 | `tarkov_health_fx_lab-v2-core.jar` | 不需要第三方编译 JAR；完整 Lab 规则、F8、命令、fallback 和反射/回调桥可编译、测试、重混淆 |
| `-PwithIntegrations=true` | `tarkov_health_fx_lab-v2-integrated.jar` | 在 core 上增加 `TaCZClientCompat.class` 与 `LrTacticalCompat.class`；使用锁定 TaCZ/LR JAR 完成编译、测试、重混淆 |

`build/libs` 是工作目录；执行 `clean` 会删除上一次 classifier，发布时应按顺序构建并把两个产物复制到发布目录，或在第二次构建前不要再次 `clean`。

复现命令：

```powershell
Set-Location 'E:\Project\TarkovHealthFxLab'
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'

.\gradlew.bat clean test build
.\gradlew.bat test build `
  -PwithIntegrations=true `
  -PintegrationModsDir='E:/ALLPCL/pcl/.minecraft/versions/wrn165/mods'
```

最近一次测试报告包含 `21` 个 test suite、`57` 个测试，`0` failure、`0` error、`0` skipped。core 和 integrated 构建均已到达 `BUILD SUCCESSFUL`。编译仅有 Forge/Minecraft 弃用 API 警告，没有测试失败。模组元数据版本为合法语义版本 `2.0.0`，文件名保留 `v2`。当前成品校验值：

- core：`95CADA9D27F2EB633B23589F97C75B2F4B8BF022E1F93C9515514F9C1CB7A618`
- integrated：`364A62D8F9182008D920704866E3D334834A953E3B2FF2C016384BC123C4C843`

## 3. 精确的八项可选运行依赖

完整集成验收使用 `E:\ALLPCL\pcl\.minecraft\versions\wrn165\mods` 中下列八个文件：

| 模组 | 锁定版本 | 当前文件名 |
| --- | --- | --- |
| EnhancedVisuals | 1.8.2 | `[增强视觉效果／拓展视觉效果] EnhancedVisuals_FORGE_v1.8.2_mc1.20.1.jar` |
| CreativeCore | 2.12.35 | `CreativeCore_FORGE_v2.12.35_mc1.20.1.jar` |
| Camera Overhaul | 1.1-1.20.1 | `cameraoverhaul-1.1-1.20.1.jar` |
| Explosion Overhaul | 0.2.3.0-forge | `Explosion-Overhaul-0.2.3.0-forge-.jar` |
| Cloth Config | 11.1.136 | `cloth-config-11.1.136-forge.jar` |
| GeckoLib | 4.8.3 | `geckolib-forge-1.20.1-4.8.3.jar` |
| TaCZ | 1.1.8-hotfix | `tacz-1.20.1-1.1.8-hotfix.jar` |
| LR Tactical | 0.4.1 | `[TaCZ：绿葡萄战术装备] lrtactical-1.20.1-0.4.1.jar` |

Forge 与 Minecraft 是基础运行环境，不计入八项。TarkovMod、PetiteInventory、MCEF 是单独的 Tarkov live 开发对照 profile，也不计入这八项。Curios 与 TaCZ Attributes 不是 v2 锁定依赖。

### 3.1 真实客户端启动冒烟

2026-08-14 使用上述八个生产 JAR、Forge 47.4.10、Java 17 执行 `runClient`。生产 JAR 经 ForgeGradle 和 Mixin refmap 映射后成功进入标题/首次引导界面；日志确认以下五条 Lab provider 桥均为 active：

- EnhancedVisuals 1.8
- Camera Overhaul 1.1
- Explosion Overhaul 0.2.3
- TaCZ 1.1.8 injury recoil
- LR Tactical 0.4.1 medical compatibility

启动时还记录到 Explosion Overhaul 自身缺少部分 glow/soft_glow 粒子贴图、blur shader uniform 警告，以及 GeckoLib 对 LR `melee_3` 动画的解析错误。这些来自未修改的外部 JAR，不是 Lab 打包内容；不阻断五条桥激活，但仍应在下方游戏内矩阵中观察其实际影响。

## 4. 自动化验证覆盖

### 健康规则

- [x] 七部位默认/最大/当前 HP 与 NBT 编解码往返。
- [x] 轻/重流血独立部位真值与每 tick 结算策略。
- [x] 任意伤情保证最低疼痛，镇痛不删除原始疼痛。
- [x] 骨折、四类黑肢、普通治疗锁、修复和再生解锁。
- [x] 腿伤移动 modifier 数值、镇痛缩放、上限和移除。
- [x] 头部转移固定 seed 的确定性、非头部不转移和记录字段。

### MobEffect 与同步

- [x] 外部有时限效果写 truth，权威隐藏投影带 ownership。
- [x] 外部效果到期/清除不会与权威投影形成每 tick 拉锯。
- [x] LR 的 `@harmful` 广泛移除后可按 truth 重投影。
- [x] repair/regeneration 同一实例只消费一次，重新应用可以重新触发。
- [x] owner 客户端快照包含七部位 HP/最大 HP/流血/疼痛/骨折/黑肢和治疗标志。

### 客户端验收入口

- [x] `ClientInjuryState` 七部位 truth 映射到 `HealthFxState`。
- [x] Mock → Lab server → Tarkov live → Mock 安全轮换。
- [x] Mock 普通 HP 操作不会偷偷清除黑肢；四肢调到 0 时显式黑化，躯干不会黑化。
- [x] Lab server 场景生成确定命令序列；Tarkov live 不接受场景写入。
- [x] 头部转移 status/test 服务端回执解析。
- [x] 黑肢、骨折、流血、疼痛和镇痛状态卡保留文字/形状通道。

### 可选兼容

- [x] EnhancedVisuals、Camera Overhaul、Explosion Overhaul 版本选择策略和未知版本安全降级。
- [x] Provider 状态区分 active、fallback、not installed、unsupported、adapter absent/failed。
- [x] TaCZ 骨折臂/黑臂计数互斥，水平/垂直倍率、镇痛超额缩放和上限。
- [x] TaCZ 线性缓存重建纯算法保留基础/附件修正，非线性输入降级。
- [x] LR 只识别 `lrtactical:blood_pack` 与 `lrtactical:ibuprofen`，完成请求映射正确。
- [x] integrated JAR 包含两个 Lab 适配类，但没有打包 `com.tacz.*`、`me.xjqsh.lrtactical.*` 第三方类树或第三方资源目录。

## 5. F8 与命令核对

F8 已实现三页：

1. 伤情与场景：部位、HP/最大 HP、流血、疼痛、骨折、黑肢、九个场景。
2. 治疗与头部转移：修复、镇痛、再生、清除、开关、固定 seed 测试、最近记录。
3. 表现与无障碍：视觉、音频、减少动态、色散、心跳、高对比状态卡、总强度、镜头强度。

命令树已包含 `/healthfx client ...` 与 `/healthfx server ...`。服务端写操作要求权限等级 2；F8 Lab server 写操作使用同一命令树，不另开未授权网络写入口。完整语法见 [`../README.md`](../README.md)。

## 6. 资源与 v1 封存核对

- [x] `archive/v1/` 未被 v2 构建覆盖。
- [x] v1 JAR SHA-256：`38776E78FCB20D7B4C9CC85EA51F2A3F156A881A09B9A0F9560EBEAEF2B16D34`。
- [x] v1 source ZIP SHA-256：`7522CC3BB68743431ED2E14D00901A34C57B7658DABC87E132CBBC56E112765A`。
- [x] v2 自带的 fallback PNG/OGG 是 Lab 原有原创/确定性合成资源。
- [x] 没有从 First Aid 或八项可选依赖复制 PNG、OGG、模型、动画、配置、JAR 或反编译源码。
- [x] integrated 只增加 Lab 自己编写的 API adapter class，第三方资源仍由外部模组提供。

详细来源边界见 [`asset-credits.md`](asset-credits.md)、[`REUSE-CANDIDATES.md`](REUSE-CANDIDATES.md) 和 [`V1-FROZEN.md`](V1-FROZEN.md)。

## 7. 仍需游戏内手工验收

以下项目不能由当前单元测试或一次编译替代，发布/移植前仍需逐项勾选：

- [x] 完整八依赖客户端通过 Forge/Mixin 加载并进入标题/首次引导界面，五条 Lab provider 桥均在日志中报告 `active`。
- [ ] 使用完整八依赖进入世界，确认 F8 Provider 五个上层提供者均显示 `active`，而不是 fallback/failed。
- [ ] EnhancedVisuals 轻/重流血、疼痛、裂纹、心跳和耳鸣强度符合最终手感，且不会与 fallback 双重绘制/播放。
- [ ] Camera Overhaul 回调和 Reduce motion/OFF 在第一、第三人称切换后无残留。
- [ ] Explosion Overhaul 在镇痛前后验证灰屏、晃动、耳鸣/低通；特别检查爆炸与镇痛同 tick 的首帧竞争。
- [ ] TaCZ 装/拆制退器、握把，切枪，死亡/重生，治疗和镇痛开始/结束后，倍率正确且无缓存残留。
- [ ] 特殊非线性 Lua 后坐力脚本触发降级时，枪械保持原始行为。
- [ ] LR 动画取消不治疗；合法完成只治疗一次；`@harmful` 移除后骨折/黑肢 truth 不丢失。
- [ ] 单肢、双肢、四类黑肢、骨折+流血叠加、repair、regeneration 和 analgesia 场景逐项验收。
- [ ] 服务端无权限玩家无法通过 F8 或命令修改 Lab truth。
- [ ] dedicated server 启动与双客户端 owner-only 同步回归。

用户此前确认的是候选模组各自的表现方向；这不自动等价于上述最终组合回归已经全部通过。

## 8. 后续 Tarkov 移植阻断项

在 standalone Lab 验收完成后，仍必须单独实现并验证：

1. Tarkov 实际 damage router → 单一 authority adapter → Tarkov capability 的一次性写入。
2. Lab 命令/MobEffect/治疗入口改为调用该 adapter，不再并行维护 `PlayerInjuryStore`。
3. Tarkov authority 激活时停用 Lab 流血 tick、移动 modifier 和重复投影源。
4. 护甲后命中部位、死亡/重生、不死图腾、owner-only 网络同步和 First Aid 冲突策略回归。

这些项目属于后续移植，不应在 v2 standalone Lab 的完成说明中写成已完成。
