package net.ktakahas.hatebuquick

import java.net.URLEncoder
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * はてなのOAuth 1.0a (HMAC-SHA1) 署名を作るための最小実装。
 * https://developer.hatena.ne.jp/ja/documents/auth/apis/oauth/
 */
object OAuth1Signer {

    fun sign(
        method: String,
        url: String,
        extraParams: Map<String, String>,
        consumerKey: String,
        consumerSecret: String,
        token: String? = null,
        tokenSecret: String? = null,
    ): String {
        val oauthParams = linkedMapOf(
            "oauth_consumer_key" to consumerKey,
            "oauth_nonce" to nonce(),
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to (System.currentTimeMillis() / 1000).toString(),
            "oauth_version" to "1.0",
        )
        if (token != null) oauthParams["oauth_token"] = token

        val allParams = (oauthParams + extraParams).toSortedMap()
        val paramString = allParams.entries.joinToString("&") {
            "${percentEncode(it.key)}=${percentEncode(it.value)}"
        }
        val baseString = listOf(
            method.uppercase(),
            percentEncode(url),
            percentEncode(paramString),
        ).joinToString("&")

        val signingKey = "${percentEncode(consumerSecret)}&${percentEncode(tokenSecret ?: "")}"
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(signingKey.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val signatureBytes = mac.doFinal(baseString.toByteArray(Charsets.UTF_8))
        oauthParams["oauth_signature"] = b64(signatureBytes)

        return "OAuth " + oauthParams.entries.joinToString(", ") {
            "${percentEncode(it.key)}=\"${percentEncode(it.value)}\""
        }
    }

    private fun nonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun b64(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun percentEncode(s: String): String =
        URLEncoder.encode(s, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
}
