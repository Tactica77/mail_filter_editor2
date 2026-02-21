package jp.d77.java.mfe2.LogAnalyser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolRDAP;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.StorableConfig;

public class RDAPCache extends StorableConfig{
    private Mfe2Config m_cfg;
    private int m_rdap_cnt;
    private boolean m_update;
    private LocalDate m_targetDate = null;
    private Map<String,Map<String,Integer>> m_rdap_summary;

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
        this.m_rdap_cnt = 0;
        this.m_update = false;
        this.m_rdap_summary = new HashMap<>();
        this.m_rdap_summary.put("cached", new HashMap<>() );
        this.m_rdap_summary.put("rdap_get", new HashMap<>() );
        this.m_rdap_summary.put("skip", new HashMap<>() );
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
        if ( ! this.m_update ) return false;
        super.setFile( this.getFilename( this.m_targetDate ) );
        this.outputInfo();
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

    public Optional<RdapResult> getRDAPcidr( String ip ){
        if ( this.get("ip_" + ip).isPresent() ) {
            //this.m_cfg.addAlertBottomInfo( "reuse ip=" + ip );
            this.countSummary( "cached", ip );
            return Optional.ofNullable( new RdapResult( this.get("ip_" + ip).get() ) );
        }

        this.m_rdap_cnt ++;
        if ( this.m_rdap_cnt > 10 ) {
            this.countSummary( "skip", ip );
            return Optional.empty();
        }

        RdapResult res = new RdapResult(
            ToolRDAP.getParam( ip, "cidr_0" ).orElse(null)
            ,ToolRDAP.getParam( ip, "country" ).orElse(null)
            ,ToolRDAP.getParam( ip, "name" ).orElse(null)
        );

        if ( res.cidr != null ) {
            //this.m_cfg.addAlertBottomInfo( "get rdap ip=" + ip );
            this.countSummary( "rdap_get", ip );
            this.overwrite( "ip_" + ip, res.join() );
            this.m_update = true;
        }else{
            res = null;
        }
        return Optional.ofNullable( res );
    }

    private void countSummary( String type, String ip ){
        if ( this.m_rdap_summary.get( type ).containsKey(ip) ){
            int i = this.m_rdap_summary.get( type ).get(ip) + 1;
            this.m_rdap_summary.get( type ).put(ip,i);
        }else{
            this.m_rdap_summary.get( type ).put(ip,1);
        }
    }

    private void outputInfo(){
        List<String> out = new ArrayList<>();
        String[] keys = { "cached", "rdap_get", "skip" };
        for ( String key: keys ){
            out.clear();
            for ( String s: this.m_rdap_summary.get( key ).keySet() ){
                out.add( s + "=" + this.m_rdap_summary.get( key ).get( s ) );
            }
            this.m_cfg.addAlertBottomInfo( key + "..." + String.join( " / ", out ) );
        }
    }
}
