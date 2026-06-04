## MODIFIED Requirements

### Requirement: 视频+文字合成GIF

系统 SHALL 将裁剪后的视频片段与用户添加的文字合成为一张 GIF 动图（视频模式）或一张 PNG 图片（照片模式）。

#### Scenario: 视频模式启动导出

- **WHEN** 用户在预览页面点击"导出"按钮且 mediaType 为 video
- **THEN** 系统在后台开始 GIF 合成处理
- **AND** 显示导出进度指示器

#### Scenario: 照片模式启动导出

- **WHEN** 用户在预览页面点击"导出"按钮且 mediaType 为 photo
- **THEN** 系统在后台开始 PNG 合成处理
- **AND** 显示导出进度指示器

#### Scenario: 导出成功（视频模式）

- **WHEN** GIF 合成完成
- **THEN** 系统将 GIF 文件保存到系统相册
- **AND** 显示"已保存到相册"提示

#### Scenario: 导出成功（照片模式）

- **WHEN** PNG 合成完成
- **THEN** 系统将 PNG 文件下载
- **AND** 预览区展示合成的图片

#### Scenario: 导出失败

- **WHEN** 合成过程中发生错误（如存储空间不足）
- **THEN** 系统显示错误提示"导出失败，请重试"
- **AND** 用户可重新点击导出按钮

#### Scenario: 导出过程中退出

- **WHEN** 用户在导出过程中按返回键
- **THEN** 系统弹出确认对话框"正在导出，确定要取消吗？"
- **AND** 用户确认后取消导出，已生成的临时文件被清理
