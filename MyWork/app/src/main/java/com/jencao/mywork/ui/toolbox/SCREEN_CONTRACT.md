# 工具箱 8 个模块 UI 开发契约（子代理必读）

本项目为 Android 单 Activity + Jetpack Compose + Hilt，主色/风格为新拟物（neumorphic）。
本文件规定 8 个工具模块页面的统一写法。每个模块由两名文件组成，**你只创建这两个文件，禁止修改任何共享文件**（ApiService / AppDatabase / AppDestinations / MyWorkApp / Modules / 已存在的 repository / entity / dao 等）。路由与导航由主代理统一注册。

## 必须先行阅读（模仿其风格与签名）
- `ui/dailypending/DailyPendingScreen.kt` 与 `ui/dailypending/DailyPendingViewModel.kt`（最贴近的模板：Screen(navController, padding) + @HiltViewModel + StateFlow + NeuCard 风格）
- `ui/components/` 目录下所有文件（确认 `NeuCard`、`NeuFab`、`neumorphic`、`NeuRadiusLarge` 的真实签名后再用）
- `ui/sport/SportScreen.kt`（嵌套导航示例，可选参考）
- 你负责的 repository 与 entity 源文件（路径由主代理在任务中给出）

## 文件与符号约定（严格遵守，主代理据此注册路由）
- 包名：`package com.jencao.mywork.ui.toolbox`
- 屏幕文件：`ui/toolbox/<Name>Screen.kt`，组合函数签名固定为
  `@Composable fun <Name>Screen(navController: NavHostController, padding: PaddingValues)`
  其中 `<Name>` ∈ {Calc, Converter, Qr, Countdown, Flashcard, Habit, Inspiration, Express}。
- ViewModel 文件：`ui/toolbox/<Name>ViewModel.kt`
  `@HiltViewModel class <Name>ViewModel @Inject constructor(private val repo: <Repo>) : ViewModel()`
  （需要网络时额外注入 `private val api: ApiService`）
- 用 `androidx.lifecycle.compose.collectAsStateWithLifecycle` 收集 Flow。
- 顶部可用 `Scaffold` + `TopAppBar`（title 为模块名），主体用 `Column(Modifier.fillMaxSize().padding(padding).padding(16.dp))` 或 `LazyColumn`；新增用 `FloatingActionButton`（可用 `NeuFab`）或对话框。
- 列表项优先用项目的新拟物风格（读 `ui/components` 后决定用 `NeuCard` 还是 `Card`）；不确定签名时一律用 Material3 `Card`，保证能编译通过。

## 通用规则
- 所有写操作（insert/update/delete）都走 repository，repository 内部已调用 `touch()` 置 `needsSync`，同步由 `SyncManager` 统一负责，UI 不必手动触发同步。
- 实体字段为驼峰（如 `createdAt`），DB 列蛇形（`created_at`），Gson 已配置自动映射；不要手写映射。
- 删除用 `repo.softDelete(id)`（软删，可被同步识别）。
- 不要在 UI 里直接访问数据库；只通过注入的 repository / api。
- 字符串尽量中文。不要引入新第三方依赖（zxing 已添加，可直接用于二维码生成）。
- 确保 Kotlin 编译无误：import 精确、Composable 函数带 @Composable、remember/derivedStateOf 正确使用。

## 网络调用片段（仅相关模块需要）
```kotlin
// 在 ViewModel 中（需注入 ApiService api）
viewModelScope.launch {
    try {
        val resp = api.ai(AiRequest(action = "extract", content = text))
        if (resp.code == 0) _result.value = resp.data?.result ?: ""
    } catch (e: Exception) { /* 处理异常 */ }
}
// 快递轨迹
val resp = api.expressTrack(ExpressTrackRequest(company = code, tracking_no = no))
// 实时汇率（货币换算时用）
val resp = api.currencyRate(from = "USD", to = "CNY", amount = 1.0)
```

## 二维码生成片段（Qr 模块用，zxing 已依赖）
```kotlin
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap

fun generateQr(text: String, sizePx: Int = 512): Bitmap {
    val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) for (y in 0 until sizePx)
        bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bmp
}
// 显示：Image(bitmap = generateQr(text).asImageBitmap())
```
相机扫描不在本次范围（需 ML Kit/权限），请用"手动录入扫描结果"对话框代替扫描。
