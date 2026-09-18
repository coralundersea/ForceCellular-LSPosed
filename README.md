# Force Cellular - LSPosed 模块

强制让 App 获取到的网络信息为**移动数据（Cellular / 流量）**，并尽量关闭 WiFi 相关检测。

## 功能

- `NetworkCapabilities.hasTransport(WIFI)` → false
- `NetworkCapabilities.hasTransport(CELLULAR)` → true
- `NetworkInfo.getType()` → TYPE_MOBILE (0)
- `NetworkInfo.getTypeName()` → "MOBILE"
- 强制 WifiManager 报告 WiFi 关闭
- TelephonyManager 报告 LTE + DATA_CONNECTED
- NetworkInterface 名字伪装成 rmnet_data0（增强绕过）

## 使用方法

1. 用 **Android Studio** 打开本项目
2. 同步 Gradle（如果提示缺少 Xposed API，可手动下载 api-82.jar 放到 libs）
3. Build → Build Bundle(s) / APK(s) → Build APK(s)
4. 把生成的 APK 安装到 MuMu 模拟器
5. 打开 LSPosed → 模块 → 启用 **Force Cellular**
6. **作用域** 勾选：
   - 你的游戏
   - 系统框架（System Framework）
7. 重启模拟器
8. 用之前的测试工具验证 WIFI 是否变成 false

## 注意事项

- 本模块比较激进，可能会影响部分正常使用 WiFi 的功能
- 如果游戏还有更深层的 native 检测，可能需要额外 Frida 配合
- 仅供学习研究使用
