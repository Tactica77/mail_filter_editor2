package jp.d77.java.mfe2.BasicIO;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import jp.d77.java.tools.BasicIO.Debugger;

//・CIDR表記...255.192.0.0/10
//・IP range...150.128.0.0-150.191.255.255
//・Network address...150.128.0.0の部分
//・Prefix.../10の部分

public class ToolNet {

    /**
     * IPアドレスか？
     * @param input
     * @return
     */
    public static boolean isIP(String input) {
        if (input == null || input.isEmpty()) return false;

        String ipPart = input;
        int prefix = -1;

        int slash = input.indexOf('/');
        if (slash >= 0) {
            ipPart = input.substring(0, slash);
            try {
                prefix = Integer.parseInt(input.substring(slash + 1));
            } catch (NumberFormatException e) {
                return false;
            }
            if (prefix < 0 || prefix > 32) return false;
        }

        int dotCount = 0;
        int value = 0;
        int partLength = 0;

        for (int i = 0; i < ipPart.length(); i++) {
            char c = ipPart.charAt(i);

            if (c == '.') {
                if (partLength == 0 || value > 255) return false;
                dotCount++;
                value = 0;
                partLength = 0;
                continue;
            }

            if (c < '0' || c > '9') return false;

            value = value * 10 + (c - '0');
            partLength++;

            if (value > 255) return false;
        }

        return dotCount == 3 && partLength > 0 && value <= 255;
    }

    /**
     * プライベート/ループバックIP判定
     * @param ip
     * @return
     */
    public static boolean isPrivateIp(String ip) {
        int p0 = 0, p1 = 0;
        int dotCount = 0;
        int value = 0;
        int partIndex = 0;

        for (int i = 0; i < ip.length(); i++) {
            char c = ip.charAt(i);

            if (c == '.') {
                if (partIndex == 0) p0 = value;
                if (partIndex == 1) p1 = value;

                value = 0;
                partIndex++;
                dotCount++;
                continue;
            }

            if (c < '0' || c > '9') return false;
            value = value * 10 + (c - '0');
            if (value > 255) return false;
        }

        if (dotCount != 3) return false;

        // 最初の2オクテットのみで判定可能
        if (p0 == 10) return true;
        if (p0 == 127) return true;
        if (p0 == 172 && (p1 >= 16 && p1 <= 31)) return true;
        if (p0 == 192 && p1 == 168) return true;

        return false;
    }

    /**
     * 255.255.255.0→24
     * @param mask
     * @return
     * @throws Exception
     */
    public static int mask2prefix(String mask) throws Exception {
        byte[] bytes = InetAddress.getByName(mask).getAddress();
        int cidr = 0;
        for (byte b : bytes) {
            int bits = b & 0xFF;
            while ((bits & 0x80) != 0) {
                cidr++;
                bits <<= 1;
            }
        }
        return cidr;
    }

    /**
     * /24→255.255.255.0
     * @param cidr
     * @return
     */
    public static String prefix2mask( int cidr ){
        int mask = 0xffffffff << (32 - cidr); // 上位cidrビットを1にする
        int octet1 = (mask >>> 24) & 0xff;
        int octet2 = (mask >>> 16) & 0xff;
        int octet3 = (mask >>> 8) & 0xff;
        int octet4 = mask & 0xff;
        return String.format("%d.%d.%d.%d", octet1, octet2, octet3, octet4);        
    }

    /**
     * ipの数値表現をnnn.nnn.nnn.nnnへ変換
     * @param ip
     * @return
     */
    public static String long2ip(long ip) {
        return String.format("%d.%d.%d.%d",
            (ip >> 24) & 0xFF,
            (ip >> 16) & 0xFF,
            (ip >> 8) & 0xFF,
            ip & 0xFF);
    }    

    /**
     * ipの10進数表現(nnn.nnn.nnn.nnn)を数値表現をへ変換
     * @param ip
     * @return
     */
    public static long ip2Long(String ip) {
        String[] parts = ip.split("\\.");
        long res = 0;
        for (String part : parts) {
            res = res * 256 + Integer.parseInt(part);
        }
        return res;
    }

