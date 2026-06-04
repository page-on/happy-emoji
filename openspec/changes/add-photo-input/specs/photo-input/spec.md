## ADDED Requirements

### Requirement: 从系统相册选取照片

系统 SHALL 允许用户从系统相册中选取一张照片，作为表情包制作的素材来源。

#### Scenario: 用户成功选取照片

- **WHEN** 用户点击"选取照片"按钮
- **THEN** 系统打开系统相册选择器，仅显示图片文件（jpg/png/webp）
- **AND** 用户选择一张照片后，系统将 mediaType 设为 photo
- **AND** 导航跳过裁剪步骤，直接进入文字编辑页面

#### Scenario: 用户取消选取

- **WHEN** 用户在相册选择器中按返回或取消
- **THEN** 系统返回素材选取页面，不做任何处理

#### Scenario: 照片格式校验

- **WHEN** 用户选取的照片格式为 jpg、png 或 webp
- **THEN** 系统正常加载预览
- **AND** 不支持的格式提示"不支持的图片格式"

### Requirement: 照片模式预览

系统 SHALL 在文字编辑和导出页面使用图片方式预览照片素材。

#### Scenario: 照片预览显示

- **WHEN** 媒体类型为 photo 时进入文字编辑页面
- **THEN** 预览区显示静态照片（使用 img 标签）
- **AND** 文字叠加层正常工作
