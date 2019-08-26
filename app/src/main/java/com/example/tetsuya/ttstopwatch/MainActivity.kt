package com.example.tetsuya.ttstopwatch

import android.app.*
import android.content.*
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.ArrayAdapter
import android.support.v4.app.NotificationCompat
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.os.Build
import android.support.annotation.RequiresApi

/*
課題メモ
・回転時、リスタートさせない。
　⇒ Manifestに「android:configChanges="orientation|screenSize">」を追加することで解決。

・非アクティブ後のアクティブ時、リスタートさせない。
　⇒
・通知テスト、OS9(S2)では表示しなかった。
　⇒ OREO以降、仕様が変わった。以下を参考に解決。
　　https://qiita.com/naoi/items/367fc23e55292c50d459
　　以下のはわかりやすい。
　　https://joyplot.com/documents/2018/04/27/kotlin-android-notification/

・通知をタップしてもアプリが開かない。
　⇒ notificationのsetContentIntent設定が必要だった。

・通知をタップしたら画面が初期化される。
　⇒ 調査中。

・アクティブ中は通知したくない。
　⇒ 調査中。

・指定秒ごと通知が出るが、テスト用の通知(MENUボタン)を出すとでなくなる。
　⇒ 指定が悪い。そもそも、メソッド化したい。


・設定で
　　①通知タイミングを設定したい。
　　②通知の有無を設定したい。
　⇒

・通知をメソッド化


 */

//public fun Context.clipboardManager(): ClipboardManager {
//    return getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//}

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    // 一度だけ代入するものは val
    val handler = Handler()
    // 繰り返し代入するため val
    var timeValue = 0
    // infoText表示用の時間
    var infoStartTimeValue = 0
    // infoText表示時間(ミリ秒)
    val infoTextViewTimeValue = 100

    // ラップタイム配列
    var lapTimeArray : MutableList<String> = mutableListOf()

//    val prefs = getSharedPreferences("TTSettings", AppCompatActivity.MODE_PRIVATE)
//    // 通知タイミング
//    // getSharedPreferences()済のプリファレンスからDATA1_KEYの文字列値を取得
//    //  getxxx()の第2引数は、値がなかった場合のデフォルト
//    val notificationSecond = prefs.getInt("notificationSecond", 10)
//    val nothificationView = prefs.getBoolean("notificationView", true)
/*
    -- 通知のオブジェクトをここで宣言しようとしたけど思うように行かず
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notificationBuilder = NotificationCompat.Builder(this, "Sample")
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("現在の時間")
            .setContentText("")
            .setSubText("SubText")
            .setContentInfo("Information")
//              .setWhen(1400000000000l)
            .setTicker("Ticker")
            .setSound(defaultSoundUri)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .build()
     */

//    var builder = NotificationCompat.Builder(applicationContext())

    // 通知？
    // https://qiita.com/SYABU555/items/c6d828b4c29c545a58f3
    inline fun notification(context: Context,
            func: Notification.Builder.() -> Unit): Notification {
        val builder = Notification.Builder(context)
        builder.func()
        return builder.build()
    }

    // メニュー作成
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // menu_main.xml
        menuInflater.inflate(R.menu.menu_main, menu)
//        return super.onCreateOptionsMenu(menu)
        return true
    }

    // オプションメニューアイテム選択時
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /*
        標準で「onOptionsItemSelected(item: MenuItem?)」と「？」が付くが、
        NULLの可能性がないため除去
         */

        val id = item.itemId
        when (id) {
            // メニュー - 「設定」のとき
            R.id.settingMenu -> {
                // Intentのインスタンスを作成 … 設定画面
                val intent = Intent(this, SettingsActivity::class.java)
                // 設定画面に遷移
                startActivity(intent)
                return true
            }
            null -> return true
        }

        return super.onOptionsItemSelected(item)
    }

//    // バックキー
//    override fun onBackPressed() {
//        // アプリ終了
//        moveTaskToBack(true)
//    }

    // キーイベント
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            // バックキーのとき
            KeyEvent.KEYCODE_BACK -> return false
        }

        // それ以外
        return super.onKeyDown(keyCode, event)
    }

    // アクティビティ作成時
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 画面レイアウトactivity_main.xmlより、オブジェクトの参照
        setContentView(R.layout.activity_main)

        // View要素を変数に代入
