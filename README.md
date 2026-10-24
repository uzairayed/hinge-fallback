# Hinge fallback

Follow these steps to switch to the cover screen when you fold the phone. The phone needs Android 14 or newer.

1. Download [hinge-fallback-1.0.apk](https://github.com/uzairayed/hinge-fallback/releases/download/v1.0/hinge-fallback-1.0.apk).
2. Open the file on the phone and install it.
3. Install [Shizuku](https://shizuku.rikka.app/download/).
4. Turn on **Developer options**.
5. Turn on **Wireless debugging**.
6. Start Shizuku. The pairing steps are in the [Shizuku setup guide](https://shizuku.rikka.app/guide/setup/).
7. Open **Hinge fallback**.
8. Tap **Allow Shizuku**.
9. Allow the permission prompt.
10. Allow notifications when Android asks.
11. Turn on **Watch the hinge**.
12. Fold the phone. The cover screen comes on.
13. Unfold the phone. The inner screen comes on.

If the phone stays on the cover screen, turn **Watch the hinge** off.

After a reboot, start Shizuku again. The watcher stays on.

If a banking app refuses to open, turn **Developer options** off and use that app. Turn **Developer options** back on and start Shizuku again before you fold the phone.

## Read next

Why the hall sensor can do this is in [How the watcher chooses a screen](docs/how-it-works.md).

Build the app from source with [Build the debug APK](docs/build.md).

The license is Apache 2.0. The terms are in [LICENSE](LICENSE).
