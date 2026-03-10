package jp.d77.java.mfe2.BasicIO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
            if ( ret.length() > 0 ) ret += "<BR>\n";
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
            + "<A href=\"/block_list?edit_cidr=" + HtmlString.UriEscape( bfd.Cidr() )
            + "&edit_cc=" + HtmlString.UriEscape( bfd.Cc() )
            + "&edit_org=" + HtmlString.UriEscape( bfd.org() )
            + "\" target=\"_blank\">(B)</A>";
    }

    /**
     * 指定した期間の秒数を算出する
     * @param start
     * @param end
     * @return
     */
    public static long secDiff( LocalDateTime start, LocalDateTime end ){
        if ( start == null || end == null ) return 0L;
        return Duration.between( start, end ).getSeconds();
    }
}
