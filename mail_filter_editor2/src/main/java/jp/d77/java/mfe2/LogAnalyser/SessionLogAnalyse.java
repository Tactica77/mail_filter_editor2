package jp.d77.java.mfe2.LogAnalyser;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.mfe2.Datas.RDAPCache;
import jp.d77.java.mfe2.Datas.SessionLogDatas;
import jp.d77.java.mfe2.Datas.SessionLogDatas.LogBasicData;
import jp.d77.java.mfe2.LogAnalyser.LogPatterns.HostAddress;
import jp.d77.java.mfe2.LogAnalyser.LogPatterns.MilterReject;
import jp.d77.java.mfe2.LogAnalyser.LogPatterns.RelayStatus;
import jp.d77.java.tools.BasicIO.Debugger;

public class SessionLogAnalyse {
    private SessionLogDatas     m_slog;
    public record splResult(Integer id, String line) {}

    public SessionLogAnalyse( SessionLogDatas slog ){
        this.m_slog = slog;
    }

    public void analyseLog( LocalDate targetDate, Mfe2Config cfg ){
        Debugger.TracePrint();
        RDAPCache cache = new RDAPCache( cfg );
        cache.load( targetDate );
        cache.rdap_get_flag( true );

        for ( String line: this.m_slog.getTempData() ){
            splResult spl = this.splitSessionLog(targetDate, line).orElse(null);
            if ( spl == null ) continue;
            LogBasicData lb = this.m_slog.setLogBasic( spl.id(), targetDate, spl.line() ).orElse(null);
            //LogBasicData lb = this.m_slog.SessionLog2LogBasic(targetDate, line).orElse(null);
            if ( lb == null ) continue;

            int id = lb.id();
            this.m_slog.newLogData( lb.id() );
            this.m_slog.addLog(id, lb);

            if ( this.detailPostfixSmtpd( id, lb ) ) {
                //continue;
            }else if ( this.detailPostfixAny( id, lb ) ) {
                //continue;
            }else if ( this.detailDovecot( id, lb ) ) {
                //continue;
            }

            for ( String ip: this.m_slog.getPropS( id, "ip" ) ){
                cache.getRDAP( ip );
            }
        }
        cache.save();
    }

    private Optional<splResult> splitSessionLog( LocalDate targetDate, String line ){
        int id;
        String res[] = line.split("<<->>");
        if ( res.length != 2 ) Optional.empty();
        try {
            id = Integer.parseInt( res[0] );
            return Optional.ofNullable( new splResult(id, res[1]) );
        } catch ( NumberFormatException e ){
            return Optional.empty();
        }
    }

    /**
     * 詳細データ取得(postfix/smtpd)
     * @param id
     * @param lb
     * @return
     */
    private boolean detailPostfixSmtpd( int id, LogBasicData lb ){
        if ( ! lb.program().toLowerCase().equals( "postfix/smtpd" )
            && !lb.program().toLowerCase().equals( "postfix/submission/smtpd" )
            ) return false;
        Optional<String> s;
        Optional<HostAddress> ha;

        // Queue
        // Feb  4 11:37:07 afef27cd6bf6 postfix/smtpd[14562]: 85C9E81BA4BE: client=fgmailer.com[35.187.195.50]
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message(), 1 );
        if ( s.isPresent() ) {
            this.m_slog.addProp( lb.logTime(), id, "postfix_queue", s.get() );
        }

        // FQDN and IP
        ha = LogPatterns.extractHostAddress( lb.message() );
        if ( ha.isPresent() ){
            this.m_slog.addProp( lb.logTime(), id, "ip", ha.get().ip() );
            this.m_slog.addProp( lb.logTime(), id, "fqdn", ha.get().fqdn() );
        }

        if ( lb.message().startsWith( "connect from " ) ){
            // Feb  4 11:37:07 afef27cd6bf6 postfix/smtpd[14562]: connect from fgmailer.com[35.187.195.50]
            // 処理なし
            return true;
        }

