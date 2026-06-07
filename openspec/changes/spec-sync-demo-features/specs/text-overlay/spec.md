## MODIFIED Requirements

### Requirement: 文字叠加显示
系统 SHALL 在预览区和导出的画面上按用户设定渲染文字。

#### Scenario: 预览区文字显示
- **WHEN** 用户在文字编辑界面输入文字内容
- **THEN** 预览区显示文字，字体大小由字号滑块决定（16-80px）

#### Scenario: 导出文字大小联动
- **WHEN** 用户设定字号 S 后导出
- **THEN** 导出画面文字大小 = S * (outputWidth / 420)
- **AND** 文字位置、字体、颜色、粗细按各自 state 字段设定
