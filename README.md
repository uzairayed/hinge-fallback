# Hinge fallback

A Pixel fold reports closed only when two readings agree. Hall sensor 0 must see the magnet, and the hinge angle must be near 0. Open is the state used when no closed condition matches.

On a phone whose second motion sensor never answers, the angle stays empty. The phone stays open, and the cover screen stays off. Hall sensor 0 still reports the magnet. A reading of 1 means closed.

Hinge fallback watches that sensor. When the reading is at least 0.5, the watcher runs `cmd device_state base-state 0`. That command forces the closed state and turns the cover screen on. When the reading is lower, the watcher runs `cmd device_state base-state reset`. The phone returns to open, and the inner screen comes back.

Android allows that command for the shell user through `android.permission.CONTROL_DEVICE_STATE`. A normal app cannot hold that permission. [Shizuku](https://shizuku.rikka.app/) runs the command as the shell user while its server is running.

## Limits

Half-open still needs a hinge angle. Hall sensor 0 is a magnet switch with two values, so it cannot supply that angle. The watcher forces closed, or it clears that force.

Developer options have to stay on. Turning them off stops the shell user, and Shizuku stops with it. A banking app that refuses Developer options cannot run in the same session as the watcher.

The cover camera is a separate hardware failure. This app does not publish it.

Sensor 0 is `DRV5032 Hall Effect Sensor 0 (wake-up)`. Sensor 1 is `DRV5032 Hall Effect Sensor 1 (wake-up)`. Both use the type `com.google.sensor.hall_effect`. The app requires Android 14, API 34. The build targets a Pixel 9 Pro Fold running Android 17. A phone that names these sensors differently shows the error "Hall sensor 0 is not visible to this app."

## Read next

The build steps are in [Build the debug APK](docs/build.md).

The phone steps are in [Use the watcher](docs/use.md).

The license is Apache 2.0. The terms are in [LICENSE](LICENSE).
