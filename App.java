import com.sun.net.httpserver.HttpServer; // Webサーバーの道具を読み込む
import java.net.InetSocketAddress; // 待ち受ける番号を指定する道具を読み込む
import java.net.URLDecoder; // フォームの文字を読み取る道具を読み込む
import java.nio.charset.StandardCharsets; // UTF-8を指定する道具を読み込む
import java.nio.file.Files; // ファイルを読み書きする道具を読み込む
import java.nio.file.Path; // ファイルの場所を表す道具を読み込む
import java.io.IOException; // ファイルの読み書きで起きるエラーを表す
import java.io.UncheckedIOException; // 保存エラーをリクエスト処理へ伝える
import java.util.ArrayList; // Todoを入れるリストの道具を読み込む
import java.util.List; // リストの型を読み込む

class AppTodo { // App専用のTodo
    private final int id; // ★変更
    private final String title; // ★変更
    private boolean done; // ★変更

    AppTodo(int id, String title) { // ★変更
        this.id = id; // ★変更
        this.title = title; // ★変更
        this.done = false; // ★変更
    } // ★変更

    int getId() {
        return id;
    } // ★変更

    String getTitle() {
        return title;
    } // ★変更

    boolean isDone() {
        return done;
    } // ★変更

    void setDone(boolean done) {
        this.done = done;
    } // ★変更
} // ★変更

public class App { // このプログラムの名前を決める
    static List<AppTodo> todos = new ArrayList<>(); // ★変更
    static int nextId = 1; // ★変更

