package jp.d77.java.mfe2.BasicIO;

import java.nio.file.FileSystems;

import jp.d77.java.mfe2.Mfe2Application;
import jp.d77.java.tools.HtmlIO.WebConfig;

public class Mfe2Config extends WebConfig {
    public Mfe2Config( String uri ){
        super( uri );
        ToolRDAPServer.loadServers( this.getDataFilePath() );
    }
    
    //******************************************************************************
    // プロパティ
    //******************************************************************************
    public String getDataFilePath(){
        if ( Mfe2Application.getFilePath().isEmpty() ){
            return FileSystems.getDefault().getPath("").toAbsolutePath().toString() + "/../mfe_data/";
        }else{
            return Mfe2Application.getFilePath().get();
        }
    }

    public String getLogFilePath(){
        if ( Mfe2Application.getFilePath().isEmpty() ){
            return FileSystems.getDefault().getPath("").toAbsolutePath().toString() + "/../logs/";
        }else{
            return Mfe2Application.getFilePath().get() + "/../logs/";
        }
    }
}
