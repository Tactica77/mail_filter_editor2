package jp.d77.java.mfe2.BasicIO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.d77.java.tools.BasicIO.Debugger;

public class ToolRDAP {
    public static class RDAPdata{
        private HashMap<String,String> params = new HashMap<String,String>();
        private JsonNode node;

        public RDAPdata( String s, JsonNode n ){
            this.params.put("rdap_server", s);
            this.node = n;
        }
        
        public void set( String key, String value ){
            //Debugger.InfoPrint( "set " + key + " = " + value );
            this.params.put( key, value );
        }

        public Optional<String> get( String key ){
            if ( ! this.params.containsKey(key) ) Optional.empty();
            return Optional.ofNullable( this.params.get(key) );
        }

        public String[] getCiders(){
            int i = 0;
            List<String> list = new ArrayList<>();
            while (true) {
                if ( this.get( "cidr_" + i ).isEmpty() ) break;
                list.add( this.get( "cidr_" + i ).get() );
                i++;
            }
            return list.toArray( new String[0] );
        }

        public Optional<String> getRdapServerFQDN(){
            if ( this.get( "rdap_server" ).isEmpty() ) return Optional.empty();
            URI uri = URI.create( this.get( "rdap_server" ).get() );
            return Optional.ofNullable( uri.getHost() );
        }
    }

    // RDAP Data<ip,Json>
    private static HashMap<String,RDAPdata>    m_rdap_data = new HashMap<String,RDAPdata>();

    // RDAPサーバアクセス制限
    private static final int WINDOW_SECONDS = 10;   // 一定時間(秒)
    private static final int MAX_REQUESTS   = 5;    // 許可回数
    private static final ConcurrentHashMap<String, Deque<Long>> accessMap = new ConcurrentHashMap<>();

    /**
     * キャッシュされたIP一覧を取得
     * @return IP一覧
     */
    public static String[] getCacheIPs(){
        return ToolRDAP.m_rdap_data.keySet().toArray(new String[0]);
    }

    /**
     * RDAPdataを取得。ローカル/ループバックIPはemptyを返す。データがキャッシュされていなければRDAPサーバからデータを取得する。
     * @param ipAddress
     * @return
     */
    public static Optional<RDAPdata> getRDAPdata(String ipAddress) {
        // ローカル/ループバックIPチェック
        if ( ToolNet.isPrivateIp(ipAddress) ) return Optional.empty();

        if ( ToolRDAP.m_rdap_data.containsKey(ipAddress) ) {
            // キャッシュを返す
            return Optional.ofNullable( ToolRDAP.m_rdap_data.get(ipAddress) );
        }

        // RDAPサーバの情報を返す
        if ( ToolRDAP.IPSearch(ipAddress) ) ToolRDAP.initParam(ipAddress);

        if ( ! ToolRDAP.m_rdap_data.containsKey(ipAddress) ) return Optional.empty();  // データなし
        return Optional.ofNullable( ToolRDAP.m_rdap_data.get(ipAddress) );
    }

    /**
     * IPアドレスからRDAP情報をJSONパス形式に変換する
     * @param ipAddress
     * @return RDAPkey,value
     */
    public static LinkedHashMap<String, String> dump(String ipAddress) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        RDAPdata data = ToolRDAP.getRDAPdata(ipAddress).orElse( null );
        if ( data == null ) return result; // データが無い場合は空の配列を返す

        JsonNode root = data.node;
        if (root == null) return result; // データなし

        ToolRDAP.walkJson(root, "", result);

