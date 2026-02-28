package jp.d77.java.mfe2.Datas;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.ToolAny.arrayCounter;
import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolRDAP;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.StorableConfig;

public class RDAPCache extends StorableConfig{
    private Mfe2Config m_cfg;
    private boolean m_update;
    private boolean m_rdap_get;
    private LocalDate m_targetDate = null;
    private arrayCounter m_rdap_summary_chached = new arrayCounter();
    private arrayCounter m_rdap_summary_get = new arrayCounter();
    private arrayCounter m_rdap_summary_skip = new arrayCounter();
    public record RdapResult( String cidr, String cc, String org ) {
        public RdapResult(String join_str) {
            this(
                split(join_str, 0),
                split(join_str, 1),
                split(join_str, 2)
            );
        }

        private static String split(String s, int index) {
            String[] raw = s.split("<<>>", -1);
            return raw.length > index ? raw[index] : null;
        }

        public String join(){
            return Optional.ofNullable( this.cidr ).orElse("?")
                + "<<>>" + Optional.ofNullable( this.cc ).orElse("?")
                + "<<>>" + Optional.ofNullable( this.org ).orElse("?");
        }
    }

    public RDAPCache( Mfe2Config cfg ){
        super( null );
        this.m_cfg = cfg;
        this.m_update = false;
        this.m_rdap_get = true;
        
    }

    /**
     * RDAPデータをインターネットから取得するかのフラグ
     * @param rdap_get false=取得しない
     */
    public void rdap_get_flag( boolean rdap_get ){
        this.m_rdap_get = rdap_get;
    }

    public boolean load( LocalDate targetDate ){
        this.m_targetDate = targetDate;
        super.setFile( this.getFilename( this.m_targetDate ) );
        return super.load();
    }

    @Override
    public boolean save(){
        Debugger.TracePrint();
        if ( this.m_targetDate == null ) return false;
        this.outputInfo();
        if ( ! this.m_update ) return false;
        super.setFile( this.getFilename( this.m_targetDate ) );
        return super.save();
    }
    
    private String getFilename( LocalDate targetDate ){
        DateTimeFormatter ymFmt  = DateTimeFormatter.ofPattern("yyyyMM");
        DateTimeFormatter ymdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        Path baseDir = Paths.get( this.m_cfg.getDataFilePath() + "session_logs/");
        Path logDir = baseDir.resolve(targetDate.format(ymFmt));
        Path logFile = logDir.resolve( "rdap_" + targetDate.format(ymdFmt) + ".txt" );

        return logFile.toString();
    }

    public Optional<RdapResult> getRDAP( String ip ){
        if ( this.get("ip_" + ip).isPresent() ) {
            // キャッシュから取得
            if ( this.m_rdap_get == true ) this.m_rdap_summary_chached.add( ip);
            return Optional.ofNullable( new RdapResult( this.get("ip_" + ip).get() ) );
        }

        if ( this.m_rdap_get == false ) {
            // RDAPへリクエストしない
            this.m_rdap_summary_skip.add( ip);
            return Optional.empty();
        }

        // RDAPへリクエスト
        RdapResult res = new RdapResult(
            ToolRDAP.getParam( ip, "cidr_0" ).orElse(null)
            ,ToolRDAP.getParam( ip, "country" ).orElse(null)
            ,ToolRDAP.getParam( ip, "name" ).orElse(null)
        );

        if ( res.cidr != null ) {
            this.m_rdap_summary_get.add( ip);
            this.overwrite( "ip_" + ip, res.join() );
            this.m_update = true;
        }else{
            this.m_rdap_summary_skip.add( ip);
            res = null;
        }
        return Optional.ofNullable( res );
    }

    private void outputInfo(){
        List<String> out = new ArrayList<>();

        out.clear();
        for ( String ip: this.m_rdap_summary_chached.keys() ){
            out.add( ip + "=" + this.m_rdap_summary_chached.get( ip ) );
        }
        this.m_cfg.addAlertBottomInfo( "cached..." + String.join( " / ", out ) );
        this.m_rdap_summary_chached.clear();

        out.clear();
        for ( String ip: this.m_rdap_summary_get.keys() ){
            out.add( ip + "=" + this.m_rdap_summary_get.get( ip ) );
        }
        this.m_cfg.addAlertBottomInfo( "rdap get..." + String.join( " / ", out ) );
        this.m_rdap_summary_get.clear();

        out.clear();
        for ( String ip: this.m_rdap_summary_skip.keys() ){
            out.add( ip + "=" + this.m_rdap_summary_skip.get( ip ) );
        }
        this.m_cfg.addAlertBottomInfo( "skip..." + String.join( " / ", out ) );
        this.m_rdap_summary_skip.clear();
    }
}
