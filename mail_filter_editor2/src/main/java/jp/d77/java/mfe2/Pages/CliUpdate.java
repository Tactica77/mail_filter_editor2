package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.LogAnalyser.MailLog;
import jp.d77.java.mfe2.LogAnalyser.SessionLogDatas;
import jp.d77.java.mfe2.LogAnalyser.SessionLogUpdate;

public class CliUpdate extends AbstractMfe{
    private LocalDate targetDate = null;
    private String m_result = "no proc";
    public SessionLogDatas  m_slog;

    public CliUpdate(String uri, Mfe2Config cfg) {
        super( uri, cfg );
    }

    // 1:init
    @Override
    public void init() {
        super.init();

        // 引数の確認と、更新日の取得
        if ( this.getConfig().getMethod( "mode" ).isEmpty() ) return;
        if ( this.getConfig().getMethod( "mode" ).get().equals( "sessionlog" ) ) {
            this.targetDate = LocalDate.now();
            if ( this.getConfig().getMethod( "edit_diffdate" ).isPresent() ) {
                String d = this.getConfig().getMethod( "edit_diffdate" ).get();
                try{
                    Long i = Long.parseLong(d);
                    this.targetDate = this.targetDate.plusDays( i * (-1L) );
                }catch( NumberFormatException e ){}
                    
            }
        }
    }

    // 2:load
    @Override
    public void load() {
        super.load();

        if ( this.targetDate == null ) return;

        this.m_slog = new SessionLogDatas();
        SessionLogUpdate slogUpdate = new SessionLogUpdate( this.m_slog );
        MailLog log = new MailLog( this.getConfig() );
        log.Load(this.targetDate);

        slogUpdate.CreateSessionLogs( log, this.targetDate );
        this.m_slog.save( this.getConfig(), this.targetDate );
        this.m_result = "update session log targetDate=" + this.targetDate;
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
        //super.displayHeader();
        //this.getHtml().addString()
        this.getHtml()
            .addStringCr( "<HTML lang=\"ja\">" )
            .addStringCr( "<HEAD>" )
            .addStringCr( "</HEAD>" )
            .addStringCr( "<BODY>" );
            //.toString() + "\n";
    }

    // 6:displayNavbar
    @Override
    public void displayNavbar(){
        //super.displayNavbar();
    }

    // 7:displayInfo
    @Override
    public void displayInfo() {
        //super.displayInfo();
    }

    // 8:displayBody
    @Override
    public void displayBody() {
        //super.displayBody();
        this.getHtml().addStringCr( m_result );
    }

    // 9:displayBottomInfo
    @Override
    public void displayBottomInfo(){
        //super.displayBottomInfo();
    }

    // 10:displayFooter
    @Override
    public void displayFooter(){
        super.displayFooter();
    }    
}
