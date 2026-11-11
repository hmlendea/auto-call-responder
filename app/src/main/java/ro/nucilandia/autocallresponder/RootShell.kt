package ro.nucilandia.autocallresponder

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs commands through `su`, when the device grants root to this app.
 *
 * Detection never blocks the caller: [isAvailable] only reports the cached state and
 * [detectAsync] performs the actual `su` call on a background thread, because the root manager
 * shows a permission prompt that the user has to answer.
 */
internal object RootShell {

    private const val TAG = "RootShell"
    private const val DETECT_TIMEOUT_MILLISECONDS = 60_000L
    private const val COMMAND_TIMEOUT_MILLISECONDS = 5_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val detectionInProgress = AtomicBoolean(false)

    @Volatile
    private var availability: Boolean? = null

    /** Cached root state: true, false, or null when it has not been determined yet. */
    val cachedAvailability: Boolean?
        get() = availability

    /** Non-blocking check. Returns false until [detectAsync] has completed successfully. */
    fun isAvailable(): Boolean = availability == true

    /**
     * Detects root on a background thread, prompting the user through the root manager when needed.
     * Concurrent calls are ignored; the callback always runs on the main thread.
     */
    fun detectAsync(onResult: ((Boolean) -> Unit)? = null) {
        if (!detectionInProgress.compareAndSet(false, true)) return

        Thread({
            val granted = runDetection()
            detectionInProgress.set(false)
            if (onResult != null) {
                mainHandler.post { onResult(granted) }
            }
        }, "RootShell-Detect").start()
    }

    /** Clears the cached state and asks the root manager again. */
    fun requestAccess(onResult: ((Boolean) -> Unit)? = null) {
        availability = null
        detectAsync(onResult)
    }

    /** Runs [command] as root and returns its combined output, or null when it cannot run. */
    fun execute(command: String, timeoutMillis: Long = COMMAND_TIMEOUT_MILLISECONDS): String? {
        if (!isAvailable()) return null

        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "Root command timed out: $command")
                process.destroy()
                return null
            }

            process.inputStream.bufferedReader().use { reader -> reader.readText() }
        } catch (exception: Exception) {
            Log.w(TAG, "Root command failed: $command", exception)
            null
        }
    }

    private fun runDetection(): Boolean {
        val output = runSu("id", DETECT_TIMEOUT_MILLISECONDS)
        val granted = output != null && output.contains("uid=0")

        availability = granted
        Log.i(TAG, if (granted) "Root access available" else "Root access not available")

        return granted
    }

    private fun runSu(command: String, timeoutMillis: Long): String? {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "Root detection timed out")
                process.destroy()
                return null
            }

            process.inputStream.bufferedReader().use { reader -> reader.readText() }
        } catch (exception: Exception) {
            Log.w(TAG, "Root is not available", exception)
            null
        }
    }
}
