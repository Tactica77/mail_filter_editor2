package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.HtmlGraph;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.BasicIO.HtmlGraph.GRAPH_TYPE;
import jp.d77.java.mfe2.Datas.BlockDatas;
import jp.d77.java.mfe2.Datas.FilterDatas;
import jp.d77.java.mfe2.Datas.FilterDatas.IpFilter;
import jp.d77.java.mfe2.Datas.RDAPCache;
import jp.d77.java.mfe2.Datas.RDAPCache.RdapResult;
import jp.d77.java.mfe2.Datas.SessionLogDatas;
import jp.d77.java.mfe2.Datas.SessionLogDatas.LogBasicData;
import jp.d77.java.mfe2.LogAnalyser.RspamdLog.RspamdLogData;
import jp.d77.java.mfe2.Datas.SessionLogManager;
import jp.d77.java.mfe2.Datas.SpotBlockDatas;
import jp.d77.java.mfe2.Pages.WebBlockEditor.BlockFormData;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolArrays.arrayCounter;
import jp.d77.java.tools.BasicIO.ToolArrays.arrayString;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;
import jp.d77.java.tools.HtmlIO.HtmlString;

public class WebLogsList {
    private Mfe2Config m_cfg;
    private SessionLogManager  m_slog;
    private record logsum_list_table_data(
        String id_link
        , String time
        , Integer logs
        , List<String> IP
        , List<String> cidr
        , List<String> cc
        , List<String> org
        , String result
        , boolean result_error
        , String from_to
        , String filter
        , String SpamScore
    ){}
    
    // IPカウンタ
    private arrayCounter m_data_cnt_ip;
    private Map<LocalDate,arrayCounter> m_data_cnt_ip2;

    // CIDRカウンタ
    private arrayCounter m_data_cnt_cidrs;

    // ORGカウンタ
    private arrayCounter m_data_cnt_org;

    // 表示データ
    private List<logsum_list_table_data> list_datas;    

    private FilterDatas  m_filter;

    /**
     * コンストラクタ
     * @param cfg
     * @param slog
     */
    public WebLogsList( Mfe2Config cfg, SessionLogManager slog ){
        this.m_cfg = cfg;
        this.m_slog = slog;

        this.m_filter = new FilterDatas();
        this.m_filter.loadCountryFilter( this.m_cfg.getDataFilePath() + "/country_filter.txt", true );
        Debugger.addHistory("loaded country_filter");

        BlockDatas bd = new BlockDatas( this.m_filter, "black list" );
        bd.load( this.m_cfg.getDataFilePath() + "/block_list_black.txt" );
        Debugger.addHistory("loaded block_list_black");

        SpotBlockDatas sbd = new SpotBlockDatas( this.m_filter, "spot block" );
        sbd.load( this.m_cfg.getDataFilePath() + "/block_list_spot.txt" );
        Debugger.addHistory("loaded block_list_spot");        
    }

    public String display(){
        this.m_data_cnt_ip = new arrayCounter();
        this.m_data_cnt_ip2 = new HashMap<>();
        this.m_data_cnt_cidrs = new arrayCounter();
        this.m_data_cnt_org = new arrayCounter();
        this.list_datas = new ArrayList<>();

        //this.m_slog.keys()

        for ( LocalDate d: this.m_slog.getDates() ){
            this.CreateData( d );
        }

        return this.CreateDisplay();
    }

    public String displayGraph( int border ){
        HtmlGraph graph = new HtmlGraph();

        for ( String ip: this.m_data_cnt_ip.keys() ){
            if ( this.m_data_cnt_ip.get(ip) > border ){
                graph.getDbf().setProp( ip, "stack_1", GRAPH_TYPE.BAR );
            }
        }
        for ( LocalDate d: this.m_data_cnt_ip2.keySet().stream().sorted().toList()  ){
            String YMD = "\"" + ToolDate.Format( d, "uuuu/MM/dd" ).orElse( "???" ) + "\"";
            for ( String ip: this.m_data_cnt_ip2.get( d ).keys() ){
                if ( graph.getDbf().getProp( ip ).isEmpty() ) continue;
                graph.getDbf().set( YMD, ip, this.m_data_cnt_ip2.get( d ).get( ip ) * 1.0f );
            }
        }

        return graph.draw_graph( "1" );
    }

