package jp.d77.java.mfe2.LogAnalyser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.tools.BasicIO.Debugger;

public class SessionLogDatas {
    private class LogData{
        private int m_idx;
        private HashMap<String,Set<String>> m_props_string;
        private HashMap<String,Set<Integer>> m_props_int;
        private LocalDateTime m_start;
        private LocalDateTime m_end;
        private ArrayList<LogBasicData> m_log;

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
        /*

        
        public String getFormatedTime(){
            String ret = "";
            ret += ToolDate.Fromat( this.m_start, "yyyy-MM-dd hh:mm:ss" ).orElse( "???" );
            ret += "-";
            ret += ToolDate.Fromat( this.m_end, "hh:mm:ss" ).orElse( "???" );

            long seconds = 0;
            if ( this.m_start != null && this.m_end != null ){
                seconds = Duration.between( this.m_start, this.m_end ).getSeconds();
            }
            
            return ret + "(" + seconds + "s)";
        }

        public String dump(){
            String ret = this.m_idx + " Time="
                + ToolDate.Fromat( this.m_start, "uu/MM/dd HH:mm:ss" ).orElse("-")
                + "-"
                + ToolDate.Fromat( this.m_end, "uu/MM/dd HH:mm:ss" ).orElse("-");
            for ( String k: this.m_props_string.keySet() ){
                ret += " " + k + "=" + String.join(",", this.m_props_string.get(k) );
            }
            for ( String k: this.m_props_int.keySet() ){
                ret += " " + k + "=" + this.m_props_int.get(k).stream()
                  .sorted()
                  .map(String::valueOf)
                  .collect(Collectors.joining(","));
            }
            return ret;
        }
        */
    }

    private static int m_idx_count = -1;
    public record LogBasicData(Integer id, LocalDateTime logTime, String program, Integer pid, String message) {}
    private ArrayList<String>   m_tempdata;
    // m_datas<idx,LogDatas>
    private HashMap<Integer,LogData>   m_logdatas = new HashMap<Integer,LogData>();
    private HashMap<String, HashMap<Integer,LogData>>   m_int_idx = new HashMap<String, HashMap<Integer,LogData>>();
    private HashMap<String, HashMap<String,LogData>>    m_string_idx = new HashMap<String, HashMap<String,LogData>>();
    //private HashMap<String,LogData>   m_ip_idx = new HashMap<Integer,LogData>();

    public Optional<LogBasicData> SessionLog2LogBasic( LocalDate targetDate, String line ){
        int id;
        String res[] = line.split("<<->>");
        if ( res.length != 2 ) Optional.empty();
        try {
            id = Integer.parseInt( res[0] );
            return this.setLogBasic( id, targetDate, res[1]);
        } catch ( NumberFormatException e ){
            return Optional.empty();
        }
    }

    public Optional<LogBasicData> MailLog2LogBasic( LocalDate targetDate, String line ){
        return this.setLogBasic( -1, targetDate, line);
    }

    public Optional<LogBasicData> setLogBasic( int id, LocalDate targetDate, String line ){
        String sYear = targetDate.getYear() + "";
        Matcher m = LogPatterns.PTN_LOGBASIC.matcher(line);
        if (!m.matches()) return Optional.empty();

        LocalDateTime logTime;
        String program;
        Integer pid;
        String message;

        // 日時
        String ts = (sYear + " " + m.group(1)) .replaceAll("\\s+", " ") .trim();
        logTime = LocalDateTime.parse(ts, LogPatterns.FMT_LOG_DATETIME);

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

    public SessionLogDatas(){
        SessionLogDatas.m_idx_count = -1;
         this.m_tempdata = new ArrayList<String>();
    }

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
        this.addIndex( key, value, this.m_logdatas.get( id ) );
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
        this.addIndex( key, value, this.m_logdatas.get( id ) );
    }

    private void addIndex( String key, String val, LogData ld ){
        if ( !this.m_string_idx.containsKey(key) ) this.m_string_idx.put(key, new HashMap<String,LogData>() );
        this.m_string_idx.get(key).put( val, ld );
    }

    private void addIndex( String key, Integer val, LogData ld ){
        if ( !this.m_int_idx.containsKey(key) ) this.m_int_idx.put(key, new HashMap<Integer,LogData>() );
        this.m_int_idx.get(key).put( val, ld );
    }

    public Optional<Integer> searchIndex( String key, String value ){
        if ( ! this.m_string_idx.containsKey(key) ) return Optional.empty();
        if ( ! this.m_string_idx.get(key).containsKey(value) ) return Optional.empty();
        return Optional.ofNullable( this.m_string_idx.get(key).get(value).getIdx() );
    }

    public Optional<Integer> searchIndex( String key, Integer value ){
        if ( ! this.m_int_idx.containsKey(key) ) return Optional.empty();
        if ( ! this.m_int_idx.get(key).containsKey(value) ) return Optional.empty();
        return Optional.ofNullable( this.m_int_idx.get(key).get(value).getIdx() );
    }

    public boolean containsId( Integer id ){
        if ( this.getLogData(id).isEmpty() ) return false;
        return true;
    }

    /**
     * インデックスのキー一覧を取得(String)
     * @return
     */
    public String[] getIndexSList( String key ){
        if ( ! this.m_string_idx.containsKey(key) ) return new String[0];
        return this.m_string_idx.get(key).keySet().toArray( new String[0] );
    }

    /**
     * インデックスのキー一覧を取得(Integer)
     * @return
     */
    public Integer[] getIndexIList(){
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
    public ArrayList<String> getTempData(){ return this.m_tempdata; }

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
    public void save( Mfe2Config cfg, LocalDate targetDate){
        DateTimeFormatter ymFmt  = DateTimeFormatter.ofPattern("yyyyMM");
        DateTimeFormatter ymdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        // logDir/yyyyMM
        Path baseDir = Paths.get( cfg.getDataFilePath() + "session_logs/");
        Path logDir = baseDir.resolve(targetDate.format(ymFmt));

        // 上書き保存
        try {
            Files.createDirectories(logDir);
            // log_yyyyMMdd.txt
            Path logFile = logDir.resolve( "log_" + targetDate.format(ymdFmt) + ".txt" );

            Debugger.InfoPrint( "size=" + this.m_tempdata.size() + " file=" + logFile.toString() );
            Files.write(
                logFile,
                List.of( this.m_tempdata.toArray( new String[0] ) ),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            Debugger.InfoPrint( "saved file=" + logFile );
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
    public boolean load( Mfe2Config cfg, LocalDate targetDate){
        this.m_tempdata.clear();

        DateTimeFormatter ymFmt  = DateTimeFormatter.ofPattern("yyyyMM");
        DateTimeFormatter ymdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        // logDir/yyyyMM
        Path baseDir = Paths.get( cfg.getDataFilePath() + "session_logs/");
        Path logDir = baseDir.resolve(targetDate.format(ymFmt));
        Path logFile = logDir.resolve( "log_" + targetDate.format(ymdFmt) + ".txt" );

        try ( var lines = Files.lines( logFile, StandardCharsets.UTF_8) ) {
            lines.forEach( this.m_tempdata::add );
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
        Debugger.InfoPrint( "loaded file=" + logFile + " lines=" + this.m_tempdata.size() );
        return true;
    }
}
