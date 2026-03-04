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
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.Datas.SessionLogDatas;
import jp.d77.java.mfe2.Datas.SessionLogDatas.LogBasicData;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolDate;

public class SessionLogNumbering {
    private Mfe2Config          m_cfg;
    private SessionLogDatas     m_ld;
    private HashMap<Integer,Integer> m_smtpd_pid_connection = new HashMap<Integer,Integer>();
    private ArrayList<String> m_log;

    public SessionLogNumbering ( Mfe2Config cfg, SessionLogDatas ld ){
        this.m_cfg = cfg;
        this.m_ld = ld;
        this.m_log = new ArrayList<String>();
    }

    /**
     * MailLogからSessionLogを作成する
     * @param log
     * @param targetDate
     */
    public void CreateSessionLogs(){
        Debugger.TracePrint();

        //MailLog log = new MailLog( this.m_cfg );
        //log.Load( targetDate );
        this.Load( this.m_ld.getTargetDate() );

        String YMD = ToolDate.Fromat( this.m_ld.getTargetDate(), "yyyyMMdd").orElse("-");

        for ( int i = 0; i < this.m_log.size(); i ++ ){
            //if ( this.m_log.get(i).isEmpty() ) continue;
            String line = this.m_log.get(i);
            
            //LogBasicData lb = this.m_ld.MailLog2LogBasic( targetDate, line ).orElse(null);
            LogBasicData lb = this.m_ld.setLogBasic( -1, line ).orElse(null);
            if ( lb == null ) continue;

            int id = -1;
            if ( id == -1 ) id = this.analyzePostfixSmtpd( i, lb );
            if ( id == -1 ) id = this.analyzePostfixAny( i, lb );
            if ( id == -1 ) id = this.analyzeDovecot( i, lb );

            String tYMD;
            if ( id >= 0 ){
                tYMD = ToolDate.Fromat( this.m_ld.getStart(id).orElse(null) , "yyyyMMdd").orElse("-");
                if ( YMD.equals( tYMD ) ) this.m_ld.getTempData().add( id + "<<->>" + this.m_log.get(i) );
            }else{
                tYMD = ToolDate.Fromat( lb.logTime() , "yyyyMMdd").orElse("-");
                if ( YMD.equals( tYMD ) ) this.m_ld.getTempData().add( "-999<<->>" + this.m_log.get(i) );
            }
        }
    }

    /**
     * postfix/smtpdとpostfix/submission/smtpを分析
     * @param line_id
     * @param lb
     * @return
     */
    private int analyzePostfixSmtpd( int line_id, LogBasicData lb ){
        if ( ! lb.program().toLowerCase().equals( "postfix/smtpd" )
            && !lb.program().toLowerCase().equals( "postfix/submission/smtpd" )
            ) return -1;

        // warning: run-time library vs. compile-time header version mismatch: OpenSSL 3.5.0 may not be compatible with OpenSSL 3.2.0
        if ( lb.message().startsWith( "warning: run-time library vs. compile-time") ) return -999;

        Optional<String> s;
        int id;
        String pid;

        if ( ! this.m_smtpd_pid_connection.containsKey( lb.pid() ) ){
            // 一度も登場していないpid
            this.m_smtpd_pid_connection.put( lb.pid(), 0 );
            pid = lb.pid() + "-0";
        }else{
            pid = lb.pid() + "-" + this.m_smtpd_pid_connection.get( lb.pid() );
        }

        if ( this.m_ld.searchProp( "postfix/smtpd_pid", pid).isEmpty() ) {
            // postfix/smtpdのpidはまだ未発見→新規登録
            id = this.m_ld.newLogData();
            this.m_ld.addProp( lb.logTime(), id, "postfix/smtpd_pid", pid );
            this.m_ld.addProp( lb.logTime(), id, "line", line_id );
        } else{
            // postfix/smtpdのpid登録済み
            id = this.m_ld.searchProp( "postfix/smtpd_pid", pid).get();
            this.m_ld.addProp( lb.logTime(), id, "line", line_id );
        }

        // Disconnect
        if ( lb.message().startsWith( "disconnect from" ) ) {
            this.m_smtpd_pid_connection.put( lb.pid(), this.m_smtpd_pid_connection.get( lb.pid() ) + 1 );
        }

        // Queue
        // Feb  4 11:37:07 afef27cd6bf6 postfix/smtpd[14562]: 85C9E81BA4BE: client=fgmailer.com[35.187.195.50]
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message(), 1 );
        if ( s.isPresent() ) {
            this.m_ld.addProp( lb.logTime(), id, "postfix_queue", s.get() );
        }

