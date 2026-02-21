package jp.d77.java.mfe2.LogAnalyser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.tools.BasicIO.Debugger;

public class MailLog {
    private ArrayList<String> m_log;
    private Mfe2Config m_cfg;

    public MailLog( Mfe2Config cfg ){
        this.m_cfg = cfg;
        this.m_log = new ArrayList<String>();
    }

    /**
     * ログ全てを取得
     * @return
     */
    public String[] getLog(){
        return this.m_log.toArray(new String[0]);
    }

    /**
     * 指定行のログを取得
     * @param line
     * @return
     */
    public Optional<String> getLog( int line ){
        if ( this.m_log == null ) return Optional.empty();
        if ( line < 0 || line >= this.m_log.size() ) return Optional.empty();
        return Optional.ofNullable( this.m_log.get(line) );
    }

    /**
     * ログの行数
     * @return
     */
    public int size(){
        if ( this.m_log == null ) return 0;
        return this.m_log.size();
    }

    /**
     * ログ読み込み
     * @param cfg
     * @param targetDate
     */
    public void Load( LocalDate targetDate ){
        Debugger.TracePrint();

        this.m_log = new ArrayList<String>();
        Path logDir = Paths.get( this.m_cfg.getLogFilePath() + "dms26-mail2/");

        Optional<Path> latestRotated = this.findLatestRotated(logDir);
        Path maillog = logDir.resolve("maillog");

        if (latestRotated.isPresent()) {
            this.m_cfg.addAlertInfo( "Load log=" + latestRotated.get().getFileName() );
            Debugger.InfoPrint( "Loaded log line=" + latestRotated.get().getFileName() );
            this.processFile(latestRotated.get(), targetDate);
        }

        if (Files.exists(maillog)) {
            this.m_cfg.addAlertInfo( "Load log=" + maillog.getFileName() );
            this.processFile(maillog, targetDate);
        }
        this.m_cfg.addAlertInfo( "Loaded line=" + this.m_log.size() );
        Debugger.InfoPrint( "Loaded line=" + this.m_log.size() );
    }

    /**
     * ログ読み込み
     * @param file
     * @param targetDate
     */
    private void processFile(Path file, LocalDate targetDate) {

        try  {
            BufferedReader br = Files.newBufferedReader(file);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() < 15) continue;

                // 例: "Feb  1 09:06:01"
                //String tsPart = targetDate.getYear() + " " + line.substring(0, 15).trim();
                String tsPart = (targetDate.getYear() + " " + line.substring(0, 15))
                    .replaceAll("\\s+", " ")
                    .trim();

                try {
                    LocalDateTime logTime =
                            LocalDateTime.parse(tsPart, LogPatterns.FMT_LOG_DATETIME);

                    if (logTime.toLocalDate().equals(targetDate)) {
                        this.m_log.add( line );
                        //System.out.println(line);
                    }else if (logTime.toLocalDate().equals(targetDate.plusDays(1))) {
                        this.m_log.add( line );
                    }

                } catch (Exception e) {
                    // フォーマット不正行は無視
                    e.printStackTrace();
                }
            }
            br.close();
            Debugger.InfoPrint( "loaded file=" + file + " date=" + targetDate );
        } catch ( IOException e ){
            this.m_cfg.addAlertError( "processFile Error file=" + file + " " + e.getMessage() );
            Debugger.ErrorPrint( "file=" + file + " " + e.getMessage() );
            e.printStackTrace();
        }
    }

    /**
     * ログファイル検索
     * @param dir
     * @return
     */
    public Optional<Path> findLatestRotated(Path dir) {
        Pattern ROTATED_PATTERN = Pattern.compile("^maillog-(\\d{8})$");
        DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

        try  {
            return Files.list(dir)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(name -> {
                        Matcher m = ROTATED_PATTERN.matcher(name);
                        if (!m.matches()) return null;
                        LocalDate date = LocalDate.parse(m.group(1), DATE_FMT);
                        return new LogFile(name, date);
                    })
                    .filter(v -> v != null)
                    .max(Comparator.comparing(LogFile::date))
                    .map(v -> dir.resolve(v.name()));
        } catch ( IOException e ){
            return Optional.empty();

        }
    }

    private record LogFile(String name, LocalDate date) {}    
}
