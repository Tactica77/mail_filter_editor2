package jp.d77.java.mfe2.Pages;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import jp.d77.java.mfe2.BasicIO.HtmlTools;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.LogAnalyser.SessionLogs;
import jp.d77.java.mfe2.LogAnalyser.MailLog;
import jp.d77.java.mfe2.LogAnalyser.SessionLogDatas.LogBasic;
import jp.d77.java.mfe2.LogAnalyser.SessionLogDatas.LogData;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebLogs extends AbstractMfe{
    SessionLogs m_SessionLogs = null;

    public WebLogs(String uri, Mfe2Config cfg) {
        super( uri, cfg );
        this.setHtmlTitle( "MFE2" );
    }

    // 1:init
    @Override
    public void init() {
        super.init();
    }

    // 2:load
    @Override
    public void load() {
        super.load();
    }

    // 3:post_save_reload
    @Override
    public void post_save_reload() {
        super.post_save_reload();

        if ( this.getConfig().getMethod( "submit_log_create" ).isPresent()
            || this.getConfig().getMethod( "submit_log_load" ).isPresent()
            || this.getConfig().getMethod( "submit_select_id" ).isPresent()
             ) {
            // submit_log_create or submit_log_loadボタンが押された
            LocalDate targetDate = null;
            if ( this.getConfig().getMethod( "edit_cal" ).isPresent() ) {
                // targetDateの読み込み
                targetDate = ToolDate.YMD2LocalDate( this.getConfig().getMethod( "edit_cal" ).get() ).orElse( null );
            }
            if ( targetDate != null ){
                // targetDateの指定あり→sessionログ読み込み
                this.m_SessionLogs = new SessionLogs();
                if ( this.getConfig().getMethod( "submit_log_load" ).isPresent()
                    || this.getConfig().getMethod( "submit_select_id" ).isPresent()
                    ) {
                    if ( ! this.m_SessionLogs.load( this.getConfig(), targetDate ) ){
                        // セッションログが無い→maillogを読み込んでsessyonログを作成する
                        MailLog log = new MailLog( this.getConfig() );
                        log.Load(targetDate);
                        this.m_SessionLogs.CreateSessionLogs( log, targetDate );
                        this.m_SessionLogs.save( this.getConfig(), targetDate );
                    }
                }else if ( this.getConfig().getMethod( "submit_log_create" ).isPresent() ) {
                    // maillogを読み込んでsessyonログを作成する
                    MailLog log = new MailLog( this.getConfig() );
                    log.Load(targetDate);
                    this.m_SessionLogs.CreateSessionLogs( log, targetDate );
                    this.m_SessionLogs.save( this.getConfig(), targetDate );
                }

                this.m_SessionLogs.AnalyseLog(targetDate);
            }
            
            //this.m_log = new MailLog( this.getConfig() );
            //this.m_log.Load( ToolDate.YMD2LocalDate("2026/02/01").get() );
        }
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
        if ( this.getConfig().getMethod( "edit_cal" ).isPresent() ){
            targetDate = this.getConfig().getMethod( "edit_cal" ).get();
        }

        // 日付選択フォームの表示
        this.getHtml().addString( this.displayLogLoadForm( targetDate ) );
        if ( this.m_SessionLogs == null ) return;

        if ( this.getConfig().getMethod( "submit_select_id" ).isPresent() ){
            // 詳細表示
            this.getHtml().addString( this.displayLogDetail( this.getConfig().getMethod( "submit_select_id" ).get(), targetDate ) );
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
        if ( this.m_SessionLogs.getLogData( id ).isEmpty() ) return "not found id=" + id;

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
        for ( String key: this.m_SessionLogs.getLogData( id ).get().getPropKeyS() ){
            lines.put( key, HtmlTools.joinDisp( this.m_SessionLogs.getLogData( id ).get().getPropS(key).orElse( null ) ).orElse("???") );
        }

        for ( String key: this.m_SessionLogs.getLogData( id ).get().getPropKeyI() ){
            lines.put( key, HtmlTools.joinDispI( this.m_SessionLogs.getLogData( id ).get().getPropI(key).orElse( null ) ).orElse("???") );
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
        for ( LogBasic lb: this.m_SessionLogs.getLogData( id ).get().getLog() ){
            if ( start == null ) start = lb.logTime;

            f.tableRowTop()
            .tableTd( ToolDate.Fromat( lb.logTime, "hh:mm:ss" ).orElse( "???" ) + "(" + this.secDiff(start, lb.logTime) + "s)" )
            .tableTd( lb.program )
            .tableTd( lb.pid + "" )
            .tableTd( lb.message )
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

    /**
     * ログ一覧表示用文字列生成
     * @return
     */
    private String displayLogSummaryList( String targetDate ){
        BSSForm f = BSSForm.newForm();

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
            .tableTh( "IP" )
            .tableTh( "Code" )
            .tableTh( "Result" )
            .tableTh( "From/To" )
            .tableRowBtm()
            .tableHeadBtm();

        for( int id: this.m_SessionLogs.getIndexsList() ){
            if ( id == -999 ) continue;
            LogData ld = this.m_SessionLogs.getLogData( id ).get();

            ArrayList<String> result = new ArrayList<String>();
            if ( ld.getPropS( "relay_status" ).isPresent() ) result.add( HtmlTools.joinDisp( ld.getPropS( "relay_status" ).get() ).orElse( "???" ) );
            if ( ld.getPropS( "error" ).isPresent() ) result.add( HtmlTools.joinDisp( ld.getPropS( "error" ).get() ).orElse( "???" ) );

            f.tableRowTop()
                .tableTdHtml( "<A href=\"" + this.getUri() + "?submit_select_id=" + id + "&edit_cal=" + targetDate + "\" target=\"_blank\">" + id + "</A>" )
                .tableTdHtml(
                    ToolDate.Fromat( ld.getStart(), "hh:mm:ss" ).orElse("???")
                    +"-<BR>"
                    + ToolDate.Fromat( ld.getEnd(), "hh:mm:ss" ).orElse("???")
                    + "(" + this.secDiff( ld.getStart(), ld.getEnd() ) + "s)"
                )
                .tableTd( ld.getLog().length + "" )
                .tableTdHtml( HtmlTools.joinDispIP( ld.getPropS( "ip" ).orElse(null) ).orElse("???") )
                .tableTdHtml( HtmlTools.joinDisp( ld.getPropS( "relay_status" ).orElse( null ) ).orElse("???") )
                .tableTdHtml( String.join("<BR>", result.toArray( new String[0] ) ) )
                .tableTdHtml( HtmlTools.joinDispEllipsis( ld.getPropS( "from" ).orElse(null)  ).orElse("???")
                    + "<BR>->" + HtmlTools.joinDispEllipsis( ld.getPropS( "to" ).orElse(null)  ).orElse("???") )
                .tableRowBtm();

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
        
        if ( this.m_SessionLogs.getLogData( -999 ).isEmpty() ) return "";
        for ( LogBasic lb: this.m_SessionLogs.getLogData( -999 ).get().getLog() ){
            if (
                lb.message.startsWith( "imap-login: Login: user=" )
                || lb.message.contains( ": Disconnected: Logged " )
                || lb.message.startsWith( "warning: run-time library vs. compile-time header version " )
                || lb.message.startsWith( "statistics: max connection " )
                || lb.message.startsWith( "statistics: max cache size " )
                || lb.message.startsWith( "managesieve-login: Login: user=" )
                ) continue;
            i++;
            f.tableRowTop()
            .tableTd( ToolDate.Fromat( lb.logTime, "yyyy-MM-dd hh:mm:ss" ).orElse( "???" ) )
            .tableTd( lb.program )
            .tableTd( lb.pid + "" )
            .tableTd( lb.message )
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
