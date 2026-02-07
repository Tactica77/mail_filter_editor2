package jp.d77.java.mfe2.LogAnalyser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.LogAnalyser.LogPatterns.HostAddress;
import jp.d77.java.mfe2.LogAnalyser.LogPatterns.MilterReject;
import jp.d77.java.mfe2.LogAnalyser.LogPatterns.RelayStatus;
import jp.d77.java.mfe2.LogAnalyser.SessionLogDatas.LogBasic;
import jp.d77.java.mfe2.LogAnalyser.SessionLogDatas.LogData;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolDate;

public class SessionLogs {
    private SessionLogDatas     m_ld;
    private ArrayList<String>   m_savedata = new ArrayList<String>();

    public SessionLogs(){
    }

    public Optional<LogData>getLogData( Integer id ){
        return this.m_ld.getLogData(id);
    }

    public Integer[] getIndexsList(){
        return this.m_ld.getIndexsList();
    }

    public void AnalyseLog( LocalDate targetDate ){
        Debugger.TracePrint();
        this.m_ld = new SessionLogDatas();

        for ( String line: this.m_savedata ){
            LogBasic lb = SessionLogDatas.LogBasicData();
            int id = lb.setSessionlog(targetDate, line);
            if ( id == -1 ) continue;   // maillogの形式不正
            this.m_ld.newLogData( id );
            LogData ld = this.m_ld.getLogData( id ).get();

            ld.addLog(lb);

            if ( this.detailPostfixSmtpd( id, lb ) ) continue;
            if ( this.detailPostfixAny( id, lb ) ) continue;
        }
    }

    /**
     * 詳細データ取得(postfix/smtpd)
     * @param id
     * @param lb
     * @return
     */
    private boolean detailPostfixSmtpd( int id, LogBasic lb ){
        if ( ! lb.program.toLowerCase().equals( "postfix/smtpd" )
            && !lb.program.toLowerCase().equals( "postfix/submission/smtpd" )
            ) return false;
        Optional<String> s;
        Optional<HostAddress> ha;

        this.m_ld.getLogData(id).get().setTime( lb.logTime );

        // Queue
        // Feb  4 11:37:07 afef27cd6bf6 postfix/smtpd[14562]: 85C9E81BA4BE: client=fgmailer.com[35.187.195.50]
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message, 1 );
        if ( s.isPresent() ) {
            this.m_ld.addProp( id, "postfix_queue", s.get() );
        }

        // FQDN and IP
        ha = LogPatterns.extractHostAddress( lb.message );
        if ( ha.isPresent() ){
            this.m_ld.addProp( id, "ip", ha.get().ip() );
            this.m_ld.addProp( id, "fqdn", ha.get().fqdn() );
        }

        if ( lb.message.startsWith( "connect from " ) ){
            // Feb  4 11:37:07 afef27cd6bf6 postfix/smtpd[14562]: connect from fgmailer.com[35.187.195.50]
            // 処理なし
            return true;
        }

        if ( lb.message.startsWith( "disconnect from " ) ){
            // disconnect from unknown[192.168.1.210] helo=1 mail=1 rcpt=1 data=1 quit=1 commands=5
            String[] tokens = lb.message.split("\\s+");
            int index = lb.message.indexOf( tokens[3] ); 
            if (index == -1) return true;
            this.m_ld.addProp( id, "disconnect", lb.message.substring( index ) );
            return true;
        }

        if ( lb.message.startsWith( "warning: hostname " ) ){
            // warning: hostname 31-13-232-165.clnet.aesbg.net does not resolve to address 31.13.232.165: Name or service not known
            Matcher m = LogPatterns.PTN_POSFTIX_SMTPD_RESOLVEADDR.matcher( lb.message );
            if (m.find()) {
                this.m_ld.addProp( id, "ip", m.group(2) );
                this.m_ld.addProp( id, "fqdn", m.group(1) );
            }
            if ( lb.message.contains("does not resolve to address") ){
                this.m_ld.addProp( id, "relay_status", "not resolve" );
            }
        }

