package jp.d77.java.mfe2.BasicIO;

import java.net.InetAddress;

public class ToolNet {
    /**
     * 255.255.255.0→24
     * @param mask
     * @return
     * @throws Exception
     */
    public static int MaskToCidr(String mask) throws Exception {
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
    public static String Cidr2MaskString( int cidr ){
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
    public static String longToIP(long ip) {
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
    public static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long res = 0;
        for (String part : parts) {
            res = res * 256 + Integer.parseInt(part);
        }
        return res;
    }
}
