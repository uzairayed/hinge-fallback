# Use the watcher

Install [Shizuku](https://shizuku.rikka.app/download/). Start its server. Developer options stay on while that server runs. After the first pairing, Wireless debugging can start it without a computer. The pairing steps are in the [Shizuku setup guide](https://shizuku.rikka.app/guide/setup/).

Install the debug APK. From the project root, run:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`adb` prints `Success`.

Open **Hinge fallback**.

Tap **Allow Shizuku**. Allow the permission prompt.

Allow notifications when Android asks.

Turn on **Watch the hinge**.

Fold the phone. The cover screen comes on. **Hall sensor 0** reads `1 · closed`, and **Fold state** reads Closed.

Unfold the phone. The inner screen comes on. **Hall sensor 0** reads `0 · open`, and **Fold state** reads Open.

If the phone stays on the cover screen, turn **Watch the hinge** off. **Turn off** on the notification does the same thing. Either action runs `cmd device_state base-state reset`.

After a reboot, start Shizuku again. The watcher stays enabled and waits until Shizuku is running.
