// Items という名前のクラスを作ります。
public class Items {
    // ここからプログラムを実行します。
    public static void main(String[] args) {
        // 4件の Todo を todos という配列に入れます。
        String[] todos = { "牛乳を買う", "卵を買う", "パンを買う", "掃除をする" };
        // 同じ順番で、済んだかどうかを true または false で記録します。
        boolean[] done = { true, false, false, false };
        // i を 0 から始め、配列の件数まで 1 ずつ増やして繰り返します。
        for (int i = 0; i < todos.length; i++) {
            // 文字がない項目は表示せず、次の項目へ進みます。
            if (todos[i].isEmpty()) {
                continue;
            }
            // まだ済んでいない項目には印を付けません。
            String mark = "";
            // 済んだ項目にだけ「[済]」の印を付けます。
            if (done[i]) {
                mark = "[済] ";
            }
            // 印と i 番目の Todo を li タグで囲み、1行表示します。
            System.out.println("<li>" + mark + todos[i] + "</li>");
        }
    }
}
