## Why

当前 GIF 制作参数全部固定（8fps、3s 上限、480p、固定画质），用户无法根据场景调节输出效果。同一段视频有人想要加速鬼畜效果，有人想要慢动作细节，有人需要小文件快速分享，有人需要高清收藏。增加倍速、画质和时长控制，让用户拥有更多创作自由度。

## What Changes

- 新增视频倍速选择：1/4x、1/2x、1x（原速）、1.5x、2x，控制 GIF 帧提取速度
- 放宽视频限制：源视频最大 30 秒，选段最大 10 秒
- 新增最大帧数上限 60 帧，防止文件过大和编码超时
- 新增画质档位选择：流畅（360px/低画质）、标准（480px/中画质）、高清（640px/高画质）
- 画质选择同时适用于视频 GIF 和照片 PNG 导出

## Capabilities

### New Capabilities

- `speed-control`: 视频倍速控制，在裁剪界面提供 5 档速度选择，影响 GIF 帧提取的时间间隔和最终播放速度
- `quality-control`: 输出画质控制，提供 3 档画质（流畅/标准/高清），同时影响分辨率和编码质量，适用于 GIF 和 PNG 导出

### Modified Capabilities

- `video-selection`: 增加源视频时长限制（最大 30 秒），超过时给出提示
- `video-trimming`: 选段上限从 3 秒放宽到 10 秒；裁剪界面新增倍速选择器
- `gif-export`: 帧数上限改为 60 帧；帧提取受倍速参数影响；画质受 quality-control 影响
- `image-export`: 输出画质受 quality-control 影响

## Impact

- Step 1（裁剪界面）：新增倍速选择器 UI，选段上限从 3s → 10s
- Step 0（素材选取）：文件选择后校验视频时长 ≤ 30s
- Step 3/4（导出）：导出参数从全局 state 读取 speed、quality，替换硬编码值
- state 对象：新增 `speed`（倍速）、`qualityLevel`（画质档位）、`maxFrames`（60）字段