        if ( lb.message.startsWith( "NOQUEUE: reject: " ) ){
            // NOQUEUE: reject: RCPT from unknown[181.229.197.169]: 450 4.7.25 Client host rejected: cannot find your hostname, [181.229.197.169]; from=<admin@d77.jp> to=<admin@d77.jp> proto=ESMTP helo=<smtpclient.apple>
            s = LogPatterns.matcher( LogPatterns.PTN_NOQUEUE_CODE, lb.message, 1 );
            if ( s.isPresent() ) this.m_ld.addProp( id, "relay_status", s.get() );
            s = LogPatterns.matcher( LogPatterns.PTN_NOQUEUE_ERROR, lb.message, 1 );
            if ( s.isPresent() ) this.m_ld.addProp( id, "error", s.get() );
        }
        if ( lb.message.startsWith( "warning: non-SMTP command " ) ){
            // warning: non-SMTP command from scan-59a.shadowserver.org[65.49.1.108]: GET / HTTP/1.1
            this.m_ld.addProp( id, "relay_status", "none-smtp" );
        }

        if ( lb.message.startsWith( "improper command pipelining " ) ){
            // improper command pipelining after CONNECT from unknown[195.3.222.78]:
            this.m_ld.addProp( id, "relay_status", "improper-cmd" );
        }

        if ( lb.message.startsWith( "timeout after " ) ){
            // timeout after CONNECT from unknown[197.205.240.221]
            this.m_ld.addProp( id, "relay_status", "timeout" );
        }


        if ( lb.message.startsWith( "timeout after " )
            // timeout after CONNECT from unknown[197.205.240.221]
            || lb.message.startsWith( "lost connection after " )
            // lost connection after DATA from unknown[181.229.197.169]
            || lb.message.startsWith( "improper command pipelining after " )
            // improper command pipelining after CONNECT from unknown[195.3.222.78]:
            || lb.message.startsWith( "SSL_accept error " )
            // SSL_accept error from unknown[185.93.89.95]: -1
            ){
            int index = lb.message.indexOf( " from " ); 
            if (index == -1) return true;
            this.m_ld.addProp( id, "error", lb.message.substring( 0, index ) );
        }
        
        // warning: run-time library vs. compile-time header version mismatch: OpenSSL 3.5.0 may not be compatible with OpenSSL 3.2.0
        // warning: TLS library problem: error:0A000412:SSL routines::ssl/tls alert bad certificate:ssl/record/rec_layer_s3.c:916:SSL alert number 42:
        /*

        */

