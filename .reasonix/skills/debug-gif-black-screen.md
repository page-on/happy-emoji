---
name: debug-gif-black-screen
description: 诊断 GIF 导出画面不完整/黑屏/乱码问题 — 覆盖帧提取、抖动、LZW 编码位宽三个环节的排查流程
---

# 诊断 GIF 导出黑屏 / 画面不完整

> 适用场景：APP 导出的 GIF 文件只有顶部一小截有画面，其余大面积黑色、乱码，或只显示几帧就中断。

---

## 第一步：排除帧提取问题

在编码入口处打印诊断日志，检查帧 Bitmap 底部像素是否正常。

**Kotlin 诊断代码：**

```kotlin
// 在 ExportEngine 导出循环前插入
val firstFrame = renderedFrames.firstOrNull()
if (firstFrame != null) {
    val fw = firstFrame.width; val fh = firstFrame.height
    val diagPixels = IntArray(fw * fh)
    firstFrame.getPixels(diagPixels, 0, fw, 0, 0, fw, fh)

    fun rowStats(label: String, rowIdx: Int) {
        val offset = rowIdx * fw
        var nonBlack = 0
        for (c in 0 until fw) {
            val p = diagPixels[offset + c]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (r > 5 || g > 5 || b > 5) nonBlack++
        }
        Log.i("[DIAG]", "$label row=$rowIdx nonBlack=$nonBlack/$fw")
    }
    rowStats("TOP", 0)
    rowStats("MID", fh / 2)
    rowStats("BOT", fh - 1)
}
```

**判定标准**：顶部、中部、底部三行的 nonBlack 都接近宽度 → 帧提取正常，问题在编码环节。

---

## 第二步：检查 GIF 二进制结构

将导出的 GIF 文件拉到电脑，用 hex dump 检查：

```powershell
$bytes = [IO.File]::ReadAllBytes("path/to/file.gif")
# 检查每个帧的 LZW 数据量
```

**关键判断**：
- 找到 `0x2C`（Image Descriptor），检查后续 LZW 数据字节数
- 正常：480×853 一帧的 LZW 数据约 50KB+
- 异常：只有几百或几千字节 → LZW 编码提前终止

---

## 第三步：排查抖动实现（Floyd-Steinberg）

如果 LZW 数据量正常但画面发黑，检查抖动代码。

### Bug 模式 A：误差用 packed Int 存储（通道截断）

```kotlin
// ❌ 错误：RGB 打包在单个 Int，每通道只有 8bit
val dithered = pixels.copyOf()  // 每个像素是 packed ARGB
dithered[idx + 1] = addFS(dithered[idx + 1], errR, errG, errB, 7)
// 误差超过 ±128 就被截断
```

```kotlin
// ✅ 正确：三通道分离缓冲，值域不受限
val workR = IntArray(nPixels); val workG = IntArray(nPixels); val workB = IntArray(nPixels)
workR[idx + 1] += errR * 7 / 16  // 直接加减，不截断
```

### Bug 模式 B：误差基于未 clamp 的工作值计算

```kotlin
// ❌ 错误：err = 未截断值 - 调色板色
val errR = workR[idx] - palR
// workR[idx] 可能是 -50，diff 是 -150+，扩散出去导致连锁负数
```

```kotlin
// ✅ 正确：err = clamp 之后的实际输出色 - 调色板色
val cr = workR[idx].coerceIn(0, 255)
val errR = cr - palR  // 始终在 [-255, 255] 范围内
```

---

## 第四步：排查 LZW 编码位宽越界（最常见的黑屏根因）

### Bug 特征
- 第一个帧偶尔正常，后续帧完全损坏
- GIF 解析器报 "UNKNOWN byte" 大量乱码
- LZW 数据量极少（如 3KB vs 预期 50KB+）

### 检查代码

搜索 `LZWEncoder` 的 `output()` 方法，找到 codeSize 增长逻辑：

```kotlin
// ❌ 错误模式
} else {
    codeSize++              // 先加 1
    if (codeSize == BITS) { // BITS=12，但 codeSize 已经是 13，不成立
        // 空分支
    }
}
```

**根因**：`codeSize++` 写在判断之前。GIF LZW 位宽上限 = 12 bit，旧代码先自增到 13 再判断 `13 == 12` 永远 false，位宽继续增长到 14、15……所有后续码字写入错误位数，解码器读到比特流错位。

### 正确实现

```kotlin
// ✅ 正确模式
} else if (codeSize < BITS) {  // BITS = 12
    codeSize++                  // 仅在未达上限时增长
}
```

对比 JS 参考实现（gif.js）：
```javascript
// JS 原版 — maxcode 在 codeSize==BITS 后被设为 1<<BITS，不再增长
++n_bits;
if (n_bits == BITS)
    maxcode = 1 << BITS;
else
    maxcode = MAXCODE(n_bits);
```

---

## 第五步：排除路径 / 缓存问题

### 检查保存路径

```powershell
adb shell "find /storage/emulated/0 -name 'emoji_*.gif' 2>/dev/null"
```

如果出现 `DCIM/DCIM/Emoji/` 嵌套路径，说明 `SAVE_DIRECTORY` 常量多了一层 `DCIM/`。

### 检查导出缓存

如果同参数导出旧缓存没清，可能复现旧 bug 产生的结果。确认 `ExportCache` 的 key 覆盖了所有影响编码的参数。

---

## 排查清单

| 步骤 | 检查项 | 正常预期 |
|------|--------|---------|
| 1 | 帧提取底部像素 | nonBlack ≈ 宽度 |
| 2 | GIF 二进制 LZW 块大小 | 每帧 50KB+ |
| 3 | 抖动通道缓冲 | 三通道分离 IntArray |
| 4 | 抖动误差来源 | 基于 clamp 值 |
| 5 | LZW codeSize 上限 | `< BITS` 先判断再增长 |
| 6 | 保存路径 | `DCIM/Emoji/` 单层 |
| 7 | 导出缓存 key | 覆盖所有参数 |
