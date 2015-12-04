# リバースジオコーダ機能

----
## 概要

リバースジオコーダ機能は，位置情報(緯度・経度)から地名を取得するためのWeb ReST APIです．
本APIは，リバースジオコーダ用データを利用し位置情報から地名情報を取得しています．

Apache Tomcat (ver.8) 上で動作するアプリケーションとして実装しており，このアプリケーションをDockerで起動させます．
検索パラメータをURLに含めてGETメソッドでリクエストを送信すると，検索結果がJSON形式で戻ります
(検索時にエラーが発生した場合もしくは検索結果が0件の場合は結果が空になります)．

リバースジオコーダ用データ(Shapeファイル)を別途用意して、Docker Imageを生成する必要があります．


----
### 1. リバースジオコーダ用データの用意

リバースジオコーダ用データを用意します．
リバースジオコーダ用データはポリゴンで表される範囲の地名情報を格納したのShapeファイルです。
以下の条件でShapeファイルを用意します。

ファイル名 |空間参照システム | データソースエンコーディング
---- |---- | ----
lv01_plg.shp | EPSG:4612, JGD2000 | Shift_JIS

フィールド

名称  |  タイプ名|  内容
---- | ---- | ---- 
行政コード  |  String|  市区町村コード
PREF  |  String|  都道府県名
PREF_YOMI  |  String|  都道府県名読み仮名
MUNI  |  String|  市区町村名
MUNI_YOMI  |  String|  市区町村名読み仮名
LV01  |  String|  地名
LV01_YOMI  |  String|  地名読み仮名

### 2. Dockerへの配備方法

1. GitHubよりZIPファイルとして本API一式をダウンロードし，Dockerの環境にコピーし，ZIPを展開します．

2. 先に用意したShapeファイルを以下にコピーします：
```
reverse-geocoder/src/main/webapp/WEB-INF/data/lv01_plg/lv01_plg.dbf
reverse-geocoder/src/main/webapp/WEB-INF/data/lv01_plg/lv01_plg.prj
reverse-geocoder/src/main/webapp/WEB-INF/data/lv01_plg/lv01_plg.qpj
reverse-geocoder/src/main/webapp/WEB-INF/data/lv01_plg/lv01_plg.shp
reverse-geocoder/src/main/webapp/WEB-INF/data/lv01_plg/lv01_plg.shx
```

3. 展開したフォルダ内直下(reverse-geocoder)に移動し，
以下のdockerコマンドを実行しDocker Imageを作成します．
```
$ docker build --no-cache -t reverse-geocoder .
```

4. Docker Containerを配備(起動)します．
```
$ docker run -itd -p 8081:8080 --name reverse-geocoder reverse-geocoder
```
**ポート番号の部分は自身の環境にあわせて変更してください．**

5. APIの動作確認
以下のURLにアクセスし，JSONが戻ることを確認します．
```
http://localhost:8081/reverse-geocoder/LonLatToAddress?lon=139.11849975585938&lat=35.38121266833199
```
**localhostではなくIPアドレスを指定してもかまいません**


----
## API仕様


APIへのリクエスト(URL)は以下となります：
```
http://[server]/reverse-geocoder/LonLatToAddress?lon=[経度]&lat=[緯度]
```
**緯経度の測地系はJGD2011で指定します．**

レスポンスとしてJSON形式の値が戻ります．フォーマットは以下のようになります：
```
{"results":{"muniCd":"[市町村コード]","lv01Nm":"[町丁目文字列]"}}"
```

例：リクエスト

```
http://localhost:8081/reverse-geocoder/LonLatToAddress?lon=139.11849975585938&lat=35.38121266833199
```

例：レスポンス(JSON)

```
{"results":{"muniCd":"14364","lv01Nm":"向原"}}
```