        return true;
    }

        /**
     * 詳細データ取得(postfix/smtpd)
     * @param id
     * @param lb
     * @return
     */
    private boolean detailPostfixAny( int id, LogBasic lb ){
        if ( ! lb.program.toLowerCase().equals( "postfix/cleanup" )
            &&  ! lb.program.toLowerCase().equals( "postfix/qmgr" )
            &&  ! lb.program.toLowerCase().equals( "postfix/local" )
            &&  ! lb.program.toLowerCase().equals( "postfix/smtp" )
            ) return false;

        Optional<String> s;
        Optional<HostAddress> ha;

        this.m_ld.getLogData(id).get().setTime( lb.logTime );

        // Queue
        // Feb 1 00:10:08 afef27cd6bf6 postfix/cleanup[41622]: 22A6581B3AE2: message-id=<20260131151008.37vQF%root@dms.d77.jp>
        // Feb 1 00:00:18 afef27cd6bf6 postfix/cleanup[41559]: 05D2881B3AE2: milter-reject: END-OF-MESSAGE from ab218251.f.west.v6connect.net[183.76.218.251]: 5.7.1 Spam message rejected; from= to= proto=ESMTP helo=
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message, 1 );
        if ( s.isPresent() ) {
            this.m_ld.addProp( id, "postfix_queue", s.get() );
        }

        // FQDN and IP
        ha = LogPatterns.extractHostAddress( lb.message );
        if ( ha.isPresent() ){
            this.m_ld.addProp( id, "ip", ha.get().ip() );
            this.m_ld.addProp( id, "fqdn", ha.get().fqdn() );
        }

        // message-id
        s = LogPatterns.matcher( LogPatterns.PTN_MESSAGEID, lb.message, 1 );
        if ( s.isPresent() ) this.m_ld.addProp( id, "msgid", s.get() );
        s = LogPatterns.matcher( LogPatterns.PTN_MSGID, lb.message, 1 );
        if ( s.isPresent() ) this.m_ld.addProp( id, "msgid", s.get() );

        // From
        s = LogPatterns.matcher( LogPatterns.PTN_ADDRESS_FROM, lb.message, 1 );
        if ( s.isPresent() ) this.m_ld.addProp( id, "from", s.get() );

        // To
        s = LogPatterns.matcher( LogPatterns.PTN_ADDRESS_TO, lb.message, 1 );
        if ( s.isPresent() ) this.m_ld.addProp( id, "to", s.get() );

        // MilterReject
        if ( lb.message.contains( "milter-reject: " ) ){
            Optional<MilterReject> mr;
            mr = LogPatterns.extractMilterReject( lb.message );
            if ( mr.isPresent() ){
                this.m_ld.addProp( id, "error", mr.get().status() + " " + mr.get().code() );
            }
            if ( lb.message.contains( "Spam message rejected" ) ){
                this.m_ld.addProp( id, "relay_status", "SPAM" );
            }
        }

        // relay
        if ( lb.program.equals( "postfix/local" )
            || lb.program.equals( "postfix/smtp" ) ){
            Boolean relay_local = null;
            s = LogPatterns.matcher( LogPatterns.PTN_RELAY_TO, lb.message, 1 );
            if ( s.isPresent() ) {
                this.m_ld.addProp( id, "relay", s.get() );
                if ( s.get().equals( "local" ) ) relay_local = true;
                else relay_local = false;
            }

            Optional<RelayStatus> rs;
            rs = LogPatterns.extractRelayStatus( lb.message );
            if ( rs.isPresent() ){
                if ( relay_local == null ){
                    this.m_ld.addProp( id, "relay_status", "send null" );
                }else if ( relay_local ){
                    this.m_ld.addProp( id, "relay_status", "send local" );
                }else{
                    this.m_ld.addProp( id, "relay_status", "send remote" );
                }
                this.m_ld.addProp( id, "relay_detail", rs.get().detail() );
            }
        }
        return true;
    }

    /**
     * ログ分析メイン
     * @param log
     * @param targetDate
     */
    public void CreateSessionLogs( MailLog log, LocalDate targetDate ){
        Debugger.TracePrint();
        this.m_ld = new SessionLogDatas();
        this.m_savedata.clear();

        String YMD = ToolDate.Fromat(targetDate, "yyyyMMdd").orElse("-");

        for ( int i = 0; i < log.size(); i ++ ){
            if ( log.getLog(i).isEmpty() ) continue;
            String line = log.getLog(i).get();
            LogBasic lb = SessionLogDatas.LogBasicData();
            if ( ! lb.setMaillog(targetDate, line) ) continue;

            int id = -1;
            if ( id == -1 ) id = this.analyzePostfixSmtpd( i, lb );
            if ( id == -1 ) id = this.analyzePostfixAny( i, lb );
            if ( id == -1 ) id = this.analyzeDovecot( i, lb );

            String tYMD;
            if ( id >= 0 ){
                tYMD = ToolDate.Fromat( this.m_ld.getLogData(id).get().getStart() , "yyyyMMdd").orElse("-");
                if ( YMD.equals( tYMD ) ) this.m_savedata.add( id + "<<->>" + log.getLog(i).get() );
            }else{
                tYMD = ToolDate.Fromat( lb.logTime , "yyyyMMdd").orElse("-");
                if ( YMD.equals( tYMD ) ) this.m_savedata.add( "-999<<->>" + log.getLog(i).get() );
            }
        }
    }

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

            Debugger.InfoPrint( "size=" + this.m_savedata.size() + " file=" + logFile.toString() );
            Files.write(
                logFile,
                List.of( this.m_savedata.toArray( new String[0] ) ),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            Debugger.InfoPrint( "saved file=" + logFile );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean load( Mfe2Config cfg, LocalDate targetDate){
        this.m_savedata.clear();

        DateTimeFormatter ymFmt  = DateTimeFormatter.ofPattern("yyyyMM");
        DateTimeFormatter ymdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

        // logDir/yyyyMM
        Path baseDir = Paths.get( cfg.getDataFilePath() + "session_logs/");
        Path logDir = baseDir.resolve(targetDate.format(ymFmt));
        Path logFile = logDir.resolve( "log_" + targetDate.format(ymdFmt) + ".txt" );

        try ( var lines = Files.lines( logFile, StandardCharsets.UTF_8) ) {
            lines.forEach( this.m_savedata::add );
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
        Debugger.InfoPrint( "loaded file=" + logFile );
        return true;
    }

    private HashMap<Integer,Integer> m_smtpd_pid_connection = new HashMap<Integer,Integer>();

    private int analyzePostfixSmtpd( int line_id, LogBasic lb ){
        if ( ! lb.program.toLowerCase().equals( "postfix/smtpd" )
            && !lb.program.toLowerCase().equals( "postfix/submission/smtpd" )
            ) return -1;

        // warning: run-time library vs. compile-time header version mismatch: OpenSSL 3.5.0 may not be compatible with OpenSSL 3.2.0
        if ( lb.message.startsWith( "warning: run-time library vs. compile-time") ) return -999;

        Optional<String> s;
        int id;
        String pid;

        if ( ! this.m_smtpd_pid_connection.containsKey( lb.pid ) ){
            // 一度も登場していないpid
            this.m_smtpd_pid_connection.put( lb.pid, 0 );
            pid = lb.pid + "-0";
        }else{
            pid = lb.pid + "-" + this.m_smtpd_pid_connection.get( lb.pid );
        }

        if ( this.m_ld.search( "postfix/smtpd_pid", pid).isEmpty() ) {
            // postfix/smtpdのpidはまだ未発見→新規登録
            id = this.m_ld.newLogData();
            this.m_ld.addProp( id, "postfix/smtpd_pid", pid );
            this.m_ld.addProp( id, "line", line_id );
        } else{
            // postfix/smtpdのpid登録済み
            id = this.m_ld.search( "postfix/smtpd_pid", pid).get();
            this.m_ld.addProp( id, "line", line_id );
        }
        this.m_ld.getLogData(id).get().setTime( lb.logTime );
        // Disconnect
        if ( lb.message.startsWith( "disconnect from" ) ) {
            this.m_smtpd_pid_connection.put( lb.pid, this.m_smtpd_pid_connection.get( lb.pid ) + 1 );
        }

        // Queue
        // Feb  4 11:37:07 afef27cd6bf6 postfix/smtpd[14562]: 85C9E81BA4BE: client=fgmailer.com[35.187.195.50]
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message, 1 );
        if ( s.isPresent() ) {
            this.m_ld.addProp( id, "postfix_queue", s.get() );
        }

        return id;
    }

    private int analyzePostfixAny( int line_id, LogBasic lb ){
        if ( ! lb.program.toLowerCase().equals( "postfix/cleanup" )
            &&  ! lb.program.toLowerCase().equals( "postfix/qmgr" )
            &&  ! lb.program.toLowerCase().equals( "postfix/local" )
            &&  ! lb.program.toLowerCase().equals( "postfix/smtp" )
            ) return -1;
        Optional<String> s;
        int id;

        // Queue
        // Feb 1 00:10:08 afef27cd6bf6 postfix/cleanup[41622]: 22A6581B3AE2: message-id=<20260131151008.37vQF%root@dms.d77.jp>
        // Feb 1 00:00:18 afef27cd6bf6 postfix/cleanup[41559]: 05D2881B3AE2: milter-reject: END-OF-MESSAGE from ab218251.f.west.v6connect.net[183.76.218.251]: 5.7.1 Spam message rejected; from= to= proto=ESMTP helo=

        String queue;
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message, 1 );
        if ( s.isPresent() ) {
            queue = s.get();
            if ( this.m_ld.search("postfix_queue", queue).isEmpty() ) return -999;
            id = this.m_ld.search("postfix_queue", queue).get();
            this.m_ld.addProp( id, "line", line_id );
            this.m_ld.getLogData(id).get().setTime( lb.logTime );
        }else{
            // queue id なし
            Debugger.ErrorPrint( "queue id not found: " + line_id );
            return -999;
        }

        // message-id
        s = LogPatterns.matcher( LogPatterns.PTN_MESSAGEID, lb.message, 1 );
        if ( s.isPresent() ) this.m_ld.addProp( id, "msgid", s.get() );
        s = LogPatterns.matcher( LogPatterns.PTN_MSGID, lb.message, 1 );
        if ( s.isPresent() ) this.m_ld.addProp( id, "msgid", s.get() );

        return id;
    }

    private int analyzeDovecot( int line_id, LogBasic lb ){
        if ( ! lb.program.toLowerCase().equals( "dovecot" ) ) return -1;

        // (除外)Feb  1 05:57:52 afef27cd6bf6 dovecot: master: Dovecot v2.3.16 (7e2e900c1a) starting up for imap, pop3, lmtp, sieve
        if ( lb.message.startsWith( "master: Dovecot v") ) return -999;

        // (除外)Feb  1 07:40:57 afef27cd6bf6 dovecot: imap-login: Login: user=<delta>, method=PLAIN, rip=172.30.0.1, lip=172.30.4.22, mpid=809, session=<W+mHyLZJFsisHgAB>
        if ( lb.message.startsWith( "imap-login: Login:") ) return -999;

        // (除外)Feb  1 07:41:37 afef27cd6bf6 dovecot: imap(delta)<846><EgPmyrZJ7NKsHgAB>: Disconnected: Logged out in=397 out=10242 deleted=0 expunged=0 trashed=0 hdr_count=17 hdr_bytes=5147 body_count=0 body_bytes=0
        if ( lb.message.contains( "Disconnected: Logged out in") ) return -999;

        Optional<String> s;
        Integer id = null;

        // msgid / mailbox
        // Feb  1 06:06:05 afef27cd6bf6 dovecot: lda(delta)<218><sbuvCr1ufmnaAAAA8Bi3+g>: sieve: msgid=<697e6ebc.Ja2ThT4kue/ojPj8%delta@d77.jp>: fileinto action: stored mail into mailbox '10,ServerAdmin.01,jobmail'
        // Feb  1 08:45:57 afef27cd6bf6 dovecot: lda(kurio)<1640><8ntICTWUfmloBgAA8Bi3+g>: msgid=<64ef0110-b09b-4f9c-ac7b-43fb15eb18c6@r16.geese-solutions.net>: saved mail to INBOX

        // message-idからメッセージIDを取得
        String msgid = null;
        s = LogPatterns.matcher( LogPatterns.PTN_MESSAGEID, lb.message, 1 );
        if ( s.isPresent() ) {
            msgid = s.get();
            if ( this.m_ld.search("msgid", msgid).isPresent() ) {
                id = this.m_ld.search("msgid", msgid).get();
                this.m_ld.addProp( id, "line", line_id );
                this.m_ld.getLogData(id).get().setTime( lb.logTime );
            }
        }

        // msgidからメッセージIDを取得
        s = LogPatterns.matcher( LogPatterns.PTN_MSGID, lb.message, 1 );
        if ( s.isPresent() ) {
            msgid = s.get();
            if ( this.m_ld.search("msgid", msgid).isPresent() ) {
                id = this.m_ld.search("msgid", msgid).get();
                this.m_ld.addProp( id, "line", line_id );
                this.m_ld.getLogData(id).get().setTime( lb.logTime );
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
     * Dovecotログ分析
     * @param line_id
     * @param logTime
     * @param program
     * @param message
     * @return true=Dovecotログ、false=Dovecotログではない
     */
    /*
    private boolean analyzeDovecot( int line_id, LocalDateTime logTime, String program, String message ){
        Optional<String> s;
        if ( ! program.toLowerCase().equals( "dovecot" ) ) return false;

        // (除外)Feb  1 05:57:52 afef27cd6bf6 dovecot: master: Dovecot v2.3.16 (7e2e900c1a) starting up for imap, pop3, lmtp, sieve
        if ( message.startsWith( "master: Dovecot v") ) return true;

        // (除外)Feb  1 07:40:57 afef27cd6bf6 dovecot: imap-login: Login: user=<delta>, method=PLAIN, rip=172.30.0.1, lip=172.30.4.22, mpid=809, session=<W+mHyLZJFsisHgAB>
        if ( message.startsWith( "imap-login: Login:") ) return true;

        // (除外)Feb  1 07:41:37 afef27cd6bf6 dovecot: imap(delta)<846><EgPmyrZJ7NKsHgAB>: Disconnected: Logged out in=397 out=10242 deleted=0 expunged=0 trashed=0 hdr_count=17 hdr_bytes=5147 body_count=0 body_bytes=0
        if ( message.contains( "Disconnected: Logged out in") ) return true;

        // msgid / mailbox
        // Feb  1 06:06:05 afef27cd6bf6 dovecot: lda(delta)<218><sbuvCr1ufmnaAAAA8Bi3+g>: sieve: msgid=<697e6ebc.Ja2ThT4kue/ojPj8%delta@d77.jp>: fileinto action: stored mail into mailbox '10,ServerAdmin.01,jobmail'
        // Feb  1 08:45:57 afef27cd6bf6 dovecot: lda(kurio)<1640><8ntICTWUfmloBgAA8Bi3+g>: msgid=<64ef0110-b09b-4f9c-ac7b-43fb15eb18c6@r16.geese-solutions.net>: saved mail to INBOX
        s = LogPatterns.matcher( LogPatterns.PTN_MSGID, message, 1 );
        if ( s.isPresent() ) {
            // mid = s.get();
            LogData ld = this.addProp( line_id, "msgid", s.get() );
            ld.setTime(logTime);
            s = LogPatterns.matcher( LogPatterns.PTN_DOVECOT_SIEVE, message, 1 );
            if ( s.isPresent() ) {
                ld.addProp( "mailbox", s.get() );
            }
            if ( message.endsWith( ": saved mail to INBOX" ) ){
                ld.addProp( "mailbox", "INBOX" );
            }
            return true;
        }
        return true;
    }
 */
}
