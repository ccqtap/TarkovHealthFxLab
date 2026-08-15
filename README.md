# Tarkov Health FX Lab v2

`TarkovHealthFxLab` 是一个独立的 Minecraft Forge 1.20.1 双端验收模组。v2 已实现文档第 7 条所需的实验室版本：七部位血量真源、轻/重流血、疼痛、骨折、四类黑肢、修复、镇痛、原版再生解锁、头部伤害转移测试、MobEffect 投影、F8 验收入口，以及可选的视觉、镜头、爆炸、TaCZ 和 LR Tactical 兼容。

它默认不依赖、也不会修改 `E:\Project\TarkovMod`。当前实现用于独立验证规则和表现，不应被理解为已经接管 TarkovMod 的真实受伤管线。详细规格见 [`docs/V2-DOCUMENT-ITEM-7-SPEC.md`](docs/V2-DOCUMENT-ITEM-7-SPEC.md)，构建与验证记录见 [`docs/V2-VERIFICATION.md`](docs/V2-VERIFICATION.md)。

## 发布形态

v2 定义两种 JAR：

| JAR | 内容 | 使用场景 |
| --- | --- | --- |
| `tarkov_health_fx_lab-v2-core.jar` | 实验室健康规则、网络同步、F8、命令、MobEffect、内置 v1 fallback，以及 EnhancedVisuals / Camera Overhaul / Explosion Overhaul 的版本隔离桥 | 不安装第三方模组也能运行；适合规则、命令和 fallback 验收 |
| `tarkov_health_fx_lab-v2-integrated.jar` | core 的全部内容，再加 TaCZ 1.1.8 与 LR Tactical 0.4.1 的直接 API 适配器 | 使用完整八项外部运行依赖验收后坐力和医疗动画完成事件 |

两种 JAR 都不内嵌第三方 JAR、贴图、模型、动画、声音或数据。`integrated` 只是多编译两个很小的 API 适配器；依赖仍由游戏实例外部提供。所有第三方依赖在 `mods.toml` 中都不是强制项，缺失或版本不支持时应安全降级，并在 F8 Provider 行显示原因。

## 完整集成所用的八项可选运行依赖

下列版本是 v2 完整集成验收的精确基线，不包含 Forge、Minecraft，也不包含单独的 Tarkov live 开发对照环境：

1. EnhancedVisuals `1.8.2`
2. CreativeCore `2.12.35`
3. Camera Overhaul `1.1-1.20.1`
4. Explosion Overhaul `0.2.3.0-forge`
5. Cloth Config `11.1.136`
6. GeckoLib `4.8.3`
7. TaCZ `1.1.8-hotfix`
8. LR Tactical `0.4.1`

对应当前稳定副本目录中的文件名见 [`docs/V2-VERIFICATION.md`](docs/V2-VERIFICATION.md)。其中 CreativeCore、Cloth Config、GeckoLib 是上层模组的运行依赖；实验室本身不会直接复制或调用它们的资源。

## 构建

使用 Java 17：

```powershell
Set-Location 'E:\Project\TarkovHealthFxLab'
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'

# 生成 build/libs/tarkov_health_fx_lab-v2-core.jar
.\gradlew.bat clean test build

# 不再次 clean，可在同一 build/libs 中生成 integrated JAR
.\gradlew.bat test build `
  -PwithIntegrations=true `
  -PintegrationModsDir='E:/ALLPCL/pcl/.minecraft/versions/wrn165/mods'

# 用锁定的八项依赖启动真实 Forge 开发客户端
.\gradlew.bat runClient `
  -PwithIntegrations=true `
  -PintegrationModsDir='E:/ALLPCL/pcl/.minecraft/versions/wrn165/mods'
```

`mods.toml` 中使用 Forge 可接受的语义版本 `2.0.0`，成品文件继续使用更直观的 `v2` 名称。`integrationModsDir` 在编译期提供 TaCZ/LR API，并在 `runClient` 时把八项生产 JAR 映射到开发命名；这些依赖均为 `compileOnly`/`runtimeOnly`，不会被打进成品。

## F8 验收入口

进入世界后按 `F8`。面板分三页：

1. **伤情与场景**：选择七个部位，查看当前/最大 HP，调节轻/重流血、疼痛、骨折、黑肢，并运行单项或第 7 条组合场景。
2. **治疗与头部转移**：执行修复、镇痛、再生、清除伤情，切换头部转移开关，运行固定种子测试并显示最近一次转移记录。
3. **表现与无障碍**：控制屏幕效果、音频、减少动态、色散、心跳、高对比状态卡、总强度、镜头强度和独立耳鸣音量。

HUD 状态卡使用独立 18×18 PNG 图标。流血、骨折、疼痛、止痛均有对应图标，左/右臂和左/右腿黑肢会显示不同图标及部位文字。

三种来源的写入边界不同：

