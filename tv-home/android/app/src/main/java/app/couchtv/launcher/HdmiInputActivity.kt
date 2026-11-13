package app.couchtv.launcher

import android.app.Activity
import android.graphics.Color
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.media.tv.TvView
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/** Plays a physical HDMI input in this app on Fire TV builds where Live TV handles the generic intent. */
class HdmiInputActivity : Activity() {
    private lateinit var tv: TvView
    private lateinit var status: TextView
    private lateinit var inputId: String
    private lateinit var label: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selected = intent.getStringExtra(INPUT_ID)
        if (selected.isNullOrBlank()) {
            finish()
            return
        }
        inputId = selected
        label = intent.getStringExtra(INPUT_LABEL).orEmpty().ifBlank { "HDMI" }
        tv = TvView(this)
        status = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 24f
        }
        val content = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(tv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(status, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        setContentView(content)
        tv.setCallback(object : TvView.TvInputCallback() {
            override fun onVideoAvailable(inputId: String) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                status.visibility = View.GONE
            }

            override fun onVideoUnavailable(inputId: String, reason: Int) {
                showStatus(if (reason == TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING) "Connecting…" else "No signal")
            }

            override fun onConnectionFailed(inputId: String) = showStatus("Could not open input")
            override fun onDisconnected(inputId: String) = showStatus("Input disconnected")
        })
    }

    override fun onStart() {
        super.onStart()
        if (!::tv.isInitialized) return
        showStatus("Connecting…")
        try {
            tv.tune(inputId, TvContract.buildChannelUriForPassthroughInput(inputId))
        } catch (exception: SecurityException) {
            showStatus("This TV did not allow access to the input")
        } catch (exception: IllegalArgumentException) {
            showStatus("Input unavailable")
        }
    }

    override fun onStop() {
        if (::tv.isInitialized) tv.reset()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    private fun showStatus(message: String) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        status.text = "$label\n$message"
        status.visibility = View.VISIBLE
    }

    companion object {
        const val INPUT_ID = "inputId"
        const val INPUT_LABEL = "inputLabel"
    }
}
