package jp.d77.java.mfe2.LogAnalyser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.d77.java.mfe2.BasicIO.Mfe2Config;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolDate;

public class RspamdLog {
    public class RspamdLogData{
        public LocalDateTime   m_datetime;
        public Map<String,String> m_props = new HashMap<>();
        public String m_spam_check_string = "";

        public String score_flag = "";
        public String score_action = "";     // no action / reject / soft reject
        public Float score_value = 0.0f;      // -0.10 

        public Map<String,score_detail> score = new HashMap<>();
        public void push_score(  String key, String value, String data  ){
            try{
                Float v = Float.parseFloat( value );
                score.put(key, new score_detail(v, data) );
            }catch( Exception e){}
        }

        public record score_detail ( Float value, String data ) {}

        public String[] getScoreDetail(){
            List<String> res = new ArrayList<>();
            for ( String key: this.score.keySet() ){
                res.add( key + "=(" + this.score.get(key).value + ") " + this.score.get(key).data );
            }
            return res.stream().sorted().toArray( String[]::new );
        }

        public String getScoreResult(){
            return this.score_value + "(" + this.score_action + ")";
        }
    }

    private Mfe2Config          m_cfg;
    private List<String>        m_log;
    private List<RspamdLogData> m_datas;
    private final Pattern HEADER_PATTERN = Pattern.compile( "^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}).*?id: <(.*?)>" );
    private final Pattern DEFAULT_HEADER_PATTERN = Pattern.compile( "^\\(default: ([A-Z]) \\((.*?)\\): \\[([-0-9.]+)/[0-9.]+\\] \\[(.*)\\]\\)$" );
    private final Pattern RULE_PATTERN = Pattern.compile( "^([A-Z0-9_]+)\\(([-0-9.]+)\\)\\{(.*)\\}$" );

    public RspamdLog ( Mfe2Config cfg ){
        this.m_cfg = cfg;
        this.m_datas = new ArrayList<>();
    }

    public Optional<RspamdLogData> getData( String qid, String mid ){
        for( RspamdLogData rld: this.m_datas ){
            if( mid != null
                && rld.m_props.containsKey( "mid" )
                && rld.m_props.get( "mid" ).equals( mid )
                ) return Optional.ofNullable( rld );
            if( qid != null
                && rld.m_props.containsKey( "qid" )
                && rld.m_props.get( "qid" ).equals( qid )
                ) return Optional.ofNullable( rld );
        }
        return Optional.empty();
    }

    private void analyseLog(){
        for( String line: this.m_log ){
            Matcher m = HEADER_PATTERN.matcher(line);
            if (!m.find()) continue;

            // Date / Time
            LocalDate date = ToolDate.Str2LocalDate( m.group(1) ).orElse(null);
            LocalTime time = ToolDate.Str2LocalTime( m.group(2) ).orElse(null);
            if ( date == null || time == null ) continue;
            RspamdLogData rld = new RspamdLogData();
            rld.m_datetime = LocalDateTime.of(date, time);

            // MID
            rld.m_props.put( "mid", m.group(3) );

            int idEnd = line.indexOf(">", line.indexOf("id: <"));
            if (idEnd == -1) continue;
            String rest = line.substring(idEnd + 2); // ", " をスキップ

            // Split
            for (String part : smartSplit(rest) ) {
                part = part.trim();

                // (default: ... ) のような括弧全体
                if (part.startsWith("(") && part.endsWith(")")) {
                    rld.m_spam_check_string = part;
                    this.analyseScore( part, rld );
                    continue;
                }

                int idx = part.indexOf(": ");
                if (idx == -1) continue;

                String key = part.substring(0, idx);
                String value = this.trimAngle( part.substring(idx + 2) );

                rld.m_props.put(key, value);
            }

            this.m_datas.add(rld);
        }
    }

    private void analyseScore( String defaultBlock, RspamdLogData rld ){
        Matcher m = DEFAULT_HEADER_PATTERN.matcher(defaultBlock);
        if (!m.find()) return;

        rld.score_flag = m.group(1);       // F / T
        rld.score_action = m.group(2);     // no action / reject / soft reject
        try {
            rld.score_value = Float.parseFloat( m.group(3) );      // -0.10
        }catch( Exception e ){}
        
        String rulesPart = m.group(4);  // 中の配列
        List<String> rules = this.splitRules(rulesPart);

        for (String rule : rules) {
            Matcher rm = RULE_PATTERN.matcher(rule.trim());
            if (!rm.find()) continue;
            rld.push_score(
                rm.group(1)
                ,rm.group(2)
                ,rm.group(3)
            );
        }
    }

