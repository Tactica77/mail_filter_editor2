package jp.d77.java.mfe2.Datas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.BasicIO.ToolNet;
import jp.d77.java.mfe2.BasicIO.ToolNet.Cidr;
import jp.d77.java.mfe2.BasicIO.ToolNet.RangeResult;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolDate;

public class FilterDatas {
    public class IpFilter {
        public Boolean m_enable = false;
        public Long m_startIp = 0L;
        public Long m_endIp = 0L;
        public String m_cidr = "";
        public String m_type = "";

        public IpFilter( String Cidr, String type ){
            Cidr cidr = ToolNet.CidrSplit( Cidr ).orElse(null);
            if ( cidr == null ) return;
            RangeResult range = ToolNet.cidr2range(cidr).orElse(null);
            if ( range == null ) return;
            this.m_enable = true;
            this.m_startIp = ToolNet.ip2Long( range.start_ip() );
            this.m_endIp = ToolNet.ip2Long( range.end_ip() );
            this.m_cidr = Cidr;
            this.m_type = type;
        }

        public boolean isEnable(){ return this.m_enable; }
        
        public String getCidr(){
            return this.m_cidr;
        }

        public IpFilter( IpFilter ipf ) {
            this.m_enable = ipf.m_enable;
            this.m_startIp = ipf.m_startIp;
            this.m_endIp = ipf.m_endIp;
            this.m_cidr = ipf.m_cidr;
            this.m_type = ipf.m_type;
        }
    }
    private List<IpFilter>  m_ipfilter = new ArrayList<>();

    /**
     * 
     * @param cidr_string
     * @param type
     * @return true = 追加した
     */
    public boolean add( String cidr_string, String type ){
        List<IpFilter> removes = new ArrayList<>();

        // IpFilterへ変換
        IpFilter ipf = new IpFilter( cidr_string, type );
        if ( ! ipf.isEnable() ) return false;

        // 登録済みに含まれるか確認
        for ( IpFilter chk_ipf: this.m_ipfilter ){
            if ( this.isWithin( chk_ipf, ipf ) ) {
                //Debugger.WarnPrint( "not add " + ipf.getCidr() + " Included " + chk_ipf.getCidr() );
                return false;
            }
            if ( this.isWithin( ipf, chk_ipf ) ) {
                // 追加するものの方が大きい
                removes.add( chk_ipf );
            }
        }

        for ( IpFilter chk_ipf: removes ){
            //Debugger.WarnPrint( "remove " + chk_ipf.getCidr() + " Included " + ipf.getCidr() );
            this.m_ipfilter.remove( chk_ipf );
        }

        this.m_ipfilter.add( ipf );
        return true;
    }

    public Optional<IpFilter> getFilter( String cidr ){
        IpFilter ipf = new IpFilter(cidr, "check");

        for ( IpFilter chk_ipf: this.m_ipfilter ){
            if ( this.isWithin( chk_ipf, ipf) ){
                return Optional.ofNullable( chk_ipf );
            }
        }
        return Optional.empty();
    }

    /**
     * レンジ範囲が、ipf_a >= ipf_bかを返す
     * 上記以外の他、両方が被らない範囲があるならfalseを返す
     * @param ipf_a
     * @param ipf_b
     * @return
     */
    protected boolean isWithin( IpFilter ipf_a, IpFilter ipf_b ){
        if ( ! ipf_a.isEnable() ) return false;
        if ( ! ipf_b.isEnable() ) return false;
        
        if ( ipf_a.m_startIp <= ipf_b.m_startIp && ipf_a.m_endIp >= ipf_b.m_endIp ) return true;
        return false;
    }

    public boolean loadCountryFilter( String filename, boolean use_cache ) {
        Debugger.TracePrint();
        this.m_ipfilter.clear();
        int lc = 0;
        int vc = 0;
        String now_date = ToolDate.Format( LocalDate.now(), "uuuuMMdd").orElse("");

        if ( use_cache ) {
            if ( ToolAny.country_filter_lines_date.isEmpty() ){
                // load
            }else if( now_date.equals( ToolAny.country_filter_lines_date ) ){
                // cache
                this.loadbackCountryFilterCache();
                Debugger.addHistory("Country Filter use cache");
                return true;
            }
        }

        List<String> country_filter_lines;

        try {
            country_filter_lines = Files.readAllLines(Path.of(filename));
        }catch ( IOException e ) {
            Debugger.ErrorPrint( "file=" + filename + " e=" + e.getMessage() );
            return false;
        }

        for (String rawLine : country_filter_lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;   // 空
            lc += 1;
            if ( ! ToolNet.isIP( line ) ) continue; // IPではない
            if ( ToolNet.isPrivateIp( line ) ) continue; // プライベートIP

            if ( this.add( line, "country_filter" ) ) vc += 1;
        }

        // save cache
        if ( use_cache ) {
            this.createCountryFilterCache();
            ToolAny.country_filter_lines_date = now_date;
        }
        Debugger.InfoPrint( "Loaded file=" + filename + "  data line=" + lc + " valid lie=" + vc );
        Debugger.addHistory("Country Filter load data line=" + lc + " valid lie=" + vc);
        return true;
    }    

    public void createCountryFilterCache(){
        ToolAny.country_filter_cache = new ArrayList<>();
        for ( IpFilter ipf: this.m_ipfilter ){
            ToolAny.country_filter_cache.add( new IpFilter( ipf ) );
        }
    }

    public void loadbackCountryFilterCache(){
        this.m_ipfilter = new ArrayList<>();
        for ( IpFilter ipf: ToolAny.country_filter_cache ){
            this.m_ipfilter.add( new IpFilter( ipf ) );
        }
    }
}