//        val timeText = findViewById<TextView>(R.id.timeText)
        val timeText: TextView = findViewById(R.id.timeText)
        val startButton: Button = findViewById(R.id.start)
        val stopButton: Button = findViewById(R.id.stop)
        val resetButton: Button = findViewById(R.id.reset)
        val lapButton: Button = findViewById(R.id.lap)
        val lapTimeListView: ListView = findViewById(R.id.lapTimeList)
        val infoText: TextView = findViewById(R.id.infoText)
        val testButton: Button = findViewById(R.id.testButton)

//        // テスト用ボタンなので非表示にする
//        testButton.visibility = View.INVISIBLE

//        // クリップボードマネージャ
//        fun Context.getClipboardManager(): ClipboardManager =
//            getSystemServiceAs(Context.CLIPBOARD_SERVICE)

        // クリップボードにコピー
        /*
            @param context the context to use
            @param label user-visible label for the clip data
            @param text the actual text in the clip
            @return result
         */
//        fun copyToClicpboard(context:Context, label:String, text:String): Boolean {
//            val clipboard = context.sys
//            val ClipboardManager: clipboardManager = Context.(Context.CLIPBOARD_SERVICE)
//
//
//            return true
//        }

        // ボタン有効・無効化
        fun buttonEnabledInTimeCount(isTimeCount: Boolean = false) {
            startButton.isEnabled = !isTimeCount   // 無効化
            stopButton.isEnabled = isTimeCount     // 有効化
            lapButton.isEnabled = isTimeCount      // 有効化
//            if (isTimeCount) {
//                // 配列の初期化
//                lapTimeArray = mutableListOf()
//            }
        }

        // STOPボタンは最初無効
        buttonEnabledInTimeCount()
//        resetButton.setOnClickListener {  }
        timeToText()?.let {
            timeText.text = it
        }

        // ストップウォッチ用 1/100秒ごとに処理を実行
        val runnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                timeValue++
                // TextView 更新
                // ?.letを用いて、nullではない場合のみ更新
                timeToText(timeValue)?.let {
                    // timeToText(timeValue)の値がlet内ではitとして使える
                    timeText.text = it
                }
