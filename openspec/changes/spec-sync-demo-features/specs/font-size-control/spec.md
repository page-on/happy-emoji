## ADDED Requirements

### Requirement: 字号调节
系统 SHALL 在文字编辑界面提供字号滑块，允许用户自由调节文字显示大小。

#### Scenario: 默认字号
- **WHEN** 用户首次进入文字编辑界面
- **THEN** 字号滑块默认值为 36px

#### Scenario: 拖动滑块调节
- **WHEN** 用户拖动字号滑块（范围 16px ~ 80px）
- **THEN** 预览区文字大小即时更新
- **AND** 滑块中间标签显示当前 px 值

#### Scenario: 字号影响导出
- **WHEN** 用户设定字号 S 后导出 GIF 或图片
- **THEN** 导出文字大小按公式 `fontSize = S * (outputWidth / 420)` 等比缩放
- **AND** 输出在不同画质档位下文字比例保持一致
