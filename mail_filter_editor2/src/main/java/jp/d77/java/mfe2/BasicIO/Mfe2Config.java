package jp.d77.java.mfe2.BasicIO;

import java.nio.file.FileSystems;

import jp.d77.java.mfe2.Mfe2Application;
import jp.d77.java.tools.BasicIO.BasicConfig;
import jp.d77.java.tools.HtmlIO.HtmlString;

public class Mfe2Config extends BasicConfig {

    public HtmlString addHeader = HtmlString.init();
    public HtmlString alertBottomInfo = HtmlString.init();
    public HtmlString alertDebug = HtmlString.init();
    public HtmlString alertInfo = HtmlString.init();
    public HtmlString alertError = HtmlString.init();

    public Mfe2Config(){
        super();
        ToolRDAPServer.loadServers( this.getDataFilePath() );
    }

    @Override
    public boolean load(){
        super.setFile( this.getDataFilePath() + "/config.json" );
        return super.load();
    }

    @Override
    public boolean save(){
        super.setFile( this.getDataFilePath() + "/config.json" );
        return super.save();
    }

    
    //******************************************************************************
    // プロパティ
    //******************************************************************************
    public String getDataFilePath(){
        if ( Mfe2Application.getFilePath().isEmpty() ){
            return FileSystems.getDefault().getPath("").toAbsolutePath().toString() + "/../mfe_data/";
        }
        return Mfe2Application.getFilePath().get();
    }

    public String getLogFilePath(){
        if ( Mfe2Application.getFilePath().isEmpty() ){
            return FileSystems.getDefault().getPath("").toAbsolutePath().toString() + "/../logs/";
        }
        return Mfe2Application.getFilePath().get();
    }
}
