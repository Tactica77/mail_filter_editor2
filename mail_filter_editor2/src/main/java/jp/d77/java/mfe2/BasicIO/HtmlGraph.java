package jp.d77.java.mfe2.BasicIO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.HtmlIO.HtmlString;

public class HtmlGraph {
    public class dbf_prop {
        public GRAPH_TYPE   m_type = GRAPH_TYPE.NULL;
        public String   m_stack = "stack_1";
    }

    public class dbf {
        protected String m_dbid;
        protected String m_title;
        protected LinkedHashMap<String, HashMap<String,Float>> m_datas;   // YMD, key, value
        protected LinkedHashMap<String, dbf_prop> m_prop;

        public dbf( String dbid, String title ){
            this.m_dbid = dbid;
            this.m_title = title;
            this.m_datas = new LinkedHashMap<String, HashMap<String,Float>>();
            this.m_prop = new LinkedHashMap<String, dbf_prop>();
        }
        public String getDbId(){ return this.m_dbid; }
        public String[] getYMDList(){ return this.m_datas.keySet().toArray( new String[0] ); }
        public String[] getKeyList(){ return this.m_prop.keySet().toArray( new String[0] ); }

        public void set( String YMD, String key, Float value ){
            if ( ! this.m_prop.containsKey( key ) ) return;
            if ( ! this.m_datas.containsKey( YMD ) ) this.m_datas.put( YMD, new HashMap<String,Float>() );
            this.m_datas.get( YMD ).put(key, value);
        }
        public String[] getValues( String key ){
            ArrayList<String> val = new ArrayList<String>();
            for ( String YMD: this.m_datas.keySet() ){
                if ( this.m_datas.get( YMD ).containsKey( key ) ){
                    val.add(this.m_datas.get( YMD ).get( key ) + "" );
                }else{
                    val.add("");
                }
            }
            return val.toArray( new String[0] );
        }
        public void setProp( String key, String stack, GRAPH_TYPE type ){
            if ( ! this.m_prop.containsKey( key ) ) this.m_prop.put( key, new dbf_prop() );
            this.m_prop.get( key ).m_stack = stack;
            this.m_prop.get( key ).m_type = type;
        }
        public Optional<dbf_prop> getProp( String key ){
            if ( ! this.m_prop.containsKey( key ) ) return Optional.empty();
            return Optional.ofNullable( this.m_prop.get(key) );
        }
    }

    public enum GRAPH_TYPE {
        NULL    ("null"),
        BAR    ("bar"),
        LINE    ("line"),
        LINE_YDIFF    ("line");
        private final String label;
        GRAPH_TYPE(String label) { this.label = label; }
        public String toString() { return "type:'" + label + "'"; }
    }

    //private TYPE2   m_type;
    private Integer m_width;
    private Integer m_height;
    private String  m_GraphTitle;
    private dbf m_dbf = null;

    // コンストラクタ
    public HtmlGraph() {
        this.m_dbf = new dbf( "defid", "defName" );
    }

    public static String getHeaderScript(){
        return "<SCRIPT src=\"https://cdn.jsdelivr.net/npm/chart.js\"></SCRIPT>"; 
    }

    public HtmlGraph setGraphTitle( String GraphTitle ) {
        this.m_GraphTitle = GraphTitle;
        return this;
    }

    public HtmlGraph setWidth( int width ) {
        this.m_width = width;
        return this;
    }

    public HtmlGraph setHeight( int height ) {
        this.m_height = height;
        return this;
    }

    public dbf getDbf(){ return this.m_dbf; }
    public String draw_graph( String graph_no ) {
        Debugger.InfoPrint( "count = " + this.m_dbf.getYMDList().length );
        String graph_id = this.m_dbf.getDbId() + "_" + graph_no;
        HtmlString  html = HtmlString.init();
        String opt = "";

        if ( this.m_width != null) opt += " width=\"" + this.m_width + "\"";
        if ( this.m_height != null) opt += " height=\"" + this.m_height + "\"";

        // CANVAS
        html.addStringCr( "<DIV>" );
        html.addStringCr( "<CANVAS id=\"" + graph_id + "\""+ opt + "></CANVAS>" );
        html.addStringCr( "</DIV>" );

        // Script
        html.addStringCr( "<SCRIPT>" );
        html.addStringCr( 1, "const ctx_" + graph_id + " = document.getElementById('" + graph_id + "');" );
        html.addStringCr( 1, "new Chart(ctx_" + graph_id + ", {" );

        /*
        // Type(All)
        if ( this.m_type != null ) {
            html.addStringCr( 2,this.m_type.toString() + "," );
        }
        */

        // Data
        html.addStringCr( 3,"data: {" );

        // X-Label
        String[] HeaderList = this.m_dbf.getYMDList();
        html.addStringCr( 4,"labels: [" + String.join( ",", HeaderList ) + "]," );
        //Debugger.LogPrint( "Label=" + this.joinData( this.getYMList() ) );
        
        // Datasets
        html.addStringCr( 4,"datasets: [" );
        for ( String key : this.m_dbf.getKeyList() ){
            // グラフ定義が無い
            if ( this.m_dbf.getProp(key).isEmpty() ) continue;
            if ( this.m_dbf.getProp(key).get().m_type.equals( GRAPH_TYPE.NULL ) ) continue;

            GRAPH_TYPE gtype = this.m_dbf.getProp(key).get().m_type;

            html.addStringCr( 5,"{" );

            // Stack Label
            html.addStringCr( 6,"label: '" + key + "'," );
            html.addStringCr( 6,gtype + "," );
            html.addStringCr( 6,"stack:'" + this.m_dbf.getProp( key ).get().m_stack + "'," );

            // Datas
            html.addStringCr( 6,"data: [" +  String.join( ",", this.m_dbf.getValues(key) ) + "]," );
            html.addStringCr( 5,"}," );
        }
        html.addStringCr( 4,"]" ); // datasets

        html.addStringCr( 3,"}," ); // data

        // Options
        html.addStringCr( 3,"options: {" );

        // animation: false
        html.addStringCr( 4,"animation: false," );

        // plugins
        html.addStringCr( 4,"plugins: {" );

        // Title
        if ( this.m_GraphTitle != null ){
            html.addStringCr( 5,"title: {" );
            html.addStringCr( 6,"display: true," );
            html.addStringCr( 6,"text: '" + this.m_GraphTitle + "'," );
            html.addStringCr( 5,"}" );
        }
        html.addStringCr( 4,"}," ); // plugins

        html.addStringCr( 4,"scales: {" );
        html.addStringCr( 5,"x: {" );
        html.addStringCr( 6,"autoSkip: false," );
        html.addStringCr( 5,"}," );
        html.addStringCr( 5,"y: {" );
        html.addStringCr( 6,"beginAtZero: true," );
        html.addStringCr( 5,"}," );
        html.addStringCr( 4,"}," );
        html.addStringCr( 3,"}" );

        html.addStringCr( 1, "});" );
        html.addStringCr( "</SCRIPT>\n" );
    
        return html.toString();
    }    
}
