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
    public AbstractMfe( String uri, Mfe2Config cfg ){
        super(uri, cfg);
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
        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("RDAP").href("/rdap") ) );
        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("LOGS").href("/logs") ) );


        /*
        this.getHtml().addString(
            BSS.NavbarDropDown(
                "アップロード"
                , BSS.getNavbarLinkItem( BSOpts.init().title("入出金明細") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("ハマギン(メイン)").href("/uploads?mode=HMG_M").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("ハマギン(引き落とし用)").href("/uploads?mode=HMG_S").fclass("dropdown-item") )
                , BSS.NavbarHR()
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI(代表口座)").href("/uploads?mode=SBI_M").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI(ハイブリッド口座)").href("/uploads?mode=SBI_S").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI入出金明細").href("/uploads?mode=SBI_INOUT_MEISAI").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI保有証券").href("/uploads?mode=SBI_KABU").fclass("dropdown-item") )
                , BSS.NavbarHR()
                , BSS.getNavbarLinkItem( BSOpts.init().title("クレカ").href("/uploads?mode=CRECA").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("取引履歴").href("/uploads?mode=TRADE_HIST").fclass("dropdown-item") )
//                , BSS.getNavbarLinkItem( BSOpts.init().title("時価(月末)").href("/uploads?mode=MARKET_VALUE").fclass("dropdown-item") )
            )
        );

        this.getHtml().addString(
            BSS.NavbarDropDown(
                "素データ"
                , BSS.getNavbarLinkItem( BSOpts.init().title("入出金明細") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("ハマギン(メイン)").href("/deposit_details?mode=HMG_M").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("ハマギン(引き落とし用)").href("/deposit_details?mode=HMG_S").fclass("dropdown-item") )
                , BSS.NavbarHR()
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI(代表口座)").href("/deposit_details?mode=SBI_M").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI(ハイブリッド口座)").href("/deposit_details?mode=SBI_S").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI入出金明細").href("/deposit_details?mode=SBI_INOUT_MEISAI").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("SBI保有証券").href("/deposit_details?mode=SBI_KABU").fclass("dropdown-item") )
                , BSS.NavbarHR()
                , BSS.getNavbarLinkItem( BSOpts.init().title("基本収支").href("/deposit_details?mode=BASE_SYUSHI").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("クレカ").href("/deposit_details?mode=CRECA").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("取引履歴").href("/deposit_details?mode=TRADE_HIST").fclass("dropdown-item") )
                , BSS.getNavbarLinkItem( BSOpts.init().title("時価(月末)").href("/deposit_details?mode=MARKET_VALUE").fclass("dropdown-item") )
            )
        );
*/
//        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("保有株").href("/sharehold") ) );
//        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("収入・支出").href("/profit_loss") ) );
        //this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("設定").href("/config") ) );
        //this.m_html.addString( BSS.getNavbarLinkItem( BSOpts.init().title("出費").href("/expenses") ) );
//        this.getHtml().addString( BSS.getNavbarLinkItem( BSOpts.init().title("AI分析").href("/analysis_ai") ) );
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
