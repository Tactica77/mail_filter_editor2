package jp.d77.java.mfe2.BasicIO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.d77.java.mfe2.Pages.WebBlockEditor.BlockFormData;
import jp.d77.java.tools.HtmlIO.HtmlString;

public class ToolAny {

    /**
     * 文字列をHTMLエスケープしながら結合する
     * IPアドレスがある場合はリンクを付与
     * @param lines
     * @return
     */
    public static Optional<String> joinDisp( String... lines ){
        if ( lines == null ) return Optional.empty();
        ArrayList<String> strings = new ArrayList<String>();
        for ( String l: lines ){
            strings.add( HtmlString.HtmlEscape( l ) );
        }
        return ToolAny.joinDispPlain( strings.toArray( new String[0] ) );
    }

    /**
     * 文字列をHTMLエスケープしながら結合する(Integer)
     * @param lines
     * @return
     */
    public static Optional<String> joinDispI( Integer... lines ){
        if ( lines == null ) return Optional.empty();
        String[] strings = Arrays.stream(lines )
                                .map(String::valueOf)
                                .toArray(String[]::new);
        return ToolAny.joinDispPlain( strings );
    }

    /**
     * 文字の幅120に制限し省略表示
     * @param lines
     * @return
     */
    public static Optional<String> joinDispEllipsis( String... lines ){
        if ( lines == null ) return Optional.empty();
        ArrayList<String> strings = new ArrayList<String>();
        for ( String l: lines ){
            String s = HtmlString.HtmlEscape( l );
            strings.add( "<span class=\"ellipsis120\" title=\"" + s + "\">" + s + "</span>" );
        }
        return ToolAny.joinDispPlain( strings.toArray( new String[0] ) );
    }

    /**
     * 文字列をHTMLエスケープしないで結合する
     * @param lines
     * @return
     */
    public static Optional<String> joinDispPlain( String[] lines ){
        String ret = "";
        for ( String l: lines ){
            if ( l.length() <= 0 ) continue;
            if ( ret.length() > 0 ) ret += "<BR>";
            ret += l;
        }
        return Optional.ofNullable( ret );
    }

    /**
     * IPアドレス用リンクを付与
     * @param ip
     * @return
     */
    public static String IPLink( String ip ){
        return "<A href=\"/rdap?edit_ip=" + HtmlString.HtmlEscape(ip) + "\" target=\"_blank\">(R)</A>";
    }

    public static String IPLink( BlockFormData bfd ){
        return "<A href=\"/rdap?edit_ip=" + HtmlString.HtmlEscape( bfd.Cidr() ) + "\" target=\"_blank\">(R)</A>"
            + "<A href=\"/block_editor?edit_cidr=" + HtmlString.UriEscape( bfd.Cidr() )
            + "&edit_cc=" + HtmlString.UriEscape( bfd.Cc() )
            + "&edit_org=" + HtmlString.UriEscape( bfd.org() )
            + "\" target=\"_blank\">(B)</A>";
    }


    /**
     * KVS。一つのキーに対して複数のvalueを持たせられる
     */
    public static class arrayString {
        Map<String, List<String>> data_work = new HashMap<>();

        public void clear(){
            this.data_work.clear();
        }

        public void add( String key, String value ){
            if ( ! this.data_work.containsKey(key) ) this.data_work.put( key, new ArrayList<>() );
            this.data_work.get(key).add(value);
        }

        public String[] gets( String key ){
            if ( ! this.data_work.containsKey(key) ) return new String[0];
            return this.data_work.get(key).toArray( new String[0] );
        }

        public List<String> toArray( String key ){
            if ( ! this.data_work.containsKey(key) ) return new ArrayList<>();
            return this.data_work.get(key);
        }

        public String join( String delimiter,  String key ){
            return String.join( delimiter, this.gets(key) );
        }
    }

    /**
     * 変数型カウンタ
     */
    public static class arrayCounter {
        Map<String, Integer> data_cnt = new HashMap<>();

        public void clear(){
            this.data_cnt.clear();
        }

        public void add( String key ){
            this.add( key, 1 );
        }

        public void add( String key, Integer i ){
            if ( i == null ) return;
            this.data_cnt.put( key, this.get(key) + i );
        }

        public int get( String key ){
            if ( ! this.data_cnt.containsKey(key) ) return 0;
            return this.data_cnt.get(key);
        }

        public String[] keys(){
            return this.data_cnt.keySet().toArray( new String[0] );
        }
    }
}
