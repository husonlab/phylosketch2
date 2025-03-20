#!/bin/zsh
#
#
dir=../../target/dependency

# macOS x86-64
 mkdir -p macos-x86_64
cd macos-x86_64
unzip -o  -j $dir/javacpp-1.5.11-macosx-x86_64.jar org/bytedeco/javacpp/macosx-x86_64/libjnijavacpp.dylib
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-macosx-x86_64.jar '*.dylib'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-macosx-x86_64.jar w '*.dylib'

cd ..

# macOS ARM64
mkdir -p macos-aarch64
cd macos-aarch64
unzip -o  -j $dir/javacpp-1.5.11-macosx-arm64.jar org/bytedeco/javacpp/macosx-arm64/libjnijavacpp.dylib
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-macosx-arm64.jar  '*.dylib'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-macosx-arm64.jar  '*.dylib'

cd ..

# Linux x86-64
mkdir -p linux-x86_64
cd linux-x86_64
unzip -o  -j $dir/javacpp-1.5.11-linux-x86_64.jar '*.so*'
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-linux-x86_64.jar '*.so*'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-linux-x86_64.jar '*.so*'
cd ..

# Linux ARM64
mkdir -p linux-aarch64
cd linux-aarch64
unzip -o  -j $dir/javacpp-1.5.11-linux-arm64.jar '*.so*'
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-linux-arm64.jar '*.so*'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-linux-arm64.jar '*.so*'
cd ..

# Windows x86
mkdir -p windows-x86_64
cd windows-x86_64
unzip -o  -j $dir/javacpp-1.5.11-windows-x86_64.jar '*.dll'
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-windows-x86_64.jar '*.dll'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-windows-x86_64.jar '*.dll'
cd ..


# Android ARM64
mkdir -p android-aarch64
cd android-aarch64
unzip -o  -j $dir/javacpp-1.5.11-android-arm64.jar '*.so*'
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-android-arm64.jar '*.so*'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-android-arm64.jar '*.so*'
cd ..

# Android x86_64
mkdir -p android-x86_64
cd android-x86_64
unzip -o  -j $dir/javacpp-1.5.11-android-x86_64.jar '*.so*'
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-android-x86_64.jar '*.so*'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-android-x86_64.jar '*.so*'
cd ..

# iOS ARM64
mkdir -p ios-aarch64
cd ios-aarch64
unzip -o  -j $dir/javacpp-1.5.11-ios-arm64.jar '*.dylib'
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-macosx-arm64.jar  '*.dylib'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-macosx-arm64.jar  '*.dylib'
cd ..


# iOS x86
mkdir -p ios-x86_64
cd ios-x86_64
unzip -o  -j $dir/javacpp-1.5.11-ios-x86_64.jar '*.dylib'
unzip -o  -j $dir/leptonica-1.85.0-1.5.11-macosx-x86_64.jar '*.dylib'
unzip -o  -j $dir/tesseract-5.5.0-1.5.11-macosx-x86_64.jar w '*.dylib'

cd ..
echo "Extraction complete. All native libraries are extracted into their respective directories."
