## burst-photo-input (Removed)

合并入 `photo-input`。帧排序逻辑仅当 `count >= 2` 时激活。

- 缩略图网格：`THUMBNAIL_SIZE_DP` × `THUMBNAIL_SIZE_DP`
- 拖拽排序、删除、追加（上限 `BURST_MAX_COUNT`）
- 帧间隔滑块：`FRAME_DELAY_MIN_MS` ~ `FRAME_DELAY_MAX_MS`，默认 `FRAME_DELAY_DEFAULT_MS`
- 实时预估：XX 帧 / X.Xs / 约 XXKB