    private List<String> splitRules(String s) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int brace = 0;  // {}
        int paren = 0;  // ()

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '{') brace++;
            else if (c == '}') brace--;
            else if (c == '(') paren++;
            else if (c == ')') paren--;

            if (c == ',' && brace == 0 && paren == 0) {
            //if (c == ',' && i + 1 < s.length() && s.charAt(i + 1) == ' '
            //        && brace == 0 && paren == 0) {

                result.add(current.toString());
                current.setLength(0);
                i++;
                continue;
            }

            current.append(c);
        }

        result.add(current.toString());
        return result;
    }

    /**
     * <>を除去
     * @param s
     * @return
     */
    private String trimAngle(String s) {
        if (s.startsWith("<") && s.endsWith(">")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * １行のログを分割する
     * @param s
     * @return
     */
    private List<String> smartSplit(String s) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int paren = 0;   // ()
        int bracket = 0; // []

//    MIME_GOOD(-0.10){text/plain;}
//	,ALIAS_RESOLVED(0.00){}

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '(') paren++;
            else if (c == ')') paren--;
            else if (c == '[') bracket++;
            else if (c == ']') bracket--;

            // 区切り判定
            if (c == ',' && i + 1 < s.length() && s.charAt(i + 1) == ' '
                    && paren == 0 && bracket == 0) {

                result.add(current.toString());
                current.setLength(0);
                i++; // skip space
                continue;
            }

            current.append(c);
        }

        result.add(current.toString());
        return result;
    }

    /**
     * ログ読み込み
     * @param cfg
     * @param targetDate
     */
    public void Load( LocalDate targetDate ){
        Debugger.TracePrint();

        this.m_log = new ArrayList<String>();
        Path logDir = Paths.get( this.m_cfg.getLogFilePath() + "dms27-rspamd/rspamd/");

        Optional<Path> latestRotated = this.findLatestRotated(logDir);
        Path rspamdlog = logDir.resolve("rspamd.log");

        if (latestRotated.isPresent()) {
            this.m_cfg.addAlertInfo( "Load log=" + latestRotated.get().getFileName() );
            Debugger.InfoPrint( "Loaded log line=" + latestRotated.get().getFileName() );
            this.processFile(latestRotated.get(), targetDate);
        }

        if (Files.exists(rspamdlog)) {
            this.m_cfg.addAlertInfo( "Load log=" + rspamdlog.getFileName() );
            this.processFile(rspamdlog, targetDate);
        }
        this.m_cfg.addAlertInfo( "Loaded line=" + this.m_log.size() );
        Debugger.InfoPrint( "Loaded line=" + this.m_log.size() );

        this.analyseLog();
    }

