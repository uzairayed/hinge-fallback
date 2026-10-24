package com.hingefallback

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ShellResult(
    val ok: Boolean,
    val code: Int,
    val stdout: String,
    val stderr: String,
)

object ShizukuShell {
    fun isManagerInstalled(packageManager: PackageManager): Boolean = try {
        packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun isRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun isGranted(): Boolean = try {
        isRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun baseState(argument: String): ShellResult = run("cmd", "device_state", "base-state", argument)

    fun printState(): ShellResult = run("cmd", "device_state", "print-state")

    fun run(vararg command: String): ShellResult {
        val remote = IShizukuService.Stub.asInterface(Shizuku.getBinder())
            .newProcess(command, null, null)
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val outThread = Thread {
            ParcelFileDescriptor.AutoCloseInputStream(remote.inputStream).use { it.copyTo(stdout) }
        }
        val errThread = Thread {
            ParcelFileDescriptor.AutoCloseInputStream(remote.errorStream).use { it.copyTo(stderr) }
        }
        outThread.start()
        errThread.start()
        val finished = remote.waitForTimeout(8, TimeUnit.SECONDS.toString())
        outThread.join(1_000)
        errThread.join(1_000)
        if (!finished) {
            remote.destroy()
            return ShellResult(false, -1, stdout.toString(), "timed out")
        }
        val code = remote.exitValue()
        remote.destroy()
        return ShellResult(code == 0, code, stdout.toString().trim(), stderr.toString().trim())
    }

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
}
