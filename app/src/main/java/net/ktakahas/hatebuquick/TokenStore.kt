package net.ktakahas.hatebuquick

import android.content.Context

/**
 * ユーザー個人のOAuthトークンをアプリ専用の非公開領域に保存する。
 * (この端末のみで使う前提のため、暗号化はせずSharedPreferencesに保存している)
 * Consumer Key/Secretはアプリ自体の識別情報であり、ユーザーごとではなく
 * ビルド時にBuildConfigへ埋め込まれているため、ここでは扱わない。
 */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("hatebuquick", Context.MODE_PRIVATE)

    val requestToken: String? get() = prefs.getString("request_token", null)
    val requestTokenSecret: String? get() = prefs.getString("request_token_secret", null)
    val accessToken: String? get() = prefs.getString("access_token", null)
    val accessTokenSecret: String? get() = prefs.getString("access_token_secret", null)

    fun hasTokens(): Boolean = accessToken != null && accessTokenSecret != null

    fun saveRequestToken(token: String, secret: String) {
        prefs.edit().putString("request_token", token).putString("request_token_secret", secret).apply()
    }

    fun saveAccessToken(token: String, secret: String) {
        prefs.edit().putString("access_token", token).putString("access_token_secret", secret).apply()
    }
}
