$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$download = Join-Path $root '.downloads'
New-Item -ItemType Directory -Force $download | Out-Null

$modelArchive = Join-Path $download 'vits-piper-vi_VN-vais1000-medium.tar.bz2'
if (!(Test-Path $modelArchive)) {
  Invoke-WebRequest 'https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-vi_VN-vais1000-medium.tar.bz2' -OutFile $modelArchive
}
tar -xf $modelArchive -C $download
$model = Join-Path $download 'vits-piper-vi_VN-vais1000-medium'
Copy-Item $model (Join-Path $root 'android/app/src/main/assets') -Recurse -Force
Copy-Item $model (Join-Path $root 'windows/assets') -Recurse -Force

$aar = Join-Path $root 'android/app/libs/sherpa-onnx-1.13.4.aar'
New-Item -ItemType Directory -Force (Split-Path $aar) | Out-Null
if (!(Test-Path $aar)) {
  Invoke-WebRequest 'https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.4/sherpa-onnx-1.13.4.aar' -OutFile $aar
}

Write-Host '模型和 Android sherpa-onnx AAR 已准备完成。'
Write-Host '请将 Han-Nom Khai 字体命名为 han-nom-khai.ttf，分别复制到 android/app/src/main/assets 与 windows/assets。'

