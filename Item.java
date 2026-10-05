// Item という名前のクラスを作ります。
public class Item {
    // プログラムを始める場所です。
    public static void main(String[] args) {
        // title に「牛乳を買う」という文字を入れます。
        String title = "牛乳を買う";
        // 前後のタグと title をつないで、HTML の1行を作ります。
        String html = "<li>" + title + "</li>";
        // 作った1行をターミナルに表示します。
        System.out.println(html);
    }
}
