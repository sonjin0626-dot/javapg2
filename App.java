import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public class App {
    static final Path DATA = Path.of("todos.txt");
    static final List<Todo> todos = new ArrayList<>();
    static int nextId = 1;
    static final Comparator<Todo> BY_DUE_DATE = Comparator
            .comparing((Todo todo) -> todo.dueDate, Comparator.nullsLast(String::compareTo))
            .thenComparingInt(todo -> todo.id);

    public static void main(String[] args) throws Exception {
        load();
        int port = Integer.getInteger("todo.port", 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", e -> {
            try {
                String path = e.getRequestURI().getPath();
                if (path.equals("/add") && e.getRequestMethod().equals("POST")) {
                    String form = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String title = formValue(form, "todo").trim();
                    String dueDate = formValue(form, "due_date").trim();
                    if (!title.isEmpty()) {
                        try {
                            if (!dueDate.isEmpty())
                                LocalDate.parse(dueDate);
                            synchronized (todos) {
                                todos.add(new Todo(nextId++, title, false, dueDate.isEmpty() ? null : dueDate));
                                save();
                            }
                        } catch (DateTimeParseException ignored) {
                            // 不正な日付は保存しない。
                        }
                    }
                    redirect(e);
                    return;
                }
                if ((path.equals("/done") || path.equals("/delete")) && e.getRequestMethod().equals("GET")) {
                    try {
                        int id = Integer.parseInt(formValue(e.getRequestURI().getRawQuery(), "id"));
                        synchronized (todos) {
                            if (path.equals("/delete")) {
                                todos.removeIf(todo -> todo.id == id);
                            } else {
                                for (Todo todo : todos)
                                    if (todo.id == id)
                                        todo.done = true;
                            }
                            save();
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    redirect(e);
                    return;
                }
                if (path.equals("/delete-done") && e.getRequestMethod().equals("POST")) {
                    synchronized (todos) {
                        todos.removeIf(todo -> todo.done);
                        save();
                    }
                    redirect(e);
                    return;
                }
                if (!path.equals("/")) {
                    send(e, 404, "text/plain", "ページが見つかりません");
                    return;
                }

                List<Todo> sorted = sortedTodos();
                String filter = formValue(e.getRequestURI().getRawQuery(), "filter");
                List<Todo> visible = new ArrayList<>();
                int remaining = 0;
                int completed = 0;
                for (Todo todo : sorted) {
                    if (todo.done) {
                        completed++;
                    } else {
                        remaining++;
                    }
                    if (filter.equals("done") && !todo.done)
                        continue;
                    if (filter.equals("remaining") && todo.done)
                        continue;
                    visible.add(todo);
                }
                StringBuilder html = new StringBuilder(
                        "<!doctype html><html lang=\"ja\"><head><meta charset=\"UTF-8\">")
                        .append("<title>わたしのTodo</title><style>body{max-width:600px;margin:40px auto;padding:0 16px;font-size:16px}</style>")
                        .append("</head><body><h1>わたしのTodo</h1>");
                if (sorted.isEmpty())
                    html.append("<p>やることは、いまゼロです</p>");
                html.append("<p>残り: ").append(remaining).append("件</p>");
                html.append("<p>").append(sorted.size()).append("件中")
                        .append(completed).append("件 完了</p>");
                if (!sorted.isEmpty() && remaining == 0)
                    html.append("<p>全件完了</p>");
                html.append("<nav><a href=\"/?filter=remaining\">未完了だけ</a> / ")
                        .append("<a href=\"/?filter=done\">完了だけ</a> / ")
                        .append("<a href=\"/\">全部</a></nav>");
                html.append("<ul style=\"list-style:none;padding-left:0\">");
                int number = 1;
                for (Todo todo : visible) {
                    html.append("<li>").append(number++).append(". ").append(escape(todo.title));
                    if (todo.dueDate != null)
                        html.append(" （期限: ").append(escape(todo.dueDate)).append("）");
                    html.append(todo.done ? " ✓" : "").append(" <a href=\"/done?id=").append(todo.id)
                            .append("\">完了</a> <a href=\"/delete?id=").append(todo.id).append("\">削除</a></li>");
                }
                html.append("</ul><form method=\"post\" action=\"/delete-done\" ")
                        .append("onsubmit=\"return confirm('完了済みをすべて削除しますか？')\">")
                        .append("<button type=\"submit\">完了済みを一括削除</button></form>")
                        .append("<form method=\"post\" action=\"/add\"><input name=\"todo\" required>")
                        .append("<label>期限（任意）<input type=\"date\" name=\"due_date\"></label>")
                        .append("<button type=\"submit\">追加</button></form></body></html>");
                send(e, 200, "text/html", html.toString());
            } catch (IOException ex) {
                ex.printStackTrace();
                try {
                    send(e, 500, "text/plain", "保存エラー");
                } catch (IOException ignored) {
                    e.close();
                }
            }
        });
        server.createContext("/api/todos", e -> {
            if (!e.getRequestMethod().equals("GET")) {
                e.sendResponseHeaders(405, -1);
                e.close();
                return;
            }
            StringBuilder json = new StringBuilder("[");
            for (Todo todo : sortedTodos()) {
                if (json.length() > 1)
                    json.append(',');
                json.append("{\"title\":\"").append(escapeJson(todo.title)).append("\",\"done\":")
                        .append(todo.done).append(",\"dueDate\":");
                json.append(todo.dueDate == null ? "null" : "\"" + escapeJson(todo.dueDate) + "\"").append('}');
            }
            send(e, 200, "application/json", json.append(']').toString());
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:" + port + " （停止: Ctrl+C）");
    }

    static synchronized void load() throws IOException {
        if (!Files.exists(DATA))
            return;
        for (String line : Files.readAllLines(DATA, StandardCharsets.UTF_8)) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 4)
                throw new IOException("保存データの形式が正しくありません");
            int id = Integer.parseInt(fields[0]);
            String title = new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8);
            todos.add(new Todo(id, title, "1".equals(fields[2]), fields[3].isEmpty() ? null : fields[3]));
            nextId = Math.max(nextId, id + 1);
        }
    }

    static void save() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Todo todo : todos) {
            String title = Base64.getEncoder().encodeToString(todo.title.getBytes(StandardCharsets.UTF_8));
            lines.add(todo.id + "\t" + title + "\t" + (todo.done ? "1" : "0") + "\t"
                    + (todo.dueDate == null ? "" : todo.dueDate));
        }
        Path temp = DATA.resolveSibling(DATA.getFileName() + ".tmp");
        Files.write(temp, lines, StandardCharsets.UTF_8);
        Files.move(temp, DATA, StandardCopyOption.REPLACE_EXISTING);
    }

    static List<Todo> sortedTodos() {
        synchronized (todos) {
            List<Todo> sorted = new ArrayList<>(todos);
            sorted.sort(BY_DUE_DATE);
            return sorted;
        }
    }

    static String formValue(String form, String key) {
        if (form == null)
            return "";
        for (String pair : form.split("&")) {
            String[] parts = pair.split("=", 2);
            if (key.equals(URLDecoder.decode(parts[0], StandardCharsets.UTF_8)))
                return parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
        }
        return "";
    }

    static void redirect(HttpExchange e) throws IOException {
        e.getResponseHeaders().set("Location", "/");
        e.sendResponseHeaders(303, -1);
        e.close();
    }

    static void send(HttpExchange e, int status, String type, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        e.sendResponseHeaders(status, body.length);
        try (OutputStream out = e.getResponseBody()) {
            out.write(body);
        }
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    static String escapeJson(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"')
                result.append("\\\"");
            else if (ch == '\\')
                result.append("\\\\");
            else if (ch < 0x20)
                result.append(String.format("\\u%04x", (int) ch));
            else
                result.append(ch);
        }
        return result.toString();
    }

    static class Todo {
        final int id;
        final String title;
        boolean done;
        final String dueDate;

        Todo(int id, String title, boolean done, String dueDate) {
            this.id = id;
            this.title = title;
            this.done = done;
            this.dueDate = dueDate;
        }
    }
}