    public static void main(String[] args) throws Exception { // プログラムをここから始める
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待つサーバーを作る
        load(); // 起動時に保存済みのTodoを読み込む
        server.createContext("/", exchange -> { // 「/」へのアクセスを受け取る
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出す
            String method = exchange.getRequestMethod(); // ★追加
            String message; // 返す文字を用意する
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常は文字をUTF-8で返す
            if (path.equals("/add") && method.equals("POST")) { // ★追加
                String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られた文字を読む
                if (form.startsWith("todo=")) { // Todo欄の値があるとき
                    String title = URLDecoder.decode(form.substring(5), StandardCharsets.UTF_8); // ★変更
                    todos.add(new AppTodo(nextId, title)); // ★変更
                    nextId++; // ★変更
                    save(); // 追加した一覧を保存する
                }
                exchange.getResponseHeaders().set("Location", "/"); // ★変更
                exchange.sendResponseHeaders(303, -1); // ★変更
                exchange.close(); // ★変更
                return; // ★変更
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加
                    try { // ★追加
                        int id = Integer.parseInt(query.substring(3)); // ★追加
                        for (AppTodo todo : todos) { // ★追加
                            if (todo.getId() == id) { // ★追加
                                todo.setDone(true); // ★追加
                                save(); // 完了状態が変わった一覧を保存する
                                break; // ★追加
                            } // ★追加
                        } // ★追加
                    } catch (NumberFormatException e) { // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加
                    try { // ★追加
                        int id = Integer.parseInt(query.substring(3)); // ★追加
                        if (todos.removeIf(todo -> todo.getId() == id)) { // 削除できた場合だけ保存する
                            save(); // 削除後の一覧を保存する
                        } // 削除時の保存処理を終える
                    } catch (NumberFormatException e) { // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/")) { // ★変更
                String html = "<!doctype html><html lang=\"ja\"><head><meta charset=\"UTF-8\">"
                        + "<title>わたしのTodo</title>"
                        + "<style>body { max-width: 600px; margin: 40px auto; padding: 0 16px; font-size: 16px; }</style>"
                        + "</head><body><h1>わたしのTodo</h1>";
                if (todos.isEmpty()) {
                    html += "<p>やることは、いまゼロです</p>";
                }
                int remaining = 0; // まだ完了していないTodoの数
                for (AppTodo todo : todos) { // Todoを一つずつ調べる
                    if (!todo.isDone()) { // 完了していなければ数える
                        remaining++;
                    }
                }
                html += "<p>残り: " + remaining + "件</p>"; // 一覧の上に表示する
                if (!todos.isEmpty() && remaining == 0) { // Todoがあり、すべて完了しているとき
                    html += "<p>全件完了</p>";
                }
                html += "<ul style=\"list-style: none; padding-left: 0;\">"; // 一覧を始める
                int displayNumber = 1; // 表示する順番の番号
                for (AppTodo todo : todos) { // ★変更
                    String mark = ""; // ★変更
                    if (todo.isDone()) { // ★変更
                        mark = " ✔"; // ★変更
                    } // ★変更
                    html += "<li>" + displayNumber + ". " + todo.getTitle() + mark + " <a href=\"/done?id="
                            + todo.getId()
                            + "\">完了</a> <a href=\"/delete?id=" + todo.getId() + "\">削除</a></li>"; // ★追加
                    displayNumber++; // 次のTodoの表示番号
                } // 繰り返しを終える
                html += "</ul>"; // 箇条書きを閉じる
                html += "<form method=\"post\" action=\"/add\"><input name=\"todo\"><button type=\"submit\">追加</button></form>"; // ★変更
                html += "</body></html>";
                message = html; // 作ったHTMLを返す文字にする
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // このパスだけHTMLとして返す
            } else {
                message = "ページが見つかりません";
            }
            byte[] body = message.getBytes("UTF-8"); // 文字を送信用のデータに変える
            exchange.sendResponseHeaders(200, body.length); // 正常に返すこととデータの長さを伝える
            exchange.getResponseBody().write(body); // データを送る
            exchange.getResponseBody().close(); // 送信を終える
        }); // 「/」への処理を閉じる
        server.start(); // サーバーの待ち受けを始める
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことを表示する
    } // mainを閉じる
    static void save() { // Todo全件をUTF-8で保存する
        List<String> lines = new ArrayList<>(); // CSVの各行を入れる
        for (AppTodo todo : todos) { // Todoを一件ずつCSVに変える
            String title = todo.getTitle().replace("\"", "\"\""); // 題名内の引用符を二重にする
            lines.add(todo.getId() + "," + (todo.isDone() ? "1" : "0") + ",\"" + title + "\""); // id、完了、題名を一行にする
        } // 全件の変換を終える
        try { // 保存時の入出力エラーを扱う
            Files.write(Path.of("todos.csv"), lines, StandardCharsets.UTF_8); // 全件を書き出す
        } catch (IOException e) { // 書き込みに失敗した場合
            throw new UncheckedIOException(e); // 保存失敗を呼び出し元に知らせる
        } // 保存時のエラー処理を終える
    } // 保存処理を終える

    static void load() throws IOException { // 保存済みのTodoを読み込む
        Path file = Path.of("todos.csv"); // 保存ファイルの場所を決める
        if (!Files.exists(file)) return; // ファイルがなければ空の一覧のままにする
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) { // UTF-8で一行ずつ読む
            String[] fields = line.split(",", 3); // id、完了、題名に分ける
            if (fields.length != 3) throw new IOException("不正なTodo行: " + line); // 欄の不足を知らせる
            try { // idと完了状態を数値に変える
                int id = Integer.parseInt(fields[0]); // 保存されたidを読む
                int done = Integer.parseInt(fields[1]); // 保存された完了状態を読む
                String title = fields[2]; // 題名の欄を取り出す
                if (!title.startsWith("\"") || !title.endsWith("\"") || (done != 0 && done != 1)) throw new IllegalArgumentException(); // 形式を確認する
                title = title.substring(1, title.length() - 1).replace("\"\"", "\""); // CSVの引用符を元に戻す
                AppTodo todo = new AppTodo(id, title); // Todoを復元する
                todo.setDone(done == 1); // 完了状態を復元する
                todos.add(todo); // 一覧に戻す
                nextId = Math.max(nextId, id + 1); // 最大idの次を次回の番号にする
            } catch (IllegalArgumentException e) { // 不正な値を受け取る
                throw new IOException("不正なTodo行: " + line, e); // 読み込みエラーとして知らせる
            } // 一行の読み込みを終える
        } // 全行の読み込みを終える
    } // 読み込み処理を終える
} // Appを閉じる
