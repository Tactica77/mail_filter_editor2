package jp.d77.java.mfe2.BasicIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import jp.d77.java.tools.BasicIO.Debugger;

public class ToolRDAPServer {
    // RDAP Server Class
    private static class RdapServerData{
        private int prefix_top = 0;
        //private int prefix_btm = 0;
        private List<String> servers = new ArrayList<String>();

        // setter
        public void setPrefix( String value ){
            if (value != null && value.matches("\\d+/\\d+")) {
                String[] parts = value.split("/");
                this.prefix_top = Integer.parseInt(parts[0]);
                //this.prefix_btm = Integer.parseInt(parts[1]);
            }
        }
        public void addServer( String value ){
            this.servers.add(value);
        }

        // getter
        public int getPrefix(){ return this.prefix_top; }
        //public Optional<String> getServer(){ return this.getServer(0); }
        public String[] getServers(){
            return this.servers.toArray(new String[0]);
        }
    }

    // RDAP Servers
    private static List<RdapServerData> m_listRdapServers;
    private static final String NS = "http://www.iana.org/assignments";

    /**
     * IPアドレスから、対象となるRDAPサーバを取得
     * @param ipAddress
     * @return RDAP Servers
     */
    public static Optional<String[]> getServerUri( String ipAddress ){
        if ( ipAddress == null ) return Optional.empty();
        String[] w = ipAddress.split("[.]");
        int prefix_top = 0;
        if ( w.length <= 0 ) return Optional.empty();
        try {
            prefix_top = Integer.parseInt(w[0]);
        } catch (Exception e) {
            return Optional.empty();
        }

        for (RdapServerData d : ToolRDAPServer.m_listRdapServers ){
            if ( d.getPrefix() == prefix_top ){
                return Optional.ofNullable( d.getServers() );
            }
        }
        return Optional.empty();
    }

    /**
     * RDAPサーバ情報をXMLから取得
     * @param FilePath
     */
    public static void loadServers( String FilePath ){
        if ( ToolRDAPServer.m_listRdapServers != null ) return;
        Debugger.TracePrint();

        ToolRDAPServer.m_listRdapServers = new ArrayList<>();
        File xmlFile = new File( FilePath + "ipv4-address-space.xml");
        Debugger.InfoPrint( "Loading file: " + FilePath + "ipv4-address-space.xml" );

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder;
        Document doc;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(xmlFile);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return;
        }

        NodeList records = doc.getElementsByTagNameNS(NS, "record");

        for (int i = 0; i < records.getLength(); i++) {
            Element record = (Element) records.item(i);
            RdapServerData rdapdata = new RdapServerData();

            // prefix
            rdapdata.setPrefix( getText(record, "prefix") );

            // server（存在しない場合あり）
            NodeList rdapList = record.getElementsByTagNameNS(NS, "rdap");
            if (rdapList.getLength() > 0) {
                Element rdap = (Element) rdapList.item(0);
                NodeList serverNodes = rdap.getElementsByTagNameNS(NS, "server");
                for (int j = 0; j < serverNodes.getLength(); j++) {
                    rdapdata.addServer( serverNodes.item(j).getTextContent().trim() );
                }
            }
            ToolRDAPServer.m_listRdapServers.add(rdapdata);
        }
    }
    
    private static String getText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagNameNS(NS, tagName);
        if (list.getLength() == 0) {
            return null;
        }
        return list.item(0).getTextContent().trim();
    }
}
