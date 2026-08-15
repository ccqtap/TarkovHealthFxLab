# Tarkov Health FX Lab v1 封存记录

封存日期：2026-08-13

## 封存范围

v1 锁定当前三张第一人称叠层、六个伤情音效、效果映射、镜头反馈、F8 实验室和 Mock/Tarkov live 桥接。后续对其他模组的调研只产生候选清单和预览，不会覆盖 v1 资源。

项目版本已设为 `v1`，发行元数据中的 `mod_version` 与 JAR 文件名均以此为准。

## v1 视觉资源校验

| 资源 | SHA-256 |
| --- | --- |
| `bleeding_border.png` | `01664C9CD71CAF9AFB3E5734950560F721AB231F0B7D10491908743CA5412919` |
| `fracture_stress.png` | `83A6DBBDFF36B09BC33CA06C1D36C9A32C02B9EF60ED4D8C2A5C858B0E9BC087` |
| `pain_periphery.png` | `2B86BA2BC1D79D533A7D2128E0CD45CBA95BC46E9FB6D2B1C0B06AFA5634D84` |
| `bleed_pulse.ogg` | `7B88E5E41CDD02589CBFAE6DF825047596A2444C54359A311F8C0C8CAAD44DCE` |
| `fracture_onset.ogg` | `2AD420FD1DA819F0492ADA9320DCF5CA94AE8F4F40B7BE2898142872D054E168` |
| `fracture_step.ogg` | `C51897470D3ACFA0E6CE8F014EF4F1E269FCA056F3096491EA92FE4E76156AB6` |
| `pain_breath.ogg` | `3DE779AB2E7B77FB6D85562473EF0A8095DCD41613701F50D69C454F5EC5153D` |
| `pain_sting.ogg` | `0079E1FE78DC04843A9BB14DABD8A1054D3BAC90E3635E30102AEBF909B5D8CD` |
| `relief.ogg` | `CFDA2C3ED90A4954860EE486DCC0F5774FC12E4AF812AD607CE0DBF7B8F57EE4` |

## 使用边界

- v1 不引入任何第三方模组资源。
- v1 不修改 `E:\Project\TarkovMod`。
- 后续如果采用第三方表现，必须先确认 Minecraft 版本、Forge 侧别、资源许可证、署名要求和是否允许再分发；未确认前只做外部预览或本地开发依赖，不打进 v1。

## 回滚基线

封存前的可执行基线保存在 `archive/v1/tarkov_health_fx_lab-0.1.0-pre-v1.jar`。

正式 v1 JAR：`archive/v1/tarkov_health_fx_lab-v1.jar`  
SHA-256：`38776E78FCB20D7B4C9CC85EA51F2A3F156A881A09B9A0F9560EBEAEF2B16D34`

完整源码封存：`archive/v1/TarkovHealthFxLab-v1-source.zip`。它包含 v1 源码、Gradle Wrapper、构建脚本、文档与资源生成工具，不包含 `.gradle/`、`build/` 或 `run/`。最终校验值记录在同目录 `SHA256SUMS.txt`。
