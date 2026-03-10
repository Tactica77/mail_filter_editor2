package jp.d77.java.mfe2.Datas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class SpotBlockDatas extends BlockDatas {

    public static class SpotBlockData extends BlockData {
        public int             m_cnt;
        public float           m_score;
        public SpotBlockData(boolean enabled, String ip, LocalDate date, String cc, String org, int cnt, float score ) {
            super(enabled, ip, date, cc, org);
            this.m_cnt = cnt;
            this.m_score = score;
        }
    }
    
    private static final Pattern LINE_PATTERN = Pattern.compile(
        "^(#)?\\s*" +                                 // disable
        "([^\\s#]+)\\s+" +                            // IP
        "#\\s+" +                                     // required #
        "(\\d{8})" +                                  // yyyyMMdd
        "(?:\\s+([A-Z]{2}))?" +                       // CC (optional)
        "(?:\\s+(.*))?" +                             // ORG
        "#\\s+" +                                     // required #
        "(\\d+)\\s+(-?\\d+(?:\\.\\d+)?)"              // cnt score
    );

    private final List<SpotBlockData> m_block_datas = new ArrayList<>();

    public SpotBlockDatas(FilterDatas filter, String blocktype) {
        super(filter, blocktype);
    }

    public boolean add( SpotBlockData rec ){
        for ( SpotBlockData r: this.m_block_datas ){
            if ( rec.ip.equals( r.ip ) ) {
                return false;
            }
        }
        this.m_block_datas.add(rec);
        if ( this.m_filter != null ) this.m_filter.add( rec.ip, this.m_blocktype );
        return true;
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
            int cnt;
            float score;
            try {
                cnt = Integer.parseInt( m.group(6) );
            }catch( NumberFormatException e ){
                cnt = 0;
            }
            try {
                score = Float.parseFloat( m.group(7) );
            }catch( NumberFormatException e ){
                score = 0;
            }

            if (!ToolNet.isIP(ip)) continue;

            LocalDate date = ToolDate.Str2LocalDate(dateStr).orElse( null );
            if ( date == null ) continue;

            if (cc == null || cc.isBlank()) cc = "??";

            if (org == null) org = "";

            vc += 1;
            this.add(new SpotBlockData(enabled, ip, date, cc, org, cnt, score));
        }

        this.setParentIp();
        Debugger.InfoPrint( "Loaded file=" + filename + "  data line=" + lc + " valid lie=" + vc );
        return true;
    }

    @Deprecated
    public Optional<BlockData> getRecord( String cidr ){
        return Optional.empty();
    }

    public Optional<SpotBlockData> getSRecord( String cidr ){
        for ( SpotBlockData r: this.m_block_datas ){
            if ( cidr.equals( r.ip ) ) return Optional.ofNullable( r );
        }
        return Optional.empty();
    }

    public String[] getCidrs(){
        List<String> ret = new ArrayList<>();

        for ( SpotBlockData r: this.m_block_datas ){
            ret.add( r.ip );
        }
        return ret.toArray( new String[0] );
    }

    protected void setParentIp(){
        if ( this.m_filter == null ) return;
        for( SpotBlockData bd: this.m_block_datas ){
            IpFilter ipf = this.m_filter.getFilter( bd.ip ).orElse(null);
            if ( ipf == null ) continue;
            if ( bd.ip.equals( ipf.m_cidr ) ) continue;
            bd.parent_ip = ipf.m_cidr + "\n" + ipf.m_type;
        }
    }

    public Optional<String> save(String filename) {
        List<String> output = new ArrayList<>();

        for (SpotBlockData r : this.m_block_datas) {
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
            sb.append("# ")
                .append(r.m_cnt)
                .append(" ")
                .append(r.m_score);

            output.add(sb.toString());
        }

        return this.saveList(filename, output);
    }
}
