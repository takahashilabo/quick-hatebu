# クイックはてブ (Quick Hatebu)

Chromeなどで見ているページを、共有メニューからワンタップではてなブックマークに保存できる非公式のAndroidアプリです。

公式のはてなブックマークアプリは「共有 → アプリ選択 → B!アイコンをタップ → 保存をタップ」という手順が必要ですが、このアプリに共有するだけでそのまま保存されます。

**非公式の個人開発ツールです。はてな株式会社とは関係ありません。**

## 使い方

1. Chromeなどのブラウザで保存したいページを開く
2. 共有メニュー(⋮ → 共有)を開く
3. 「クイックはてブ」を選択する
4. それだけで保存完了。画面下に「はてブに保存しました」と表示されます

初回だけ、はてなアカウントとの連携(OAuth認証)が必要です(下記参照)。

## インストール方法(APK配布版)

このアプリはGoogle Playでは配布していません。[Releases](../../releases)ページから最新のAPKをダウンロードしてインストールしてください。

1. Android端末で [Releases](../../releases) を開き、最新版の `quick-hatebu-*.apk` をダウンロード
2. ダウンロードしたAPKをタップしてインストール
   - 「不明なアプリのインストールを許可」を求められたら許可してください(Playストア経由ではないため表示されます)
3. アプリ一覧から「クイックはてブ」を一度起動し、「連携する」をタップ
4. ブラウザが開くので、はてなアカウントでログインして許可する
5. 表示された認証コードをアプリの入力欄に貼り付けて送信する
6. 「連携済みです」と表示されたら準備完了

### 更新時の注意

新しいバージョンのAPKも同じ署名鍵で配布されるため、インストール済みのアプリに上書きインストールできます(アンインストール不要)。

## 自分でビルドする場合

このアプリを自分でビルドするには、はてなのOAuth Consumer Key/Secretが必要です。[はてなのAPIキー発行ページ](https://www.hatena.ne.jp/oauth/develop)から取得してください(`write_public` スコープが必要)。

1. リポジトリをクローン
2. プロジェクトルートに `local.properties` を作成し、以下を追記:
   ```properties
   sdk.dir=/path/to/your/Android/sdk
   hatena.consumerKey=あなたのConsumer Key
   hatena.consumerSecret=あなたのConsumer Secret
   ```
3. デバッグビルドする場合はこれだけでOK:
   ```sh
   ./gradlew :app:assembleDebug
   ```
4. リリース用の署名付きAPKを作る場合は、署名鍵を作成して `local.properties` に追記:
   ```sh
   keytool -genkeypair -v -keystore my-release.keystore -alias my-key \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
   ```properties
   release.storeFile=my-release.keystore
   release.storePassword=...
   release.keyAlias=my-key
   release.keyPassword=...
   ```
   ```sh
   ./gradlew :app:assembleRelease
   ```

   **署名鍵は一度作ったら無くさないように保管してください。** 鍵を失うと、同じアプリIDで配布した次のバージョンに既存ユーザーが上書き更新できなくなります。

### 開発環境

- AGP 8.5.2 / Gradle 8.9 / Kotlin 1.9.24
- JDK 17 を推奨(`org.gradle.java.home` で指定可能)
- minSdk 26 / targetSdk 34

## 技術的な仕組み

- Android の共有インテント(`ACTION_SEND`)を受け取り、はてなブックマークREST APIに直接POSTします
- 認証は OAuth 1.0a(署名は本リポジトリ内で自前実装)。はてなのOAuth実装はコールバックURLとして `oob`(out-of-band)のみに対応しているため、認証コードは画面に表示されたものを手動で貼り付ける方式になっています
- Consumer Key/Secretはビルド時に `BuildConfig` へ埋め込まれ、配布後の利用者は自分のはてなアカウントで連携するだけで使えます(自分でアプリ登録する必要はありません)

## ライセンス

特に指定なし(個人利用目的)
