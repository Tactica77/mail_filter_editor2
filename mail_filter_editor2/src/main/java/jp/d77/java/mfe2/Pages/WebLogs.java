package jp.d77.java.mfe2.Pages;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.BasicIO.ToolAny.arrayCounter;
import jp.d77.java.mfe2.BasicIO.ToolAny.arrayString;
import jp.d77.java.mfe2.Datas.RDAPCache;
import jp.d77.java.mfe2.Datas.SessionLogDatas;
import jp.d77.java.mfe2.Datas.RDAPCache.RdapResult;
import jp.d77.java.mfe2.Datas.SessionLogDatas.LogBasicData;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.LogAnalyser.SessionLogAnalyse;
import jp.d77.java.mfe2.LogAnalyser.MailLog;
import jp.d77.java.mfe2.LogAnalyser.SessionLogNumbering;
import jp.d77.java.mfe2.Pages.WebBlockEditor.BlockFormData;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebLogs extends AbstractMfe{
    private LocalDate targetDate = null;
    public SessionLogDatas  m_slog;

    public WebLogs( Mfe2Config cfg ) {
        super( cfg );
        this.setHtmlTitle( "MFE2" );
    }

    // 1:init
    @Override
    public void init() {
        super.init();

        // 対象日の読み込み
        if ( this.getConfig().get( "edit_cal" ).isPresent() ) {
            // targetDateの読み込み
            this.targetDate = ToolDate.YMD2LocalDate( this.getConfig().get( "edit_cal" ).get() ).orElse( null );
        }
    }

    // 2:load
    @Override
    public void load() {
        super.load();
        if ( this.targetDate == null ) return;

        this.m_slog = new SessionLogDatas();

        // targetDateの指定あり→sessionログ読み込み
        //this.m_SessionLogs = new SessionLogs();
        boolean create_session_log = false;

        if ( this.getConfig().get( "submit_log_create" ).isPresent() ) {
            // 強制的にログの再作成
            create_session_log = true;
        }

        if ( !create_session_log && ! this.m_slog.load( this.getConfig(), this.targetDate ) ){
            // セッションログが無い→maillogを読み込んでsessyonログを作成する
            create_session_log = true;
        }

        if ( create_session_log ) {
            // ログの再作成
            MailLog log = new MailLog( this.getConfig() );
            log.Load(this.targetDate);

            SessionLogNumbering slogUpdate = new SessionLogNumbering( this.m_slog );
            slogUpdate.CreateSessionLogs( log, this.targetDate );
            this.m_slog.save( this.getConfig(), this.targetDate );
        }

        // ログを分析
        SessionLogAnalyse  sLogDetail = new SessionLogAnalyse( this.m_slog );
        sLogDetail.analyseLog( targetDate, this.getConfig() );
    }

    // 3:post_save_reload
    @Override
    public void post_save_reload() {
        super.post_save_reload();
    }

    // 4 proc
    @Override
    public void proc(){
        super.proc();
    }
    
    // 5:displayHeader
    @Override
    public void displayHeader(){
        super.displayHeader();
        this.getHtml().addString( BSSForm.getTableHeader( "detail" ) );
        this.getHtml().addString( BSSForm.getTableHeader( "logs" ) );
        this.getHtml().addString( BSSForm.getTableHeader( "logs-999" ) );
    }

    // 6:displayNavbar
    @Override
    public void displayNavbar(){
        super.displayNavbar();
    }

    // 7:displayInfo
    @Override
    public void displayInfo() {
        super.displayInfo();
    }

    // 8:displayBody
    @Override
    public void displayBody() {
        super.displayBody();

        String targetDate = ToolDate.Fromat( LocalDate.now(), "yyyy-MM-dd" ).orElse("");
        if ( this.getConfig().get( "edit_cal" ).isPresent() ){
            targetDate = this.getConfig().get( "edit_cal" ).get();
        }

        // 日付選択フォームの表示
        this.getHtml().addString( this.displayLogLoadForm( targetDate ) );
        if ( this.m_slog == null ) return;

        if ( this.getConfig().get( "submit_select_id" ).isPresent() ){
            // 詳細表示
            this.getHtml().addString( this.displayLogDetail( this.getConfig().get( "submit_select_id" ).get(), targetDate ) );
        }else{
            // -999 ログ
            this.getHtml().addString( this.displayLog999() );
            // サマリーリスト
            this.getHtml().addString( this.displayLogSummaryList( targetDate ) );
        }
    }

    // 9:displayBottomInfo
    @Override
    public void displayBottomInfo(){
        super.displayBottomInfo();
    }

    // 10:displayFooter
    @Override
    public void displayFooter(){
        super.displayFooter();
    }    

    /**
     * ログ詳細表示用文字列生成
     * @return
     */
    private String displayLogDetail( String ids, String targetDate ){
        String res = "";
        BSSForm f;
        int id;

        try {
            id = Integer.parseInt( ids );
        } catch( NumberFormatException e ) {
            return "Error id=" + ids + "<BR>" + e.getMessage();
        }
        if ( ! this.m_slog.containsId( id ) ) return "not found id=" + id;

        f = BSSForm.newForm();
        f.addStringCr( "<H2>connection detail - " + targetDate + " - id:" + ids + "</H2>" );
        f.tableTop(
            new BSOpts()
                .id( "detail-table")
                .fclass("table table-bordered table-striped")
                .border("1")
        );

        // Table Header
        f.tableHeadTop()
            .tableRowTop()
            .tableTh( "Name" )
            .tableTh( "Value" )
            .tableRowBtm()
            .tableHeadBtm();

        HashMap<String,String> lines = new HashMap<String,String>();
        for ( String key: this.m_slog.getPropKeyS( id ) ){
            lines.put( key, ToolAny.joinDisp( this.m_slog.getPropS( id, key ) ).orElse("???") );
        }

        for ( String key: this.m_slog.getPropKeyI( id ) ){
            lines.put( key, ToolAny.joinDispI( this.m_slog.getPropI( id, key ) ).orElse("???") );
        }

        for ( String key: lines.keySet().stream().sorted().toArray(String[]::new) ){
            f.tableRowTop()
                .tableTdHtml( key )
                .tableTdHtml( lines.get(key) )
                .tableRowBtm();
        }
        f.tableBodyBtm();
        f.tableBtm();
        res += f.toString();


        f = BSSForm.newForm();
        f.addStringCr( "<H2>Logs - " + targetDate + " - id:" + ids + "</H2>" );
        f.tableTop(
            new BSOpts()
                .id( "logs-table")
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
        
        LocalDateTime start = null;
        for ( LogBasicData lb: this.m_slog.getLog( id ) ){
            if ( start == null ) start = lb.logTime();

            f.tableRowTop()
            .tableTd( ToolDate.Fromat( lb.logTime(), "HH:mm:ss" ).orElse( "???" ) + "(" + this.secDiff( start, lb.logTime() ) + "s)" )
            .tableTd( lb.program() )
            .tableTd( lb.pid() + "" )
            .tableTd( lb.message() )
            .tableRowBtm();
        }

        f.tableBodyBtm();
        f.tableBtm();
        
        res += f.toString();
        return res;
    }

    private long secDiff( LocalDateTime start, LocalDateTime end ){
        if ( start == null || end == null ) return 0L;
        return Duration.between( start, end ).getSeconds();
    }

    public record logsum_list_table_data(
        String id_link
        , String time
        , Integer logs
        , List<String> IP
        , List<String> cidr
        , List<String> cc
        , List<String> org
        , String result
        , String from_to
    ){}


    /**
     * ログ一覧表示用文字列生成
     * @return
     */
    private String displayLogSummaryList( String targetDate ){
        RDAPCache cache = new RDAPCache( this.getConfig() );
        arrayString data_work = new arrayString();
        arrayCounter data_cnt_ip = new arrayCounter();
        arrayCounter data_cnt_cidrs = new arrayCounter();
        arrayCounter data_cnt_org = new arrayCounter();
        BSSForm f;
        List<logsum_list_table_data> list_datas = new ArrayList<>();

        LocalDate date = ToolDate.YMD2LocalDate( targetDate ).orElse( null );
        if ( date != null ) cache.load( date );
        cache.rdap_get_flag( false );

        for( int id: this.m_slog.getIdLists() ){
            if ( id == -999 ) continue;
            logsum_list_table_data dat;
            data_work.clear();

            data_work.add( "result", ToolAny.joinDisp( this.m_slog.getPropS( id, "relay_status" ) ).orElse( "???" ) );
            data_work.add( "result", ToolAny.joinDisp( this.m_slog.getPropS( id, "error" ) ).orElse( "???" ) );

            for ( String ip: this.m_slog.getPropS( id, "ip" ) ){
                data_work.add( "ip", ip );
                Optional<RdapResult> rdap = cache.getRDAP( ip );
                if ( rdap.isEmpty() ) continue;
                data_cnt_ip.add( ip );

                if ( rdap.get().cidr() != null ) {
                    data_work.add( "cidrs", rdap.get().cidr() );
                    data_cnt_cidrs.add( rdap.get().cidr() );
                }
                if ( rdap.get().cc() != null ) data_work.add( "cc", rdap.get().cc() ); 
                if ( rdap.get().org() != null ) {
                    data_work.add( "org", rdap.get().org() );
                    data_cnt_org.add( rdap.get().org() );
                }

            }

            dat = new logsum_list_table_data(
                // id_link
                "<A href=\"" + this.getUri() + "?submit_select_id=" + id + "&edit_cal=" + targetDate + "\" target=\"_blank\">" + id + "</A>"

                // time
                , ToolDate.Fromat( this.m_slog.getStart(id).orElse(null), "HH:mm:ss" ).orElse("???")
                    +"-<BR>"
                    + ToolDate.Fromat( this.m_slog.getEnd(id).orElse(null), "HH:mm:ss" ).orElse("???")
                    + "(" + this.secDiff( this.m_slog.getStart(id).orElse(null), this.m_slog.getEnd(id).orElse(null) ) + "s)"
                
                // logs
                , this.m_slog.getLog(id).length

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

                // from_to
                , ToolAny.joinDispEllipsis( this.m_slog.getPropS( id, "from" ) ).orElse("???")
                    + "<BR>->" + ToolAny.joinDispEllipsis( this.m_slog.getPropS( id, "to" ) ).orElse("???")
            );
            list_datas.add(dat);
        }

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
            .tableTh( "I" )
            .tableTh( "R" )
            .tableTh( "O" )
            .tableTh( "IP" )
            .tableTh( "CIDR" )
            .tableTh( "CC" )
            .tableTh( "Org" )
            .tableTh( "Result" )
            .tableTh( "From/To" )
            .tableRowBtm()
            .tableHeadBtm();

        f.tableBodyTop();

        for( logsum_list_table_data dat: list_datas ){
            String ip = "";
            String cidrs = "";
            String cc = "";
            String org = "";
            if ( dat.IP.size() > 0 ) ip = dat.IP.get(0);
            if ( dat.cidr.size() > 0 ) cidrs = dat.cidr.get(0);
            if ( dat.cc.size() > 0 ) cc = dat.cc.get(0);
            if ( dat.org.size() > 0 ) org = dat.org.get(0);
            f.tableRowTop();
            // ID
            f.tableTdHtml( dat.id_link );

            // Time
            f.tableTdHtml( dat.time );

            // Logs
            f.tableTd( dat.logs + "" );

            // I
            f.tableTd( data_cnt_ip.get( ip ) + "" );

            // R
            f.tableTd( data_cnt_cidrs.get( cidrs ) + "" );

            // O
            f.tableTd( data_cnt_org.get( org ) + "" );

            // IP
            if ( ip.equals( "" ) ){
                f.tableTdHtml( ToolAny.joinDisp( dat.IP.toArray( new String[0] ) ).orElse("?"));
            }else{
                f.tableTdHtml( ToolAny.joinDisp( dat.IP.toArray( new String[0] ) ).orElse("?")
                + ToolAny.IPLink( new BlockFormData( ToolDate.Fromat( LocalDate.now(), "uuuuMMdd").orElse( null), cc, ip, org ) ));
            }
            
            // CIDR
            if ( cidrs.equals( "" ) ){
                f.tableTdHtml( ToolAny.joinDisp( dat.cidr.toArray( new String[0] ) ).orElse("?") );
            }else{
                f.tableTdHtml( ToolAny.joinDisp( dat.cidr.toArray( new String[0] ) ).orElse("?")
                + ToolAny.IPLink( new BlockFormData( ToolDate.Fromat( LocalDate.now(), "uuuuMMdd").orElse( null), cc, cidrs, org ) ));
            }

            // CC
            f.tableTdHtml( ToolAny.joinDisp( dat.cc.toArray( new String[0] ) ).orElse("?") );

            // Org
            f.tableTdHtml( ToolAny.joinDisp( dat.org.toArray( new String[0] ) ).orElse("?") );

            // Reesult
            f.tableTdHtml( dat.result );

            // From/To
            f.tableTdHtml( dat.from_to );
            f.tableRowBtm();
        }

        f.tableBodyBtm();
        f.tableBtm();

        return f.toString();
    }

    /**
     * 表示非対象ログ一覧表示用文字列生成
     * 表示対象がない場合は何も表示しない
     * @return
     */
    private String displayLog999(){
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
        
        if ( ! this.m_slog.containsId( -999 ) ) return "";
        for ( LogBasicData lb: this.m_slog.getLog( -999 ) ){
            if (
                lb.message().startsWith( "imap-login: Login: user=" )
                || lb.message().contains( ": Disconnected: Logged " )
                || lb.message().startsWith( "warning: run-time library vs. compile-time header version " )
                || lb.message().startsWith( "statistics: max connection " )
                || lb.message().startsWith( "statistics: max cache size " )
                || lb.message().startsWith( "managesieve-login: Login: user=" )
                ) continue;
            i++;
            f.tableRowTop()
            .tableTd( ToolDate.Fromat( lb.logTime(), "yyyy-MM-dd HH:mm:ss" ).orElse( "???" ) )
            .tableTd( lb.program() )
            .tableTd( lb.pid() + "" )
            .tableTd( lb.message() )
            .tableRowBtm();
        }

        f.tableBodyBtm();
        f.tableBtm();
        if ( i <= 0 ) return "";
        return f.toString();
    }

    /**
     * 日付指定入力欄生成
     * @return
     */
    private String displayLogLoadForm( String targetDate ){
        BSSForm f = BSSForm.newForm()
        .formTop( "/logs", false )

        .divRowTop()
        .divTop(2)
        .formLabel(
            BSOpts.init("name", "submit_cal")
            .label("SELECT")
            .value("submit_cal")
        )
        .divBtm(2)

        .divTop(2)
        .formInput(
            BSOpts.init("name", "edit_cal")
            .type( "date" )
            .value( targetDate )
        )
        .divBtm(2)

        .divTop(2)
        .formSubmit(
            BSOpts.init("name", "submit_log_load")
                .label("Load")
                .value("1")
        )
        .divBtm(2)

        .divTop(2)
        .formSubmit(
            BSOpts.init("name", "submit_log_create")
                .label("Create")
                .value("1")
        )
        .divBtm(2)

        .divTop(4)
        .divBtm(4)
        .divRowBtm()

        .formBtm();
        return f.toString();
    }
}