/*
通常受信
[root@dms rspamd]# grep 465A3817E4B0 rspamd.log
2026-03-20 08:09:40 #38(normal) <73b3a9>; task; rspamd_message_parse: loaded message; id: <20260320080939.12789785750137@10.163.48.23.sbisec.co.jp>; queue-id: <465A3817E4B0>; size: 5936; checksum: <a468b427a30edc7c77e88c0dbc77b128>
2026-03-20 08:09:41 #38(normal) <73b3a9>; task; rspamd_task_write_log: id: <20260320080939.12789785750137@10.163.48.23.sbisec.co.jp>, qid: <465A3817E4B0>, ip: 103.71.158.4, from: <alert_error@sbisec.co.jp>, (default: F (no action): [-0.50/15.00] [DMARC_POLICY_ALLOW(-0.50){sbisec.co.jp;reject;},FORGED_SENDER(0.30){root@sbisec.co.jp;alert_error@sbisec.co.jp;},ONCE_RECEIVED(0.20){},R_DKIM_ALLOW(-0.20){sbisec.co.jp:s=p20190830;},R_SPF_ALLOW(-0.20){+ip4:103.71.158.0/24;},MIME_GOOD(-0.10){text/plain;},ALIAS_RESOLVED(0.00){},ARC_NA(0.00){},ASN(0.00){asn:133203, ipnet:103.71.158.0/24, country:JP;},DBL_BLOCKED_OPENRESOLVER(0.00){10.163.48.23.sbisec.co.jp:mid;sbisec.co.jp:dkim;sbisec.co.jp:from_mime;sbisec.co.jp:from_smtp;sbisec.co.jp:replyto;sbisec.co.jp:url;psmtp004.sbisec.co.jp:helo;psmtp004.sbisec.co.jp:rdns;},DKIM_TRACE(0.00){sbisec.co.jp:+;},DWL_DNSWL_BLOCKED(0.00){sbisec.co.jp:dkim;},FROM_HAS_DN(0.00){},FROM_NEQ_ENVFROM(0.00){root@sbisec.co.jp;alert_error@sbisec.co.jp;},HAS_REPLYTO(0.00){info@sbisec.co.jp;},LOCAL_INBOUND(0.00){},MID_RHS_MATCH_FROMTLD(0.00){},MIME_TRACE(0.00){0:+;},MISSING_XM_UA(0.00){},PREVIOUSLY_DELIVERED(0.00){delta@d77.jp;},RBL_SPAMHAUS_BLOCKED_OPENRESOLVER(0.00){103.71.158.4:from;},RCPT_COUNT_ONE(0.00){1;},RCVD_COUNT_ONE(0.00){1;},RCVD_TLS_LAST(0.00){},REPLYTO_ADDR_EQ_FROM(0.00){},REPLYTO_DOM_NEQ_TO_DOM(0.00){},TO_DN_NONE(0.00){},TO_MATCH_ENVRCPT_ALL(0.00){}]), len: 5936, time: 952.934ms, dns req: 42, digest: <7649050532bae696eddd51d6c65ae37b>, rcpts: <delta@d77.jp>, mime_rcpts: <delta@d77.jp>


通常送信
[root@dms rspamd]# grep '5358D817E4B0' rspamd.log
2026-03-20 08:37:51 #37(normal) <7d7d2e>; task; rspamd_message_parse: loaded message; id: <27e86f506a4881125fff808a5622e098@d77.jp>; queue-id: <5358D817E4B0>; size: 428; checksum: <c9f5b87b73aa76774ed12418f92146fd>
2026-03-20 08:37:51 #37(normal) <7d7d2e>; task; rspamd_task_write_log: id: <27e86f506a4881125fff808a5622e098@d77.jp>, qid: <5358D817E4B0>, ip: 172.30.0.1, user: delta, from: <delta@d77.jp>, (default: F (no action): [-0.10/15.00] [MIME_GOOD(-0.10){text/plain;},ALIAS_RESOLVED(0.00){},ARC_NA(0.00){},DKIM_SIGNED(0.00){d77.jp:s=202601;},FREEMAIL_ENVRCPT(0.00){gmail.com;},FREEMAIL_TO(0.00){gmail.com;},FROM_EQ_ENVFROM(0.00){},FROM_HAS_DN(0.00){},LOCAL_OUTBOUND(0.00){},MID_RHS_MATCH_FROM(0.00){},MIME_TRACE(0.00){0:+;},MISSING_XM_UA(0.00){},RCPT_COUNT_ONE(0.00){1;},RCVD_COUNT_ZERO(0.00){0;},TO_DN_NONE(0.00){},TO_MATCH_ENVRCPT_ALL(0.00){}]), len: 428, time: 317.656ms, dns req: 11, digest: <978ea82e6183d74b48d5ee627e8bc807>, rcpts: <colonel.wohc@gmail.com,colonelwohc@gmail.com>, mime_rcpts: <colonel.wohc@gmail.com,>


SPAM
[root@dms rspamd]# grep D0773817F93C rspamd.log
2026-03-20 03:22:17 #38(normal) <a91f69>; task; rspamd_message_parse: loaded message; id: <002991.002991@72496.com>; queue-id: <D0773817F93C>; size: 2675; checksum: <582ff03a5cbdc89b93bbbfacdc05b5ea>
2026-03-20 03:22:18 #38(normal) <a91f69>; task; rspamd_task_write_log: id: <002991.002991@72496.com>, qid: <D0773817F93C>, ip: 2.134.10.187, from: <JohnGreen67947@gmail.com>, (default: T (reject): [26.79/15.00] [LEAKED_PASSWORD_SCAM(7.00){},ONCE_RECEIVED_STRICT(4.00){dynamic;},VIOLATED_DIRECT_SPF(3.50){},HFILTER_HELO_BAREIP(3.00){2.134.10.187;1;},HFILTER_HOSTNAME_5(3.00){2.134.10.187.dynamic.telecom.kz;},SUBJ_ALL_CAPS(2.10){28;},RBL_MAILSPIKE_WORST(2.00){2.134.10.187:from;},CT_EXTRA_SEMI(1.00){},MV_CASE(0.50){},R_MISSING_CHARSET(0.50){},DMARC_POLICY_SOFTFAIL(0.10){gmail.com : No valid SPF, No valid DKIM;none;},MIME_GOOD(-0.10){text/plain;},ONCE_RECEIVED(0.10){},RCVD_NO_TLS_LAST(0.10){},ARC_NA(0.00){},ASN(0.00){asn:9198, ipnet:2.134.8.0/22, country:KZ;},BITCOIN_ADDR(0.00){18zeXFVpsVbgikMJCGasA1R4J48HLjUiNL;},FREEMAIL_ENVFROM(0.00){gmail.com;},FREEMAIL_FROM(0.00){gmail.com;},FROM_EQ_ENVFROM(0.00){},FROM_HAS_DN(0.00){},LEAKED_PASSWORD_SCAM_RE(0.00){},LOCAL_INBOUND(0.00){},MIME_TRACE(0.00){0:+;},MISSING_XM_UA(0.00){},RCPT_COUNT_ONE(0.00){1;},RCVD_COUNT_ONE(0.00){1;},R_DKIM_NA(0.00){},R_SPF_SOFTFAIL(0.00){~all;},SUBJECT_ENDS_EXCLAIM(0.00){},TO_DN_NONE(0.00){},TO_MATCH_ENVRCPT_ALL(0.00){}]), len: 2675, time: 1093.702ms, dns req: 44, digest: <582ff03a5cbdc89b93bbbfacdc05b5ea>, rcpts: <delta@d77.jp>, mime_rcpts: <delta@d77.jp>


soft reject
[root@dms rspamd]# grep CB13E817F93F rspamd.log
2026-03-19 19:26:54 #38(normal) <f829e7>; task; rspamd_message_parse: loaded message; id: <1773916012500231572.1f6928e47afd@mtaat40.bnmjkli.com>; queue-id: <CB13E817F93F>; size: 4148; checksum: <c36cb18b6a660dc16647eebdb65d91b6>
2026-03-19 19:26:55 #38(normal) <f829e7>; task; rspamd_task_write_log: id: <1773916012500231572.1f6928e47afd@mtaat40.bnmjkli.com>, qid: <CB13E817F93F>, ip: 34.16.172.18, from: <noreply-ogawa2501=d77.jp@mtaat40.bnmjkli.com>, (default: F (soft reject): [5.40/15.00] [ABUSE_SURBL(5.00){midccd.com:url;},MIME_HTML_ONLY(0.20){},ONCE_RECEIVED(0.20){},R_DKIM_ALLOW(-0.20){mtaat40.bnmjkli.com:s=mtaat40;},BAD_REP_POLICIES(0.10){},MIME_BASE64_TEXT(0.10){},ARC_NA(0.00){},ASN(0.00){asn:396982, ipnet:34.16.128.0/17, country:US;},DBL_BLOCKED_OPENRESOLVER(0.00){midccd.com:url;mtaat40.bnmjkli.com:dkim;mtaat40.bnmjkli.com:helo;mtaat40.bnmjkli.com:mid;mtaat40.bnmjkli.com:from_mime;mtaat40.bnmjkli.com:rdns;mtaat40.bnmjkli.com:from_smtp;},DKIM_MIXED(0.00){},DKIM_TRACE(0.00){mtaat40.bnmjkli.com:+;mtaat40.bnmjkli.com:~;},DMARC_POLICY_ALLOW(0.00){mtaat40.bnmjkli.com;quarantine;},DNSWL_BLOCKED(0.00){195.244.110.171:received;},FORGED_SENDER_VERP_SRS(0.00){},FROM_HAS_DN(0.00){},FROM_NEQ_ENVFROM(0.00){noreply@mtaat40.bnmjkli.com;noreply-ogawa2501=d77.jp@mtaat40.bnmjkli.com;},GREYLIST(0.00){greylisted;Thu, 19 Mar 2026 10:31:55 GMT;new record;},LOCAL_INBOUND(0.00){},MID_RHS_MATCH_FROM(0.00){},MIME_TRACE(0.00){0:~;},MISSING_XM_UA(0.00){},PREVIOUSLY_DELIVERED(0.00){ogawa2501@d77.jp;},RBL_SPAMHAUS_BLOCKED_OPENRESOLVER(0.00){34.16.172.18:from;},RCPT_COUNT_ONE(0.00){1;},RCVD_COUNT_ONE(0.00){1;},RCVD_TLS_ALL(0.00){},RCVD_VIA_SMTP_AUTH(0.00){},RECEIVED_SPAMHAUS_BLOCKED_OPENRESOLVER(0.00){195.244.110.171:received;},R_DKIM_PERMFAIL(0.00){mtaat40.bnmjkli.com:s=mail;},R_SPF_ALLOW(0.00){+a;},TO_DN_NONE(0.00){},TO_MATCH_ENVRCPT_ALL(0.00){}]), len: 4148, time: 1181.880ms, dns req: 39, digest: <c36cb18b6a660dc16647eebdb65d91b6>, rcpts: <ogawa2501@d77.jp>, mime_rcpts: <ogawa2501@d77.jp>, forced: soft reject "Try again later"; score=nan (set by greylist)
*/

