# Build the debug APK

You need JDK 17 and Android SDK platform 35.

If `java -version` reports an older JDK, set `JAVA_HOME` to a JDK 17 install before the Gradle command.

Create `local.properties` in the project root. Set `sdk.dir` to your Android SDK path. Leave that file uncommitted. `.gitignore` already excludes it.

```
sdk.dir=C:\\Android\\Sdk
```

On Windows, from the project root, run:

```
gradlew.bat :app:assembleDebug
```

On macOS or Linux, run:

```
./gradlew :app:assembleDebug
```

Gradle prints `BUILD SUCCESSFUL`. The APK is `app/build/outputs/apk/debug/app-debug.apk`.

If `dl.google.com` does not respond, replace both `repositories` blocks in `settings.gradle.kts`. The plugin block becomes:

```
maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
maven { url = uri("https://maven.aliyun.com/repository/google") }
maven { url = uri("https://maven.aliyun.com/repository/public") }
maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
```

The dependency block becomes:

```
maven { url = uri("https://maven.aliyun.com/repository/google") }
maven { url = uri("https://maven.aliyun.com/repository/public") }
maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
```

Keep those mirror URLs as a local edit. The published file uses `google()` and `mavenCentral()`.
