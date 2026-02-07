package jp.d77.java.mfe2.BasicIO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import jp.d77.java.tools.HtmlIO.HtmlString;

public class HtmlTools {
    public static Optional<String> joinDispIP( String... lines ){
        if ( lines == null ) return Optional.empty();
        ArrayList<String> strings = new ArrayList<String>();
        for ( String l: lines ){
            strings.add( HtmlTools.IPLink( l ) );
        }
        return HtmlTools.joinDispPlain( strings.toArray( new String[0] ) );
    }

    public static Optional<String> joinDispEllipsis( String... lines ){
        if ( lines == null ) return Optional.empty();
        ArrayList<String> strings = new ArrayList<String>();
        for ( String l: lines ){
            String s = HtmlString.HtmlEscape( l );
            strings.add( "<span class=\"ellipsis120\" title=\"" + s + "\">" + s + "</span>" );
        }
        return HtmlTools.joinDispPlain( strings.toArray( new String[0] ) );
    }

    /**
     * 文字列をHTMLエスケープしながら結合する
     * @param lines
     * @return
     */
    public static Optional<String> joinDisp( String... lines ){
        if ( lines == null ) return Optional.empty();
        ArrayList<String> strings = new ArrayList<String>();
        for ( String l: lines ){
            strings.add( HtmlString.HtmlEscape( l ) );
        }
        return HtmlTools.joinDispPlain( strings.toArray( new String[0] ) );
    }

    /**
     * 文字列をHTMLエスケープしながら結合する
     * @param lines
     * @return
     */
    public static Optional<String> joinDispI( Integer... lines ){
        if ( lines == null ) return Optional.empty();
        String[] strings = Arrays.stream(lines )
                                .map(String::valueOf)
                                .toArray(String[]::new);
        return HtmlTools.joinDispPlain( strings );
    }

    /**
     * 文字列をHTMLエスケープしながら結合する
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

    public static String IPLink( String ip ){
        return HtmlString.HtmlEscape(ip) + "<A href=\"/rdap?edit_ip=" + HtmlString.HtmlEscape(ip) + "\" target=\"_blank\">(R)</A>";
    }
}
