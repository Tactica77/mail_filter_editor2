package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.BasicIO.ToolNet;
import jp.d77.java.mfe2.Datas.BlockDatas;
import jp.d77.java.mfe2.Datas.IpTablesDatas;
import jp.d77.java.mfe2.Datas.RDAPCache;
import jp.d77.java.mfe2.Datas.RDAPCache.RdapResult;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolArrays.arrayCounter;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.BasicIO.ToolNums;
import jp.d77.java.mfe2.Datas.SessionLogDatas;
import jp.d77.java.mfe2.Datas.SessionLogManager;
import jp.d77.java.mfe2.Datas.SpotBlockDatas;
import jp.d77.java.mfe2.Datas.SpotBlockDatas.SpotBlockData;

public class CliUpdateFilter {

    private class IpRank {
        private int             m_cnt;
        private float           m_score;
        private String          m_cc;
        private String          m_cidr;
        private String          m_org;
        private List<String>    m_histry;
        public IpRank() {
            this.m_cnt = 0;
            this.m_score = 0;
            this.m_cc = "??";
            this.m_cidr = "??";
            this.m_org = "??";
            this.m_histry = new ArrayList<>();
        }
        public void newCount(){ this.m_cnt += 1; }
        public void add( float c, Optional<LocalDateTime> d, String s ){
            this.m_score += c;
            this.m_histry.add( ToolDate.Format(d.orElse(null), "MM-dd hh:mm").orElse("??:??") + " (" + ToolNums.Float2Str( c ) + ")" + s);
        }
        public void setCc( String cc ){
            if ( cc == null ) this.m_cc = "??";
            else this.m_cc = cc;
        }
        public void setCidr( String cidr ){
            if ( cidr == null ) this.m_cidr = "??";
            else this.m_cidr = cidr;
        }
        public void setOrg( String org ){
            if ( org == null ) this.m_org = "??";
            else this.m_org = org;
        }
        public float getScore(){ 
            if ( this.m_cc.toLowerCase().equals( "jp" ) ) return this.m_score / 4;
            return this.m_score;
        }
        public int getCount(){ return this.m_cnt; }
        public String getCc(){ return this.m_cc; }
        public String getCidr(){ return this.m_cidr; }
        public String getOrg(){ return this.m_org; }
        public String[] getHistory(){ return this.m_histry.toArray( new String[0] ); }
    }

    private Mfe2Config      m_cfg;
    //private FilterDatas     m_filter;
    private IpTablesDatas     m_iptdata;
    private SessionLogManager   m_slog;
    private Map<String, IpRank> m_rank;
    private arrayCounter    m_data_cnt_cidrs;  // CIDRカウンタ
    private arrayCounter    m_data_cnt_org;    // ORGカウンタ
    private List<String>    m_proc_histry;
    private Long            m_proc_histry_start;
    private int             m_block_score = 5;

    public void ProcHistory( boolean start, String msg ){
        if ( ! start ) {
            this.m_proc_histry.add( "[" + ( Debugger.elapsedTimer() - this.m_proc_histry_start ) + "ms] " + msg  );
        }
        this.m_proc_histry_start = Debugger.elapsedTimer();
    }
    public CliUpdateFilter( Mfe2Config cfg ){
        Debugger.TracePrint();
        this.m_cfg = cfg;
        this.m_slog = new SessionLogManager( cfg );
        this.m_rank = new HashMap<>();
        this.m_data_cnt_cidrs = new arrayCounter();
        this.m_data_cnt_org = new arrayCounter();
        this.m_proc_histry = new ArrayList<>();

        // Load Filter
        this.ProcHistory( true, "Start");

        this.m_iptdata = new IpTablesDatas();
        // iptablesを読み込む ※一旦すべて削除ステータス
        this.m_iptdata.loadIptables(  this.m_cfg.getDataFilePath() + "/iptables_dump.txt"  );

        // country_filterを読み込む
        this.m_iptdata.loadCountryFilter( this.m_cfg.getDataFilePath() + "/country_filter.txt" );

        // black listを読み込む
        BlockDatas bd = new BlockDatas( this.m_iptdata, "black list" );
        bd.load( this.m_cfg.getDataFilePath() + "/block_list_black.txt" );
        this.ProcHistory( false, "Load block_list_black.txt"  );
    }

