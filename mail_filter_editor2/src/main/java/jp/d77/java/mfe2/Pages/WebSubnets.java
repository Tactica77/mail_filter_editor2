package jp.d77.java.mfe2.Pages;

import java.util.HashMap;

import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolNet;
import jp.d77.java.mfe2.BasicIO.ToolNet.Cidr;
import jp.d77.java.mfe2.BasicIO.ToolNet.RangeResult;
import jp.d77.java.mfe2.BasicIO.ToolRDAP.RDAPdata;
import jp.d77.java.mfe2.BasicIO.ToolRDAP;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.HtmlIO.BSSForm;
import jp.d77.java.tools.HtmlIO.HtmlString;

public class WebSubnets extends AbstractMfe{
public WebSubnets( Mfe2Config cfg ) {
        super( cfg );
        this.setHtmlTitle( "MFE2-Subnets" );
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
        //this.getHtml().addString( BSSForm.getTableHeader( "subnets" ) );
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

    public record RDAPresults(String cidr, String cc, String org, String server, String start_ip, String end_ip) {}

    // 8:displayBody
    @Override
    public void displayBody() {
        super.displayBody();

        if ( this.getConfig().get("ip").isEmpty() ) {
            this.getHtml().addStringCr( HtmlString.h( 1, "ip=null") );
            return;
        }else{
            this.getHtml().addStringCr( HtmlString.h( 1, "ip=" + this.getConfig().get("ip").get() ) );
        }
        String a[] = this.getConfig().get("ip").get().split( "/");
        String ip = a[0];

        HashMap<Integer,RDAPresults> rdap_results = new HashMap<Integer,RDAPresults>();
        int cidr_i = 33;
        int cidr_before = 33;
        for( int i = 32; i >= 1; i-- ){
            RangeResult r = ToolNet.cidr2range( ip, i ).orElse( null );
            if ( r == null ) continue;
            RDAPdata rd = ToolRDAP.getRDAPdata( r.start_ip() ).orElse( null );
            if ( rd == null ) continue;

            if ( cidr_i <= i ) {
                Debugger.InfoPrint( "i=" + i + " reuse=" + cidr_before + " to=" + cidr_i );
                rdap_results.put( i,  rdap_results.get( cidr_before ) );
                continue;
            }
            Debugger.InfoPrint( "i=" + i );
            cidr_i = i;
            cidr_before = i;

            String[] cidrs = rd.getCiders();
            String cidr = "-";
            if ( cidrs.length >= 1 ) cidr = cidrs[0];
            String cc = rd.get( "country" ).orElse( "-" );
            String org = rd.get( "name" ).orElse( "-" );
            String rir = rd.getRdapServerFQDN().orElse( "-" );
            String start_ip = "-";
            String end_ip = "-";

            Cidr cidr_data = ToolNet.CidrSplit( cidr ).orElse( null );
            if ( cidr_data != null ){
                cidr_i = cidr_data.cidr();
                RangeResult r2 = ToolNet.cidr2range( cidr_data.ip(), cidr_data.cidr() ).orElse( null );
                if ( r2 != null ) {
                    start_ip = r2.start_ip();
                    end_ip = r2.end_ip();
                }
            }
            rdap_results.put( i,  new RDAPresults( cidr, cc, org, rir, start_ip, end_ip ) );
        }

        BSSForm f = BSSForm.newForm();
        f.tableTop("subnets-table");

        f.tableHeadTop();
        f.tableRowTh("CIDR", "MASK", "RANGE", "RDAP CIDR", "RDAP RANGE", "CC", "ORG NAME", "RIR");
        f.tableHeadBtm();

        f.tableBodyTop();
        for( int i = 1; i <= 32; i++ ){
            RangeResult r = ToolNet.cidr2range( ip, i ).orElse( null );
            if ( r == null ) continue;
            if ( ! rdap_results.containsKey( i ) ) continue;

            f.tableRowTop();
            // CIDR
            f.tableTd( "/" + i );

            // MASK
            f.tableTd( ToolNet.prefix2mask(i) );

            // RANGE
            f.tableTd( r.start_ip() + "-" + r.end_ip() );

            // RDAP CIDR
            //f.tableTdHtml( SharedWebLib.linkBlockEditor(cidr, cc, org) );
            f.tableTdHtml( rdap_results.get(i).cidr + ToolAny.IPLink( rdap_results.get(i).start_ip ) );

            // RDAP RANGE
            f.tableTdHtml( rdap_results.get(i).start_ip + "-" + rdap_results.get(i).end_ip );

            // CC
            f.tableTd( rdap_results.get(i).cc );

            // ORG NAME
            f.tableTd( rdap_results.get(i).org );

            // RIR
            f.tableTd( rdap_results.get(i).server );

            f.tableRowBtm();
        }
        f.tableBodyBtm();

        f.tableBtm();
        this.getHtml().addString( f.toString() );
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
