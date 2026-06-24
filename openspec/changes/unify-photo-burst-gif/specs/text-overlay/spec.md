## text-overlay (Modified)

### 自适应模式

- count == 1：基础文字编辑（无照片导航器，无继承规则）
- count ≥ 2：逐张配文（← N/M → 导航器 + 向前查找继承规则）

### 不变项

- 文字控件（输入 ≤ `TEXT_MAX_LENGTH` 字、字体 `FONT_LIST`、颜色 `COLOR_PRESETS`、字号 `FONT_SIZE_MIN_PX`~`FONT_SIZE_MAX_PX`、拖拽+双击居中）
- 导出渲染公式：`exportSize = textSize × (outputWidth / BASE_CANVAS_WIDTH_PX)`
