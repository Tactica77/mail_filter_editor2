package jp.d77.java.mfe2.Pages;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;

public class WebTop extends AbstractMfe{
    public WebTop( Mfe2Config cfg ) {
        super( cfg );
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
        this.getHtml().addStringBr( WebForms.RDAPsearch( this.getConfig() ) );
        this.getHtml().addStringBr( "Data Path=" + this.getConfig().getDataFilePath() );
        this.getHtml().addStringBr( "Log Path=" + this.getConfig().getLogFilePath() );
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
}
