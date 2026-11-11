package ro.nucilandia.autocallresponder

import android.content.Context
import android.media.AudioManager
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log

/**
 * Controls the microphone and the call audio route for a single handled call.
 *
 * The microphone is muted for the whole call and unmuted only while the recording is playing, so
 * ambient sound is never sent to the caller.
 *
 * When this app owns the call, [InCallService.setMuted] and [InCallService.setAudioRoute] are used:
 * they are the only supported APIs, because the `AudioManager` equivalents require the signature
 * permission `MODIFY_AUDIO_ROUTING` and silently do nothing for a normal app. Without an
 * [InCallService] (the legacy broadcast path) root is attempted, then the platform APIs as a
 * best-effort fallback.
 */
internal class CallAudioController(
    private val context: Context,
    private val inCallService: InCallService? = null
) {

    private val tag = "CallAudioController"
    private val audioManager = context.getSystemService(AudioManager::class.java)

    /** True when the microphone was muted by this instance. */
    var isMicrophoneMuted: Boolean = false
        private set

    /** True when root routing into the call uplink was applied for this call. */
    var isUplinkRoutingActive: Boolean = false
        private set

    /** True when root was detected during this call. */
    var isRootAvailable: Boolean = false
        private set

    /** Mutes the microphone; call before or right after answering. */
    fun muteMicrophone() {
        if (applyMicrophoneMute(true)) {
            Log.d(tag, "Microphone muted")
            return
        }

        Log.w(tag, "Could not mute the microphone")
    }

    /** Unmutes the microphone; call once playback has finished. */
    fun unmuteMicrophone() {
        if (applyMicrophoneMute(false)) {
            Log.d(tag, "Microphone unmuted")
            return
        }

        Log.w(tag, "Could not unmute the microphone")
    }

    /** Applies the audio route used before playback, and attempts root uplink routing. */
    fun prepareForPlayback(earpieceOnly: Boolean) {
        setAudioRoute(earpieceOnly)

        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        } catch (exception: Exception) {
            Log.w(tag, "Could not set the communication audio mode", exception)
        }

        applyRootUplinkRouting()
    }

    /** Restores the audio route after playback. */
    fun restoreAfterPlayback() {
        restoreRootUplinkRouting()

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (exception: Exception) {
            Log.w(tag, "Could not restore the audio mode", exception)
        }
    }

    private fun applyMicrophoneMute(muted: Boolean): Boolean {
        inCallService?.let { service ->
            try {
                service.setMuted(muted)
                isMicrophoneMuted = muted
                Log.d(tag, "InCallService microphone mute set to $muted")
                return true
            } catch (exception: Exception) {
                Log.w(tag, "Could not change the InCallService mute state", exception)
            }
        }

        if (RootShell.isAvailable()) {
            RootShell.execute("cmd audio set-mic-mute $muted")
            isMicrophoneMuted = muted
            Log.d(tag, "Microphone mute set to $muted through root")
            return true
        }

        return try {
            audioManager?.isMicrophoneMute = muted
            isMicrophoneMuted = muted
            Log.d(tag, "Microphone mute set to $muted through AudioManager")
            true
        } catch (exception: Exception) {
            Log.w(tag, "Could not change the microphone mute state", exception)
            false
        }
    }

    private fun setAudioRoute(earpieceOnly: Boolean) {
        inCallService?.let { service ->
            try {
                service.setAudioRoute(
                    if (earpieceOnly) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER
                )
                Log.d(tag, "InCallService audio route set to ${if (earpieceOnly) "earpiece" else "speaker"}")
                return
            } catch (exception: Exception) {
                Log.w(tag, "Could not change the InCallService audio route", exception)
            }
        }

        try {
            audioManager?.let { manager ->
                @Suppress("DEPRECATION")
                manager.isSpeakerphoneOn = !earpieceOnly
            }
            Log.d(tag, "AudioManager speakerphone set to ${!earpieceOnly}")
        } catch (exception: Exception) {
            Log.w(tag, "Could not change the audio route", exception)
        }
    }

    /**
     * Attempts to route local playback into the voice uplink through the audio HAL.
     *
     * Public Android APIs cannot do this: the voice uplink is a hardware capture path. The routes
     * below are the common Qualcomm/MediaTek mixer names, so this only succeeds on devices whose
     * kernel exposes a loopback control. Failure is expected and leaves local playback intact.
     */
    private fun applyRootUplinkRouting() {
        isRootAvailable = RootShell.isAvailable()
        if (!isRootAvailable) {
            Log.d(tag, "No root, keeping local playback only")
            return
        }

        val mixer = findMixerBinary()
        if (mixer == null) {
            Log.d(tag, "No mixer binary found, keeping local playback only")
            return
        }

        val applied = UPLINK_ROUTES.count { route -> RootShell.execute("$mixer ${route.first} ${route.second}") != null }
        isUplinkRoutingActive = applied > 0

        Log.d(
            tag,
            if (isUplinkRoutingActive) "Applied $applied uplink route controls"
            else "Mixer exposed no uplink route, keeping local playback only"
        )
    }

    private fun restoreRootUplinkRouting() {
        if (!isUplinkRoutingActive) return

        val mixer = findMixerBinary() ?: return
        UPLINK_RESTORE_ROUTES.forEach { route -> RootShell.execute("$mixer ${route.first} ${route.second}") }
        isUplinkRoutingActive = false
    }

    private fun findMixerBinary(): String? =
        MIXER_CANDIDATES.firstOrNull { candidate ->
            RootShell.execute("command -v $candidate")?.contains(candidate) == true
        }

    private companion object {
        val MIXER_CANDIDATES = listOf("tinymix", "tiny-mix", "/vendor/bin/tinymix")

        /** (control name, value) pairs that commonly loop local playback into the voice uplink. */
        val UPLINK_ROUTES = listOf(
            "Voice Tx Mixer MultiMedia1" to "1",
            "Voice Tx Mixer MultiMedia2" to "1",
            "Incall_Music Audio Mixer MultiMedia1" to "1",
            "Incall_Music Audio Mixer MultiMedia2" to "1",
            "SEC_AUDIO_LOOPBACK" to "1"
        )

        val UPLINK_RESTORE_ROUTES = listOf(
            "Voice Tx Mixer MultiMedia1" to "0",
            "Voice Tx Mixer MultiMedia2" to "0",
            "Incall_Music Audio Mixer MultiMedia1" to "0",
            "Incall_Music Audio Mixer MultiMedia2" to "0",
            "SEC_AUDIO_LOOPBACK" to "0"
        )
    }
}