- `Mock`：只改客户端内存，不写服务器、玩家存档或能力。
- `Lab server`：读取 v2 实验室自己的七部位服务端真源；F8 写操作通过独立的 `/healthfx_server ...` 执行，需要权限等级 2。场景先清理实验室伤情再写入确定的服务端状态。
- `Tarkov live`：通过反射读取 TarkovMod 已同步到客户端的健康快照，只读对照；F8 不会借此写入 Tarkov capability。

Provider 行会显示 EnhancedVisuals、Camera Overhaul、Explosion Overhaul、TaCZ、LR Tactical 当前是适配器激活、fallback、未安装、版本不支持，还是适配器缺失/失败。

## 命令

客户端入口保留旧别名，并提供明确的 v2 命名空间：

```text
/healthfx client ui
/healthfx client source mock|lab|tarkov
/healthfx client part head|thorax|stomach|left_arm|right_arm|left_leg|right_leg
/healthfx client bleed none|light|heavy
/healthfx client fracture true|false
/healthfx client blackened true|false
/healthfx client pain 0..100
/healthfx client hp 0..100
/healthfx client preset <name>   # 明确切到 Mock
/healthfx client scene <name>    # 按当前来源应用；Tarkov live 拒绝写入
```

服务端命令需要权限等级 2。F8 使用不会与客户端命令冲突的 `/healthfx_server` 根；旧的 `/healthfx server ...` 仍作为手动兼容别名：

```text
/healthfx_server status [target]
/healthfx_server clear <target>
/healthfx_server set part_hp <target> <part> <value>
/healthfx_server set max_hp <target> <part> <value>
/healthfx_server set bleeding <target> <part> none|light|heavy
/healthfx_server set pain <target> <part> <value>
/healthfx_server set fracture <target> <limb> true|false
/healthfx_server set blackened <target> <limb> true|false
/healthfx_server damage <target> <part> <amount> [seed]
/healthfx_server repair <target>
/healthfx_server analgesia on <target> <seconds>
/healthfx_server analgesia off <target>
/healthfx_server regeneration <target> <seconds>
/healthfx_server head_redirect status|on|off
/healthfx_server head_redirect test <damage> [seed]
```

伤情还注册为 `tarkov_health_fx_lab:*` MobEffect，供 `/effect` 和命令方块验证。外部有时限的效果会写入实验室真源；由真源生成的隐藏投影不会重复扣血或重复治疗。原版 `minecraft:regeneration` 可按一次效果应用解锁一个黑肢。

## 已实现的可选复用

- EnhancedVisuals：复用其受伤、低血、隧道、裂纹、心跳和耳鸣资源；桥失败时使用实验室内置 fallback。
- Camera Overhaul：通过公开相机变换回调叠加低幅疼痛和腿伤步态；失败时使用 Forge fallback。
- Explosion Overhaul：仅对锁定版本调用停止/清理接口，在镇痛期间抑制已排队的灰屏、脑震荡晃动和相关音频。
- TaCZ：`integrated` 适配器在附件属性缓存重建时动态调整受伤手臂的水平/垂直后坐力，不写枪械 NBT 或枪包 JSON。
- LR Tactical：`integrated` 适配器只在 `blood_pack` / `ibuprofen` 的原有使用动画合法完成事件后申请修复/镇痛。

候选来源、许可证和调用边界见 [`docs/REUSE-CANDIDATES.md`](docs/REUSE-CANDIDATES.md) 与 [`docs/V2-TACZ-LR-COMPAT.md`](docs/V2-TACZ-LR-COMPAT.md)。

## v1 封存与资源边界

v1 已完整封存在 `archive/v1/`，包含只读 JAR、源码 ZIP 和 SHA-256 清单；v2 不覆盖、删除或重写这些文件。v2 仍保留 v1 的实验室原创 fallback 贴图和确定性合成音频，以便第三方依赖缺失时验收。

项目没有从 First Aid、EnhancedVisuals、Camera Overhaul、Explosion Overhaul、TaCZ 或 LR Tactical 提取或再分发 PNG、OGG、模型、动画、配置、JAR 或反编译实现。第三方效果全部来自用户游戏实例中的原模组，并通过公开 API、事件或版本隔离反射调用。

## Standalone Lab 边界

当前 v2 的服务端权威是实验室自己的 `PlayerInjuryStore`，只覆盖实验室命令、MobEffect、流血 tick、移动惩罚、治疗和同步。它没有接入 TarkovMod 的真实子弹/近战/爆炸伤害路由，也不会从 F8 或 `/healthfx server` 写入 Tarkov capability。

迁回 TarkovMod 时必须增加一个**单一权威适配器**：把命令、效果、治疗和表现读取统一连接到 Tarkov 的 capability/健康服务，并在 Tarkov authority 激活时停用实验室存储与重复结算。真实 Tarkov damage 写入、护甲后部位判定、死亡生命周期和 owner-only 同步的最终接线属于后续移植，不在这个 standalone Lab 成品中。