        if ( lb.message().startsWith( "disconnect from " ) ){
            // disconnect from unknown[192.168.1.210] helo=1 mail=1 rcpt=1 data=1 quit=1 commands=5
            String[] tokens = lb.message().split("\\s+");
            int index = lb.message().indexOf( tokens[3] ); 
            if (index == -1) return true;
            this.m_slog.addProp( lb.logTime(), id, "disconnect", lb.message().substring( index ) );
            return true;
        }

        if ( lb.message().startsWith( "warning: hostname " ) ){
            // warning: hostname 31-13-232-165.clnet.aesbg.net does not resolve to address 31.13.232.165: Name or service not known
            Matcher m = LogPatterns.PTN_POSFTIX_SMTPD_RESOLVEADDR.matcher( lb.message() );
            if (m.find()) {
                this.m_slog.addProp( lb.logTime(), id, "ip", m.group(2) );
                this.m_slog.addProp( lb.logTime(), id, "fqdn", m.group(1) );
            }
            if ( lb.message().contains("does not resolve to address") ){
                //this.m_slog.addProp( lb.logTime(), id, "relay_status", "not resolve" );
                this.m_slog.addProp( lb.logTime(), id, "error", "not resolve" );
            }
        }

        if ( lb.message().startsWith( "NOQUEUE: reject: " ) ){
            // NOQUEUE: reject: RCPT from unknown[181.229.197.169]: 450 4.7.25 Client host rejected: cannot find your hostname, [181.229.197.169]; from=<admin@d77.jp> to=<admin@d77.jp> proto=ESMTP helo=<smtpclient.apple>
            s = LogPatterns.matcher( LogPatterns.PTN_NOQUEUE_CODE, lb.message(), 1 );
            if ( s.isPresent() ) this.m_slog.addProp( lb.logTime(), id, "relay_status", s.get() );
            s = LogPatterns.matcher( LogPatterns.PTN_NOQUEUE_ERROR, lb.message(), 1 );
            if ( s.isPresent() ) this.m_slog.addProp( lb.logTime(), id, "error", s.get() );
        }
        if ( lb.message().startsWith( "warning: non-SMTP command " ) ){
            // warning: non-SMTP command from scan-59a.shadowserver.org[65.49.1.108]: GET / HTTP/1.1
            this.m_slog.addProp( lb.logTime(), id, "relay_status", "none-smtp" );
        }

        if ( lb.message().startsWith( "improper command pipelining " ) ){
            // improper command pipelining after CONNECT from unknown[195.3.222.78]:
            this.m_slog.addProp( lb.logTime(), id, "relay_status", "improper-cmd" );
        }

        if ( lb.message().startsWith( "timeout after " ) ){
            // timeout after CONNECT from unknown[197.205.240.221]
            this.m_slog.addProp( lb.logTime(), id, "relay_status", "timeout" );
        }


