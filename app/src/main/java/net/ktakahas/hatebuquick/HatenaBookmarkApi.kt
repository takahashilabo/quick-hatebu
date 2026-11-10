package net.ktakahas.hatebuquick

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/**
 * はてなブックマーク REST API でブックマークを追加する。
 * https://developer.hatena.ne.jp/ja/documents/bookmark/apis/rest/bookmark
 */
object HatenaBookmarkApi {
    private val client = OkHttpClient()
    private const val ENDPOINT = "https://bookmark.hatenaapis.com/rest/1/my/bookmark"
    private const val USER_AGENT = "HatebuQuick/1.0"

    fun addBookmark(store: TokenStore, url: String) {
        val accessToken = store.accessToken ?: error("未連携です")
        val accessTokenSecret = store.accessTokenSecret ?: error("未連携です")

        val params = mapOf("url" to url)
        val authHeader = OAuth1Signer.sign(
            "POST", ENDPOINT, params, BuildConfig.HATENA_CONSUMER_KEY, BuildConfig.HATENA_CONSUMER_SECRET,
            token = accessToken, tokenSecret = accessTokenSecret,
        )

        val fullUrl = "$ENDPOINT?url=${URLEncoder.encode(url, "UTF-8")}"
        val request = Request.Builder()
            .url(fullUrl)
            .post(ByteArray(0).toRequestBody(null))
            .header("Authorization", authHeader)
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("HTTP ${resp.code}: ${resp.body?.string()}")
            }
        }
    }
}
