# 构建与资源验收记录

日期：2026-08-13

## v1 封存构建（2026-08-13）

在 Java 17（`C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`）下执行：

```powershell
.\gradlew.bat clean build --stacktrace
.\gradlew.bat compileJava -PwithTarkov=true --stacktrace
.\gradlew.bat dependencies --configuration runtimeClasspath -PwithTarkov=true
```

结果：三条命令均成功；JUnit 测试、PNG 透明度检查、OGG 容器检查通过。`withTarkov` profile 能解析 TarkovMod 5.0.0、PetiteInventory 1.0.8 和 MCEF 2.1.6 的本地依赖。正式 v1 JAR 已复制到 `archive/v1/`。

另生成 `archive/v1/TarkovHealthFxLab-v1-source.zip`，用于完整恢复 v1 的源码、资源和构建环境；校验值见 `archive/v1/SHA256SUMS.txt`。

## 客户端烟测

此前 v0.1.0 基线执行过 `runClient --no-daemon`：Forge 47.4.10 成功完成 GL 初始化、资源重载、OpenAL 初始化，并识别 `tarkov_health_fx_lab` 0.1.0；烟测进程按测试时限结束且没有发现该模组异常。v1 本次完成了干净构建、单元测试和桥接依赖解析，未在用户整合包中启动或覆盖客户端。首次启动产生的 `run/` 仅是开发运行目录，不属于发布包。

## 资源检查

- 三张叠层均为 1254×1254 RGBA；中心区域保持透明，边缘有可控 alpha。
- 六个音效均为单声道 48 kHz Ogg Vorbis；持续时间约 0.46–1.75 秒。
- 音频峰值保持在约 -6 到 -16 dBFS，并由独立 `effectVolume` 控制；每个事件都有字幕键。
- 资源由本项目原创生成/合成，没有复制 First Aid 的 PNG、OGG、GUI 或运行时代码。

## 发布 JAR

`build/libs/tarkov_health_fx_lab-v1.jar`

SHA-256：`38776E78FCB20D7B4C9CC85EA51F2A3F156A881A09B9A0F9560EBEAEF2B16D34`