        if ( lb.message().startsWith( "timeout after " )
            // timeout after CONNECT from unknown[197.205.240.221]
            || lb.message().startsWith( "lost connection after " )
            // lost connection after DATA from unknown[181.229.197.169]
            || lb.message().startsWith( "improper command pipelining after " )
            // improper command pipelining after CONNECT from unknown[195.3.222.78]:
            || lb.message().startsWith( "SSL_accept error " )
            // SSL_accept error from unknown[185.93.89.95]: -1
            ){
            int index = lb.message().indexOf( " from " ); 
            if (index == -1) return true;
            this.m_slog.addProp( lb.logTime(), id, "error", lb.message().substring( 0, index ) );
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
    private boolean detailPostfixAny( int id, LogBasicData lb ){
        if ( ! lb.program().toLowerCase().equals( "postfix/cleanup" )
            &&  ! lb.program().toLowerCase().equals( "postfix/qmgr" )
            &&  ! lb.program().toLowerCase().equals( "postfix/local" )
            &&  ! lb.program().toLowerCase().equals( "postfix/smtp" )
            ) return false;

        Optional<String> s;
        Optional<HostAddress> ha;

        // Queue
        // Feb 1 00:10:08 afef27cd6bf6 postfix/cleanup[41622]: 22A6581B3AE2: message-id=<20260131151008.37vQF%root@dms.d77.jp>
        // Feb 1 00:00:18 afef27cd6bf6 postfix/cleanup[41559]: 05D2881B3AE2: milter-reject: END-OF-MESSAGE from ab218251.f.west.v6connect.net[183.76.218.251]: 5.7.1 Spam message rejected; from= to= proto=ESMTP helo=
        s = LogPatterns.matcher( LogPatterns.PTN_QUEUEID, lb.message(), 1 );
        if ( s.isPresent() ) {
            this.m_slog.addProp( lb.logTime(), id, "postfix_queue", s.get() );
        }

        // FQDN and IP
        ha = LogPatterns.extractHostAddress( lb.message() );
        if ( ha.isPresent() ){
            this.m_slog.addProp( lb.logTime(), id, "ip", ha.get().ip() );
            this.m_slog.addProp( lb.logTime(), id, "fqdn", ha.get().fqdn() );
        }

        // message-id
        s = LogPatterns.matcher( LogPatterns.PTN_MESSAGEID, lb.message(), 1 );
        if ( s.isPresent() ) this.m_slog.addProp( lb.logTime(), id, "msgid", s.get() );
        s = LogPatterns.matcher( LogPatterns.PTN_MSGID, lb.message(), 1 );
        if ( s.isPresent() ) this.m_slog.addProp( lb.logTime(), id, "msgid", s.get() );

        // From
        s = LogPatterns.matcher( LogPatterns.PTN_ADDRESS_FROM, lb.message(), 1 );
        if ( s.isPresent() ) this.m_slog.addProp( lb.logTime(), id, "from", s.get() );

        // To
        s = LogPatterns.matcher( LogPatterns.PTN_ADDRESS_TO, lb.message(), 1 );
        if ( s.isPresent() ) this.m_slog.addProp( lb.logTime(), id, "to", s.get() );

        // MilterReject
        if ( lb.message().contains( "milter-reject: " ) ){
            Optional<MilterReject> mr;
            mr = LogPatterns.extractMilterReject( lb.message() );
            if ( mr.isPresent() ){
                this.m_slog.addProp( lb.logTime(), id, "error", mr.get().status() + " " + mr.get().code() );
            }
            if ( lb.message().contains( "5.7.1 Spam message rejected" ) ){
                this.m_slog.addProp( lb.logTime(), id, "relay_status", "SPAM" );
            }
            if ( lb.message().contains( "4.7.1 Try again later" ) ){
                this.m_slog.addProp( lb.logTime(), id, "relay_status", "soft reject" );
            }
        }

        // relay
        if ( lb.program().equals( "postfix/local" )
            || lb.program().equals( "postfix/smtp" ) ){
            Boolean relay_local = null;
            s = LogPatterns.matcher( LogPatterns.PTN_RELAY_TO, lb.message(), 1 );
            if ( s.isPresent() ) {
                this.m_slog.addProp( lb.logTime(), id, "relay", s.get() );
                if ( s.get().equals( "local" ) ) relay_local = true;
                else relay_local = false;
            }

            Optional<RelayStatus> rs;
            rs = LogPatterns.extractRelayStatus( lb.message() );
            if ( rs.isPresent() ){
                if ( relay_local == null ){
                    this.m_slog.addProp( lb.logTime(), id, "relay_status", "send null" );
                }else if ( relay_local ){
                    this.m_slog.addProp( lb.logTime(), id, "relay_status", "send local" );
                }else{
                    this.m_slog.addProp( lb.logTime(), id, "relay_status", "send remote" );
                }
                this.m_slog.addProp( lb.logTime(), id, "relay_detail", rs.get().detail() );
            }
        }
        return true;
    }

    private boolean detailDovecot( int id, LogBasicData lb ){
        if ( ! lb.program().toLowerCase().equals( "dovecot" ) ) return false;

        Optional<String> s;
        //Optional<HostAddress> ha;
        s = LogPatterns.matcher( LogPatterns.PTN_DOVECOT_SIEVE, lb.message(), 1 );
        if ( s.isPresent() ) {
            this.m_slog.addProp( lb.logTime(), id, "mailbox", s.get() );
        }
        if ( lb.message().endsWith( ": saved mail to INBOX" ) ){
            this.m_slog.addProp( lb.logTime(), id, "mailbox", "INBOX" );
        }

        return true;
    }
}
