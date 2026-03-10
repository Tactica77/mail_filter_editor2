package jp.d77.java.mfe2.Pages;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.HtmlIO.AbstractWebPage;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSS;
import jp.d77.java.tools.HtmlIO.BSSForm;
import jp.d77.java.tools.HtmlIO.HtmlString;
import jp.d77.java.tools.HtmlIO.InterfaceWebPage;

public abstract class AbstractMfe extends AbstractWebPage implements InterfaceWebPage {
    // コンストラクタ
    public AbstractMfe( Mfe2Config cfg ){
        super(cfg);
        Debugger.TracePrint();
    }
    // 1:init
    @Override
    public void init(){
        Debugger.TracePrint();
    }

    // 2:load
    @Override
    public void load(){
        Debugger.TracePrint();
    }

    // 3:post_save_reload
    @Override
    public void post_save_reload(){
        Debugger.TracePrint();
    }

    // 4 proc
    @Override
    public void proc(){
        Debugger.TracePrint();
    }

    // 5:displayHeader
    @Override
    public void displayHeader(){
        super.displayHeader();
        this.getHtml().addStringCr( "<STYLE>" );
        this.getHtml().addStringCr( ".ellipsis120 {" );
        this.getHtml().addStringCr( "    max-width: 120px;" );
        this.getHtml().addStringCr( "    display: inline-block;" );
        this.getHtml().addStringCr( "    overflow: hidden;" );
        this.getHtml().addStringCr( "    white-space: nowrap;" );
        this.getHtml().addStringCr( "    text-overflow: ellipsis;" );
        this.getHtml().addStringCr( "    vertical-align: bottom;" );
        this.getHtml().addStringCr( "}");
        this.getHtml().addStringCr( "</STYLE>" );
    }
    
    // 6:displayNavbar
    @Override
    public void displayNavbar(){
        Debugger.TracePrint();
        
        String title = this.getHtmlTitle();

        this.getHtml().addString( BSS.getNavbarHeader( title ) );
        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("TOP").href("/") ) );
        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("LOGS").href("/logs") ) );

        this.getHtml().addString(
            BSS.NavbarDropDown(
                "BlockLists"
                , BSS.getNavbarLinkItem( BSOpts.init().title("Block Editor").href("/block_list") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("Spot Block List").href("/block_list?mode=spot") )
            )
        );
        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("RDAP").href("/rdap") ) );
        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("RSPAMD").href("https://rspamd.d77.jp/").target("_blank") ) );

        this.getHtml().addString(
            BSS.NavbarDropDown(
                "CLI"
                , BSS.getNavbarLinkItem( BSOpts.init().title("Session Update(Today)").href("/cli_update?mode=sessionlog&edit_diffdate=1&submit_gui_on=1").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("Create Block Data").href("/cli_update?mode=blockdata&submit_gui_on=1").fclass("dropdown-item") )
            )
        );

        this.getHtml().addString( BSS.getNavbarFooter() );

        this.getHtml().addStringCr( "<DIV class=\"container-fluid\">")
            .addStringCr("<DIV class=\"row\">" );
    }

    // 7:displayInfo
    @Override
    public void displayInfo(){
        super.displayInfo();
        //Debugger.TracePrint();
    }

    // 8:displayBody
    @Override
    public void displayBody(){
        super.displayBody();
        //Debugger.TracePrint();
    }

    // 9:displayBottomInfo
    @Override
    public void displayBottomInfo(){
        super.displayBottomInfo();
        //Debugger.TracePrint();
    }

    // 10:displayFooter
    @Override
    public void displayFooter(){
        super.displayFooter();
        //Debugger.TracePrint();
    }

    public String getTableHeader( String id ){
        String h;
        h = "<SCRIPT>\n"
            + "jQuery(function($){\n"
            + BSSForm.sp() + "$(\"#" + HtmlString.HtmlEscape(id) + "-table\").DataTable({\n"
            + BSSForm.sp() + "searching: true,\n"
            + BSSForm.sp() + "fixedHeader: true,\n"
            + BSSForm.sp() + "ordering: true,\n"
            + BSSForm.sp() + "info: true,\n"
            + BSSForm.sp() + "paging: true,\n"
            + BSSForm.sp() + "lengthMenu: [[20,40,80,100,-1],[20,40,80,100,'ALL']],\n"
            + BSSForm.sp() + "pagingType: 'full_numbers',\n"
            + BSSForm.sp() + "pageLength: 20\n"
            + BSSForm.sp() + "});\n"
            + "});\n"
            + "</SCRIPT>\n";
        return h;
    }

    @Override
    public Mfe2Config getConfig(){
        return (Mfe2Config) super.getConfig();
    }
    
}
