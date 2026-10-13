# VoskControl

基于 **Kotlin + Vosk** 的 Android 离线语音交互应用。将中文语音映射到角色与动作，在本地播放相应视频，面向专用交互展示终端。

这是大学期间参与真实企业委托需求后独立完成的 Android 项目。当前公开版本同步自本地 `VoskControl` 源码，保留客户沟通后形成的多角色交互逻辑。仓库中的视频与启动动画由作者替换为演示素材，不包含甲方原始素材。

## 当前功能

- **离线中文识别**：Vosk Android、本地中文模型、16 kHz 录音；识别过程不需要云端 ASR。
- **角色与动作两级映射**：代码配置 7 个角色，根据角色名称切换上下文，再匹配该角色支持的动作指令；识别结果会去除空格，并配置部分常见误识别别名。
- **视频播放状态管理**：角色默认视频循环播放；动作视频单次播放结束后恢复默认视频。
- **角色记忆**：通过 SharedPreferences 恢复上一次选择的角色。
- **应用启动流程**：启动 Logo 视频 → 启动动画视频 → 语音交互主界面。
- **终端界面**：沉浸式全屏、HOME 类别桌面入口、录音权限申请及 Activity 生命周期内的监听启停。

语音处理链路：麦克风 → Vosk partial result → 中文关键词匹配 → 角色/动作选择 → VideoView。

## 公开演示资源

`app/src/main/res/raw/` 包含作者提供的演示视频。源码中的角色与动作命名保留原有指令映射，演示画面不代表客户最终交付素材。替换自己的视频时，保持相同资源名即可；添加角色或指令时，修改 `MainActivity.initializeAnimals()` 中的配置。

## 构建与运行

| 项目 | 当前配置 |
| --- | --- |
| Android | 最低 Android 7.0 / API 24；compile/target SDK 34 |
| Kotlin | 2.2.0 |
| Android Gradle Plugin | 8.13.0 |
| Gradle Wrapper | 8.13 |
| Java | 建议使用 JDK 17 |
| 语音识别 | Vosk Android 0.3.45（本地 AAR）、JNA 5.14.0 |
| 应用 ID | `com.example.voskcontrol` |

```powershell
git clone https://github.com/Dapper-YF/VoskControl.git
cd VoskControl
# 设置 JAVA_HOME 为本机 JDK 17，ANDROID_HOME 为 Android SDK 目录。
.\gradlew.bat :app:assembleDebug
```

也可以用 Android Studio 打开项目，安装 Android SDK 34 后同步 Gradle。首次构建需要联网下载未缓存的依赖。

调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。安装到测试设备：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.example.voskcontrol/.SplashLogoActivity
```

清单注册了 HOME 入口，未注册常规 LAUNCHER 入口，因此不一定出现在普通应用抽屉。需要桌面终端模式时，在系统设置中将 VoskControl 选为默认主屏幕应用；退出时通过系统设置切回原有桌面。

授予录音权限并完成模型加载后，可以尝试“博美”“跳舞”“柴犬”“摇尾巴”等指令。公开版可结合 Logcat 的 `VoskControl` 标签观察匹配结果。

## 代码结构

```text
app/src/main/
├── java/com/example/voskcontrol/
│   ├── MainActivity.kt             # 角色/动作配置、Vosk、视频与生命周期
│   ├── SplashLogoActivity.kt       # 启动 Logo 视频
│   └── SplashAnimationActivity.kt  # 启动动画视频
├── assets/model-zh-cn/             # 离线中文模型
└── res/
    ├── layout/                    # 三个 Activity 的界面
    ├── raw/                       # 作者提供的演示视频
    └── values/                    # 应用名称、主题、颜色
```

## 当前边界与后续改进

- HOME 入口与沉浸模式不等同于设备管理员 / Lock Task 的强制单应用锁定。
- 当前版本播放应用内启动视频；旧版的 Root 系统开机动画替换代码不属于当前实现。
- 语音采用固定关键词与匹配顺序，尚未实现通用意图理解或大模型 Agent。部分长短词存在包含关系，后续可补充更精确的优先级与防重复触发策略。
- 识别效果取决于麦克风、环境噪声、口音及指令词表。当前没有可复现的准确率或时延基准。
- 监听错误恢复、启动视频异常处理和不同 Android 厂商的桌面行为仍需设备测试。
- 仓库自带测试为 Android 模板测试，不代表语音业务逻辑已有完整自动化覆盖。

第三方模型、Vosk/JNA 及演示素材遵循各自的许可；本 README 不额外授予客户素材的使用权。
