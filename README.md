# Mail Filter Editor 2 (MFE2)

Mail Filter Editor 2 (MFE2) は、メールサーバーのログを分析し、フィルタリングルールを編集するためのWebベースのツールです。Spring Boot を使用して構築されており、Postfix や Dovecot などのメールサーバーのログを解析し、ブロックリストの管理やRDAPによるIP情報取得をサポートします。

## 機能

- **ログ分析**: Postfix および Dovecot のログを解析し、セッションログを管理。
- **ブロックリストエディタ**: IP アドレスやCIDRブロックのフィルタリングルールを編集。
- **RDAP 統合**: IP アドレスの所有者情報をRDAPプロトコルで取得。
- **Web インターフェース**: ブラウザからアクセス可能なWeb UI。
- **CLI 更新**: コマンドラインからのフィルタルール更新。
- **サブネット管理**: IP サブネットの管理と分析。

## 必要条件

- Java 20 以上
- Maven 3.6 以上
- Spring Boot 3.5.12

## インストール

1. リポジトリをクローンまたはダウンロードします。
2. プロジェクトディレクトリに移動します。
3. Maven を使用して依存関係をインストールします。

   ```bash
   mvn clean install
   ```

## ビルド

プロジェクトをビルドするには、以下のコマンドを実行します。

```bash
mvn compile
```

JAR ファイルを生成するには：

```bash
mvn package
```

## 使用方法

### 実行

Spring Boot アプリケーションとして実行します。

```bash
mvn spring-boot:run
```

または、生成された JAR ファイルを実行します。

```bash
java -jar target/mfe2-1.1.5.jar
```

アプリケーションはデフォルトでポート 8082 で起動します。ブラウザで `http://localhost:8082` にアクセスしてください。

### Web インターフェース

- `/`: トップページ
- `/logs`: ログ分析ページ
- `/rdap`: RDAP 情報ページ
- `/subnets`: サブネット管理ページ
- `/block_list`: ブロックリストエディタ
- `/cli_update`: CLI 更新ページ

### コマンドライン引数

アプリケーション起動時にファイルパスを引数として渡すことができます。

```bash
java -jar target/mfe2-1.1.5.jar /path/to/config/file
```

## プロジェクト構造

- `src/main/java/jp/d77/java/mfe2/`: メインソースコード
  - `Mfe2Application.java`: Spring Boot メインクラス
  - `Mfe2Main.java`: Web コントローラ
  - `BasicIO/`: 基本入出力ユーティリティ
  - `Datas/`: データ管理クラス
  - `LogAnalyser/`: ログ分析クラス
  - `Pages/`: Web ページクラス
- `src/main/resources/`: リソースファイル
  - `application.properties`: アプリケーション設定
  - `version.properties`: バージョン情報
- `pom.xml`: Maven 設定ファイル

## 貢献

バグ報告や機能リクエストは、GitHub のイシューを作成してください。プルリクエストも歓迎します。

## ライセンス

このプロジェクトは [ライセンス名] の下で公開されています。詳細は LICENSE ファイルを参照してください。

## バージョン

- バージョン: 1.1.5
- リリース日: 2026-03-21</content>
<parameter name="filePath">d:\delta\Documents\_program\java\20260125_MailFilterEditor2\mail_filter_editor2\README.md