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

#### Scenario: 导出受倍速影响
- **WHEN** 用户在视频模式下设定倍速 S 后导出
- **THEN** 帧时间间隔按 t = trimStart + i/(8*S) 计算
- **AND** 总帧数 = min(ceil((trimEnd - trimStart) * 8 * S), 60)

#### Scenario: 导出受画质影响
- **WHEN** 用户设定画质档位后导出
- **THEN** GIF 输出宽度和 quality 参数按档位映射表设定
- **AND** PNG 输出宽度同样按档位映射

#### Scenario: 帧数截断提示
- **WHEN** 理论帧数超过 60 帧上限
- **THEN** 导出前截断至 60 帧
- **AND** 预览区显示截断提示

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

#### Scenario: 导出超时
- **WHEN** 帧提取或编码超过超时限制
- **THEN** 系统显示具体超时原因（帧提取/编码超时）
- **AND** 用户可返回修改参数后重试
