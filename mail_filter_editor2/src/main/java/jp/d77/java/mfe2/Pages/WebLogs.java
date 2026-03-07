package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;

import jp.d77.java.mfe2.Datas.SessionLogManager;
import jp.d77.java.mfe2.BasicIO.HtmlGraph;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebLogs extends AbstractMfe{
    private LocalDate m_targetDate = null;
    public SessionLogManager  m_slog;
    private int m_edit_days = 3;
    private int m_select_id = -1;

    public WebLogs( Mfe2Config cfg ) {
        super( cfg );
        this.setHtmlTitle( "MFE2" );
        this.m_slog = new SessionLogManager( cfg );
    }

    // 1:init
    @Override
    public void init() {
        super.init();

        // 対象日の読み込み
        if ( this.getConfig().get( "edit_cal" ).isPresent() ) {
            // targetDateの読み込み
            this.m_targetDate = ToolDate.YMD2LocalDate( this.getConfig().get( "edit_cal" ).get() ).orElse( null );
        }
        if ( this.m_targetDate == null ){
            this.m_targetDate = LocalDate.now();
        }

        // 表示範囲日
        if ( this.getConfig().get( "edit_days" ).isPresent() ) {
            try {
                this.m_edit_days = Integer.parseInt( this.getConfig().get( "edit_days" ).get() );
            } catch ( NumberFormatException  e ){
                this.m_edit_days = 3;
            }
        }else{
            this.m_edit_days = 3;
        }

        if ( this.getConfig().get( "submit_select_id" ).isPresent() ){
            try {
                this.m_select_id = Integer.parseInt( this.getConfig().get( "submit_select_id" ).get() );
                this.m_edit_days = 1;
            } catch ( NumberFormatException  e ){
                this.m_select_id = -1;
            }
        }
    }

    // 2:load
    @Override
    public void load() {
        super.load();
        if ( this.m_targetDate == null ) return;
        // 1-7日内
        if ( this.m_edit_days < 1 || this.m_edit_days > 7 ) this.m_edit_days = 3;

        if ( this.getConfig().get( "submit_select_id" ).isPresent() ){
            // 選択したIDのみ読み込む
            this.m_slog.load( this.m_targetDate, this.m_select_id );
        }else{
            LocalDate startDate = this.m_targetDate.minusDays( this.m_edit_days - 1);
            for (LocalDate d = startDate; !d.isAfter( m_targetDate ); d = d.plusDays(1)) {
                if ( this.getConfig().get( "submit_log_create" ).isPresent() ) {
                    this.m_slog.create( d );
                }
                this.m_slog.load( d, null );
            }
        }
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
        this.getHtml().addString( HtmlGraph.getHeaderScript() );
    }

    // 6:displayNavbar
    @Override
    public void displayNavbar(){
        super.displayNavbar();
        String t = this.getConfig().get( "HtmlTitle" ).orElse("");
        t += " - " + this.m_targetDate + " - " + this.m_edit_days + "days";
        this.getConfig().overwrite( "HtmlTitle", t );
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

        // 日付選択フォームの表示
        this.getHtml().addString( this.displayLogLoadForm() );
        if ( this.m_slog == null ) return;

        if ( this.getConfig().get( "submit_select_id" ).isPresent() ){
            // 詳細表示
            WebLogsDetail detail = new WebLogsDetail( this.m_slog);
            this.getHtml().addString( detail.display( this.m_select_id, this.m_targetDate ) );
        }else{
            WebLogsList list = new WebLogsList( this.getConfig(), this.m_slog);
            String res = "";

            // -999 ログ
            res += list.display999();

            // サマリーリスト
            res += list.display();

            this.getHtml().addString( list.displayGraph( this.m_edit_days ) );
            this.getHtml().addString( res );
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
     * 日付指定入力欄生成
     * @return
     */
    private String displayLogLoadForm(){
        BSSForm f = BSSForm.newForm()
        .formTop( "/logs", false )

        .divRowTop()
        .divTop(2)
        //.formLabel( BSOpts.init("name", "edit_days").label( "Days" ) )
        .addString( 
            "<LABEL for=\"edit_days\">Days</LABEL>"
            + "<INPUT id=\"edit_days\" type=\"range\" min=\"1\" max=\"7\" step=\"1\" name=\"edit_days\" value=\"" + this.m_edit_days + "\"/>" 
            + "Value: <SPAN id=\"valueDisplay\">" + this.m_edit_days + "</SPAN>" )
        .divBtm(2)

        .divTop(2)
        .formLabel( BSOpts.init("name", "edit_cal").label( "Select Date" ) )
        .formInput(
            BSOpts.init("name", "edit_cal")
            .type( "date" )
            .value( ToolDate.Fromat( this.m_targetDate, "uuuu-MM-dd" ).orElse( null ) )
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
        f.addString( "<SCRIPT>\n"
        + "const slider = document.getElementById(\"edit_days\");\n"
        + "const display = document.getElementById(\"valueDisplay\");\n"
        + "slider.addEventListener(\"input\", () => {\n"
        + "  display.textContent = slider.value;\n"
        + "});\n"
            //+ "slider.addEventListener(\"input\", function () {\n"
            //+ "  display.textContent = this.value;\n"
            //+ "});\n"
        + "</SCRIPT>\n");
        return f.toString();
    }
}