    private void CreateData( LocalDate date ){
        arrayString data_work = new arrayString();
        RDAPCache cache = new RDAPCache( this.m_cfg );
        SessionLogDatas sd = this.m_slog.getData( date ).orElse(null);
        if ( sd == null ) return;

        // RDAP cacheをロード
        cache.load( date );
        cache.server_get_flag( false );   // キャッシュのみから取得する(ネットから取得しない)

        //RspamdLog rspam = new RspamdLog( this.m_cfg );
        //rspam.Load(date);
        //this.m_rspamdLog.put( date, rspam );


        for( int id: sd.getIdLists() ){
            if ( id == -999 ) continue;
            logsum_list_table_data display_data;

            data_work.clear();
            boolean result_error = false;
            String filter = null;
            String spam_score = null;
            result_error = sd.isError(id);
            data_work.add( "result", ToolAny.joinDisp( sd.getResult(id) ).orElse( "???" ) );

            //this.m_filter
            for ( String ip: sd.getPropS( id, "ip" ) ){
                if ( filter == null ){
                    Optional<IpFilter> ipf = this.m_filter.getFilter( ip );
                    if ( ipf.isPresent() && ipf.get().isEnable() ){
                        filter = HtmlString.HtmlEscape( ipf.get().m_cidr ) + "<BR>" + HtmlString.HtmlEscape( ipf.get().m_type + " " );
                    }
                }
                // IPを格納
                data_work.add( "ip", ip );
                this.m_data_cnt_ip.add( ip );
                if ( ! this.m_data_cnt_ip2.containsKey( date ) ) this.m_data_cnt_ip2.put( date, new arrayCounter() );
                this.m_data_cnt_ip2.get( date ).add(ip);

                Optional<RdapResult> rdap = cache.getRDAP( ip );
                if ( rdap.isEmpty() ) continue;

                // CIDRを格納
                if ( rdap.get().cidr() != null ) {
                    data_work.add( "cidrs", rdap.get().cidr() );
                    this.m_data_cnt_cidrs.add( rdap.get().cidr() );
                }

                // Ccを格納
                if ( rdap.get().cc() != null ) data_work.add( "cc", rdap.get().cc() ); 

                // Orgを格納
                if ( rdap.get().org() != null ) {
                    data_work.add( "org", rdap.get().org() );
                    this.m_data_cnt_org.add( rdap.get().org() );
                }
            }
            if ( filter == null ) filter = "";
            String qid = "",mid = "",w[];
            w = sd.getPropS( id, "msgid" );
            if ( w.length > 0 ) mid = w[0];
            w = sd.getPropS( id, "postfix_queue" );
            if ( w.length > 0 ) qid = w[0];
            RspamdLogData rld = this.m_slog.getRspamData(date,qid, mid ).orElse(null);
            if ( rld == null ){
                spam_score = "";
            }else{
                spam_score = rld.getScoreResult();
            }

            display_data = new logsum_list_table_data(
                // id_link
                "<A href=\"" + this.m_cfg.getUri() + "?submit_select_id=" + id + "&edit_cal=" + date + "\" target=\"_blank\">" + id + "</A>"

                // time
                ,
                    ToolDate.Format(date, "MM/dd").orElse("???")
                    + "<BR>"
                    + ToolDate.Format( sd.getStart(id).orElse(null), "HH:mm:ss" ).orElse("???")
                    +"-<BR>"
                    + ToolDate.Format( sd.getEnd(id).orElse(null), "HH:mm:ss" ).orElse("???")
                    + "(" + ToolAny.secDiff( sd.getStart(id).orElse(null), sd.getEnd(id).orElse(null) ) + "s)"
                
                // logs
                , sd.getLog(id).length

                // IP
                , data_work.toArray( "ip" )

                // cidr
                , data_work.toArray( "cidrs" )

                // cc
                , data_work.toArray( "cc" )

                // org
                , data_work.toArray( "org" )

                // result
                , String.join("<BR>", data_work.gets( "result" ) )

                // result_error
                , result_error

                // from_to
                , ToolAny.joinDispEllipsis( sd.getPropS( id, "from" ) ).orElse("???")
                    + "<BR>->" + ToolAny.joinDispEllipsis( sd.getPropS( id, "to" ) ).orElse("???")

                // filter
                , filter

                // SpamScore
                , spam_score
            );
            this.list_datas.add(display_data);
        }
        Debugger.addHistory("done");
    }