    /**
     * セッションログを読み込み、ipスコアを作成する
     * @param days
     */
    public void loadSessionLog( int days ) {
        Debugger.TracePrint();
        LocalDate targetDate = LocalDate.now();
        LocalDate startDate = targetDate.minusDays( days - 1);

        // SessionLogを読み込み
        for (LocalDate d = startDate; !d.isAfter( targetDate ); d = d.plusDays(1)) {
            this.m_slog.load( d, null );
            this.ProcHistory( false, "Load SessionLog: " + ToolDate.Format( d, "uuuu-MM-dd").orElse("???") );
        }

        RDAPCache cache = new RDAPCache( this.m_cfg );
        float waight = 1f;
        for ( LocalDate d: this.m_slog.getDatesReverce() ){
            cache.load( d );
            cache.server_get_flag( false );   // キャッシュのみから取得する(ネットから取得しない)
            this.ProcHistory( false, "Load RDAP cache: " + ToolDate.Format( d, "uuuu-MM-dd").orElse("???")  );

            SessionLogDatas sd = this.m_slog.getData( d ).orElse(null);
            for( int id: sd.getIdLists() ){
                if ( id == -999 ) continue;

                for ( String ip: sd.getPropS( id, "ip" ) ){
                    if ( ! ToolNet.isIP(ip) ) continue;
                    if ( ToolNet.isPrivateIp(ip) ) continue;
                    this.getRank(ip).newCount();

                    for ( String to: sd.getPropS(id, "to") ){
                        if ( to.startsWith( "aaa@d77.jp" ) ) this.getRank(ip).add( waight * 2, sd.getStart(id), "Horny word:" + to );
                        else if ( to.startsWith( "bbb@d77.jp" ) ) this.getRank(ip).add( waight * 2, sd.getStart(id), "Horny word:" + to );
                        else if ( to.startsWith( "hoge@d77.jp" ) ) this.getRank(ip).add( waight * 2, sd.getStart(id), "Horny word:" + to );
                        else if ( to.startsWith( "bdelta@d77.jp" ) ) this.getRank(ip).add( waight * 8, sd.getStart(id), "Horny word:" + to );
                        else if ( to.startsWith( "udelta@d77.jp" ) ) this.getRank(ip).add( waight * 8, sd.getStart(id), "Horny word:" + to );
                        else if ( to.startsWith( "xxx@d77.jp" ) ) this.getRank(ip).add( waight * 2, sd.getStart(id), "Horny word:" + to );
                        else if ( to.startsWith( "transfar-bbb@d77.jp" ) ) this.getRank(ip).add( waight * 8, sd.getStart(id), "Horny word:" + to );
                    }

                    for( String res: sd.getResult( id ) ){
                        if ( res.equals( "connect only" ) ) this.getRank(ip).add( waight / 2, sd.getStart(id), res );

                        else if ( res.equals( "send null" ) ) this.getRank(ip).add( waight * (-4), sd.getStart(id), res  );
                        else if ( res.equals( "send local" ) ) this.getRank(ip).add( waight * (-4), sd.getStart(id), res  );
                        else if ( res.equals( "send remote" ) ) this.getRank(ip).add( waight * (-4), sd.getStart(id), res  );

                        else if ( res.equals( "SPAM" ) ) this.getRank(ip).add( waight, sd.getStart(id), res  );
                        else if ( res.equals( "soft reject" ) ) this.getRank(ip).add( waight / 2, sd.getStart(id), res  );
                        else if ( res.equals( "timeout" ) ) this.getRank(ip).add( waight / 4, sd.getStart(id), res  );
                        else if ( res.equals( "improper-cmd" ) ) this.getRank(ip).add(waight * 4, sd.getStart(id), res );    // 異常コマンド
                        else if ( res.equals( "none-smtp" ) ) this.getRank(ip).add( waight * 2, sd.getStart(id), res  );

                        else if ( res.equals( "DNS not resolve" ) ) this.getRank(ip).add( waight / 4, sd.getStart(id), res  );
                        else if ( res.startsWith( "SSL_accept error" ) ) this.getRank(ip).add( waight / 4, sd.getStart(id), res  );
                        else if ( res.startsWith( "lost connection after AUTH" ) ) this.getRank(ip).add( waight, sd.getStart(id), res  );
                        else if ( res.startsWith( "lost connection after" ) ) this.getRank(ip).add( waight / 4, sd.getStart(id), res  );
                        else if ( res.startsWith( "450 " ) ) this.getRank(ip).add(waight, sd.getStart(id), res );    // ホスト名の偽装
                        else if ( res.startsWith( "504 " ) ) this.getRank(ip).add(waight, sd.getStart(id), res );    // 異常コマンド
                        else if ( res.startsWith( "550 " ) ) this.getRank(ip).add(waight, sd.getStart(id), res );    // 宛先(local)が無い
                        else if ( res.startsWith( "554 " ) ) this.getRank(ip).add(waight * 2, sd.getStart(id), res );    // メールアドレスのドメイン偽装
                    }

                    Optional<RdapResult> rdap = cache.getRDAP( ip );
                    if ( rdap.isEmpty() ) continue;
                    this.getRank(ip).setCc( rdap.get().cc());
                    this.getRank(ip).setCidr( rdap.get().cidr());
                    this.getRank(ip).setOrg( rdap.get().org());
                    this.m_data_cnt_cidrs.add( rdap.get().cidr() );
                    this.m_data_cnt_org.add( rdap.get().org() );
                }
            }
            this.ProcHistory( false, "Analyse Session Log: " + ToolDate.Format( d, "uuuu-MM-dd").orElse("???")  );
            waight /= 1.25;
        }

        for ( String ip: this.getIps( false ) ){
            String s;
            int c;
            float score = this.getRank(ip).getScore();

            if ( score > 0 ){
                s = this.getRank(ip).getCidr();
                c = this.m_data_cnt_cidrs.get(s);
                if ( c > 0 ) this.getRank(ip).add( c / 2, Optional.empty() , "CIDRS: " + s );

                s = this.getRank(ip).getOrg();
                c = this.m_data_cnt_org.get(s);
                if ( c > 0 ) this.getRank(ip).add( c / 8, Optional.empty() , "Organization: " + s );
            }
        }
        this.ProcHistory( false, "IP Summary" );
    }

