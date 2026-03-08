package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;
import java.util.Arrays;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.Datas.SessionLogManager;
import jp.d77.java.tools.BasicIO.ToolNums;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class CliUpdate extends AbstractMfe{
    private LocalDate       m_targetDate = null;
    private String          m_result = "no proc";
    private boolean         m_gui_on = false;
    private CliUpdateFilter m_filter;

    public SessionLogManager  m_slog;

    public CliUpdate( Mfe2Config cfg ) {
        super( cfg );
        this.m_slog = new SessionLogManager( cfg );
    }

    // 1:init
    @Override
    public void init() {
        super.init();

        if ( this.getConfig().get( "submit_gui_on" ).isPresent() ) this.m_gui_on = true;

        if ( this.getConfig().get( "mode" ).get().equals( "sessionlog" ) ) {
            // mode=sessionlog & edit_diffdate=日の差 でtargetDateを埋める
            this.m_targetDate = LocalDate.now();
            if ( this.getConfig().get( "edit_diffdate" ).isPresent() ) {
                String d = this.getConfig().get( "edit_diffdate" ).get();
                try{
                    Long i = Long.parseLong(d);
                    this.m_targetDate = this.m_targetDate.plusDays( i * (-1L) );
                }catch( NumberFormatException e ){}
                    
            }
        }

        if ( this.getConfig().get( "mode" ).get().equals( "blockdata" ) ) {
            this.m_filter = new CliUpdateFilter( this.getConfig() );
        }
    }

    // 2:load
    @Override
    public void load() {
        super.load();


        if ( this.getConfig().get( "mode" ).get().equals( "sessionlog" ) ) {
            if ( this.m_targetDate == null ) return;
            this.m_slog.create( m_targetDate );
            this.m_result = "update session log targetDate=" + this.m_targetDate;
        }

        if ( this.getConfig().get( "mode" ).get().equals( "blockdata" ) ) {
            this.m_filter.loadSessionLog( 7 );
        }
    }

    // 3:post_save_reload
    @Override
    public void post_save_reload() {
        super.post_save_reload();
        if ( this.getConfig().get( "mode" ).get().equals( "blockdata" ) ) {
            this.m_filter.createSpotBlockData();
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
        if ( this.m_gui_on ){
            super.displayHeader();
            this.getHtml().addString( BSSForm.getTableHeader( "logs" ) );
        }else{
            this.getHtml()
                .addStringCr( "<HTML lang=\"ja\">" )
                .addStringCr( "<HEAD>" )
                .addStringCr( "</HEAD>" )
                .addStringCr( "<BODY>" );
        }
    }

    // 6:displayNavbar
    @Override
    public void displayNavbar(){
        if ( this.m_gui_on ) super.displayNavbar();
    }

    // 7:displayInfo
    @Override
    public void displayInfo() {
        if ( this.m_gui_on ) super.displayInfo();
    }

    // 8:displayBody
    @Override
    public void displayBody() {
        if ( this.m_gui_on ) super.displayBody();
        this.getHtml().addStringCr( this.m_result );

        if ( this.getConfig().get( "mode" ).get().equals( "blockdata" ) && !this.m_gui_on ) {
            this.getHtml().addString( ToolAny.joinDisp( this.m_filter.getProcessHistory() ).orElse( "???" ) );

        }else if ( this.getConfig().get( "mode" ).get().equals( "blockdata" ) && this.m_gui_on ) {
            this.getHtml().addString( ToolAny.joinDisp( this.m_filter.getProcessHistory() ).orElse( "???" ) );

            BSSForm f;
            f = BSSForm.newForm();

            String[] ips = this.m_filter.getIps( false );
            Arrays.sort( ips );

            f.tableTop(
                new BSOpts()
                .id( "logs-table")
                .fclass("table table-bordered table-striped")
                .border("1")
            );

            // Table Header
            f.tableHeadTop()
                .tableRowTh( "IP", "CIDR", "Org", "CC", "C", "Score", "Histry" )
                .tableHeadBtm();

            f.tableBodyTop();

            for ( String ip: ips ){
                f.tableRowTop();
                
                // IP
                f.tableTd( ip );

                // CIDR
                f.tableTd( this.m_filter.getCidr(ip) );

                // Org
                if ( this.m_filter.getOrg(ip).length() < 8 ){
                    f.tableTd( this.m_filter.getOrg(ip) );
                }else{
                    f.tableTd( this.m_filter.getOrg(ip).substring( 0,8 ) );
                }

                // CC
                f.tableTd( this.m_filter.getCc(ip) );

                // C
                f.tableTd( this.m_filter.getCount(ip) + "" );

                // Schore
                f.tableTd( ToolNums.Float2Str( this.m_filter.getScore(ip) ) );

                // History
                f.tableTdHtml( ToolAny.joinDisp( this.m_filter.getHistry(ip) ).orElse( "???" ) );

                f.tableRowBtm();
            }

            f.tableBodyBtm();
            f.tableBtm();
            this.getHtml().addString( f.toString() );
        }
    }

    // 9:displayBottomInfo
    @Override
    public void displayBottomInfo(){
        if ( m_gui_on ) super.displayBottomInfo();
    }

    // 10:displayFooter
    @Override
    public void displayFooter(){
        super.displayFooter();
    }    
}
