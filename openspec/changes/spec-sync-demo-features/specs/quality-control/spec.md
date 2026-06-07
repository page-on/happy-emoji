## MODIFIED Requirements

### Requirement: 画质适用范围
系统 SHALL 将画质档位同时应用于视频 GIF 导出和照片 PNG 导出。

#### Scenario: 视频 GIF 导出
- **WHEN** 用户在视频模式下导出 GIF
- **THEN** GIF 的分辨率和编码质量按所选画质档位设定
- **AND** 画质选择器在导出预览界面可见

#### Scenario: 照片 PNG 导出
- **WHEN** 用户在照片模式下导出图片
- **THEN** PNG 的分辨率固定为标准 480px
- **AND** 画质选择器在照片模式下自动隐藏
