package jp.d77.java.mfe2.LogAnalyser;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import jp.d77.java.tools.BasicIO.ToolDate;

public class SessionLogDatas {
    private static int m_idx_count = -1;

    public static class LogBasic{
        public Integer id;
        public LocalDateTime logTime;
        public String program;
        public Integer pid;
        public String message;

        public int setSessionlog( LocalDate targetDate, String line ){
            String res[] = line.split("<<->>");
            if ( res.length != 2 ) return -1;
            try {
                int id = Integer.parseInt( res[0] );
                if ( ! this.setMaillog(targetDate, res[1]) ) return -1;
                return id;
            } catch ( NumberFormatException e ){
                return -1;
            }
        }

        public boolean setMaillog( LocalDate targetDate, String line ){
            String sYear = targetDate.getYear() + "";

            Matcher m = LogPatterns.PTN_LOGBASIC.matcher(line);
            if (!m.matches()) return false;

            // 日時
            String ts = (sYear + " " + m.group(1)) .replaceAll("\\s+", " ") .trim();
            this.logTime = LocalDateTime.parse(ts, LogPatterns.FMT_LOG_DATETIME);

            // ③ プログラム名
            this.program = m.group(2);

            // ③ PID（無い場合 null）
            this.pid = (m.group(3) != null)
                    ? Integer.valueOf(m.group(3))
                    : null;

            // ④ メッセージ
            this.message = m.group(4);

            return true;
        }
    }

    public static LogBasic LogBasicData(){
        return new LogBasic();
    }

    public class LogData{
        private int m_idx;
        private HashMap<String,Set<String>> m_props_string;
        private HashMap<String,Set<Integer>> m_props_int;
        private LocalDateTime m_start;
        private LocalDateTime m_end;
        private ArrayList<LogBasic> m_log;

        public LogData(){
            SessionLogDatas.m_idx_count += 1;
            this.m_idx = SessionLogDatas.m_idx_count;
            this.m_props_string = new HashMap<String,Set<String>>();
            this.m_props_int = new HashMap<String,Set<Integer>>();
            this.m_log = new ArrayList<LogBasic>();
        }

        public LogData( int id ){
            if ( SessionLogDatas.m_idx_count < id ) SessionLogDatas.m_idx_count = id;
            this.m_idx = id;
            this.m_props_string = new HashMap<String,Set<String>>();
            this.m_props_int = new HashMap<String,Set<Integer>>();
            this.m_log = new ArrayList<LogBasic>();
        }

        /**
         * add propaty(String)
         * @param key
         * @param value
         */
        public void addProp( String key, String value ){
            if ( ! this.m_props_string.containsKey(key) ) this.m_props_string.put( key, new HashSet<String>() );
            this.m_props_string.get( key ).add( value );
        }

        /**
         * add propaty(Integer)
         * @param key
         * @param value
         */
        public void addProp( String key, Integer value ){
            if ( ! this.m_props_int.containsKey(key) ) this.m_props_int.put( key, new HashSet<Integer>() );
            this.m_props_int.get( key ).add( value );
        }

        /**
         * set time
         * @param time
         */
        public void setTime(LocalDateTime time) {
            if (time == null) return;
            if (m_start == null || time.isBefore(m_start)) m_start = time;
            if (m_end == null || time.isAfter(m_end)) m_end = time;
        }

        public int getIdx() { return this.m_idx; }

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

        public Optional<String[]>getPropS( String key ){
            if ( ! this.m_props_string.containsKey(key) ) return Optional.empty();
            return Optional.ofNullable( this.m_props_string.get(key).toArray( new String[0] ) );
        }
        public Optional<Integer[]>getPropI( String key ){
            if ( ! this.m_props_int.containsKey(key) ) return Optional.empty();
            return Optional.ofNullable( this.m_props_int.get(key).toArray( new Integer[0] ) );
        }
        public LocalDateTime getStart() {   return m_start; }
        public LocalDateTime getEnd() {     return m_end;   }
        public void addLog( LogBasic lb ){  this.m_log.add( lb );   }
        public LogBasic[] getLog(){  return this.m_log.toArray( new LogBasic [0] ); }

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
    }