    /**
     * スポットブロックデータと、IPTables差分データを出力する
     */
    public void createSpotBlockData(){
        // スポットブロックデータ作成
        SpotBlockDatas bds = new SpotBlockDatas( this.m_iptdata, "spot block" );
        for( String ip: this.getIps( true ) ){
            SpotBlockData bd = new SpotBlockData(
                 true
                 , ip
                 , LocalDate.now()
                 , this.getRank(ip).getCc()
                 , this.getRank(ip).getOrg()
                 , this.getRank(ip).getCount()
                 , this.getRank(ip).getScore()
            );
            bds.add( bd );
        }
        bds.save( this.m_cfg.getDataFilePath() + "/block_list_spot.txt" );
        this.ProcHistory( false, "create block_list_spot.txt"  );

        // iptables Deleteファイルを作成
        this.m_iptdata.saveBlockSetting( this.m_cfg.getDataFilePath() + "/iptables_deletes.txt",true );
        this.ProcHistory( false, "create iptables_deletes.txt"  );

        // iptables Addファイルを作成
        this.m_iptdata.saveBlockSetting( this.m_cfg.getDataFilePath() + "/iptables_adds.txt",false );
        this.ProcHistory( false, "create iptables_adds.txt"  );
    }

    /**
     * スコアリング対象IPを取得
     * @param blocked true=ブロックのみ取得
     * @return
     */
    public String[] getIps( boolean blocked ){
        if ( blocked ){
            // ブロックのみ
            List<String> ret = new ArrayList<>();
            for ( String ip: this.m_rank.keySet() ){
                if ( this.m_rank.get(ip).getScore() > this.m_block_score ) ret.add(ip);
            }
            return ret.toArray( new String[0] ) ;
        }else{
            // すべて
            return this.m_rank.keySet().stream().sorted().toList().toArray( new String[0] ) ;
        }
    }

    public float getCount( String ip ){
        if ( ! this.m_rank.containsKey(ip) ) return 0f;
        return this.m_rank.get(ip).getCount();
    }

    public float getScore( String ip ){
        if ( ! this.m_rank.containsKey(ip) ) return 0f;
        return this.m_rank.get(ip).getScore();
    }

    public String getCc( String ip ){
        if ( ! this.m_rank.containsKey(ip) ) return "";
        return this.m_rank.get(ip).getCc();
    }

    public String getCidr( String ip ){
        if ( ! this.m_rank.containsKey(ip) ) return "";
        return this.m_rank.get(ip).getCidr();
    }

    public String getOrg( String ip ){
        if ( ! this.m_rank.containsKey(ip) ) return "";
        return this.m_rank.get(ip).getOrg();
    }

    public String[] getHistry( String ip ){
        if ( ! this.m_rank.containsKey(ip) ) return new String[0];
        return this.m_rank.get(ip).getHistory();
    }

    public String[] getProcessHistory(){
        return this.m_proc_histry.toArray( new String[0] );
    }

    private IpRank getRank( String ip ){
        if ( ! this.m_rank.containsKey(ip) ) this.m_rank.put( ip, new IpRank() );
        return this.m_rank.get(ip);
    }
}
