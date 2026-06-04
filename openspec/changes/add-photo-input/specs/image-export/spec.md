## ADDED Requirements

### Requirement: 照片+文字合成图片

系统 SHALL 将照片与用户添加的文字合成为一张 PNG 格式图片。

#### Scenario: 启动导出

- **WHEN** 用户在预览页面点击"导出图片"按钮且 mediaType 为 photo
- **THEN** 系统在后台开始合成处理
- **AND** 显示导出进度指示器

#### Scenario: 导出成功

- **WHEN** 图片合成完成
- **THEN** 系统将 PNG 文件下载（Demo 中模拟保存到相册）

#### Scenario: 导出失败

- **WHEN** 图片合成过程中发生错误
- **THEN** 系统显示错误提示"导出失败，请重试"

### Requirement: 图片输出规格

系统 SHALL 按以下规格输出图片文件。

#### Scenario: PNG格式与参数

- **WHEN** 系统合成图片
- **THEN** 输出格式为 PNG
- **AND** 输出分辨率宽度为 480 像素（等比缩放）
- **AND** 文字按照用户设定的位置、字体、颜色渲染在图片上
