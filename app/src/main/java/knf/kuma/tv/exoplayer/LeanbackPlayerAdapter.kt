package knf.kuma.tv.exoplayer

import android.content.Context
import android.os.Handler
import android.view.Surface
import android.view.SurfaceHolder
import androidx.leanback.media.PlaybackGlueHost
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.media.SurfaceHolderGlueHost
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.DiscontinuityReason
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.video.VideoListener

/**
 * Leanback `PlayerAdapter` implementation for [SimpleExoPlayer].
 */
class LeanbackPlayerAdapter
/**
 * Builds an instance. Note that the `PlayerAdapter` does not manage the lifecycle of the
 * [SimpleExoPlayer] instance. The caller remains responsible for releasing the exoPlayer when
 * it's no longer required.
 *
 * @param context        The current context (activity).
 * @param player         Instance of your exoplayer that needs to be configured.
 * @param updatePeriodMs The delay between exoPlayer control updates, in milliseconds.
 */
(private val context: Context, private val player: SimpleExoPlayer, updatePeriodMs: Int) : PlayerAdapter() {
    private val handler: Handler = Handler()
    private val componentListener: ComponentListener
    private val updateProgressRunnable: Runnable

    private var controlDispatcher: ControlDispatcher? = null
    private var errorMessageProvider: ErrorMessageProvider<in ExoPlaybackException>? = null
    private var surfaceHolderGlueHost: SurfaceHolderGlueHost? = null
    private var hasSurface: Boolean = false
    private var lastNotifiedPreparedState: Boolean = false

    init {
        componentListener = ComponentListener()
        controlDispatcher = DefaultControlDispatcher()
        updateProgressRunnable = object : Runnable {
            override fun run() {
                val callback = callback
                callback.onCurrentPositionChanged(this@LeanbackPlayerAdapter)
                callback.onBufferedPositionChanged(this@LeanbackPlayerAdapter)
                handler.postDelayed(this, updatePeriodMs.toLong())
            }
        }
    }

    // PlayerAdapter implementation.

    override fun onAttachedToHost(host: PlaybackGlueHost?) {
        if (host is SurfaceHolderGlueHost) {
            surfaceHolderGlueHost = host
            surfaceHolderGlueHost?.setSurfaceHolderCallback(componentListener)
        }
        notifyStateChanged()
        player.addListener(componentListener)
        player.addVideoListener(componentListener)
    }

    override fun onDetachedFromHost() {
        player.removeListener(componentListener)
        player.removeVideoListener(componentListener)
        surfaceHolderGlueHost?.setSurfaceHolderCallback(null)
        surfaceHolderGlueHost = null
        hasSurface = false
        val callback = callback
        callback.onBufferingStateChanged(this, false)
        callback.onPlayStateChanged(this)
        maybeNotifyPreparedStateChanged(callback)
    }

    override fun setProgressUpdatingEnabled(enabled: Boolean) {
        handler.removeCallbacks(updateProgressRunnable)
        if (enabled) {
            handler.post(updateProgressRunnable)
        }
    }

    override fun isPlaying(): Boolean {
        val playbackState = player.playbackState
        return (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
                && player.playWhenReady)
    }

    override fun getDuration(): Long {
        val durationMs = player.duration
        return if (durationMs == C.TIME_UNSET) -1 else durationMs
    }

    override fun getCurrentPosition(): Long {
        return if (player.playbackState == Player.STATE_IDLE) -1 else player.currentPosition
    }

    override fun play() {
        if (player.playbackState == Player.STATE_ENDED) {
            controlDispatcher?.dispatchSeekTo(player, player.currentWindowIndex, C.TIME_UNSET)
        }
        if (controlDispatcher?.dispatchSetPlayWhenReady(player, true) == true) {
            callback.onPlayStateChanged(this)
        }
    }

    override fun pause() {
        if (controlDispatcher?.dispatchSetPlayWhenReady(player, false) == true) {
            callback.onPlayStateChanged(this)
        }
    }

    override fun seekTo(positionMs: Long) {
        controlDispatcher?.dispatchSeekTo(player, player.currentWindowIndex, positionMs)
    }

    override fun getBufferedPosition(): Long {
        return player.bufferedPosition
    }

    override fun isPrepared(): Boolean {
        return player.playbackState != Player.STATE_IDLE && (surfaceHolderGlueHost == null || hasSurface)
    }

    // Internal methods.

    /* package */ internal fun setVideoSurface(surface: Surface?) {
        hasSurface = surface != null
        player.setVideoSurface(surface)
        maybeNotifyPreparedStateChanged(callback)
    }

    /* package */ internal fun notifyStateChanged() {
        val playbackState = player.playbackState
        val callback = callback
        maybeNotifyPreparedStateChanged(callback)
        callback.onPlayStateChanged(this)
        callback.onBufferingStateChanged(this, playbackState == Player.STATE_BUFFERING)
        if (playbackState == Player.STATE_ENDED) {
            callback.onPlayCompleted(this)
        }
    }

    private fun maybeNotifyPreparedStateChanged(callback: Callback) {
        val isPrepared = isPrepared
        if (lastNotifiedPreparedState != isPrepared) {
            lastNotifiedPreparedState = isPrepared
            callback.onPreparedStateChanged(this)
        }
    }

    private inner class ComponentListener : Player.EventListener, VideoListener, SurfaceHolder.Callback {

        // SurfaceHolder.Callback implementation.

        override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
            setVideoSurface(surfaceHolder.surface)
        }

        override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
            // Do nothing.
        }

        override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
            setVideoSurface(null)
        }

        // Player.EventListener implementation.

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            notifyStateChanged()
        }

        override fun onPlayerError(error: PlaybackException) {
            callback.onError(this@LeanbackPlayerAdapter, error.errorCode, error.message)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            callback.apply {
                onDurationChanged(this@LeanbackPlayerAdapter)
                onCurrentPositionChanged(this@LeanbackPlayerAdapter)
                onBufferedPositionChanged(this@LeanbackPlayerAdapter)
            }
        }

        override fun onPositionDiscontinuity(@DiscontinuityReason reason: Int) {
            val callback = callback
            callback.onCurrentPositionChanged(this@LeanbackPlayerAdapter)
            callback.onBufferedPositionChanged(this@LeanbackPlayerAdapter)
        }

        // SimpleExoplayerView.Callback implementation.

        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int,
                                        pixelWidthHeightRatio: Float) {
            callback.onVideoSizeChanged(this@LeanbackPlayerAdapter, width, height)
        }

        override fun onRenderedFirstFrame() {
            // Do nothing.
        }

    }

    companion object {

        init {
            ExoPlayerLibraryInfo.registerModule("goog.exo.leanback")
        }
    }

}
