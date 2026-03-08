package jp.d77.java.mfe2.Datas;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.LogAnalyser.LogPatterns;
import jp.d77.java.tools.BasicIO.Debugger;

public class SessionLogDatas {
    // ID counter
    private static int m_idx_count = -1;
    // raw log
    public record LogBasicData(Integer id, LocalDateTime logTime, String program, Integer pid, String message) {}

    // 生ログ(session logの内容そのまま)一時保管用
    private List<String>   m_tempdata;

    private class LogData{
        private int m_idx;
        private Map<String,Set<String>> m_props_string;
        private Map<String,Set<Integer>> m_props_int;
        private LocalDateTime m_start;
        private LocalDateTime m_end;
        private List<LogBasicData> m_log;

        public LogData(){
            SessionLogDatas.m_idx_count += 1;
            this.m_idx = SessionLogDatas.m_idx_count;
            this.m_props_string = new HashMap<String,Set<String>>();
            this.m_props_int = new HashMap<String,Set<Integer>>();
            this.m_log = new ArrayList<LogBasicData>();
        }

        public LogData( int id ){
            if ( SessionLogDatas.m_idx_count < id ) SessionLogDatas.m_idx_count = id;
            this.m_idx = id;
            this.m_props_string = new HashMap<String,Set<String>>();
            this.m_props_int = new HashMap<String,Set<Integer>>();
            this.m_log = new ArrayList<LogBasicData>();
        }

        /**
         * add propaty(String)
         * @param key
         * @param value
         */
        private void addProp( String key, String value ){
            if ( ! this.m_props_string.containsKey(key) ) this.m_props_string.put( key, new HashSet<String>() );
            this.m_props_string.get( key ).add( value );
        }

        /**
         * add propaty(Integer)
         * @param key
         * @param value
         */
        private void addProp( String key, Integer value ){
            if ( ! this.m_props_int.containsKey(key) ) this.m_props_int.put( key, new HashSet<Integer>() );
            this.m_props_int.get( key ).add( value );
        }

        public void addLog( LogBasicData lb ){  this.m_log.add( lb );   }

        /**
         * set time
         * @param time
         */
        private void setTime(LocalDateTime time) {
            if (time == null) return;
            if (m_start == null || time.isBefore(m_start)) m_start = time;
            if (m_end == null || time.isAfter(m_end)) m_end = time;
        }

        public int getIdx() { return this.m_idx; }

        public LocalDateTime getStart() {   return m_start; }
        public LocalDateTime getEnd() {     return m_end;   }

        public String[]getPropKeyS(){
            return this.m_props_string.keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);
        }

        public String[]getPropKeyI(){
            return this.m_props_int.keySet()
                .stream()
                .sorted()
                .toArray(String[]::new);
        }
 
        public String[] getPropS( String key ){
            if ( ! this.m_props_string.containsKey(key) ) return new String[0];
            return this.m_props_string.get(key).toArray( new String[0] );
        }

        public Integer[] getPropI( String key ){
            if ( ! this.m_props_int.containsKey(key) ) return new Integer[0];
            return this.m_props_int.get(key).toArray( new Integer[0] );
        }

        public LogBasicData[] getLog(){  return this.m_log.toArray( new LogBasicData [0] ); }

    }

    // 加工済みSessionLog
    private Map<Integer,LogData>   m_logdatas = new HashMap<Integer,LogData>();
    private LocalDate m_targetDate;

    /**
     * コンストラクタ
     */
    public SessionLogDatas( LocalDate targetDate ){
        SessionLogDatas.m_idx_count = -1;
        this.m_tempdata = new ArrayList<String>();
        this.m_targetDate = targetDate;
    }

    /**
     * MailLog、あるいはSessionLogからLogBasicDataデータを作成する
     * @param id
     * @param targetDate
     * @param line
     * @return
     */
    public Optional<LogBasicData> setLogBasic( int id, String line ){
        String sYear = this.m_targetDate.getYear() + "";
        Matcher m = LogPatterns.PTN_LOGBASIC.matcher(line);
        if (!m.matches()) return Optional.empty();

        LocalDateTime logTime;
        String program;
        Integer pid;
        String message;

        // 日時
        String ts = (sYear + " " + m.group(1)) .replaceAll("\\s+", " ") .trim();
        logTime = LocalDateTime.parse(ts, LogPatterns.FMT_LOG_DATETIME);
        if ( logTime.getMonthValue() == 1 && this.m_targetDate.getMonthValue() == 12 ){
            // targetDateが12月で、Logが1月の場合は、ログを翌年とみなす。
            logTime = LocalDateTime.parse(ts, LogPatterns.FMT_LOG_DATETIME).plusMonths(1);
        }

        // ③ プログラム名
        program = m.group(2);

        // ③ PID（無い場合 null）
        pid = (m.group(3) != null)
                ? Integer.valueOf(m.group(3))
                : null;

        // ④ メッセージ
        message = m.group(4);

        return Optional.ofNullable( new LogBasicData(id, logTime, program, pid, message) );
    }

    public LocalDate getTargetDate(){ return this.m_targetDate; }
    /**
     * 新しいログを1件作成(idは新規採番)
     * @return id
     */
    public int newLogData(){
        LogData ld = new LogData();
        this.m_logdatas.put( ld.m_idx, ld );
        return ld.getIdx();
    }

    /**
     * 新しいログを1件作成(idは指定)
     * @return
     */
    public void newLogData( int id ){
        LogData ld;
        if ( this.getLogData( id ).isEmpty() ){
            ld = new LogData( id );
            this.m_logdatas.put( ld.m_idx, ld );
        }
        return;
    }

    public void addLog( int id, LogBasicData lb ){
        if ( ! this.m_logdatas.containsKey( id ) ) return;
        this.m_logdatas.get( id ).addLog(lb);
    }

    /**
     * set propaty & indexed
     * @param id
     * @param key
     * @param value
     */
    public void addProp( LocalDateTime time, int id, String key, String value ){
        if ( ! this.m_logdatas.containsKey( id ) ) return;
        this.m_logdatas.get( id ).addProp(key, value);
        this.m_logdatas.get( id ).setTime(time);
//        this.addIndex( key, value, this.m_logdatas.get( id ) );
    }

    /**
     * set propaty & indexed
     * @param id
     * @param key
     * @param value
     */
    public void addProp( LocalDateTime time, int id, String key, Integer value ){
        if ( ! this.m_logdatas.containsKey( id ) ) return;
        this.m_logdatas.get( id ).addProp(key, value);
        this.m_logdatas.get( id ).setTime(time);
//        this.addIndex( key, value, this.m_logdatas.get( id ) );
    }
