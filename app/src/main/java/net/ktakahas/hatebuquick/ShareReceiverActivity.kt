package net.ktakahas.hatebuquick

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Chromeなどの共有メニューから直接呼ばれるActivity。
 * 共有されたURLをそのまま、はてなブックマークへPOSTし、
 * 画面下に自前のフィードバックカードを一瞬出してから閉じる。
 * (システムToastはこのActivityの透明テーマの影響で見えなくなることがあるため使わない)
 */
class ShareReceiverActivity : Activity() {

    private lateinit var feedbackText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_feedback)
        feedbackText = findViewById(R.id.feedbackText)

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val url = extractUrl(sharedText)

        if (url == null) {
            showAndFinish("共有されたURLが見つかりませんでした", success = false)
            return
        }

        val store = TokenStore(this)
        if (!store.hasTokens()) {
            startActivity(
                Intent(this, SetupActivity::class.java).apply {
                    putExtra(SetupActivity.EXTRA_PENDING_URL, url)
                }
            )
            finish()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) { HatenaBookmarkApi.addBookmark(store, url) }
                showAndFinish("はてブに保存しました", success = true)
            } catch (e: Exception) {
                showAndFinish("保存失敗: ${e.message}", success = false)
            }
        }
    }

    private fun showAndFinish(message: String, success: Boolean) {
        feedbackText.text = message
        vibrate(if (success) SUCCESS_PATTERN else FAILURE_PATTERN)
        CoroutineScope(Dispatchers.Main).launch {
            delay(FEEDBACK_VISIBLE_DELAY_MS)
            finish()
        }
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    companion object {
        private val URL_REGEX = Regex("""https?://\S+""")
        private val SUCCESS_PATTERN = longArrayOf(0, 80)
        private val FAILURE_PATTERN = longArrayOf(0, 80, 80, 80)
        private const val FEEDBACK_VISIBLE_DELAY_MS = 1000L

        fun extractUrl(text: String?): String? =
            text?.let { URL_REGEX.find(it)?.value }
    }
}
