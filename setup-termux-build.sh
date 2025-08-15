#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "[1/6] Updating packages..."
pkg update -y
pkg install -y openjdk-17 wget unzip zip

echo "[2/6] Setting JAVA_HOME..."
grep -q 'JAVA_HOME' ~/.bashrc || echo 'export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk' >> ~/.bashrc
grep -q 'JAVA_HOME' ~/.zshrc 2>/dev/null || echo 'export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk' >> ~/.zshrc
export JAVA_HOME=/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

echo "[3/6] Installing Gradle 8.2.1..."
cd ~
if [ ! -d "$HOME/gradle-8.2.1" ]; then
  wget -O gradle-8.2.1-bin.zip https://services.gradle.org/distributions/gradle-8.2.1-bin.zip
  unzip -q gradle-8.2.1-bin.zip
fi
export PATH=$HOME/gradle-8.2.1/bin:$PATH
grep -q 'gradle-8.2.1/bin' ~/.bashrc || echo 'export PATH=$HOME/gradle-8.2.1/bin:$PATH' >> ~/.bashrc

echo "[4/6] Installing Android SDK (cmdline-tools)..."
SDK=$HOME/android-sdk
mkdir -p $SDK/cmdline-tools
cd $SDK/cmdline-tools
if [ ! -d "latest" ]; then
  # If link breaks, update it manually.
  wget -O cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q cmdtools.zip
  mv cmdline-tools latest
fi
export ANDROID_SDK_ROOT=$SDK
export ANDROID_HOME=$SDK
export PATH=$SDK/cmdline-tools/latest/bin:$SDK/platform-tools:$PATH
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "[5/6] Writing local.properties..."
APPDIR=$HOME/sakura-ai
echo "sdk.dir=$ANDROID_SDK_ROOT" > $APPDIR/local.properties

echo "[6/6] Building APK..."
cd $APPDIR
gradle assembleDebug

APK="$APPDIR/app/build/outputs/apk/debug/app-debug.apk"
echo "APK built at: $APK"
termux-open "$APK" || true
