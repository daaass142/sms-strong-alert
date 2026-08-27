# 短信叫叫叫 / SmsStrongAlert

一个专门用于“违停短信到了但普通通知太容易错过”的 Android 强提醒工具。

## 核心目标

- **本地识别**：收到 `SMS_RECEIVED` 后在设备本地匹配违停关键词与可选车牌。
- **不联网**：Manifest 不声明 `INTERNET` / `ACCESS_NETWORK_STATE`。
- **低耗电**：不跑常驻 Service、不轮询、不保活；平时没有后台任务。
- **后台被清理后仍可工作**：依赖系统短信广播唤起进程，而不是依赖常驻后台。
- **锁屏强提醒**：高重要性 Alarm 通知 + Full-screen Intent；全屏页面会亮屏、循环响铃并重复震动。
- **超小依赖面**：Kotlin + Android Framework Views；没有 Compose、Material、AppCompat、网络库、数据库、分析 SDK。

> 重要限制：Android 的“强行停止 / Force stop”会让系统暂停向该 App 投递广播，直到用户再次手动打开 App。某些厂商的极端省电策略也可能影响行为。因此不要把任何普通第三方 App 宣传成“任何情况下 100% 不掉”。

## 为什么不用常驻后台

这个场景的触发源本来就是系统的短信广播。最佳做法不是建立一个永远运行的前台服务，而是：

1. Manifest 注册 `SMS_RECEIVED` BroadcastReceiver；
2. 收到短信时做几毫秒的本地字符串匹配；
3. 命中后立刻发高优先级通知；
4. 获得 Full-screen Intent 权限时拉起锁屏全屏告警 Activity；
5. 用户确认或达到设置时长后停止。

这比“后台常驻 + 保活”更省电、代码更少，也更符合现代 Android 的后台限制。

## Android 14+ 的全屏提醒

Android 14 起，Full-screen Intent 权限由用户控制。App 会检测 `NotificationManager.canUseFullScreenIntent()` 并提供设置入口。没有这项权限时，系统仍可显示高优先级 heads-up 通知，但不保证自动弹出全屏页面。

Android 13+ 还需要用户授予 `POST_NOTIFICATIONS`。

## 权限

| 权限 | 用途 |
| --- | --- |
| `RECEIVE_SMS` | 接收新短信广播并在本地判断是否命中 |
| `POST_NOTIFICATIONS` | Android 13+ 显示强提醒 |
| `USE_FULL_SCREEN_INTENT` | 锁屏/熄屏时尽可能显示全屏告警 |
| `VIBRATE` | 强提醒震动 |
| `ACCESS_NOTIFICATION_POLICY` | 可选：用户授权后允许提醒通道绕过勿扰模式 |

刻意不申请：`READ_SMS`、`INTERNET`、`ACCESS_NETWORK_STATE`、定位、通讯录、存储、悬浮窗。

## 车牌识别策略

默认关键词包括：违停、违法停车、违法停放、违规停车、立即驶离、移车、临时停车、占道停车。

- 未保存车牌：只要命中上述违停关键词就提醒。
- 已保存一个或多个车牌：必须同时命中违停关键词和其中一个车牌才提醒，降低误报。
- App 不读取短信数据库，只处理“刚刚收到”的广播内容。

## UI

主页参考“状态卡 + 车牌卡 + 提醒设置卡”的结构；告警页采用大面积红色背景、车牌高对比展示、短信摘要和一个明确的“收到，马上去处理”按钮。

## 构建

项目使用：

- Android Gradle Plugin 8.13.2
- Kotlin 2.3.20
- Gradle 8.13
- JDK 17
- compileSdk / targetSdk 36
- minSdk 26

仓库没有提交 Gradle Wrapper 二进制；GitHub Actions 会通过 `gradle/actions/setup-gradle` 安装固定版本的 Gradle。Android Studio 直接打开项目即可同步；命令行可使用 Gradle 8.13：

```bash
gradle testDebugUnitTest lintDebug assembleDebug
```

可安装调试 APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release 构建应使用你自己的签名密钥，密钥绝不要提交到公开仓库。

## APKM 说明

`.apkm` 是 APKMirror 为“base APK + 多个 split APK”设计的容器格式，不是 Android 官方的标准构建产物。这个项目没有 ABI/native 依赖、没有语言/密度 split，最合理的发布物就是**单一 universal APK**：体积更小、用户点一下就能安装，也不需要额外安装 APKMirror Installer。

如果你说的“APKM”只是泛指 Android 安装包，那么本项目直接产出 APK；如果你确实需要 split 发行，建议使用官方 AAB + bundletool 生成 `.apks`，而不是伪造 `.apkm`。

## 隐私与安全

详见 [PRIVACY.md](PRIVACY.md) 和 [SECURITY.md](SECURITY.md)。

## License

MIT