    /**
     * IPとCIDRから、IPレンジを返す
     * @param ip
     * @param cidr
     * @return [0]=Start IP,[1]=End IP
     */
    public static Optional<RangeResult> cidr2range(String ip, int cidr) {
        try{
            InetAddress ipAddress = InetAddress.getByName(ip);
            byte[] ipBytes = ipAddress.getAddress();

            int mask = 0xffffffff << (32 - cidr);
            byte[] maskBytes = new byte[] {
                (byte) (mask >>> 24),
                (byte) (mask >>> 16),
                (byte) (mask >>> 8),
                (byte) mask
            };

            byte[] networkBytes = new byte[4];
            byte[] broadcastBytes = new byte[4];

            for (int i = 0; i < 4; i++) {
                networkBytes[i] = (byte) (ipBytes[i] & maskBytes[i]);
                broadcastBytes[i] = (byte) (ipBytes[i] | ~maskBytes[i]);
            }

            InetAddress start = InetAddress.getByAddress(networkBytes);
            InetAddress end = InetAddress.getByAddress(broadcastBytes);

            RangeResult res = new RangeResult( start.getHostAddress(), end.getHostAddress() );

            return Optional.ofNullable( res );
        }catch( UnknownHostException e ){
            return Optional.empty();
        }
    }
    public record RangeResult( String start_ip, String end_ip ) {}

    /**
     * nnn.nnn.nnn.nnn/nnからnnn.nnn.nnn.nnnとnnへ分割
     * 通常のIPの場合は/32を付与
     * @param cidr
     * @return
     */
    public static Optional<Cidr> CidrSplit( String cidr ){
        String[] r = cidr.split("/");
        if ( r.length >= 2 ){
            try {
                int c = Integer.parseInt( r[1] );
                return Optional.ofNullable( new Cidr( r[0], c ) );
            }catch( NumberFormatException e ){
                return Optional.empty();
            }
            
        }else if ( r.length >= 1 ){
            return Optional.ofNullable( new Cidr( r[0], 32 ) );
        }else{
            return Optional.empty();
        }
    }
    public record Cidr( String ip, Integer cidr ) {}

    /**
     * 片方のCIDRがもう片方の範囲かを判定し、範囲の広い方を返す。範囲に含まれない場合はempty
     * @param cidrA
     * @param cidrB
     * @return 広い方の範囲を返す。完全一致の場合はcidrAを返します
     */
    public static Optional<String> isWithinCIDR(String cidrA, String cidrB) {
        String[] aParts = cidrA.split("/");
        String[] bParts = cidrB.split("/");

        try {
            int prefixA,prefixB;
            if ( aParts.length <= 1 ){
                prefixA = 32;
            }else{
                prefixA = Integer.parseInt(aParts[1]);
            }
            if ( bParts.length <= 1 ){
                prefixB = 32;
            }else{
                prefixB = Integer.parseInt(bParts[1]);
            }

            long ipA = ToolNet.ip2Long(aParts[0]);
            long ipB = ToolNet.ip2Long(bParts[0]);

            long networkA = ipA & ToolNet.maskBits(prefixA);
            long networkB = ipB & ToolNet.maskBits(prefixB);

            // BがAに含まれる
            if (prefixB >= prefixA && (ipB & ToolNet.maskBits(prefixA)) == networkA) {
                return Optional.of(cidrA);
            }
            // AがBに含まれる
            if (prefixA >= prefixB && (ipA & ToolNet.maskBits(prefixB)) == networkB) {
                return Optional.of(cidrB);
            }
            return Optional.empty();
        } catch (Exception e) {
            Debugger.ErrorPrint( "cmp error: " + cidrA + " " + cidrB + " " + e.getMessage() );
            return Optional.empty();
        }
    }

    private static long maskBits(int prefix) {
        return ~((1L << (32 - prefix)) - 1) & 0xFFFFFFFFL;
    }
}
