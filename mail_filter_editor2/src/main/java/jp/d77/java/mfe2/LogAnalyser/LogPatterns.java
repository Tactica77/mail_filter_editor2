package jp.d77.java.mfe2.LogAnalyser;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogPatterns {
    // ログの日時形式に合わせたLocalDateTime用フォーマット
    public static DateTimeFormatter FMT_LOG_DATETIME = DateTimeFormatter.ofPattern("yyyy MMM d HH:mm:ss", Locale.ENGLISH);

    // 正規表現 -------------------------------------------------

    // ログの「Feb 6 00:10:08 afef27cd6bf6 postfix/smtpd[32043]: 」の部分とそれ以降を分割する。
    public static final Pattern PTN_LOGBASIC = Pattern.compile( "^(\\w{3}\\s+\\d+\\s+\\d{2}:\\d{2}:\\d{2})\\s+\\S+\\s+([^:\\[]+)(?:\\[(\\d+)])?:\\s+(.*)$" );

    // msgid=<20260205213106.JG0h6%delta@d77.jp>からmessage-idを取得
    public static final Pattern PTN_MSGID = Pattern.compile("\\smsgid=<([^<>]+)>");

    // message-id=<20260205213106.JG0h6%delta@d77.jp>からmessage-idを取得
    public static final Pattern PTN_MESSAGEID = Pattern.compile("\\smessage-id=<([^<>]+)>");

    // stored mail into mailbox '01,Gmail'からメールボックス名を取得
    public static final Pattern PTN_DOVECOT_SIEVE = Pattern.compile(":\\s+stored\\s+mail\\s+into\\s+mailbox\\s+'([^']+)'");

    // キューIDを取得する
    public static final Pattern PTN_QUEUEID = Pattern.compile("^([A-F0-9]+):\\s+");

    // From/Toアドレス
    public static final Pattern PTN_ADDRESS_FROM = Pattern.compile(" from=<([^>]+)>(?=[,\\s]|$)");
    public static final Pattern PTN_ADDRESS_TO = Pattern.compile(" to=<([^>]+)>(?=[,\\s]|$)");

    // 「warning: hostname adsl.viettel.vn does not resolve to address 115.76.50.243」からホスト名とIPを取得する
    public static final Pattern PTN_POSFTIX_SMTPD_RESOLVEADDR = Pattern.compile("warning:\\s+hostname\\s+(\\S+)\\s+does not resolve to address\\s+(\\S+):\\s+");

    // NOQUEUEエラー取得
    public static final Pattern PTN_NOQUEUE_CODE = Pattern.compile("^NOQUEUE:.*?:\\s+([45]\\d{2})\\s");
    public static final Pattern PTN_NOQUEUE_ERROR = Pattern.compile("^NOQUEUE:.*?:\\s+([45]\\d{2}[^;]*)");

    // relay取得
    public static final Pattern PTN_RELAY_TO = Pattern.compile(", relay=([^,]+), ");

    public static Optional<String> matcher( Pattern p, String msg, int idx ){
        Matcher m = p.matcher( msg );
        if (m.find()) {
            return Optional.ofNullable( m.group(idx ) );
        }
        return Optional.empty();
    }

    // ホストアドレス取得用
    //  122x218x100x90.ap122.ftth.ucom.ne.jp[122.218.100.90] 
    //  client=122x218x100x90.ap122.ftth.ucom.ne.jp[122.218.100.90]
    //  122x218x100x90.ap122.ftth.ucom.ne.jp[122.218.100.90]:

    private static final Pattern FQDN_IP_PATTERN =
        Pattern.compile(
            "(?:^|\\s)(?:client=)?([^\\[\\s:]+)\\[([0-9a-fA-F:.]+)](?=\\s|:|$)"
        );    
    public record HostAddress(String fqdn, String ip) {}
    public static Optional<HostAddress> extractHostAddress(String line) {
        Matcher m = FQDN_IP_PATTERN.matcher(line);
        if (m.find()) {
            return Optional.of(new HostAddress(m.group(1), m.group(2)));
        }
        return Optional.empty();
    }

    // milter-reject取得用
    // milter-reject: END-OF-MESSAGE from FQDN: 4.7.1 Try again later; ～
    public static final Pattern PTN_MILTER_REJECT = Pattern.compile(": milter-reject:\\s+(\\S+)\\s+from\\s+.*?:\\s+([^;]+);");
    public record MilterReject(String status, String code) {}
    public static Optional<MilterReject> extractMilterReject(String line) {
        Matcher m = PTN_MILTER_REJECT.matcher(line);
        if (m.find()) {
            return Optional.of(new MilterReject(m.group(1), m.group(2)));
        }
        return Optional.empty();
    }

    // status
    public static final Pattern PTN_RELAY_STATUS = Pattern.compile(" status=([^\\s(]+)\\s*\\(([^)]+)\\)");
    public record RelayStatus(String status, String detail) {}
    public static Optional<RelayStatus> extractRelayStatus(String line) {
        Matcher m = PTN_RELAY_STATUS.matcher(line);
        if (m.find()) {
            return Optional.of(new RelayStatus(m.group(1), m.group(2)));
        }
        return Optional.empty();
    }
}