    private String CreateDisplay(){
        BSSForm f;

        f = BSSForm.newForm();
        f.tableTop(
            new BSOpts()
                .id( "logs-table")
                .fclass("table table-bordered table-striped")
                .border("1")
        );

        // Table Header
        f.tableHeadTop()
            .tableRowTop()
            .tableTh( "ID" )
            .tableTh( "Time(Sec)" )
            .tableTh( "Logs" )
            .tableTh( "Result" )
            .tableTh( "I" )
            .tableTh( "R" )
            .tableTh( "O" )
            .tableTh( "IP" )
            .tableTh( "CIDR" )
            .tableTh( "CC" )
            .tableTh( "Org" )
            .tableTh( "SPAM Score" )
            .tableTh( "Filter" )
            .tableTh( "From/To" )
            .tableRowBtm()
            .tableHeadBtm();

        f.tableBodyTop();

        for( logsum_list_table_data display_data: this.list_datas ){
            String ip = "";
            String cidrs = "";
            String cc = "";
            String org = "";
            String add_opt = "";
            if ( display_data.IP.size() > 0 ) ip = display_data.IP.get(0);
            if ( display_data.cidr.size() > 0 ) cidrs = display_data.cidr.get(0);
            if ( display_data.cc.size() > 0 ) cc = display_data.cc.get(0);
            if ( display_data.org.size() > 0 ) org = display_data.org.get(0);
            if ( ! display_data.filter.equals( "" ) ) add_opt = " style=\"background-color: #888888;\"";
            f.tableRowTop();
            // ID
            f.tableTdHtml( display_data.id_link, add_opt );

            // Time
            f.tableTdHtml( display_data.time, add_opt );

            // Logs
            f.tableTd( display_data.logs + "", add_opt );

            // Reesult
            if ( display_data.result_error ){
                f.tableTdHtml( display_data.result, add_opt + " style=\"color: #FF0000;\"" );
            }else {
                f.tableTdHtml( display_data.result, add_opt );
            }

            // I
            f.tableTd( this.m_data_cnt_ip.get( ip ) + "", add_opt );

            // R
            f.tableTd( this.m_data_cnt_cidrs.get( cidrs ) + "", add_opt );

            // O
            f.tableTd( this.m_data_cnt_org.get( org ) + "", add_opt );

            // IP
            if ( ip.equals( "" ) ){
                f.tableTdHtml( ToolAny.joinDisp( display_data.IP.toArray( new String[0] ) ).orElse("?"), add_opt);
            }else{
                f.tableTdHtml( ToolAny.joinDisp( display_data.IP.toArray( new String[0] ) ).orElse("?")
                + ToolAny.IPLink( new BlockFormData( ToolDate.Format( LocalDate.now(), "uuuuMMdd").orElse( null), cc, ip, org ) ), add_opt);
            }
            
            // CIDR
            if ( cidrs.equals( "" ) ){
                f.tableTdHtml( ToolAny.joinDisp( display_data.cidr.toArray( new String[0] ) ).orElse("?"), add_opt );
            }else{
                f.tableTdHtml(
                    ToolAny.joinDisp( display_data.cidr.toArray( new String[0] ) ).orElse("?")
                    + ToolAny.IPLink( new BlockFormData( ToolDate.Format( LocalDate.now(), "uuuuMMdd").orElse( null), cc, cidrs, org ) )
                    , add_opt
                );
            }

            // CC
            f.tableTdHtml( ToolAny.joinDisp( display_data.cc.toArray( new String[0] ) ).orElse("?"), add_opt );

            // Org
            f.tableTdHtml( ToolAny.joinDisp( display_data.org.toArray( new String[0] ) ).orElse("?"), add_opt );

            // SPAM Score
            f.tableTdHtml( display_data.SpamScore, add_opt );

            // Filter
            f.tableTdHtml( display_data.filter, add_opt );

            // From/To
            f.tableTdHtml( display_data.from_to, add_opt );
            f.tableRowBtm();
        }

        f.tableBodyBtm();
        f.tableBtm();

        Debugger.addHistory("done");
        return f.toString();
    }


    /**
     * 表示非対象ログ一覧表示用文字列生成
     * 表示対象がない場合は何も表示しない
     * @return
     */
    public String display999(){
        int i = 0;
        BSSForm f = BSSForm.newForm();

        f.addStringCr( "<H2>-999 Logs</H2>" );
        f.tableTop(
            new BSOpts()
                .id( "logs-999-table")
                .fclass("table table-bordered table-striped")
                .border("1")
        );

        // Table Header
        f.tableHeadTop()
            .tableRowTop()
            .tableTh( "Date" )
            .tableTh( "Program" )
            .tableTh( "PID" )
            .tableTh( "message" )
            .tableRowBtm()
            .tableHeadBtm();

        for ( LocalDate d: this.m_slog.getDates() ){
            SessionLogDatas sd = this.m_slog.getData( d ).orElse( null );
            if ( sd == null ) continue;
            if ( ! sd.containsId( -999 ) ) return "";

            for ( LogBasicData lb: sd.getLog( -999 ) ){
                if (
                    lb.message().startsWith( "imap-login: Login: user=" )
                    || lb.message().contains( ": Disconnected: Logged " )
                    || lb.message().startsWith( "warning: run-time library vs. compile-time header version " )
                    || lb.message().startsWith( "statistics: max connection " )
                    || lb.message().startsWith( "statistics: max cache size " )
                    || lb.message().startsWith( "managesieve-login: Login: user=" )
                    || lb.message().startsWith( "starting the Postfix mail system" )
                    || lb.message().startsWith( "daemon started -- version" )
                    || lb.message().contains( "starting up for" )
                    ) continue;
                i++;
                f.tableRowTop()
                .tableTd( ToolDate.Format( lb.logTime(), "yyyy-MM-dd HH:mm:ss" ).orElse( "???" ) )
                .tableTd( lb.program() )
                .tableTd( lb.pid() + "" )
                .tableTd( lb.message() )
                .tableRowBtm();
            }
        }

        f.tableBodyBtm();
        f.tableBtm();
        Debugger.addHistory("done");
        if ( i <= 0 ) return "";
        return f.toString();
    }

}