//                val postDelayed = handler.postDelayed(this, 1000)
                // 1/100秒
                handler.postDelayed(this, 10)

                // (1秒60カウント)通知を出してみる
                if (timeValue % (60 * 10) == 0) {
                    // 通知テスト (2) >>>
                    // NotificationCompat.Builder の contact の書き方に注意("this"では無効だった)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    // カテゴリー名（通知設定画面に表示される情報）
                    val name = "通知のタイトル的情報を設定"
                    // システムに登録するChannelのID
                    val id = "casareal_chanel"
                    // 通知の詳細情報（通知設定画面に表示される情報）
                    val notifyDescription = "この通知の詳細情報を設定します"

                    // Channelの取得と生成
                    if (notificationManager.getNotificationChannel(id) == null) {
                        val mChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
                        mChannel.apply {
                            description = notifyDescription
                        }
                        notificationManager.createNotificationChannel(mChannel)
                    }

                    // 通知タップでアプリを開くための定義
//                    val pending: PendingIntent = PendingIntent.getActivity(this@MainActivity, 0, Intent(this@MainActivity, MainActivity::class.java), 0)
//                    val intent = Intent(this@MainActivity, MainActivity::class.java)
                    val intent = Intent(
                            this@MainActivity, MainActivity::class.java)
//                    intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP)
                    // ↑「対象アクティビティが最終履歴（Back Stack 先頭）であれば、アクティブにするのみで履歴に新規追加しない。」はず。。。
                    val stackBuilder = TaskStackBuilder.create(this@MainActivity)
                    stackBuilder.addParentStack(MainActivity::class.java)
                    stackBuilder.addNextIntent(intent)
                    val pendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_NO_CREATE)
                    // FLAG_NO_CREATE ← 直前のやつを再利用？
                    // FLAG_UPDATE_CURRENT ← もともとこっち

                    // 通知
                    val notification = NotificationCompat
                            .Builder(this@MainActivity, id)
                            .apply {
                                setSmallIcon(R.drawable.ic_launcher_background)
//                                mContentTitle = "現在の時間"
//                                mContentText = timeText.text
                                setContentTitle("現在の時間")
                                setContentText(timeText.text)
                                setSmallIcon(android.R.drawable.sym_def_app_icon)   // 通知アイコン
                                setContentIntent(pendingIntent)           // 戻り先？
                                setAutoCancel(true)         // 通知クリックでクリア
                                setDefaults(Notification.DEFAULT_ALL)   // 通知が届いたとき発音・振動
                            }.build()
                    notificationManager.notify(1, notification)
                    // <<< 通知テスト (2)
                }
            }
        }

        // infoTextフェードアウト用 1/100秒ごとに処理を実行
        val runnable2 = object : Runnable {
            override fun run() {
                // infoTextが消えたとき、タイマーハンドラ(自分自身)を停止
                // ……させたいができないので強引にIF文で……
                if (infoText.alpha > 0) {

                    // カウンタをインクリメント
                    infoStartTimeValue++

                    // 表示時間を超えたとき
                    if (infoStartTimeValue > infoTextViewTimeValue) {
                        // 徐々に消える(はずが、即消える)
                        infoText.alpha = 1.0F - (infoStartTimeValue - infoTextViewTimeValue).toFloat() / 100F
                    }

                    // 1/100秒
                    handler.postDelayed(this, 10)
                }
            }
        }

        // 情報表示
        fun setInfoText(messageText: String) {
            infoText.alpha = 1.0F
            infoStartTimeValue = 0
            infoText.text = messageText
            setTitle(messageText)   // アクションバーのタイトルを変更
            handler.post(runnable2)
        }

        /*
            フェードアウトの処理を考えてみる。
            ・1秒表示。
            ・そこから1秒かけてフェードアウト。
         */
        // start
        startButton.setOnClickListener {
            // カウント開始
            handler.post(runnable)
            // ボタン設定
            buttonEnabledInTimeCount(true)
        }

        // stop
        stopButton.setOnClickListener {
            // カウントを停止する
            handler.removeCallbacks(runnable)
            // ボタン設定
            buttonEnabledInTimeCount()
        }

        // reset
        resetButton.setOnClickListener {
            // カウントを停止する
            handler.removeCallbacks(runnable)
            // カウントリセット
            timeValue = 0
            // timeToTextの引数はデフォルト値が設定されているので、引数省略できる
            timeToText()?.let {
                timeText.text = it
            }
            // ボタン設定
            buttonEnabledInTimeCount()

            // 配列の初期化
            lapTimeArray = mutableListOf()
        }

        // lap Time 追加
        lapButton.setOnClickListener {
//            // ※ ListViewへのお試し
//            val dataArray = arrayOf("Kotlin","Android","iOS","Swift","Java")
//            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataArray)
//            lapTimeListView.adapter = adapter

            // Index最大値取得
            val maxIndex = lapTimeArray.size
            setInfoText("Lap > " + timeText.text)
            // 現在のタイムを配列にセット
            // [ラップ数] + " : " + [ラップタイム]
            lapTimeArray.add((maxIndex + 1).toString() + " : " + timeText.text.toString())
//            infoText.text = lapTimeArray[maxIndex] // デバッグ用

            // adapterに配列をセット
            val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lapTimeArray)
            // listViewのアイテムにセット
            lapTimeListView.adapter = adapter
            // 最下行をアクティブに
            lapTimeListView.setSelection(maxIndex)

//            infoText.text = timeText.text
        }
        
        // ラップタイムリストをロングタップ
        lapTimeListView.setOnItemLongClickListener { parent, view, i, l ->
            // ラップタイムを取得 (取り敢えずTextViewに)
            // 値取得
            val itemText: TextView = view.findViewById(android.R.id.text1)
            // 値確認用
            setInfoText("To Clipboard > " + itemText.text)
            val textTemp = itemText.text

            // クリップボード
            if (SetClipData(textTemp.toString())) {

            }
            // ↓これは違う、クリップボードイベントを取得するもののよう
//            ClipboardManager.OnPrimaryClipChangedListener { itemText.text }
//            val cm = ClipboardManager


            return@setOnItemLongClickListener true
        }
        // テスト用ボタン
