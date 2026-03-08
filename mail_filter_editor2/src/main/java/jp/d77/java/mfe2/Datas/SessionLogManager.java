package jp.d77.java.mfe2.Datas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.LogAnalyser.SessionLogAnalyse;
import jp.d77.java.mfe2.LogAnalyser.SessionLogNumbering;
import jp.d77.java.tools.BasicIO.ToolDate;

public class SessionLogManager {
    private Mfe2Config m_cfg;
    private Map< String, SessionLogDatas> m_sessionLog;
    
    public SessionLogManager( Mfe2Config cfg ){
        this.m_cfg = cfg;
        this.m_sessionLog = new HashMap<>();
    }

    /**
     * LocalDateを文字列へ変換
     * @param targetDate
     * @return
     */
    private Optional<String> date2str( LocalDate targetDate ){
        if ( targetDate.isAfter( LocalDate.now() ) ) return Optional.empty();
        String d = ToolDate.Fromat( targetDate, "uuuuMMdd" ).orElse(null);
        if ( d == null ) return Optional.empty();
        return Optional.ofNullable( d );
    }

    /**
     * SessionLogファイルを作成
     * @param targetDate
     */
    public void create( LocalDate targetDate ){
        Optional<String> d = this.date2str(targetDate);
        if ( d.isEmpty() ) return;

        SessionLogDatas sd = new SessionLogDatas( targetDate );
        SessionLogNumbering slogUpdate = new SessionLogNumbering( this.m_cfg, sd );
        slogUpdate.CreateSessionLogs();
        sd.save( this.m_cfg );
    }

    /**
     * SessionLogを読み込み(無ければ作成)
     * @param targetDate
     */
    public void load( LocalDate targetDate, Integer targetId ){
        Optional<String> d = this.date2str(targetDate);
        if ( d.isEmpty() ) return;

        SessionLogDatas sd = new SessionLogDatas( targetDate );
        if ( ! sd.load( this.m_cfg ) ){
            // SessionLogが作成されていないので作成
            this.create(targetDate);
            sd = new SessionLogDatas( targetDate );
        }

        // ログを分析
        SessionLogAnalyse  sLogDetail = new SessionLogAnalyse( sd );
        sLogDetail.analyseLog( this.m_cfg, targetId );

        this.m_sessionLog.put( d.get(), sd );
    }

    /**
     * SessionLogDatasを取得
     * @param targetDate
     * @return
     */
    public Optional<SessionLogDatas> getData( LocalDate targetDate ){
        Optional<String> d = this.date2str(targetDate);
        if ( d.isEmpty() ) return Optional.empty();
        return this.getData( d.get() );
    }

    /**
     * SessionLogDatasを取得
     * @param targetDate
     * @return
     */
    public Optional<SessionLogDatas> getData( String targetDate ){
        if ( ! this.m_sessionLog.containsKey( targetDate ) ) return Optional.empty();
        return Optional.ofNullable( this.m_sessionLog.get( targetDate ) );
    }

    @Deprecated
    public String[] keys(){
        return this.m_sessionLog.keySet().stream()
            .sorted()
            .toArray(String[]::new);
    }

    public LocalDate[] getDates(){
        List<LocalDate> dates = new ArrayList<>();
        for ( String day: this.m_sessionLog.keySet().stream().sorted().toList() ){
            if ( ToolDate.YMD2LocalDate(day).isPresent() ) dates.add( ToolDate.YMD2LocalDate(day).get() );
        }
        return dates.toArray( new LocalDate[0] );
    }

    public LocalDate[] getDatesReverce(){
        List<LocalDate> dates = new ArrayList<>();
        for ( String day: this.m_sessionLog.keySet().stream().sorted( Comparator.reverseOrder() ).toList() ){
            if ( ToolDate.YMD2LocalDate(day).isPresent() ) dates.add( ToolDate.YMD2LocalDate(day).get() );
        }
        return dates.toArray( new LocalDate[0] );
    }
}
