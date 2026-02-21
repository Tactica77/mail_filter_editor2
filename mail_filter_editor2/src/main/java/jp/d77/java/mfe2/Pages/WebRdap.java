package jp.d77.java.mfe2.Pages;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolRDAP;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebRdap extends AbstractMfe{
    public WebRdap( Mfe2Config cfg) {
        super( cfg );
        this.setHtmlTitle( "MFE2 - RDAP" );
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
        this.getHtml().addString( BSSForm.getTableHeader( "param" ) );
        this.getHtml().addString( BSSForm.getTableHeader( "dump" ) );

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
    
        this.getHtml().addString( WebForms.RDAPsearch( this.getConfig() ) );

        // 検索結果
        if ( this.getConfig().get( "edit_ip" ).isPresent() ){
            BSSForm f;
            // 基本パラメータ
            f = BSSForm.newForm();
            f.tableTop(
            new BSOpts()
                .id( "param")
                .fclass("table table-bordered table-striped")
                .border("1")
            );

            String ip = this.getConfig().get( "edit_ip" ).get();

            
            // Table Header
            f.tableHeadTop();
            f.tableRowTop();
            f.tableTh( "key" );
            f.tableTh( "Data" );
            f.tableRowBtm();
            f.tableHeadBtm();

            // Table Body
            f.tableBodyTop();

            f.tableRowTop();
            f.tableTd( "IP" );
            f.tableTd( ip );
            f.tableRowBtm();

            f.tableRowTop();
            f.tableTd( "RDAP Server" );
            f.tableTd( ToolRDAP.getParam( ip, "rdap_server" ).orElse("-") );
            f.tableRowBtm();

            f.tableRowTop();
            f.tableTd( "name" );
            f.tableTd( ToolRDAP.getParam( ip, "name" ).orElse("-") );
            f.tableRowBtm();

            f.tableRowTop();
            f.tableTd( "country" );
            f.tableTd( ToolRDAP.getParam( ip, "country" ).orElse("-") );
            f.tableRowBtm();

            f.tableRowTop();
            f.tableTd( "range" );
            f.tableTd(
                ToolRDAP.getParam( ip, "startAddress" ).orElse("-")
                + " - "
                + ToolRDAP.getParam( ip, "endAddress" ).orElse("-")
            );
            f.tableRowBtm();

            f.tableRowTop();
            f.tableTd( "ipVersion" );
            f.tableTd( ToolRDAP.getParam( ip, "ipVersion" ).orElse("-") );
            f.tableRowBtm();

            for ( int i = 0; i < 10 ; i++ ){
                if ( ToolRDAP.getParam( ip, "cidr_" + i ).isEmpty() ) break;
                f.tableRowTop();
                f.tableTd( "CIDR " + i );
                f.tableTd( ToolRDAP.getParam( ip, "cidr_" + i ).get() );
                f.tableRowBtm();
            }

            f.tableBodyBtm();
            f.tableBtm();
            this.getHtml().addString( f.toString() );

            f = BSSForm.newForm();
        
            f.tableTop(
            new BSOpts()
                .id( "dump")
                .fclass("table table-bordered table-striped")
                .border("1")
            );
            // Table Header
            f.tableHeadTop();
            f.tableRowTop();
            f.tableTh( "Path" );
            f.tableTh( "Data" );
            f.tableRowBtm();
            f.tableHeadBtm();

            // Table Body
            f.tableBodyTop();

            LinkedHashMap<String, String> result = ToolRDAP.dump(ip);
            for ( String path: result.keySet() ){
                f.tableRowTop();
                f.tableTd( path );
                f.tableTd( result.get(path) );
                f.tableRowBtm();
            }
            f.tableBodyBtm();
            f.tableBtm();
            this.getHtml().addString( f.toString() );

            ArrayList<String> cache = new ArrayList<String>();
            for ( String s: ToolRDAP.getCacheIPs() ){
                cache.add( "<A Href=\"/rdap?edit_ip=" + s + "\">" + s + "</A>" );
            }
            
            this.getHtml().addString( String.join(" / ", cache) );
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
}
