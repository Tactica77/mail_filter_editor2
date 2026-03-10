package jp.d77.java.mfe2.Datas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jp.d77.java.mfe2.BasicIO.ToolNet;
import jp.d77.java.tools.BasicIO.Debugger;

public class IpTablesDatas extends FilterDatas {
    private class ipTablesData extends IpFilter {
        public ipTablesData(String Cidr, String type) {
            super(Cidr, type);
        }
        private boolean m_delete = false;
        private boolean m_add = false;
        public void setDelete( boolean b ) { this.m_delete = b; }
        public void setAdd( boolean b ) { this.m_add = b; }
        public boolean isDelete() { return this.m_delete; }
        public boolean isAdd() { return this.m_add; }
    }
    private List<ipTablesData>  m_ipTablesData = new ArrayList<>();

    public IpTablesDatas(){
    }

    public boolean add( String cidr_string, String type ){
        // IpFilterへ変換
        ipTablesData ipf = new ipTablesData( cidr_string, type );
        if ( ! ipf.isEnable() ) return false;

        // 削除フラグを立てる。
        // この後、country_filter、block_list_black、block_list_spotを読み込み、残すならDeleteフラグは消される
        if ( type.equals( "iptables" ) ) ipf.setDelete( true );
        else ipf.setAdd( true ); // 一旦新規追加フラグを立てる

        // 登録済みに含まれるか確認
        for ( ipTablesData chk_ipf: this.m_ipTablesData ){
            // 削除予定はチェック対象としない
            //if ( chk_ipf.isDelete() ) continue;

            // iptablesでは無い場合は追加フラグを立てる
            //if ( ! type.equals( "iptables" ) ) ipf.setAdd( true ); // 追加

            if ( this.isWithin( chk_ipf, ipf ) ) {
                // 同一、あるいはチェック対象が内包してる
                if ( type.equals( "iptables" ) ){
                    // 既にある定義で賄えるので追加しない
                    return false;
                }else{
                    // 既にある定義を残す
                    chk_ipf.setAdd( false );
                    chk_ipf.setDelete( false );
                    return false;
                }
            }
            if ( this.isWithin( ipf, chk_ipf ) ) {
                // 元々ある方を消す
                //Debugger.WarnPrint( "remove " + chk_ipf.getCidr() + " Included " + ipf.getCidr() );
                chk_ipf.setAdd( false );
                chk_ipf.setDelete( true );
            }
        }

        this.m_ipTablesData.add( ipf );
        return true;
    }

    public boolean loadIptables(String filename) {
        Debugger.TracePrint();
        int lc = 0;
        int vc = 0;
        this.m_ipTablesData.clear();

        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(filename));
        }catch ( IOException e ) {
            Debugger.ErrorPrint( "file=" + filename + " e=" + e.toString() );
            return false;
        }

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            lc += 1;
            // 12   DROP       tcp  --  61.97.192.0/19       0.0.0.0/0            state NEW tcp multiport dports 25,110,143,993,995
            String[] items = line.trim().split("\\s+");
            if ( items.length < 7 ) continue;
            if ( ! items[1].equals("DROP") ) continue;
            if ( items[4].equals("0.0.0.0/0") ) continue;
            if ( ! ToolNet.isIP( items[4] ) ) continue;
            if ( ToolNet.isPrivateIp( items[4] ) ) continue;
            
            this.add( items[4], "iptables" );

            vc += 1;
        }

        Debugger.InfoPrint( "Loaded file=" + filename + "  data line=" + lc + " valid lie=" + vc );
        return true;
    }

    public Optional<String> saveBlockSetting( String filename, boolean delete ) {
        List<String> output = new ArrayList<>();

        for ( ipTablesData itd: this.m_ipTablesData ){
            if ( delete && itd.isDelete() ){
                output.add( itd.getCidr() );
            }else if ( ! delete && itd.isAdd() ){
                output.add( itd.getCidr() );
            }
        }

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
