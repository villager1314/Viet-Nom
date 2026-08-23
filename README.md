# 越喃 · Việt Nôm

离线越南国语字转汉喃工具，包含 Android 与 Windows 版本。汉喃字采用内置 Han-Nom Khai 字体，越南语朗读使用内置 `vi_VN-vais1000-medium` sherpa-onnx/Piper 模型；未识别为越南语的外来词调用系统语音。

## 发布文件

请在 GitHub Releases 下载：

- `viet-nom-v1.0.11.apk`：Android ARM64 离线版。
- `viet-nom-windows-setup-v1.0.2.exe`：Windows x64 安装版，安装后启动更快。
- `viet-nom-windows-folder-v1.0.2.zip`：Windows x64 免安装文件夹版，完整解压后运行 `越喃.exe`。

## 源码结构

- `android/`：原生 Android WebView 容器、sherpa-onnx Java 接口及全部离线资源。
- `windows/`：Electron 主程序、页面、词典、字体及离线模型。

## 构建 Android APK

要求：JDK 17 或 21、Android SDK 35、Gradle 8.13。

1. 先运行根目录的 `prepare-assets.ps1` 下载官方语音模型和 Android AAR，并按脚本提示放入 Han-Nom Khai 字体。
2. 在 `android/local.properties` 写入 `sdk.dir=你的 Android SDK 路径`。
3. 在 `android/` 执行：

```powershell
gradle assembleRelease
```

输出位于 `android/app/build/outputs/apk/release/app-release.apk`。当前配置仅构建 `arm64-v8a`。

## 构建 Windows

要求：Windows x64、Node.js 22 或更高版本。

```powershell
cd windows
npm install
```

生成安装版：

```powershell
npm run dist
```

生成便携单 EXE：

```powershell
npx electron-builder --win portable --x64
```

生成快速启动的免安装文件夹：

```powershell
npx electron-builder --win --x64 --dir
```

`win-unpacked` 文件夹可直接压缩为 ZIP。便携单 EXE 每次启动都需要解压资源，推荐使用安装版或 ZIP 文件夹版。

## 数据与模型

- 汉喃词典资料参考：[汉喃研究与应用会转换器](https://www.hannom-rcv.org/converter/?uiLang=zh)
- 离线语音运行时：[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)

词典与字体请遵守各自来源的授权要求；本项目仅供研究与教育用途。