        return result;
    }

    /**
     * 取得したRDAP情報から定数を定義する。
     * @param ipAddress
     */
    private static void initParam( String ipAddress ){
        RDAPdata data = ToolRDAP.m_rdap_data.get(ipAddress);
        LinkedHashMap<String, String> node = ToolRDAP.dump(ipAddress);
        String candidate_country = null;
        String candidate_fn = null;
        String candidate_org = null;
        String org = null;
        String rdap_server = data.getRdapServerFQDN().orElse( null );

        for ( String key: node.keySet() ){
            String value = node.get( key );
            switch (key) {
                case "name":    data.set( "name", node.get(key) ); break;
                case "country": data.set( "country", node.get(key) ); break;
                case "startAddress": data.set( "startAddress", node.get(key) ); break;
                case "endAddress": data.set( "endAddress", node.get(key) ); break;
                case "ipVersion": data.set( "ipVersion", node.get(key) ); break;
            }
            if ( key.startsWith( "cidr0_cidrs" ) ){
                Pattern pattern = Pattern.compile("\\[(\\d+)]");
                Matcher matcher = pattern.matcher(key);
                if (matcher.find()) {
                    int i = Integer.parseInt(matcher.group(1));
                    String res = "";
                    if ( data.params.containsKey( "cidr_" + i ) ) res = data.params.get( "cidr_" + i );
                    if ( key.endsWith( "v4prefix" ) ){
                        res += node.get(key);
                        data.set( "cidr_" + i, res );
                    }else if ( key.endsWith( "length" ) ){
                        res += "/" + node.get(key);
                        data.set( "cidr_" + i, res );
                    }
                }                
            }
            if ( candidate_country == null && key.matches( "entities\\[0\\]\\.vcardArray\\[1\\]\\[\\d+\\]\\[1\\].label" ) ) {
                if ( value.matches( "[A-Z]{2}" ) ) candidate_country = value.substring( value.length() - 2);
                else{
                    for ( String c: ToolRDAPServer.Country2Code.keySet() ){
                        if ( value.toLowerCase().contains( c ) ){
                            candidate_country = ToolRDAPServer.Country2Code.get(c);
                        }
                    }
                }
                //else if ( value.contains( "United States" ) ) candidate_country = "US";
                //else if ( value.contains( "Luxembourg" ) ) candidate_country = "LU";
            }

            // name

            //rdap.lacnic.net
            //rdap.arin.net

            //rdap.db.ripe.net
            //rdap.apnic.net
            //rdap.afrinic.net

            // registrant entity を特定
            // entities[0].roles[0] = registrant
            if ( key.equals( "notices[0].description[0]")
                && value.toLowerCase().contains("afrinic rdap server") ){
                Debugger.InfoPrint( "rdap server change from " + rdap_server + " to rdap.afrinic.net" );
                rdap_server = "rdap.afrinic.net";
                continue;
            }

            if ( rdap_server != null
                && rdap_server.contains( "rdap.afrinic.net" ) ){
                // AFRINIC
                if ( key.equals("remarks[0].description[0]" ) ) org = value;
                if ( org == null && key.equals("entities[0].vcardArray[1][2][3]" ) ) org = value;

            }else if ( rdap_server != null
                && rdap_server.contains( "rdap.apnic.net" )
                && key.equals("remarks[0].description[0]" ) ){
                // APNIC
                org = value;

            }else if ( rdap_server != null
                && rdap_server.contains( "rdap.db.ripe.net" ) ){
                // RIPE
                if ( key.equals("name" ) ) org = value;
                if ( key.equals("entities[0].vcardArray[1][1][3]" )
                    && !value.equals( "Abuse Contact" )
                    && !value.equals( "individual" )
                    ) org = value;
                /* 
            }else if ( rdap_server != null
                && rdap_server.contains( "rdap.apnic.net" )
                && key.equals("entities[0].vcardArray[1][2][3]" ) ){
                // AFRINIC
                org = value;
*/
            }else if (key.matches("entities\\[\\d+\\]\\.roles\\[\\d+\\]")
                && "registrant".equals(value)
                && org == null ) {
                boolean org_flg = true;
                if ( rdap_server != null
                && rdap_server.contains( "ripe.net" ) ){
                    org_flg = false;
                }

                // entity index を抽出
                String entityIndex = key.replaceAll("entities\\[(\\d+)\\].*", "$1");

                // その entity 内の org を探す
                for (Map.Entry<String, String> sub : node.entrySet()) {

                    String subKey = sub.getKey();
                    String subValue = sub.getValue();

                    if (subKey.matches("entities\\[" + entityIndex + "\\]\\.handle")
                        && subValue.toLowerCase().contains( "org-" )) {
                        org_flg = true;
                    }

                    // entities[0].vcardArray[1][*][0] = org
                    if (subKey.matches("entities\\[" + entityIndex + "\\]\\.vcardArray\\[1\\]\\[\\d+\\]\\[0\\]")
                            && "org".equals(subValue)) {

                        // entities[0].vcardArray[1][*][3][0] = org
                        String baseKey = subKey.replaceAll("\\[0\\]$", "");
                        String orgKey = baseKey + "[3][0]";

                        candidate_org = node.get(orgKey);
                    }
                    // entities[0].vcardArray[1][*][0] = fn
                    if (subKey.matches("entities\\[" + entityIndex + "\\]\\.vcardArray\\[1\\]\\[\\d+\\]\\[0\\]")
                            && "fn".equals(subValue)) {

                        //entities[0].vcardArray[1][*][3]
                        String baseKey = subKey.replaceAll("\\[0\\]$", "");
                        String orgKey = baseKey + "[3]";

                        candidate_fn = node.get(orgKey);
                    }
                }

                if ( org_flg ){
                    if ( candidate_org != null ) org = candidate_org;
                    if ( candidate_fn != null ) {
                        if ( org == null ) org = candidate_fn;
                        else org += " " + candidate_fn;
                    }
                }
            }

            // country
            // entities[*].vcardArray[1][*][0] = adr -> entities[*].vcardArray[1][*][3]->org"
            if (
                ( key.matches("entities\\[\\d+\\]\\.vcardArray\\[1\\]\\[\\d+\\]\\[0\\]") && "adr".equals( node.get( key ) ) )
                ) {

                // adr のベースキー取得
                String baseKey = key.replaceAll("\\[0\\]$", "");
                String orgKey = baseKey + "[1].label";
                String s = node.get(orgKey);
                
                if ( s.length() > 3 ) {
                    String cvalue = s.substring(s.length() - 2);
                    if (candidate_country == null && cvalue.matches("[A-Z]{2}")) {
                        candidate_country = cvalue;
                    }
                }
            }
        }

        // RDAP戻り値にcountryが無い場合になるべく埋める
        if ( data.get( "country").isEmpty() && candidate_country != null ) data.set( "country", candidate_country );
        if ( org != null ) data.set( "name", org );
    }

    /**
     * RDAPデータから定数を返す
     * @param ipAddress
     * @param key
     * @return
     */
    public static Optional<String> getParam( String ipAddress, String key ){
        RDAPdata data = ToolRDAP.getRDAPdata(ipAddress).orElse( null );
        if ( data == null ) return Optional.empty();
        if ( ! data.params.containsKey( key ) ) return Optional.empty();
        return Optional.ofNullable( data.params.get(key) );
    }

    /**
     * CIDRを取得
     * @param ipAddress
     * @return
     */
    public static String[] getCidrs( String ipAddress ){
        RDAPdata data = ToolRDAP.getRDAPdata(ipAddress).orElse( null );
        if ( data == null ) return new String[0];
        return data.getCiders();
    }

    /**
     * Load RDAP data
     * @param ipAddress
     * @return true=データが取れた / false=データが取れなかった
     */
    private static boolean IPSearch( String ipAddress ){
        // ローカル/ループバックIPチェック
        if ( ToolNet.isPrivateIp(ipAddress) ) return false;

        // IPアドレス情報からRDAPサーバ情報を取得
        String[] RdapServers = ToolRDAPServer.getServerUri( ipAddress ).orElse( null );
        if ( RdapServers == null ) return false;

        ObjectMapper mapper = new ObjectMapper();
        for ( String RdapServer: RdapServers ){
            // RDAPサーバへリクエスト
            String rdap_server = RdapServer;
            if (!rdap_server.endsWith("/")) rdap_server += "/";
            if ( !ToolRDAP.checkRDAPAccessAllow(rdap_server) ){
                // リクエスト数が多すぎる
                Debugger.ErrorPrint( "too many requests: " + rdap_server );
                continue;
            }
            if ( Debugger.elapsedTimer() > 60000L ){
                Debugger.ErrorPrint( "timeout requests: " + rdap_server );
                continue;
            }
            String url = rdap_server + "ip/" + ipAddress;
            Debugger.InfoPrint( "request: " + url + " queue=" + ToolRDAP.getQueueSize(rdap_server) );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/rdap+json")
                .build();
            //HttpClient client = HttpClient.newHttpClient()
            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    Debugger.ErrorPrint( "not responsep[HTTP status=" + response.statusCode() + "]:" + url );
                    continue;
                }
                ToolRDAP.m_rdap_data.put(ipAddress, new RDAPdata( RdapServer, mapper.readTree(response.body()) ));
                return true;
            } catch (IOException e) {
                Debugger.ErrorPrint( "not responsep[IOException]:" + url );
                continue;
            } catch (InterruptedException e) {
                Debugger.ErrorPrint( "not responsep[InterruptedException]:" + url );
                continue;
            }
        }

        return false;
    }

    /**
     * JsonNodeを再帰的に走査してパスと値を格納
     */
    private static void walkJson(JsonNode node, String path, LinkedHashMap<String, String> result) {

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String newPath = path.isEmpty()
                        ? entry.getKey()
                        : path + "." + entry.getKey();
                walkJson(entry.getValue(), newPath, result);
            }

        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String newPath = path + "[" + i + "]";
                walkJson(node.get(i), newPath, result);
            }

        } else {
            // 値ノード
            result.put(path, node.asText());
        }
    }

    /**
     * 
     * @param server
     * @return
     */
    private static boolean checkRDAPAccessAllow(String server) {
        long now = System.currentTimeMillis();
        long windowStart = now - ToolRDAP.WINDOW_SECONDS * 1000L;

        Deque<Long> timestamps =
                ToolRDAP.accessMap.computeIfAbsent(server, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // 古いアクセスを削除
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= ToolRDAP.MAX_REQUESTS) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    private static int getQueueSize(String server) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_SECONDS * 1000L;

        Deque<Long> timestamps = accessMap.get(server);
        if (timestamps == null) {
            return 0;
        }

        synchronized (timestamps) {
            // 古いアクセス削除
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            return timestamps.size();
        }
    }
}
