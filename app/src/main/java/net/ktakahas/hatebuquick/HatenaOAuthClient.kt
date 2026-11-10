package net.ktakahas.hatebuquick

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

/**
 * はてなのOAuth 1.0a 3-leggedフローのうち、
 * リクエストトークン取得とアクセストークン取得を行う。
 * https://developer.hatena.ne.jp/ja/documents/auth/apis/oauth/
 */
object HatenaOAuthClient {
    private val client = OkHttpClient()
    private const val USER_AGENT = "HatebuQuick/1.0"

    fun fetchRequestToken(
        consumerKey: String,
        consumerSecret: String,
        callbackUrl: String,
    ): Pair<String, String> {
        val url = "https://www.hatena.com/oauth/initiate"
        val params = mapOf("oauth_callback" to callbackUrl, "scope" to "write_public")
        val authHeader = OAuth1Signer.sign("POST", url, params, consumerKey, consumerSecret)

        val body = FormBody.Builder()
            .add("oauth_callback", callbackUrl)
            .add("scope", "write_public")
            .build()

        val map = execute(url, body, authHeader)
        return (map["oauth_token"] ?: error("oauth_tokenが取得できませんでした"))
            .let { it to (map["oauth_token_secret"] ?: error("oauth_token_secretが取得できませんでした")) }
    }

    fun fetchAccessToken(
        consumerKey: String,
        consumerSecret: String,
        requestToken: String,
        requestTokenSecret: String,
        verifier: String,
    ): Pair<String, String> {
        val url = "https://www.hatena.com/oauth/token"
        val params = mapOf("oauth_verifier" to verifier)
        val authHeader = OAuth1Signer.sign(
            "POST", url, params, consumerKey, consumerSecret,
            token = requestToken, tokenSecret = requestTokenSecret,
        )

        val body = FormBody.Builder().add("oauth_verifier", verifier).build()

        val map = execute(url, body, authHeader)
        return (map["oauth_token"] ?: error("oauth_tokenが取得できませんでした"))
            .let { it to (map["oauth_token_secret"] ?: error("oauth_token_secretが取得できませんでした")) }
    }

    private fun execute(url: String, body: FormBody, authHeader: String): Map<String, String> {
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", authHeader)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: $bodyStr")
            return parseForm(bodyStr)
        }
    }

    private fun parseForm(body: String): Map<String, String> =
        body.split("&").filter { it.contains("=") }.associate {
            val (k, v) = it.split("=", limit = 2)
            URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
        }
}