/*
(default: F (no action): [-0.50/15.00]
(default: F (no action): [-0.10/15.00]
(default: T (reject): [26.79/15.00]
(default: F (soft reject): [5.40/15.00]

(
default: F (no action): [-0.10/15.00] [
	MIME_GOOD(-0.10){text/plain;}
	,ALIAS_RESOLVED(0.00){}
	,ARC_NA(0.00){}
	,DKIM_SIGNED(0.00){d77.jp:s=202601;}
	,FREEMAIL_ENVRCPT(0.00){gmail.com;}
	,FREEMAIL_TO(0.00){gmail.com;}
	,FROM_EQ_ENVFROM(0.00){}
	,FROM_HAS_DN(0.00){}
	,LOCAL_OUTBOUND(0.00){}
	,MID_RHS_MATCH_FROM(0.00){}
	,MIME_TRACE(0.00){0:+;}
	,MISSING_XM_UA(0.00){}
	,RCPT_COUNT_ONE(0.00){1;}
	,RCVD_COUNT_ZERO(0.00){0;}
	,TO_DN_NONE(0.00){}
	,TO_MATCH_ENVRCPT_ALL(0.00){}
	]
)
*/

    /**
     * ログ読み込み
     * @param file
     * @param targetDate
     */
    private void processFile(Path file, LocalDate targetDate) {
        String target_date = ToolDate.Format( targetDate, "uuuu-MM-dd" ).orElse("???");

        try  {
            BufferedReader br = Files.newBufferedReader(file);
            String line;
            while ((line = br.readLine()) != null) {
                // 明らかに短い文字の入った行は見ない
                if (line.length() < 15) continue;

                // 「; task; rspamd_task_write_log:」の無い行は無視
                if ( !line.contains( "; task; rspamd_task_write_log: " ) ) continue;

                // 先頭のyyyy-MM-ddが調査対象の日時に一致しなければ見ない
                if ( ! line.substring(0, 10).equals( target_date ) ) continue;

                // "^(\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}).*?id: <(.*?)>"にマッチした行のみ取得
                Matcher m = HEADER_PATTERN.matcher(line);
                if (!m.find()) continue;
                this.m_log.add( line );
            }
            br.close();

            Debugger.InfoPrint( "loaded file=" + file + " date=" + targetDate );
        } catch ( IOException e ){
            this.m_cfg.addAlertError( "processFile Error file=" + file + " " + e.getMessage() );
            Debugger.ErrorPrint( "file=" + file + " " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private record LogFile(String name, LocalDate date) {}    
    
    /**
     * ログファイル検索
     * @param dir
     * @return
     */
    public Optional<Path> findLatestRotated(Path dir) {
        Pattern ROTATED_PATTERN = Pattern.compile("^rspamd.log-(\\d{8})$");
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
}
