package net.ktakahas.hatebuquick

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 初回のOAuth連携設定。
 * Consumer Key/Secretはこのアプリ自体の識別情報としてビルド時に埋め込み済み(BuildConfig)。
 * ユーザーは自分のはてなアカウントでログイン・許可するだけでよい。
 * はてなのOAuthはコールバックURLに任意の値を受け付けず "oob" 固定のため、
 * ブラウザでの許可後に表示される認証コードを手入力してもらう。
 */
class SetupActivity : Activity() {

    private lateinit var store: TokenStore
    private lateinit var statusText: TextView
    private lateinit var instructionText: View
    private lateinit var reconnectButton: Button
    private lateinit var connectButton: Button
    private lateinit var verifierLabel: View
    private lateinit var verifierInput: EditText
    private lateinit var submitVerifierButton: Button
    private var pendingUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        store = TokenStore(this)
        statusText = findViewById(R.id.statusText)
        instructionText = findViewById(R.id.instructionText)
        reconnectButton = findViewById(R.id.reconnectButton)
        connectButton = findViewById(R.id.connectButton)
        verifierLabel = findViewById(R.id.verifierLabel)
        verifierInput = findViewById(R.id.verifierInput)
        submitVerifierButton = findViewById(R.id.submitVerifierButton)
        pendingUrl = intent.getStringExtra(EXTRA_PENDING_URL)

        connectButton.setOnClickListener { startAuthorize() }
        reconnectButton.setOnClickListener { showConnectForm() }

        submitVerifierButton.setOnClickListener {
            val verifier = verifierInput.text.toString().trim()
            if (verifier.isEmpty()) {
                Toast.makeText(this, "認証コードを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            exchangeAccessToken(verifier)
        }

        // すでに連携済みで、共有からの保存待ちでもなければ、誤って再連携しないよう状態表示だけにする
        if (store.hasTokens() && pendingUrl == null) {
            showConnectedState()
        } else {
            showConnectForm()
        }
    }

    private fun startAuthorize() {
        statusText.text = "リクエストトークンを取得中..."
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val (reqToken, reqSecret) = withContext(Dispatchers.IO) {
                    HatenaOAuthClient.fetchRequestToken(
                        BuildConfig.HATENA_CONSUMER_KEY,
                        BuildConfig.HATENA_CONSUMER_SECRET,
                        OOB_CALLBACK,
                    )
                }
                store.saveRequestToken(reqToken, reqSecret)

                val authorizeUri = Uri.parse("https://www.hatena.ne.jp/oauth/authorize")
                    .buildUpon()
                    .appendQueryParameter("oauth_token", reqToken)
                    .build()
                statusText.text = "ブラウザで許可すると認証コードが表示されます。下に入力してください。"
                verifierLabel.visibility = View.VISIBLE
                verifierInput.visibility = View.VISIBLE
                submitVerifierButton.visibility = View.VISIBLE

                CustomTabsIntent.Builder().build().launchUrl(this@SetupActivity, authorizeUri)
            } catch (e: Exception) {
                statusText.text = "失敗: ${e.message}"
            }
        }
    }

    private fun exchangeAccessToken(verifier: String) {
        val reqToken = store.requestToken
        val reqSecret = store.requestTokenSecret
        if (reqToken == null || reqSecret == null) {
            statusText.text = "先に「連携する」を押してください"
            return
        }

        statusText.text = "アクセストークンを取得中..."
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val (accessToken, accessSecret) = withContext(Dispatchers.IO) {
                    HatenaOAuthClient.fetchAccessToken(
                        BuildConfig.HATENA_CONSUMER_KEY,
                        BuildConfig.HATENA_CONSUMER_SECRET,
                        reqToken,
                        reqSecret,
                        verifier,
                    )
                }
                store.saveAccessToken(accessToken, accessSecret)
                statusText.text = "連携完了しました"

                val url = pendingUrl
                if (url != null) {
                    withContext(Dispatchers.IO) { HatenaBookmarkApi.addBookmark(store, url) }
                    Toast.makeText(this@SetupActivity, "はてブに保存しました", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                statusText.text = "失敗: ${e.message}"
            }
        }
    }

    private fun showConnectedState() {
        statusText.text = "連携済みです"
        instructionText.visibility = View.GONE
        connectButton.visibility = View.GONE
        verifierLabel.visibility = View.GONE
        verifierInput.visibility = View.GONE
        submitVerifierButton.visibility = View.GONE
        reconnectButton.visibility = View.VISIBLE
    }

    private fun showConnectForm() {
        statusText.text = ""
        instructionText.visibility = View.VISIBLE
        connectButton.visibility = View.VISIBLE
        reconnectButton.visibility = View.GONE
        verifierLabel.visibility = View.GONE
        verifierInput.visibility = View.GONE
        submitVerifierButton.visibility = View.GONE
    }

    companion object {
        const val EXTRA_PENDING_URL = "pending_url"
        const val OOB_CALLBACK = "oob"
    }
}