//        testButton.setOnClickListener {
//            // infoTextのalpha値を取得
//            infoText.alpha = (infoText.alpha * 0.75).toFloat()
//            testButton.text = "alpha = " + infoText.alpha
//        }

        testButton.setOnClickListener {
            /*
            // infoTextのalpha値を取得
            infoText.alpha = (infoText.alpha * 0.75).toFloat()
            testButton.text = "alpha = " + infoText.alpha
            */
            /*
            // Intentのインスタンスを作成 … 設定画面
            val intent = Intent(this, SettingsActivity::class.java)
            // 設定画面に遷移
            startActivity(intent)
            */

//            // 通知テスト (1) >>>
//            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//            val builder = NotificationCompat.Builder(this, "Sample")
//                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
//                    .setContentTitle("Title")
////                    .setContentText("Hello World!!")
//                    .setContentText("Hello World!!")
//                    .setSubText("SubText")
//                    .setContentInfo("Information")
////                    .setWhen(1400000000000l)
//                    .setTicker("Ticker")
//                    .setSound(defaultSoundUri)
//                    .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
//                    .setGroup("none")
//                    .build()
//
//            val notificationId = 1
//
//            NotificationManagerCompat.from(applicationContext)
//                    .notify(notificationId, builder)
//            // <<< 通知テスト (1)

            // 通知テスト (2) >>>
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // カテゴリー名（通知設定画面に表示される情報）
            val name = "通知のタイトル的情報を設定"
            // システムに登録するChannelのID
            val id = "casareal_chanel"
            // 通知の詳細情報（通知設定画面に表示される情報）
            val notifyDescription = "この通知の詳細情報を設定します"

            // Channelの取得と生成
            if (notificationManager.getNotificationChannel(id) == null) {
                val mChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
                mChannel.apply {
                    description = notifyDescription
                }
                notificationManager.createNotificationChannel(mChannel)
            }

            // 通知タップでアプリを開くための定義
            val pending: PendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),0)
            // 通知
            val notification = NotificationCompat
                    .Builder(this, id)
                    .apply {
                        setSmallIcon(R.drawable.ic_launcher_background)
//                        mContentTitle = "タイトルだよ"
//                        mContentText = "内容だよ"
                        setContentTitle("タイトルだよ2")     // タイトル
                        setContentText("内容だよ2")         // 内容
                        setSmallIcon(android.R.drawable.sym_def_app_icon)   // 通知アイコン
                        setContentIntent(pending)           // 戻り先？
                    }.build()
            notificationManager.notify(1, notification)
            // <<< 通知テスト (2)
        }
    }

    // 数値を 00:00:00 形式の文字列に変換する関数
    // 引数timeにはデフォルト値0を設定、返却する型はnullableなString?型
    private fun timeToText(time: Int = 0): String? {
        // if形式は値を返すため、そのままreturnできる
        return if (time < 0) {
            null
        } else if (time == 0) {
            // リセット
            "00:00:00.00"
        } else {
            val timeDevide100 = time / 60
            val h = timeDevide100 / 3600        // 時
            val m = timeDevide100 % 3600 / 60   // 分
            val s = timeDevide100 % 60          // 秒
            val ss = time % 100                 // ミリ秒
            // 時:分:秒.ミリ秒 書式で返す
            "%1$02d:%2$02d:%3\$02d.%4\$02d".format(h, m, s, ss)
        }
    }

    // クリップボードに文字列セット
    private fun SetClipData(text: String): Boolean {
        try {
            //クリップボードに格納するItemを作成
            val item = ClipData.Item(text)
            //MIMETYPEの作成
            val mimeType = arrayOfNulls<String>(1)
            mimeType[0] = ClipDescription.MIMETYPE_TEXT_URILIST
            //クリップボードに格納するClipDataオブジェクトの作成
            val clipData = ClipData(ClipDescription("text_data", mimeType), item)
            //クリップボードにデータを格納
            val clipManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipManager.primaryClip = clipData
            return true

//            textView.text = "this is " + this::class.java.simpleName + ". " + navigationItem.title
        } catch (e: Exception) {
            return false
        }
    }

//    // 設定画面表示
//    fun openSettingsActivity(view: View) {
//        startActivity(Intent(this, SettingsActivity::class.java))
//    }
}
