import com.sun.net.httpserver.HttpServer; // Webサーバーの道具を読み込む
import java.net.InetSocketAddress; // 待ち受ける番号を指定する道具を読み込む
import java.net.URLDecoder; // フォームの文字を読み取る道具を読み込む
import java.nio.charset.StandardCharsets; // UTF-8を指定する道具を読み込む
import java.util.ArrayList; // Todoを入れるリストの道具を読み込む
import java.util.List; // リストの型を読み込む

class Todo { // ★変更
    private final int id; // ★変更
    private final String title; // ★変更
    private boolean done; // ★変更

    Todo(int id, String title) { // ★変更
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
    static List<Todo> todos = new ArrayList<>(); // ★変更
    static int nextId = 1; // ★変更

    public static void main(String[] args) throws Exception { // プログラムをここから始める
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待つサーバーを作る
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更
        egg.setDone(true); // ★変更
        todos.add(egg); // ★変更
        server.createContext("/", exchange -> { // 「/」へのアクセスを受け取る
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出す
            String method = exchange.getRequestMethod(); // ★追加
            String message; // 返す文字を用意する
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常は文字をUTF-8で返す
            if (path.equals("/add") && method.equals("POST")) { // ★追加
                String form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られた文字を読む
                if (form.startsWith("todo=")) { // Todo欄の値があるとき
                    String title = URLDecoder.decode(form.substring(5), StandardCharsets.UTF_8); // ★変更
                    todos.add(new Todo(nextId, title)); // ★変更
                    nextId++; // ★変更
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
                        for (Todo todo : todos) { // ★追加
                            if (todo.getId() == id) { // ★追加
                                todo.setDone(true); // ★追加
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
                        todos.removeIf(todo -> todo.getId() == id); // ★変更
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
                html += "<ul>"; // 箇条書きを始める
                for (Todo todo : todos) { // ★変更
                    String mark = ""; // ★変更
                    if (todo.isDone()) { // ★変更
                        mark = " ✔"; // ★変更
                    } // ★変更
                    html += "<li>" + todo.getTitle() + mark + " <a href=\"/done?id=" + todo.getId() + "\">完了</a> <a href=\"/delete?id=" + todo.getId() + "\">削除</a></li>"; // ★追加
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
} // Appを閉じる
