package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.BasicIO.ToolNet;
import jp.d77.java.mfe2.Datas.BlockDatas;
import jp.d77.java.mfe2.Datas.BlockDatas.Record;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebBlockEditor extends AbstractMfe{
    private BlockDatas  m_bd;
    public record  BlockFormData( String ymd, String Cc, String Cidr, String org ){}
    private BlockFormData m_block_form_data = null;

    public WebBlockEditor(Mfe2Config cfg) {
        super( cfg );
        this.setHtmlTitle( "MFE2" );
    }

    // 1:init
    @Override
    public void init() {
        super.init();
        this.m_block_form_data = new BlockFormData(
            this.getConfig().get( "edit_ymd" ).orElse( ToolDate.Fromat( LocalDate.now(), "uuuuMMdd" ).orElse("") )
            ,this.getConfig().get( "edit_cc" ).orElse("")
            ,this.getConfig().get( "edit_cidr" ).orElse("")
            ,this.getConfig().get( "edit_org" ).orElse("")
        );
    }

    // 2:load
    @Override
    public void load() {
        super.load();
        this.m_bd = new BlockDatas();
        this.m_bd.load( this.getConfig().getDataFilePath() + "/block_list_black.txt" );
        
    }

    // 3:post_save_reload
    @Override
    public void post_save_reload() {
        super.post_save_reload();
        boolean change = false;

        if ( this.getConfig().get( "submit_block_enable" ).isPresent() ){
            // enableへ
            this.getConfig().addAlertInfo( "to ENABLE start");
            for (String cidr: this.getConfig().gets( "edit_select_cidr" ) ){
                if ( this.m_bd.getRecord(cidr).isEmpty() ){
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...未発見データ");
                }else if ( this.m_bd.getRecord(cidr).get().enabled == true ){
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...未処理(enable->enable)");
                }else{
                    this.m_bd.getRecord(cidr).get().enabled = true;
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...disable->enable");
                    change = true;
                }
            }
            this.getConfig().addAlertInfo( "to ENABLE done");
        }
        if ( this.getConfig().get( "submit_block_disable" ).isPresent() ){
            // disableへ
            this.getConfig().addAlertInfo( "to DISABLE start");
            for (String cidr: this.getConfig().gets( "edit_select_cidr" ) ){

                if ( this.m_bd.getRecord(cidr).isEmpty() ){
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...未発見データ");
                }else if ( this.m_bd.getRecord(cidr).get().enabled == false ){
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...未処理(disable->disable)");
                }else{
                    this.m_bd.getRecord(cidr).get().enabled = false;
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...enable->disable");
                    change = true;
                }
            }
            this.getConfig().addAlertInfo( "to DISABLE done");
        }
        if ( this.getConfig().get( "submit_block_delete" ).isPresent() ){
            this.getConfig().addAlertInfo( "to DELETE start");
            for (String cidr: this.getConfig().gets( "edit_select_cidr" ) ){
                if ( this.m_bd.remove(cidr) ){
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...remove");
                    change = true;
                }else{
                    this.getConfig().addAlertError( "checked=" + cidr + "...not removed");
                }
            }
            this.getConfig().addAlertInfo( "to DELETE done");
        }
        if ( this.getConfig().get( "submit_block_add" ).isPresent() ){
            String ymd = this.getConfig().get( "edit_ymd" ).orElse( null );
            String cc = this.getConfig().get( "edit_cc" ).orElse( "??" );
            String cidr = this.getConfig().get( "edit_cidr" ).orElse( null );
            String org = this.getConfig().get( "edit_org" ).orElse( "??" );
            if ( ymd == null || cidr == null || ymd.isBlank() || cidr.isEmpty() ){
                this.getConfig().addAlertError( "YYYYMMDD or CIDR is empty" );
            }else{
                Record rec = new Record(
                    true
                    , cidr
                    , ToolDate.YMD2LocalDate( ymd ).orElse( LocalDate.now() )
                    , cc
                    , org
                );
                if ( this.m_bd.add(rec) ){
                    this.getConfig().addAlertInfo( "checked=" + cidr + "...add");
                    change = true;
                }else{
                    this.getConfig().addAlertError( "checked=" + cidr + "...登録済み?");
                }
            }
        }
        if ( change ){
            Optional<String> res = this.m_bd.save( this.getConfig().getDataFilePath() + "/block_list_black.txt" );
            if ( res.isEmpty() ){
                this.getConfig().addAlertInfo( "block_list_black.txtへ保存しました。" );
            }else{
                this.getConfig().addAlertError( "block_list_black.txtへ保存の保存に失敗しました。" + res.get() );
            }
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
        this.getHtml().addString( BSSForm.getTableHeader( "editor-table" ) );
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

        this.getHtml().addString( this.displayEditor() );
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

    private String displayEditor(){
        String res = "";
        BSSForm f = BSSForm.newForm();

        f.tableTop(
            new BSOpts()
                .id( "editor-table")
                .fclass("table table-bordered table-striped")
                .border("1")
        );

        // Table Header
        f.tableHeadTop()
            .tableRowTop()
            .tableTh( "CMD" )
            .tableTh( "YMD" )
            .tableTh( "CC" )
            .tableTh( "CIDR" )
            .tableTh( "Organization" )
            .tableTh( "Parent" )
            .tableRowBtm()
            .tableHeadBtm();

        f.tableBodyTop();
        for ( String r: this.m_bd.getCidrs() ){
            if ( this.m_bd.getRecord(r).isEmpty() ) continue;
            String ymd = ToolDate.Fromat( this.m_bd.getRecord(r).get().date, "uuuuMMdd" ).orElse("?");

            // parentIPの有無確認
            String parentIP = "-";
            for ( String chk: this.m_bd.getCidrs() ){
                if ( r.equals( chk ) ) continue;    // 同一CIDR
                if ( this.m_bd.getRecord(chk).isEmpty() ) continue; // 空
                String s = ToolNet.isWithinCIDR(
                    this.m_bd.getRecord(r).get().ip
                    , this.m_bd.getRecord(chk).get().ip
                ).orElse( null );
                if ( s == null ) continue;  // IP以外
                if ( s.equals( this.m_bd.getRecord(chk).get().ip ) ){
                    // 内包するIP特定
                    parentIP = this.m_bd.getRecord(chk).get().ip;
                    continue;
                }
            }
            
            String add_opt = "";
            if ( ! this.m_bd.getRecord(r).get().enabled ){
                // disable行
                add_opt = "style=\"background-color: #cccccc;\"";
            }

            // 描画
            f.tableRowTop();
            f.tableTdHtml( 
                BSSForm.newForm().formInput(
                    BSOpts
                        .init( "type", "checkbox")
                        .set("name", "edit_select_cidr" )
                        .set("value", this.m_bd.getRecord(r).get().ip )
                ).toString()
                , add_opt );
            f.tableTd( ymd, add_opt );
            f.tableTd( this.m_bd.getRecord(r).get().cc, add_opt );
            f.tableTdHtml(
                    this.m_bd.getRecord(r).get().ip
                    + ToolAny.IPLink(
                        new BlockFormData(
                            ymd
                            ,this.m_bd.getRecord(r).get().cc
                            ,this.m_bd.getRecord(r).get().ip
                            ,this.m_bd.getRecord(r).get().org
                        )
                    )
                    , add_opt
                );
            f.tableTd( this.m_bd.getRecord(r).get().org, add_opt );
            f.tableTd( parentIP, add_opt );
            f.tableRowBtm();
        }
        f.tableBodyBtm();
        f.tableBtm();
        
        res += BSSForm.newForm().formTop( this.getUri(), false);

        // command buttons
        res += BSSForm.newForm().divRowTop();
        res += BSSForm.newForm().divTop(12);
        res += BSSForm.newForm().formSubmit(
            BSOpts.init()
                .label( "ENABLE" )
                .name( "submit_block_enable" )
                .value( "ENABLE" )
        );
        res += BSSForm.newForm().formSubmit(
            BSOpts.init()
                .label( "DISABLE" )
                .name( "submit_block_disable" )
                .value( "DISABLE" )
        );
        res += BSSForm.newForm().formSubmit(
            BSOpts.init()
                .label( "DELETE" )
                .name( "submit_block_delete" )
                .value( "DELETE" )
        );
        res += BSSForm.newForm().divBtm(12);
        res += BSSForm.newForm().divRowBtm();

        // Labels
        res += BSSForm.newForm().divRowTop();
        res += BSSForm.newForm().divTop(2).formLabel( BSOpts.init().label( "YYYYMMDD" ) ).divBtm(2);
        res += BSSForm.newForm().divTop(2).formLabel( BSOpts.init().label( "CC" ) ).divBtm(2);
        res += BSSForm.newForm().divTop(2).formLabel(  BSOpts.init().label( "CIDR" ) ).divBtm(2);
        res += BSSForm.newForm().divTop(2).formLabel(  BSOpts.init().label( "Organization" ) ).divBtm(2);
        res += BSSForm.newForm().divTop(2).divBtm(2);
        res += BSSForm.newForm().divTop(2).divBtm(2);
        res += BSSForm.newForm().divRowBtm();

        // Forms
        res += BSSForm.newForm().divRowTop();
        res += BSSForm.newForm().divTop(2)
            .formInput( BSOpts.init().name( "edit_ymd" ).value( this.m_block_form_data.ymd ) )
            .divBtm(2);
        res += BSSForm.newForm().divTop(2)
            .formInput( BSOpts.init().name( "edit_cc" ).value( this.m_block_form_data.Cc ) )
            .divBtm(2);
        res += BSSForm.newForm().divTop(2)
            .formInput( BSOpts.init().name( "edit_cidr" ).value( this.m_block_form_data.Cidr ) )
            .divBtm(2);
        res += BSSForm.newForm().divTop(2)
            .formInput( BSOpts.init().name( "edit_org" ).value( this.m_block_form_data.org ) )
            .divBtm(2);
        res += BSSForm.newForm().divTop(2)
            .formSubmit(
                BSOpts.init()
                    .label( "ADD" )
                    .name( "submit_block_add" )
                    .value( "ADD" )
            )
            .divBtm(2);
        res += BSSForm.newForm().divTop(2).divBtm(2);
        res += BSSForm.newForm().divRowBtm();

        res += f.toString();
        res += BSSForm.newForm().formBtm();

        return res;
    }
}
