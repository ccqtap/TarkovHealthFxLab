# v2 可复用表现候选

本表是只读调研结果，供在实现 v2 前确认组合。没有下载、修改或复制任何第三方资源；当前实验室仍只包含 v1 自有资源。

## 推荐组合

| 候选 | 当前整合包 | 可复用内容 | 许可证/边界 | 预览与一手资料 |
| --- | --- | --- | --- | --- |
| EnhancedVisuals 1.8.2 | 已安装 | 受伤/低血/裂纹/隧道叠层，心跳与耳鸣；通过 `VisualManager` / `VisualHandler` API 调用现成资源 | 本地 JAR `META-INF/mods.toml` 为 `LGPL-3.0-only`；以随包许可证为准，不复制 PNG/OGG | [Modrinth 项目](https://modrinth.com/mod/enhancedvisuals)、[官方源码](https://github.com/CreativeMD/EnhancedVisuals/tree/1.20)、[官方 Gallery](https://www.curseforge.com/minecraft/mc-mods/enhancedvisuals/gallery)、[官方视频](https://www.youtube.com/watch?v=2GB3QhtXb4I) |
| Camera Overhaul Forge 1.1 | 已安装 | 通过 `CameraUpdateCallback` / `ModifyCameraTransformCallback` 叠加疼痛抖动和腿部步态 bob | 当前整合包旧 Forge JAR/本地源码标 MIT；只接公开 callback，不重写 GameRenderer，不引入仓库后来版本的实现 | [Forge 源码分支](https://github.com/AvacadoWizard120/CameraOverhaulForge/tree/Forge-1.20-1.20.4)、[项目页](https://www.curseforge.com/minecraft/mc-mods/cameraoverhaul) |
| TaCZ 原生属性/事件 | 已安装 TaCZ 1.1.8 | `AttachmentPropertyEvent`、`GunProperties.RECOIL`、缓存属性流水线；可按骨折状态动态调整水平/垂直后坐力 | TaCZ 本体随包为 GPL3 / CC BY-NC-ND 4.0；只编译可选 API 或反射桥，不复制代码/资源 | [TaCZ Additions 效果对照页](https://www.curseforge.com/minecraft/mc-mods/tacz-additions) |
| TaCZ Attributes 1.4（版本待锁定） | 未安装，候选新增依赖 | 将通用/垂直/水平 recoil 变成原生属性，可由 MobEffect/命令/装备修饰；实现骨折倍率最干净 | MIT；需先在当前 TaCZ 1.1.8 整合包做兼容验证，验证通过后才锁定具体版本 | [官方 CurseForge](https://www.curseforge.com/minecraft/mc-mods/tacz-attributes)、[官方源码/README](https://github.com/leopoko/TaCZ_Attributes)、[官方视频](https://www.youtube.com/watch?v=pJjKRPxrQa8) |
| LR Tactical 0.4.1 | 已安装 | `blood_pack` / `ibuprofen` 等 consumable、第一人称使用动画、`ConsumableUseEvent`；由实验室在事件后授予修复/镇痛 | 本地 JAR 声明 GNU GPL 3.0，CurseForge 文件页标 Custom License；只作为运行时依赖并调用事件/API，不复制动画、模型或贴图。其 `@harmful` 移除规则需与 Tarkov 状态投影隔离 | [官方文件页](https://www.curseforge.com/minecraft/mc-mods/tacz-lesraisins-tactical-equipements/files/8087337)、[项目页](https://www.curseforge.com/minecraft/mc-mods/tacz-lesraisins-tactical-equipements) |

## 只做兼容、不能当作资源库

### Explosion Overhaul 0.2.3.0

这是文档截图对应的 Vinlanx 版本，当前整合包已安装。官方描述包含爆炸后脑震荡、灰屏/模糊、镜头失稳、耳鸣和心跳，正好可作为镇痛抑制的验收对象。其 1.20.1 Forge 文件和 Custom License 见[官方项目页](https://www.curseforge.com/minecraft/mc-mods/explosion-overhaul-a-new-level-of-destruction)。本地 JAR 的 `LICENSE.txt` 是 All Rights Reserved：只允许按原模组规则使用，不复制、修改后再分发音频、贴图、shader 或代码。

v2 只建立可选兼容桥：检测 `ModList`，在镇痛期间调用其公开停止入口（灰屏、镜头抖动、耳鸣/低通），并对版本变化安全降级。它没有通用的“阻止未来触发”API，因此需要测试爆炸与镇痛同 tick 的首帧竞争；不在未获授权时做内部 Mixin。

### TaCZ Additions 1.0.5-hotfix2

它的官方页面明确展示水平/垂直 recoil multiplier、visual recoil、枪械移动和倾斜，适合让验收人员先看“目标手感”。但本地 JAR 与项目页标记 ARR；官方源码仓库的许可证文本与项目元数据并不完全一致。故只提供[官方项目页](https://www.curseforge.com/minecraft/mc-mods/tacz-additions)作对照，不链接其内部类，不复制其实现，也不把它打进实验室。

## v2 实施顺序

1. 先用当前整合包中的 EnhancedVisuals、Camera Overhaul、TaCZ、LR Tactical 做外部预览和 API 探针。
2. 用户确认视觉组合后，优先实现可选依赖路径；依赖缺失时保留稳定图标、文字、字幕和基础 fallback。
3. 后坐力先比较 TaCZ 原生缓存属性与 TaCZ Attributes；只有前者无法稳定按伤情更新时，才引入后者。
4. Explosion Overhaul 永远只走兼容桥；任何资源复用都需要作者明确许可。
5. 所有新实现放在 v2 分支/版本，`archive/v1/` 与 v1 JAR 保持不变。
