package jp.d77.java.mfe2.Pages;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebForms {
    public static String RDAPsearch( Mfe2Config cfg ){
        // 入力フォーム
        BSSForm f = BSSForm.newForm()
            .formTop( "/rdap", false )
                .formInputHidden(
                    BSOpts.init("name", "mode" )
                    .value( cfg.getMethod("mode").orElse("-") )
                )
                .formInput(
                    BSOpts.init("name", "edit_ip" )
                    .value( cfg.getMethod("edit_ip").orElse(null) )
                )
                .formSubmit(
                    BSOpts.init("name", "submit_rdap_ip" )
                    .label("Search")
                    .value("1")
                )
                .formBtm();
        return f.toString();

    }
}