        return id;
    }

    /**
     * postfixその他ログを解析
     * @param line_id
     * @param lb
     * @return
     */
    private int analyzePostfixAny( int line_id, LogBasicData lb ){
        if ( ! lb.program().toLowerCase().equals( "postfix/cleanup" )
            &&  ! lb.program().toLowerCase().equals( "postfix/qmgr" )
            &&  ! lb.program().toLowerCase().equals( "postfix/local" )
            &&  ! lb.program().toLowerCase().equals( "postfix/smtp" )
            ) return -1;
        Optional<String> s;
        int id;

        // Queue
        // Feb 1 00:10:08 afef27cd6bf6 postfix/cleanup[41622]: 22A6581B3AE2: message-id=<20260131151008.37vQF%root@dms.d77.jp>
        // Feb 1 00:00:18 afef27cd6bf6 postfix/cleanup[41559]: 05D2881B3AE2: milter-reject: END-OF-MESSAGE from ab218251.f.west.v6connect.net[183.76.218.251]: 5.7.1 Spam message rejected; from= to= proto=ESMTP helo=

        String queue;
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message(), 1 );
        if ( s.isPresent() ) {
            queue = s.get();
            if ( this.m_ld.searchProp("postfix_queue", queue).isEmpty() ) return -999;
            id = this.m_ld.searchProp("postfix_queue", queue).get();
            this.m_ld.addProp( lb.logTime(), id, "line", line_id );
        }else{
            // queue id なし
            Debugger.ErrorPrint( "queue id not found: " + line_id );
            return -999;
        }

        // message-id
        s = LogPatterns.matcher( LogPatterns.PTN_MESSAGEID, lb.message(), 1 );
        if ( s.isPresent() ) this.m_ld.addProp( lb.logTime(), id, "msgid", s.get() );
        s = LogPatterns.matcher( LogPatterns.PTN_MSGID, lb.message(), 1 );
        if ( s.isPresent() ) this.m_ld.addProp( lb.logTime(), id, "msgid", s.get() );

        return id;
    }

    /**
     * dovecotログを解析
     * @param line_id
     * @param lb
     * @return
     */
    private int analyzeDovecot( int line_id, LogBasicData lb ){
        if ( ! lb.program().toLowerCase().equals( "dovecot" ) ) return -1;

        // (除外)Feb  1 05:57:52 afef27cd6bf6 dovecot: master: Dovecot v2.3.16 (7e2e900c1a) starting up for imap, pop3, lmtp, sieve
        if ( lb.message().startsWith( "master: Dovecot v") ) return -999;

        // (除外)Feb  1 07:40:57 afef27cd6bf6 dovecot: imap-login: Login: user=<delta>, method=PLAIN, rip=172.30.0.1, lip=172.30.4.22, mpid=809, session=<W+mHyLZJFsisHgAB>
        if ( lb.message().startsWith( "imap-login: Login:") ) return -999;

        // (除外)Feb  1 07:41:37 afef27cd6bf6 dovecot: imap(delta)<846><EgPmyrZJ7NKsHgAB>: Disconnected: Logged out in=397 out=10242 deleted=0 expunged=0 trashed=0 hdr_count=17 hdr_bytes=5147 body_count=0 body_bytes=0
        if ( lb.message().contains( "Disconnected: Logged out in") ) return -999;

        Optional<String> s;
        Integer id = null;

        // msgid / mailbox
        // Feb  1 06:06:05 afef27cd6bf6 dovecot: lda(delta)<218><sbuvCr1ufmnaAAAA8Bi3+g>: sieve: msgid=<697e6ebc.Ja2ThT4kue/ojPj8%delta@d77.jp>: fileinto action: stored mail into mailbox '10,ServerAdmin.01,jobmail'
        // Feb  1 08:45:57 afef27cd6bf6 dovecot: lda(kurio)<1640><8ntICTWUfmloBgAA8Bi3+g>: msgid=<64ef0110-b09b-4f9c-ac7b-43fb15eb18c6@r16.geese-solutions.net>: saved mail to INBOX

        // message-idからメッセージIDを取得
        String msgid = null;
        s = LogPatterns.matcher( LogPatterns.PTN_MESSAGEID, lb.message(), 1 );
        if ( s.isPresent() ) {
            msgid = s.get();
            if ( this.m_ld.searchProp("msgid", msgid).isPresent() ) {
                id = this.m_ld.searchProp("msgid", msgid).get();
                this.m_ld.addProp( lb.logTime(), id, "line", line_id );
            }
        }

        // msgidからメッセージIDを取得
        s = LogPatterns.matcher( LogPatterns.PTN_MSGID, lb.message(), 1 );
        if ( s.isPresent() ) {
            msgid = s.get();
            if ( this.m_ld.searchProp("msgid", msgid).isPresent() ) {
                id = this.m_ld.searchProp("msgid", msgid).get();
                this.m_ld.addProp( lb.logTime(), id, "line", line_id );
            }
        }

        // 不明行
        if ( msgid == null || id == null ){
            // queue id なし
            Debugger.ErrorPrint( "msg id not found: " + line_id );
            return -999;
        }

        return id;
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
