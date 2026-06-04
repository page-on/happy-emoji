## ADDED Requirements

### Requirement: 视频+文字合成GIF

系统 SHALL 将裁剪后的视频片段与用户添加的文字合成为一张GIF动图。

#### Scenario: 启动导出

- **WHEN** 用户在预览页面点击"导出GIF"按钮
- **THEN** 系统在后台开始合成处理
- **AND** 显示导出进度指示器（百分比或进度条）

#### Scenario: 导出成功

- **WHEN** GIF合成完成
- **THEN** 系统将GIF文件保存到系统相册
- **AND** 显示"已保存到相册"提示

#### Scenario: 导出失败

- **WHEN** GIF合成过程中发生错误（如存储空间不足）
- **THEN** 系统显示错误提示"导出失败，请重试"
- **AND** 用户可重新点击导出按钮

#### Scenario: 导出过程中退出

- **WHEN** 用户在导出过程中按返回键
- **THEN** 系统弹出确认对话框"正在导出，确定要取消吗？"
- **AND** 用户确认后取消导出，已生成的临时文件被清理

### Requirement: GIF输出规格

系统 SHALL 按以下规格输出GIF文件。

#### Scenario: GIF格式与参数

- **WHEN** 系统合成GIF
- **THEN** 输出格式为 GIF89a
- **AND** 帧率不低于 8fps，不高于 10fps
- **AND** 输出分辨率宽度不超过480像素（等比缩放）
- **AND** 不包含音频

### Requirement: 保存到系统相册

系统 SHALL 将生成的GIF文件保存到系统相册目录。

#### Scenario: 保存成功

- **WHEN** GIF文件成功写入MediaStore
- **THEN** 用户在系统相册应用的"GIF"或"图片"分类中可以看到该文件

#### Scenario: 存储空间不足

- **WHEN** 设备存储空间不足以保存GIF
- **THEN** 系统在导出前检测并提示"存储空间不足，请清理后重试"
