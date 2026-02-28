package jp.d77.java.mfe2;

import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpServletRequest;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.Pages.CliUpdate;
import jp.d77.java.mfe2.Pages.WebBlockEditor;
import jp.d77.java.mfe2.Pages.WebLogs;
import jp.d77.java.mfe2.Pages.WebRdap;
import jp.d77.java.mfe2.Pages.WebSubnets;
import jp.d77.java.mfe2.Pages.WebTop;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolNums;
import jp.d77.java.tools.HtmlIO.AbstractWebPage;
import jp.d77.java.tools.HtmlIO.WebConfig;

@RestController
public class Mfe2Main {

    @RequestMapping("/")  // ルートへこのメソッドをマップする
    public String Mfe( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebTop( new Mfe2Config( "/" ) );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @RequestMapping("/rdap")  // ルートへこのメソッドをマップする
    public String Rdap( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebRdap( new Mfe2Config( "/rdap" ) );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @RequestMapping("/logs")  // ルートへこのメソッドをマップする
    public String Logs( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebLogs( new Mfe2Config( "/logs" ) );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @RequestMapping("/subnets")  // ルートへこのメソッドをマップする
    @SuppressWarnings("null")
    public String Subnets( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebSubnets( new Mfe2Config( "/subnets" ) );
        web.getConfig().add("ip", WebUtils.findParameterValue(request, "ip") );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @RequestMapping("/cli_update")  // ルートへこのメソッドをマップする
    public String CliUpdate( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new CliUpdate( new Mfe2Config( "/cli_update" ) );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @RequestMapping("/block_editor")  // ルートへこのメソッドをマップする
    public String BlockEditor( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebBlockEditor( new Mfe2Config( "/block_editor" ) );
        this.setForm( request, web.getConfig() );
        web.getConfig().overwrite( "edit_select_cidr", request.getParameterValues("edit_select_cidr") );

        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            System.out.println(name + " = " + request.getParameter(name));
        }
        return this.procWeb( web );
    }

    @SuppressWarnings("null")
    private void setForm( HttpServletRequest request, WebConfig cfg ){
        // Modeを取得
        cfg.overwrite("mode", WebUtils.findParameterValue(request, "mode") );
        Map<String, Object> params;

        // フォーム投稿を取得(edit_から始まる項目を取得)
        params = WebUtils.getParametersStartingWith(request, "edit_");
        if (!params.isEmpty()) {
            for (Entry<String, Object> e : params.entrySet()) {
                //Debugger.InfoPrint( "---------------------------->edit_" + e.getKey() );
                if ( e.getValue() instanceof String[] ){
                    // 配列の場合
                    cfg.overwrite("edit_" + e.getKey(), (String[])e.getValue() );
                }else{
                    cfg.overwrite("edit_" + e.getKey(), e.getValue().toString() );
                }
            }
        }

        // フォーム投稿を取得(submit_から始まる項目を取得)

        params = WebUtils.getParametersStartingWith(request, "submit_");
        if (!params.isEmpty()) {
            for (Entry<String, Object> e : params.entrySet()) {
                cfg.overwrite("submit_" + e.getKey(), e.getValue().toString() );
            }
        }
    }

    private String procWeb( AbstractWebPage Web ){
        Debugger.TracePrint();
        Web.init();
        Web.load();
        Web.post_save_reload();
        Web.proc();
        Web.displayHeader();
        Web.displayNavbar();
        Web.displayInfo();
        Web.displayBody();
        Web.displayBottomInfo();
        Web.displayFooter();
        Debugger.InfoPrint( "------ Done bytes="  + ToolNums.FromatedNum( Web.toString().length() ) + " ------" );
        return Web.toString();
    }
}
