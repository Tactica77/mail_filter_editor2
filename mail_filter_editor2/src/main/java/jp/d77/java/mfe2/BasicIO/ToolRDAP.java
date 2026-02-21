package jp.d77.java.mfe2.BasicIO;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public static String[] getCacheIPs(){
        return ToolRDAP.m_rdap_data.keySet().toArray(new String[0]);
    }

    public static Optional<RDAPdata> getRDAPdata(String ipAddress) {
        if ( ! ToolRDAP.m_rdap_data.containsKey(ipAddress) ) {
            ToolRDAP.IPSearch(ipAddress);  // Load
            ToolRDAP.initeParam(ipAddress);
        }
        if ( ! ToolRDAP.m_rdap_data.containsKey(ipAddress) ) Optional.empty();  // データなし
        return Optional.ofNullable( ToolRDAP.m_rdap_data.get(ipAddress) );
    }

    /**
     * IPアドレスからRDAP情報をJSONパス形式に変換する
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

    private static void initeParam( String ipAddress ){
        RDAPdata data = ToolRDAP.m_rdap_data.get(ipAddress);
        LinkedHashMap<String, String> node = ToolRDAP.dump(ipAddress);

        for ( String p: node.keySet() ){
            switch (p) {
                case "name":    data.set( "name", node.get(p) ); break;
                case "country": data.set( "country", node.get(p) ); break;
                case "startAddress": data.set( "startAddress", node.get(p) ); break;
                case "endAddress": data.set( "endAddress", node.get(p) ); break;
                case "ipVersion": data.set( "ipVersion", node.get(p) ); break;
            }
            if ( p.startsWith( "cidr0_cidrs" ) ){
                Pattern pattern = Pattern.compile("\\[(\\d+)]");
                Matcher matcher = pattern.matcher(p);
                if (matcher.find()) {
                    int i = Integer.parseInt(matcher.group(1));
                    String res = "";
                    if ( data.params.containsKey( "cidr_" + i ) ) res = data.params.get( "cidr_" + i );
                    if ( p.endsWith( "v4prefix" ) ){
                        res += node.get(p);
                        data.set( "cidr_" + i, res );
                    }else if ( p.endsWith( "length" ) ){
                        res += "/" + node.get(p);
                        data.set( "cidr_" + i, res );
                    }
                }                
            }
        }
    }

    public static Optional<String> getParam( String ipAddress, String key ){
        RDAPdata data = ToolRDAP.getRDAPdata(ipAddress).orElse( null );
        if ( data == null ) return Optional.empty();
        if ( ! data.params.containsKey( key ) ) return Optional.empty();
        return Optional.ofNullable( data.params.get(key) );
    }

    public static String[] getCidrs( String ipAddress ){
        RDAPdata data = ToolRDAP.getRDAPdata(ipAddress).orElse( null );
        if ( data == null ) return new String[0];
        return data.getCiders();
    }

    /**
     * Load RDAP data
     * @param ipAddress
     */
    private static void IPSearch( String ipAddress ){
        String[] RdapServers = ToolRDAPServer.getServerUri( ipAddress ).orElse( null );
        if ( RdapServers == null ) return;
        ObjectMapper mapper = new ObjectMapper();

        for ( String RdapServer: RdapServers ){
            String url = RdapServer;
            if (!url.endsWith("/")) url += "/";
            url += "ip/" + ipAddress;
            Debugger.InfoPrint( "request: " + url );

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
                return;
            } catch (IOException e) {
                Debugger.ErrorPrint( "not responsep[IOException]:" + url );
                continue;
            } catch (InterruptedException e) {
                Debugger.ErrorPrint( "not responsep[InterruptedException]:" + url );
                continue;
            }
        }

        return;
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


}
