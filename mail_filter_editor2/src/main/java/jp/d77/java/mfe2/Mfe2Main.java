package jp.d77.java.mfe2;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpServletRequest;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.Pages.WebLogs;
import jp.d77.java.mfe2.Pages.WebRdap;
import jp.d77.java.mfe2.Pages.WebTop;
import jp.d77.java.tools.BasicIO.BasicConfig;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolNums;
import jp.d77.java.tools.HtmlIO.AbstractWebPage;

@RestController
public class Mfe2Main {

    @RequestMapping("/")  // ルートへこのメソッドをマップする
    public String Mfe( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebTop( "/", new Mfe2Config() );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @RequestMapping("/rdap")  // ルートへこのメソッドをマップする
    public String Rdap( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebRdap( "/rdap", new Mfe2Config() );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @RequestMapping("/logs")  // ルートへこのメソッドをマップする
    public String Logs( HttpServletRequest request ) {
        Debugger.init();
        Debugger.InfoPrint( "------ START ------" );

        // 表示用クラスの設定
        AbstractWebPage web = new WebLogs( "/logs", new Mfe2Config() );
        this.setForm( request, web.getConfig() );

        return this.procWeb( web );
    }

    @SuppressWarnings("null")
    private void setForm( HttpServletRequest request, BasicConfig cfg ){
        // Modeを取得
        cfg.addMethod("mode", WebUtils.findParameterValue(request, "mode") );
        Map<String, Object> params;

        // フォーム投稿を取得(edit_から始まる項目を取得)
        params = WebUtils.getParametersStartingWith(request, "edit_");
        if (!params.isEmpty()) {
            for (Entry<String, Object> e : params.entrySet()) {
                cfg.addMethod("edit_" + e.getKey(), e.getValue().toString() );
            }
        }

        // フォーム投稿を取得(submit_から始まる項目を取得)

        params = WebUtils.getParametersStartingWith(request, "submit_");
        if (!params.isEmpty()) {
            for (Entry<String, Object> e : params.entrySet()) {
                cfg.addMethod("submit_" + e.getKey(), e.getValue().toString() );
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
