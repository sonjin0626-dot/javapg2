
// List は Todo を複数件まとめて持つために使います。
import java.util.List;
// ArrayList は List に実際の値を入れるために使います。
import java.util.ArrayList;

// 実行を始める Main クラスです。
public class Main {
    // この main メソッドからプログラムが始まります。
    public static void main(String[] args) {
        // Todo を入れる空の List を作ります。
        List<Todo> todos = new ArrayList<>();
        // まだ済んでいない Todo を追加します。
        todos.add(new Todo("牛乳を買う", false));
        // 済んだ Todo を追加します。
        todos.add(new Todo("ゴミを出す", true));
        // List の Todo を先頭から1件ずつ取り出します。
        todos.add(new Todo("掃除をする", false));
        for (Todo todo : todos) {
            // Todo を HTML の1行に変えて表示します。
            System.out.println(todo.toItem());
        }
    }
}

// Todo 1件分の情報と表示方法をまとめるクラスです。
class Todo {
    // title は Todo の名前です。
    String title;
    // done は済んだかどうかを表します。
    boolean done;

    // 新しい Todo を作るときに名前と済んだ状態を受け取ります。
    Todo(String title, boolean done) {
        // 受け取った名前を、この Todo に保存します。
        this.title = title;
        // 受け取った状態を、この Todo に保存します。
        this.done = done;
    }

    // Todo を li タグで囲んだ1行の文字列に変えます。
    String toItem() {
        // 済んでいる場合は名前の前に印を付けます。
        if (done) {
            return "<li>[済] " + title + "</li>";
        }
        // まだ済んでいない場合は名前だけを表示します。
        return "<li>" + title + "</li>";
    }
}
