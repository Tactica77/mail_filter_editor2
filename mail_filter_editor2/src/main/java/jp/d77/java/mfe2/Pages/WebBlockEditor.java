package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.Datas.BlockDatas;
import jp.d77.java.mfe2.Datas.FilterDatas;
import jp.d77.java.mfe2.Datas.SpotBlockDatas;
import jp.d77.java.mfe2.Datas.BlockDatas.BlockData;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.BasicIO.ToolNums;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebBlockEditor extends AbstractMfe{
    private BlockDatas      m_bd;
    private SpotBlockDatas  m_sbd;
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
        if ( this.getConfig().get("mode").orElse("").equals("spot") ){

        }else{
            this.m_block_form_data = new BlockFormData(
                this.getConfig().get( "edit_ymd" ).orElse( ToolDate.Format( LocalDate.now(), "uuuuMMdd" ).orElse("") )
                ,this.getConfig().get( "edit_cc" ).orElse("")
                ,this.getConfig().get( "edit_cidr" ).orElse("")
                ,this.getConfig().get( "edit_org" ).orElse("")
            );
        }
    }

    // 2:load
    @Override
    public void load() {
        super.load();
        FilterDatas fd;

        fd = new FilterDatas();
        fd.loadCountryFilter( this.getConfig().getDataFilePath() + "/country_filter.txt", true );
        if ( this.getConfig().get("mode").orElse("").equals("spot") ){
            this.m_sbd = new SpotBlockDatas( fd, "spot block" );
            this.m_sbd.load( this.getConfig().getDataFilePath() + "/block_list_spot.txt" );

        }else{
            this.m_bd = new BlockDatas( fd, "black list" );
            this.m_bd.load( this.getConfig().getDataFilePath() + "/block_list_black.txt" );

        }
    }

    // 3:post_save_reload
    @Override
    public void post_save_reload() {
        super.post_save_reload();
        boolean change = false;

        // スポットは処理しない
        if ( this.getConfig().get("mode").orElse("").equals("spot") ) return;

        // ENABLE
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

        // DISABLE
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

        // DELETE
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

        // ADD
        if ( this.getConfig().get( "submit_block_add" ).isPresent() ){
            String ymd = this.getConfig().get( "edit_ymd" ).orElse( null );
            String cc = this.getConfig().get( "edit_cc" ).orElse( "??" );
            String cidr = this.getConfig().get( "edit_cidr" ).orElse( null );
            String org = this.getConfig().get( "edit_org" ).orElse( "??" );
            if ( ymd == null || cidr == null || ymd.isBlank() || cidr.isEmpty() ){
                this.getConfig().addAlertError( "YYYYMMDD or CIDR is empty" );
            }else{
                BlockData rec = new BlockData(
                    true
                    , cidr
                    , ToolDate.Str2LocalDate( ymd ).orElse( LocalDate.now() )
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

        // 変更したら保存
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
        BSSForm f = BSSForm.newForm();

        f.tableTop(
            new BSOpts()
                .id( "editor-table")
                .fclass("table table-bordered table-striped")
                .border("1")
        );
        // Table Header
        f.tableHeadTop();
        f.tableRowTop();
        if ( ! this.getConfig().get("mode").orElse("").equals("spot") ) f.tableTh( "CMD" );
        f.tableTh( "YMD" );
        f.tableTh( "CC" );
        f.tableTh( "CIDR" );
        f.tableTh( "Organization" );
        if ( this.getConfig().get("mode").orElse("").equals("spot") ){
            f.tableTh( "cnt" );
            f.tableTh( "score" );
        }
        f.tableTh( "Parent" );
        f.tableRowBtm();
        f.tableHeadBtm();

        f.tableBodyTop();
        String[] cidrs;
        if ( this.getConfig().get("mode").orElse("").equals("spot") ) {
            cidrs = this.m_sbd.getCidrs();
        }else{
            cidrs = this.m_bd.getCidrs();
        }

        for ( String cidr: cidrs ){
            String ymd,cc,org,parent_ip;
            String add_opt = "";

            if ( this.getConfig().get("mode").orElse("").equals("spot") ) {
                // Spot
                if ( this.m_sbd.getSRecord(cidr).isEmpty() ) continue;
                ymd = ToolDate.Format( this.m_sbd.getSRecord(cidr).get().date, "uuuuMMdd" ).orElse("?");
                cc = this.m_sbd.getSRecord(cidr).get().cc;
                org = this.m_sbd.getSRecord(cidr).get().org;
                parent_ip = this.m_sbd.getSRecord(cidr).get().parent_ip;
            }else{
                if ( this.m_bd.getRecord(cidr).isEmpty() ) continue;
                // disable行
                if ( ! this.m_bd.getRecord(cidr).get().enabled ) add_opt = "style=\"background-color: #cccccc;\"";
                ymd = ToolDate.Format( this.m_bd.getRecord(cidr).get().date, "uuuuMMdd" ).orElse("?");
                cc = this.m_bd.getRecord(cidr).get().cc;
                org = this.m_bd.getRecord(cidr).get().org;
                parent_ip = this.m_bd.getRecord(cidr).get().parent_ip;
            }


            // 描画
            f.tableRowTop();
            if ( ! this.getConfig().get("mode").orElse("").equals("spot") ) {
                f.tableTdHtml( 
                    BSSForm.newForm().formInput(
                        BSOpts
                            .init( "type", "checkbox")
                            .set("name", "edit_select_cidr" )
                            .set("value", this.m_bd.getRecord(cidr).get().ip )
                    ).toString()
                    , add_opt );
            }
            f.tableTd( ymd, add_opt );
            f.tableTd( cc, add_opt );
            f.tableTdHtml(
                    cidr
                    + ToolAny.IPLink(
                        new BlockFormData( ymd, cc, cidr, org )
                    )
                    , add_opt
                );
            if ( org.length() > 20 ){
                f.tableTd( org.substring(0, 19) + "...", add_opt );
            }else{
                f.tableTd( org, add_opt );
            }
            if ( this.getConfig().get("mode").orElse("").equals("spot") ){
                f.tableTd( this.m_sbd.getSRecord(cidr).get().m_cnt + "", add_opt );
                f.tableTd( ToolNums.Float2Str( this.m_sbd.getSRecord(cidr).get().m_score, 2 ) , add_opt );
            }
            f.tableTdHtml( parent_ip.replace("\n", "<BR>") , add_opt );
            f.tableRowBtm();
        }
        f.tableBodyBtm();
        f.tableBtm();
        if ( this.getConfig().get("mode").orElse("").equals("spot") ) {
            return f.toString();

        }else{
            f.formBtm();
            return this.BlockEditForm() + f.toString();
        }
    }

    private String BlockEditForm(){
        BSSForm f = BSSForm.newForm();

        f.formTop( this.getUri(), false);

        // command buttons
        f.divRowTop();
        f.divTop(12);
        f.formSubmit(
            BSOpts.init()
                .label( "ENABLE" )
                .name( "submit_block_enable" )
                .value( "ENABLE" )
        );
        f.formSubmit(
            BSOpts.init()
                .label( "DISABLE" )
                .name( "submit_block_disable" )
                .value( "DISABLE" )
        );
        f.formSubmit(
            BSOpts.init()
                .label( "DELETE" )
                .name( "submit_block_delete" )
                .value( "DELETE" )
        );
        f.divBtm(12);
        f.divRowBtm();

        // Labels
        f.divRowTop();
        f.divTop(2).formLabel( BSOpts.init().label( "YYYYMMDD" ) ).divBtm(2);
        f.divTop(2).formLabel( BSOpts.init().label( "CC" ) ).divBtm(2);
        f.divTop(2).formLabel(  BSOpts.init().label( "CIDR" ) ).divBtm(2);
        f.divTop(2).formLabel(  BSOpts.init().label( "Organization" ) ).divBtm(2);
        f.divTop(2).divBtm(2);
        f.divTop(2).divBtm(2);
        f.divRowBtm();

        // Forms
        f.divRowTop();
        f.divTop(2)
            .formInput( BSOpts.init().name( "edit_ymd" ).value( this.m_block_form_data.ymd ) )
            .divBtm(2);
        f.divTop(2)
            .formInput( BSOpts.init().name( "edit_cc" ).value( this.m_block_form_data.Cc ) )
            .divBtm(2);
        f.divTop(2)
            .formInput( BSOpts.init().name( "edit_cidr" ).value( this.m_block_form_data.Cidr ) )
            .divBtm(2);
        f.divTop(2)
            .formInput( BSOpts.init().name( "edit_org" ).value( this.m_block_form_data.org ) )
            .divBtm(2);
        f.divTop(2)
            .formSubmit(
                BSOpts.init()
                    .label( "ADD" )
                    .name( "submit_block_add" )
                    .value( "ADD" )
            )
            .divBtm(2);
        f.divTop(2).divBtm(2);
        f.divRowBtm();

        return f.toString();
    }
}