    // m_datas<idx,LogDatas>
    private HashMap<Integer,LogData>   m_logdatas = new HashMap<Integer,LogData>();
    private HashMap<String, HashMap<Integer,LogData>>   m_int_idx = new HashMap<String, HashMap<Integer,LogData>>();
    private HashMap<String, HashMap<String,LogData>>    m_string_idx = new HashMap<String, HashMap<String,LogData>>();

    public SessionLogDatas(){
        SessionLogDatas.m_idx_count = -1;
    }

    public int newLogData(){
        LogData ld = new LogData();
        this.m_logdatas.put( ld.m_idx, ld );
        return ld.getIdx();
    }

    public void newLogData( int id ){
        LogData ld;
        if ( this.getLogData( id ).isEmpty() ){
            ld = new LogData( id );
            this.m_logdatas.put( ld.m_idx, ld );
        }else{
            ld = this.getLogData( id ).get();
        }
        return;
    }

    public void addLog( int id, LogBasic lb ){
        if ( ! this.m_logdatas.containsKey( id ) ) return;
        this.m_logdatas.get( id );
    }

    /**
     * set propaty & indexed
     * @param id
     * @param key
     * @param value
     */
    public void addProp( int id, String key, String value ){
        if ( ! this.m_logdatas.containsKey( id ) ) return;
        this.m_logdatas.get( id ).addProp(key, value);
        this.addIndex( key, value, this.m_logdatas.get( id ) );
    }

    /**
     * set propaty & indexed
     * @param id
     * @param key
     * @param value
     */
    public void addProp( int id, String key, Integer value ){
        if ( ! this.m_logdatas.containsKey( id ) ) return;
        this.m_logdatas.get( id ).addProp(key, value);
        this.addIndex( key, value, this.m_logdatas.get( id ) );
    }

    public Optional<Integer> search( String key, String value ){
        Optional<LogData> ld = this.getIndexS( key, value );
        if ( ld.isEmpty() ) return Optional.empty();
        return Optional.ofNullable( ld.get().getIdx() );
    }

    public Optional<Integer> search( String key, Integer value ){
        Optional<LogData> ld = this.getIndexI( key, value );
        if ( ld.isEmpty() ) return Optional.empty();
        return Optional.ofNullable( ld.get().getIdx() );
    }

    private void addIndex( String key, String val, LogData ld ){
        if ( !this.m_string_idx.containsKey(key) ) this.m_string_idx.put(key, new HashMap<String,LogData>() );
        this.m_string_idx.get(key).put( val, ld );
    }

    private void addIndex( String key, Integer val, LogData ld ){
        if ( !this.m_int_idx.containsKey(key) ) this.m_int_idx.put(key, new HashMap<Integer,LogData>() );
        this.m_int_idx.get(key).put( val, ld );
    }

    public Optional<LogData>getIndexS( String key, String val ){
        if ( ! this.m_string_idx.containsKey(key) ) return Optional.empty();
        if ( ! this.m_string_idx.get(key).containsKey(val) ) return Optional.empty();
        return Optional.ofNullable( this.m_string_idx.get(key).get(val) );
    }

    public Optional<LogData>getIndexI( String key, Integer val ){
        if ( ! this.m_int_idx.containsKey(key) ) return Optional.empty();
        if ( ! this.m_int_idx.get(key).containsKey(val) ) return Optional.empty();
        return Optional.ofNullable( this.m_int_idx.get(key).get(val) );
    }

    public Optional<LogData>getLogData( Integer id ){
        if ( ! this.m_logdatas.containsKey(id) ) Optional.empty();
        return Optional.ofNullable( this.m_logdatas.get(id) );
    }

    public Integer[] getIndexsList(){
        return this.m_logdatas.keySet().stream()
            .sorted()
            .toArray(Integer[]::new);
    }
}
