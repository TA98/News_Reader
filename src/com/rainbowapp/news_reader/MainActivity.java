package com.rainbowapp.news_reader;


import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.xmlpull.v1.XmlPullParser;
import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

 
public class MainActivity extends Activity {
	
	static String strUrl, title;
 
	Future<?> waiting = null;
	ExecutorService executorService;
	static String content;
 
	//別スレッドで実行しているコードからUIを操作することができない為、画面の書き換え等できるようにinMainThreadという名前のRunnableを作成する。
	Runnable inMainThread = new Runnable() {
		@Override
		public void run() {
			// main.xmlで作成したButtonと紐付けし、画面に表示させる
			View btn = findViewById(R.id.button1);
			// main.xmlで作成したTextViewと紐付けし、画面に表示させる
			TextView textView = (TextView) findViewById(R.id.textview);
			// Javaのコードからリソース文字列（メッセージ）を参照する
			if (content == "")
				content = getResources().getString(R.string.message_error);
			textView.setText(Html.fromHtml(content));
			// TextViewのLinkMovementMethodを登録。この一行がないとリンク表示されるけどクリックした時に何も起こらない
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			// 貼られたリンクを、クリックできるかどうかを指定する。trueは、クリック時に対応するアクティビティを起動する。(デフォルト)
			textView.setLinksClickable(true);
			setTitle(title);
		}
	};
 
	// 「runメソッド」に定義したコードを別のスレッドで実行することが可能であるため、
	// 「inReadingThread」という名前でOverrideした「readRssメソッド」を呼び出す形にした。
	Runnable inReadingThread = new Runnable() {
		@Override
		public void run() {
			content = readRss(false);
			runOnUiThread(inMainThread);
		}
	};
 
	// onCreateメソッドで、RSSのURLをSharedPreferrencesまたはリソースから読み取って文字列「strUrl」に格納し
	// 「UpDate」Buttonのリスナーを登録した。
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
//RSSのURLを SharedPreferencesまたは、リソースからファイル読み取って文字列値「strUrl」に格納する。
SharedPreferences prefs = getSharedPreferences("RssReaderPrefs",
				MODE_PRIVATE);
		
	strUrl= prefs.getString("server",getResources().
		getTextArray(R.array.ServiceUrl)[0].toString());
 
//Buttonをクリックすると、Rssの読み取りを実行する「showRss」メソッドが呼び出される為に
//setOnClickListenerを使用し、起動直後にも「showRss」メソッドが呼び出されるよう、「showRss();」で指定している。
 
 
	findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
 
	@Override
	public void onClick(View v) {
		showRss();
	}
});
		showRss();
	}
 
	// 「title」はアプリのタイトルバーに表示する文字列を格納するための変数である。
	// readRssメソッド内でRSSのタイトルを読み込み格納するが、読み取れない可能性がある為、最初にタイトルをセットしている。
 
	private void showRss() {
		// Titleはアプリのタイトルバーに表示する文字列を格納するための変数である。
		title = getResources().getString(R.string.app_name);
 
		// 「UpDate」ButtonがRSSの読み込み中に押されないよう、無効化にする。
		// そのソースコードが「btn.set.Enabled(faise);」である。
		// 「UpDate」ボタンが読み込み中に押されないよう、無効化する。
		View btn = findViewById(R.id.button1);
		btn.setEnabled(false);
		// ExecutorServiceクラスのオブジェクトを作成し、「inReadingThread」を別スレッドで実行する。
		executorService = Executors.newSingleThreadExecutor();
		if (waiting != null)
			waiting.cancel(true);
		waiting = executorService.submit(inReadingThread);
	}
 
	// RSSの読み取りと解析を行うには「readRssメソッド」なので、readRssメソッドを呼び出す。
	public String readRss(boolean simple) {
		
		String str = "";
		HttpURLConnection connection = null;
 
	try {
 
//サーバとの接続を表す抽象クラス。このオブジェクトは上記 URL クラスの openConnection() メソッドで返される。
		URL url = new URL(strUrl);			
connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
//XmlPullParserを使用して、XMLの内容を解析する。流れとして、「next」メソッドでタグやテキストを順に移動し、「getname」メソッドでタグ名を読み取る。必要なデータを含むタグであれば「nextText」でテキストを読み取って変数に格納していき、ドキュメントの末尾に到達するまで処理を繰り返す。
	XmlPullParser xmlpp = Xml.newPullParser();
	xmlpp.setInput(new InputStreamReader(connection.getInputStream(),
					"UTF-8"));
 
		int eventType = xmlpp.getEventType();
 
	while (eventType != XmlPullParser.END_DOCUMENT) {
		if (eventType == XmlPullParser.START_TAG) {
		if (xmlpp.getName().equalsIgnoreCase("channel")) {
//RSSタイトルを取得する。
			do {
				eventType = xmlpp.next();
	if (xmlpp.getName() != null&& xmlpp.getName()
		.equalsIgnoreCase("title")) {
			title = xmlpp.nextText();
		break;
	}
	} while (xmlpp.getName() != "item");
	}
if (xmlpp.getName() != null && xmlpp.getName().
	equalsIgnoreCase("item")) {
		String itemtitle = "title";
		String linkurl = "";
		String pubdate = "";
//記事タイトルとURL、日付を取得する。
			do {
	eventType = xmlpp.next();
		if (eventType == XmlPullParser.START_TAG) {
			String tagName = xmlpp.getName();
		if (tagName.equalsIgnoreCase("title"))
			itemtitle = xmlpp.nextText();
		else if (tagName.equalsIgnoreCase("link"))
			linkurl = xmlpp.nextText();
		else if (tagName.equalsIgnoreCase("pubdate"))
			pubdate = xmlpp.nextText();
							}
	} while (!((eventType == XmlPullParser.END_TAG) && (xmlpp
		.getName().equalsIgnoreCase("item"))));
		if(simple){
	str = str + Html.fromHtml(itemtitle).toString() + "¥n";
		}else{
	str = str + "<a href=¥ + linkurl + ¥>" + itemtitle + "</a><br>" + pubdate + "<br>";
		}
	}
}
	eventType = xmlpp.next();
}
	} catch (Exception e) {
	e.printStackTrace();
}
	finally{
	if(connection != null){
	connection.disconnect();
}
		}
//読み取ったデータから整形したテキストを生成し、メソッドの呼び出し元に返して終了する。
	return str;
	}
 
	// 「onCreateOptionsMenu」メソッドと「onOptionsItemSelected」メソッドを追加した。
	// 「onCreateOptionsMenu」に、メニュー用のxmlファイルではなく、servers.xmlファイルの内容をもとにaddメソッドでメニュー項目を追加した。
 
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
 
		// server.xmlファイルの内容をもとにaddメソッドでメニュー項目を追加。
		String[] items = getResources().getStringArray(R.array.ServiceNAME);
		for (int i = 0; i < items.length; i++)
			menu.add(0, Menu.FIRST + i, 0, items[i]);
		return super.onCreateOptionsMenu(menu);
	}
 
	// ここでは、「onCreateOptionsMenu」メソッドでメニュー項目にセットしたIDをインデックス指定して、
	// servers.xmlのServiceUrlから対応するURLを取得している。
 
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
 
		String[] items = getResources().getStringArray(R.array.ServiceUrl);
		strUrl = items[item.getItemId() - Menu.FIRST];
		SharedPreferences prefs = getSharedPreferences("RssReaderPrefs",
				MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString("server", strUrl);
		editor.commit();
		showRss();
 
		return super.onOptionsItemSelected(item);
	}
 
}