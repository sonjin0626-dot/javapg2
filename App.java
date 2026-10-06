import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*; // ★ SQLite
import java.util.*;

public class App {
    static final String DB = "jdbc:sqlite:todos.db"; // ★

    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection(DB); Statement s = c.createStatement()) { // ★
            s.executeUpdate("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"); // ★
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", e -> {
            try {
                String path = e.getRequestURI().getPath();
                if (path.equals("/add") && e.getRequestMethod().equals("POST")) {
                    String form = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (form.startsWith("todo=")) {
                        String title = URLDecoder.decode(form.substring(5), StandardCharsets.UTF_8);
                        try (Connection c = DriverManager.getConnection(DB);
                             PreparedStatement p = c.prepareStatement("INSERT INTO todos (title, done) VALUES (?, 0)")) { // ★
                            p.setString(1, title); p.executeUpdate(); // ★
                        }
                    }
                    redirect(e); return;
                }
                if ((path.equals("/done") || path.equals("/delete")) && e.getRequestMethod().equals("GET")) {
                    String query = e.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id=")) {
                        try {
                            int id = Integer.parseInt(query.substring(3));
                            String sql = path.equals("/done") ? "UPDATE todos SET done = 1 WHERE id = ?"
                                    : "DELETE FROM todos WHERE id = ?"; // ★
                            try (Connection c = DriverManager.getConnection(DB);
                                 PreparedStatement p = c.prepareStatement(sql)) { // ★
                                p.setInt(1, id); p.executeUpdate(); // ★
                            }
                        } catch (NumberFormatException ignored) { }
                    }
                    redirect(e); return;
                }
                if (!path.equals("/")) { send(e, 404, "text/plain", "ページが見つかりません"); return; }
                List<Todo> todos = new ArrayList<>(); // ★
                try (Connection c = DriverManager.getConnection(DB);
                     PreparedStatement p = c.prepareStatement("SELECT id, title, done FROM todos ORDER BY id");
                     ResultSet r = p.executeQuery()) { // ★
                    while (r.next()) todos.add(new Todo(r.getInt("id"), r.getString("title"), r.getInt("done") != 0)); // ★
                }
                int remaining = 0;
                StringBuilder html = new StringBuilder("<!doctype html><html lang=\"ja\"><head><meta charset=\"UTF-8\">")
                        .append("<title>わたしのTodo</title><style>body{max-width:600px;margin:40px auto;padding:0 16px;font-size:16px}</style>")
                        .append("</head><body><h1>わたしのTodo</h1>");
                if (todos.isEmpty()) html.append("<p>やることは、いまゼロです</p>");
                for (Todo todo : todos) if (!todo.done) remaining++;
                html.append("<p>残り: ").append(remaining).append("件</p>");
                if (!todos.isEmpty() && remaining == 0) html.append("<p>全件完了</p>");
                html.append("<ul style=\"list-style:none;padding-left:0\">");
                int number = 1;
                for (Todo todo : todos) {
                    html.append("<li>").append(number++).append(". ").append(escape(todo.title))
                            .append(todo.done ? " ✓" : "").append(" <a href=\"/done?id=").append(todo.id)
                            .append("\">完了</a> <a href=\"/delete?id=").append(todo.id).append("\">削除</a></li>");
                }
                html.append("</ul><form method=\"post\" action=\"/add\"><input name=\"todo\">")
                        .append("<button type=\"submit\">追加</button></form></body></html>");
                send(e, 200, "text/html", html.toString());
            } catch (SQLException ex) { // ★
                ex.printStackTrace();
                try { send(e, 500, "text/plain", "データベースエラー"); } catch (IOException ignored) { e.close(); }
            } catch (IOException ex) { ex.printStackTrace(); e.close(); }
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （停止: Ctrl+C）");
    }

    static void redirect(HttpExchange e) throws IOException {
        e.getResponseHeaders().set("Location", "/");
        e.sendResponseHeaders(303, -1); e.close();
    }
    static void send(HttpExchange e, int status, String type, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        e.sendResponseHeaders(status, body.length);
        try (OutputStream out = e.getResponseBody()) { out.write(body); }
    }
    static String escape(String text) { // ★
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    static class Todo {
        final int id; final String title; final boolean done;
        Todo(int id, String title, boolean done) { this.id = id; this.title = title; this.done = done; } // ★
    }
}
