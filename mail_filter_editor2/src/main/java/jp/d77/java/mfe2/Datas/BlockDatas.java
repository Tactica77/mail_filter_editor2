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
import jp.d77.java.mfe2.Datas.FilterDatas.IpFilter;
import jp.d77.java.tools.BasicIO.Debugger;
import jp.d77.java.tools.BasicIO.ToolDate;

public class BlockDatas {
    public static class BlockData {
        public boolean enabled;
        public String ip;
        public LocalDate date;
        public String cc;
        public String org;
        public String parent_ip = "-";

        public BlockData(boolean enabled, String ip, LocalDate date, String cc, String org) {
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
    private final List<BlockData> m_block_datas = new ArrayList<>();
    protected FilterDatas m_filter = null;
    protected String m_blocktype;

    public BlockDatas ( FilterDatas filter, String blocktype ){
        this.m_filter = filter;
        this.m_blocktype = blocktype;
    }

    /**
     * アイテム追加
     * @param rec
     * @return false = 被ってる
     */
    public boolean add( BlockData rec ){
        for ( BlockData r: this.m_block_datas ){
            if ( rec.ip.equals( r.ip ) ) {
                return false;
            }
        }
        this.m_block_datas.add(rec);
        if ( this.m_filter != null ) this.m_filter.add( rec.ip, this.m_blocktype );
        return true;
    }

    /**
     * アイテム削除
     * @param cidr
     * @return true = 削除成功、false = 無い?
     */
    public boolean remove( String cidr ){
        for ( BlockData r: this.m_block_datas ){
            if ( cidr.equals( r.ip ) ) {
                this.m_block_datas.remove( r );
                return true;
            }
        }
        return false;
    }

    public Optional<BlockData> getRecord( String cidr ){
        for ( BlockData r: this.m_block_datas ){
            if ( cidr.equals( r.ip ) ) return Optional.ofNullable( r );
        }
        return Optional.empty();
    }
    
    public String[] getCidrs(){
        List<String> ret = new ArrayList<>();

        for ( BlockData r: this.m_block_datas ){
            ret.add( r.ip );
        }
        return ret.toArray( new String[0] );
    }

    protected void setParentIp(){
        if ( this.m_filter == null ) return;
        for( BlockData bd: this.m_block_datas ){
            IpFilter ipf = this.m_filter.getFilter( bd.ip ).orElse(null);
            if ( ipf == null ) continue;
            if ( bd.ip.equals( ipf.m_cidr ) ) continue;
            bd.parent_ip = ipf.m_cidr + "\n" + ipf.m_type;
        }
    }

    /**
     * Load Black List
     * @param filename
     * @return
     */
    public boolean load(String filename) {
        Debugger.TracePrint();
        this.m_block_datas.clear();
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

            boolean enabled = m.group(1) == null;
            String ip = m.group(2);
            String dateStr = m.group(3);
            String cc = m.group(4);
            String org = m.group(5);

            if (!ToolNet.isIP(ip)) continue;

            LocalDate date = ToolDate.Str2LocalDate(dateStr).orElse(null);
            if ( date == null ) continue;

            if (cc == null || cc.isBlank()) cc = "??";

            if (org == null) org = "";

            vc += 1;
            this.add(new BlockData(enabled, ip, date, cc, org));
        }

        this.setParentIp();
        Debugger.InfoPrint( "Loaded file=" + filename + "  data line=" + lc + " valid lie=" + vc );
        return true;
    }

    public Optional<String> save(String filename) {
        List<String> output = new ArrayList<>();

        for (BlockData r : this.m_block_datas) {
            if (!ToolNet.isIP(r.ip)) continue;

            StringBuilder sb = new StringBuilder();

            if (!r.enabled) {
                sb.append("#");
            }

            sb.append(r.ip)
              .append("\t# ")
              .append( ToolDate.Format( r.date, "uuuuMMdd" ).orElse("yyyyMMdd" ) )
              .append(" ")
              .append(r.cc);

            if (!r.org.isBlank()) {
                sb.append(" ").append(r.org);
            }

            output.add(sb.toString());
        }

        return this.saveList(filename, output);
    }

    protected Optional<String> saveList(String filename, List<String> output ) {
        try {
            Path tmpPath = Path.of(filename + ".tmp");
            Path filePath = Path.of(filename);
            Path backPath = Path.of(filename + ".bak");
            Files.write( tmpPath, output);
            if (Files.exists(filePath)) Files.copy( filePath, backPath, StandardCopyOption.REPLACE_EXISTING);
            if (Files.exists(tmpPath)) Files.copy( tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            if (Files.exists(tmpPath)) Files.delete( tmpPath );
            return Optional.empty();
        }catch ( IOException e ) {
            e.printStackTrace();
            Debugger.ErrorPrint( "file=" + filename + " e=" + e.getMessage() );
            return Optional.ofNullable( e.getMessage() );
        }
    }
}