/*
    private void addIndex( String key, String val, LogData ld ){
        if ( !this.m_string_idx.containsKey(key) ) this.m_string_idx.put(key, new HashMap<String,LogData>() );

        if ( this.m_string_idx.get(key).containsKey( val ) ){
            if ( ! this.m_string_idx.get(key).get( val ).equals( ld ) ){
                Debugger.WarnPrint( "Duplicat index key=" + key + " val=" + val );
            }
        }
        this.m_string_idx.get(key).put( val, ld );
    }

    private void addIndex( String key, Integer val, LogData ld ){
        if ( !this.m_int_idx.containsKey(key) ) this.m_int_idx.put(key, new HashMap<Integer,LogData>() );
        if ( this.m_int_idx.get(key).containsKey( val ) ){
            if ( ! this.m_int_idx.get(key).get( val ).equals( ld ) ){
                Debugger.WarnPrint( "Duplicat index key=" + key + " val=" + val );
            }
        }
        this.m_int_idx.get(key).put( val, ld );
    }
*/
    public Optional<Integer> searchProp( String key, String value ){
        for ( int id: this.getIdLists() ){
            for ( String prop_key: this.getPropKeyS( id ) ){
                for ( String prop_val: this.getPropS( id, prop_key ) ){
                    if ( prop_val.equals( value ) ) return Optional.ofNullable( id );
                }
            }
        }
        return Optional.empty();
    }

    public String[] searchProps( String key ){
        Map<String,Boolean> ret = new HashMap<>();

        for ( int id: this.getIdLists() ){
            for ( String prop_val: this.getPropS( id, key ) ){
                ret.put( prop_val, true );
            }
        }
        return ret.keySet().toArray( new String[0] );
    }

    public Integer[] searchProps( String key, String val ){
        //Map<String,Boolean> ret = new HashMap<>();
        List<Integer> ret = new ArrayList<>();

        for ( int id: this.getIdLists() ){
            for ( String prop_val: this.getPropS( id, key ) ){
                if ( prop_val.equals( val ) )
                ret.add( id );
            }
        }
        return ret.toArray( new Integer[0] );
    }

    public boolean containsId( Integer id ){
        if ( this.getLogData(id).isEmpty() ) return false;
        return true;
    }

    /**
     * キーの値に指定の文字が含まれるか
     * @param id
     * @param key
     * @param value
     * @return
     */
    public boolean containsValue( Integer id, String key, String value ){
        for ( String s: this.getPropS( id, key) ){
            if ( s.contains( value ) ) return true;
        }
        return false;
    }

    /**
     * インデックスのキー一覧を取得(Integer)
     * @return
     */
    public Integer[] getIdLists(){
        return this.m_logdatas.keySet().stream()
            .sorted()
            .toArray(Integer[]::new);
    }

    /**
     * 指定のIDのキー一覧を取得
     * @param id
     * @return
     */
    public String[] getPropKeyS( Integer id ){
        if ( this.getLogData(id).isEmpty() ) return new String[0];
        return this.getLogData(id).get().getPropKeyS();
    }

    /**
     * 指定のIDのキー一覧を取得
     * @param id
     * @return
     */
    public String[] getPropKeyI( Integer id ){
        if ( this.getLogData(id).isEmpty() ) return new String[0];
        return this.getLogData(id).get().getPropKeyI();
    }

    public String[] getPropS( Integer id, String key ){
        if ( this.getLogData(id).isEmpty() ) return new String[0];
        return this.getLogData(id).get().getPropS(key);
    }

    public Integer[] getPropI( Integer id, String key ){
        if ( this.getLogData(id).isEmpty() ) return new Integer[0];
        return this.getLogData(id).get().getPropI(key);
    }

    /**
     * 指定のログデータを取得(ローカル用)
     * @param id
     * @return
     */
    private Optional<LogData>getLogData( Integer id ){
        if ( ! this.m_logdatas.containsKey(id) ) Optional.empty();
        return Optional.ofNullable( this.m_logdatas.get(id) );
    }

    public LogBasicData[] getLog( Integer id ){
        if ( this.getLogData(id).isEmpty() ) return new LogBasicData[0];
        return this.getLogData(id).get().getLog();
    }

    /**
     * 一時データをすべて取得
     * @return
     */
    public List<String> getTempData(){ return this.m_tempdata; }

    /**
     * ログの最初の日時を取得
     * @param id
     * @return
     */
    public Optional<LocalDateTime> getStart( int id ){
        LogData ld = this.getLogData( id ).orElse(null);
        if ( ld == null ) return Optional.empty();
        return Optional.ofNullable( ld.getStart() );
    }

    /**
     * ログの最後の日時を取得
     * @param id
     * @return
     */
    public Optional<LocalDateTime> getEnd( int id ){
        LogData ld = this.getLogData( id ).orElse(null);
        if ( ld == null ) return Optional.empty();
        return Optional.ofNullable( ld.getEnd() );
    }

    /**
     * セッションログの保存
     * @param cfg
     * @param targetDate
     */
    public void save( Mfe2Config cfg ){
        DateTimeFormatter ymFmt  = DateTimeFormatter.ofPattern("yyyyMM");
        DateTimeFormatter ymdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        // logDir/yyyyMM
        Path baseDir = Paths.get( cfg.getDataFilePath() + "session_logs/");
        Path logDir = baseDir.resolve( this.m_targetDate.format(ymFmt));

        // 上書き保存
        try {
            Files.createDirectories(logDir);
            // log_yyyyMMdd.txt
            Path tmpFile = logDir.resolve( "log_" + this.m_targetDate.format(ymdFmt) + ".tmp" );
            Path logFile = logDir.resolve( "log_" + this.m_targetDate.format(ymdFmt) + ".txt" );

            Files.write(
                tmpFile,
                List.of( this.m_tempdata.toArray( new String[0] ) ),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            if (Files.exists(tmpFile)) Files.copy( tmpFile, logFile, StandardCopyOption.REPLACE_EXISTING);
            if (Files.exists(tmpFile)) Files.delete( tmpFile );

            Debugger.InfoPrint( "saved file=" + logFile + " size=" + this.m_tempdata.size() );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * セッションログの読み込み
     * @param cfg
     * @param targetDate
     * @return
     */
    public boolean load( Mfe2Config cfg ){
        this.m_tempdata.clear();

        DateTimeFormatter ymFmt  = DateTimeFormatter.ofPattern("yyyyMM");
        DateTimeFormatter ymdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        // logDir/yyyyMM
        Path baseDir = Paths.get( cfg.getDataFilePath() + "session_logs/");
        Path logDir = baseDir.resolve( this.m_targetDate.format(ymFmt));
        Path logFile = logDir.resolve( "log_" + this.m_targetDate.format(ymdFmt) + ".txt" );

        try ( var lines = Files.lines( logFile, StandardCharsets.UTF_8) ) {
            lines.forEach( this.m_tempdata::add );
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
        Debugger.InfoPrint( "loaded file=" + logFile + " lines=" + this.m_tempdata.size() );
        return true;
    }

    /**
     * ログから得られた注目すべき問題をリスト化
     * @param id
     * @return
     */
    public String[] getResult( int id ){
        List<String> res = new ArrayList<>();
        res.addAll( Arrays.asList( this.getPropS( id, "relay_status" ) ) );
        res.addAll( Arrays.asList( this.getPropS( id, "error" ) ) );
        if ( res.size() <= 0 ){
            res.add( "connect only" );
        }
        return res.toArray( new String[0] );
    }

    /**
     * 結果的に送受信できたのか?
     * @param id
     * @return
     */
    public boolean isError( int id ){
        // Result check
        if ( this.getLog(id).length >= 2
            && this.getPropS( id, "error" ).length <= 0
            && this.getPropS( id, "relay_status" ).length <= 0
            ){
            // ログが2行しかなく、何もステータスが無い場合はエラー
            return true;
        }
        if (
            ! this.containsValue( id, "relay_status", "send null")
            && ! this.containsValue( id, "relay_status", "send local")
            && ! this.containsValue( id, "relay_status", "send remote")
            ){
            // send云々が無ければエラー(それ以外のメッセージはワーニング扱い)
            return true;
        }        
        return false;
    }
}
