package jp.d77.java.mfe2.Datas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.d77.java.mfe2.BasicIO.ToolNet;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolDate;

public class BlockDatas {
    public static class Record {
        public boolean enabled;
        public String ip;
        public LocalDate date;
        public String cc;
        public String org;

        public Record(boolean enabled, String ip, LocalDate date, String cc, String org) {
            this.enabled = enabled;
            this.ip = ip;
            this.date = date;
            this.cc = cc;
            this.org = org;
        }
    }    

    private static final Pattern LINE_PATTERN = Pattern.compile(
        "^(#)?\\s*" +                                 // disable
        "([^\\s#]+)\\s+" +                            // IP
        "#\\s+" +                                     // required #
        "(\\d{8})" +                                  // yyyyMMdd
        "(?:\\s+([A-Z]{2}))?" +                       // CC (optional)
        "(?:\\s+(.*))?$"                              // ORG
    );
    private final List<Record> m_records = new ArrayList<>();

    /**
     * アイテム追加
     * @param rec
     * @return false = 被ってる
     */
    public boolean add( Record rec ){
        for ( Record r: this.m_records ){
            if ( rec.ip.equals( r.ip ) ) {
                return false;
            }
        }
        this.m_records.add(rec);
        return true;
    }

    /**
     * アイテム削除
     * @param cidr
     * @return true = 削除成功、false = 無い?
     */
    public boolean remove( String cidr ){
        for ( Record r: this.m_records ){
            if ( cidr.equals( r.ip ) ) {
                this.m_records.remove( r );
                return true;
            }
        }
        return false;
    }

    public Optional<Record> getRecord( String cidr ){
        for ( Record r: this.m_records ){
            if ( cidr.equals( r.ip ) ) return Optional.ofNullable( r );
        }
        return Optional.empty();
    }
    
    public String[] getCidrs(){
        List<String> ret = new ArrayList<>();

        for ( Record r: this.m_records ){
            ret.add( r.ip );
        }
        return ret.toArray( new String[0] );
    }

    /**
     * Load Black List
     * @param filename
     * @return
     */
    public boolean load(String filename) {
        Debugger.TracePrint();
        m_records.clear();
        int lc = 0;
        int vc = 0;

        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(filename));
        }catch ( IOException e ) {
            Debugger.ErrorPrint( "file=" + filename + " e=" + e.getMessage() );
            return false;
        }


        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            lc += 1;
            Matcher m = LINE_PATTERN.matcher(line);
            if (!m.matches()) continue; // 異常行は無視

            boolean enabled = m.group(1) != null;
            String ip = m.group(2);
            String dateStr = m.group(3);
            String cc = m.group(4);
            String org = m.group(5);

            if (!ToolNet.isIP(ip)) continue;

            LocalDate date = ToolDate.YMD2LocalDate(dateStr).orElse(null);
            if ( date == null ) continue;

            if (cc == null || cc.isBlank()) cc = "??";

            if (org == null) org = "";

            vc += 1;
            m_records.add(new Record(enabled, ip, date, cc, org));
        }

        Debugger.InfoPrint( "Loaded file=" + filename + "  data line=" + lc + " valid lie=" + vc );
        return true;
    }

    public Optional<String> save(String filename) {
        List<String> output = new ArrayList<>();

        for (Record r : m_records) {
            if (!ToolNet.isIP(r.ip)) continue;

            StringBuilder sb = new StringBuilder();

            if (!r.enabled) {
                sb.append("#");
            }

            sb.append(r.ip)
              .append("\t# ")
              .append( ToolDate.Fromat( r.date, "uuuuMMdd" ).orElse("yyyyMMdd" ) )
              .append(" ")
              .append(r.cc);

            if (!r.org.isBlank()) {
                sb.append(" ").append(r.org);
            }

            output.add(sb.toString());
        }

        try {
            Path tmpPath = Path.of(filename + ".tmp");
            Path filePath = Path.of(filename);
            Path fileBack = Path.of(filename + ".bak");
            Files.write( tmpPath, output);
            Files.copy( filePath, fileBack, StandardCopyOption.REPLACE_EXISTING);
            Files.copy( tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            Files.delete( tmpPath );
            return Optional.empty();
        }catch ( IOException e ) {
            Debugger.ErrorPrint( "file=" + filename + " e=" + e.getMessage() );
            return Optional.ofNullable( e.getMessage() );
        }
    }
}
