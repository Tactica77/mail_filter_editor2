package jp.d77.java.mfe2.Pages;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import jp.d77.java.mfe2.BasicIO.ToolAny;
import jp.d77.java.mfe2.Datas.SessionLogDatas;
import jp.d77.java.mfe2.Datas.SessionLogDatas.LogBasicData;
import jp.d77.java.mfe2.LogAnalyser.RspamdLog.RspamdLogData;
import jp.d77.java.mfe2.Datas.SessionLogManager;
import jp.d77.java.tools.BasicIO.ToolDate;
import jp.d77.java.tools.HtmlIO.BSOpts;
import jp.d77.java.tools.HtmlIO.BSSForm;

public class WebLogsDetail {
    private SessionLogManager  m_slog;

    public WebLogsDetail( SessionLogManager slog ){
        this.m_slog = slog;
    }

    /**
     * ログ詳細表示用文字列生成
     * @return
     */
    public String display( int id, LocalDate targetDate ){
        String res = "";

        String displayYMD = ToolDate.Format( targetDate,"uuuu-MM-dd" ).orElse( "???" );
        BSSForm f;

        SessionLogDatas sd = this.m_slog.getData( targetDate ).orElse( null );
        if ( sd == null ) return "error";

        if ( ! sd.containsId( id ) ) return "not found id=" + id;

        f = BSSForm.newForm();
        f.addStringCr( "<H2>connection detail - " + displayYMD + " - id:" + id + "</H2>" );
        f.tableTop(
            new BSOpts()
                .id( "detail-table")
                .fclass("table table-bordered table-striped")
                .border("1")
        );

        // Table Header
        f.tableHeadTop()
            .tableRowTop()
            .tableTh( "Name" )
            .tableTh( "Value" )
            .tableRowBtm()
            .tableHeadBtm();

        HashMap<String,String> lines = new HashMap<String,String>();
        for ( String key: sd.getPropKeyS( id ) ){
            lines.put( key, ToolAny.joinDisp( sd.getPropS( id, key ) ).orElse("???") );
        }

        for ( String key: sd.getPropKeyI( id ) ){
            lines.put( key, ToolAny.joinDispI( sd.getPropI( id, key ) ).orElse("???") );
        }

        String mid = null,qid = null;
        for ( String key: lines.keySet().stream().sorted().toArray(String[]::new) ){
            f.tableRowTop()
                .tableTdHtml( key )
                .tableTdHtml( lines.get(key) )
                .tableRowBtm();
            if ( key.equals( "msgid") ) mid = lines.get(key);
            if ( key.equals( "postfix_queue") ) qid = lines.get(key);
        }

        RspamdLogData rld = this.m_slog.getRspamData(targetDate, qid, mid ).orElse(null);
        if ( rld != null ){
            f.tableRowTop()
                .tableTdHtml( "(RSPAMD)DateTime" )
                .tableTdHtml( ToolDate.Format( rld.m_datetime, "uuuu-MM-dd hh:mm:ss").orElse("???") )
                .tableRowBtm();
            /*
            f.tableRowTop()
                .tableTdHtml( "(RSPAMD)SpamScoreString" )
                .tableTdHtml( rld.m_spam_check_string )
                .tableRowBtm();
            */
            f.tableRowTop()
                .tableTdHtml( "(RSPAMD)SpamScore/Action" )
                .tableTdHtml( rld.getScoreResult() )
                .tableRowBtm();

            f.tableRowTop()
                .tableTdHtml( "(RSPAMD)SpamDetail" )
                .tableTdHtml( String.join("<BR>", rld.getScoreDetail() ) )
//                .tableTdHtml( ToolAny.joinDisp( rld.getScoreDetail() ).orElse("??") )
                .tableRowBtm();

            for ( String key: rld.m_props.keySet() ){
                f.tableRowTop()
                    .tableTdHtml( "(RSPAMD)" + key )
                    .tableTdHtml( rld.m_props.get(key) )
                    .tableRowBtm();
            }
        }

        f.tableBodyBtm();
        f.tableBtm();
        res += f.toString();

        f = BSSForm.newForm();
        f.addStringCr( "<H2>Logs - " + displayYMD + " - id:" + id + "</H2>" );
        f.tableTop(
            new BSOpts()
                .id( "logs-table")
                .fclass("table table-bordered table-striped")
                .border("1")
        );

        // Table Header
        f.tableHeadTop()
            .tableRowTop()
            .tableTh( "Date" )
            .tableTh( "Program" )
            .tableTh( "PID" )
            .tableTh( "message" )
            .tableRowBtm()
            .tableHeadBtm();
        
        LocalDateTime start = null;
        for ( LogBasicData lb: sd.getLog( id ) ){
            if ( start == null ) start = lb.logTime();

            f.tableRowTop()
            .tableTd( ToolDate.Format( lb.logTime(), "HH:mm:ss" ).orElse( "???" ) + "(" + ToolAny.secDiff( start, lb.logTime() ) + "s)" )
            .tableTd( lb.program() )
            .tableTd( lb.pid() + "" )
            .tableTd( lb.message() )
            .tableRowBtm();
        }

        f.tableBodyBtm();
        f.tableBtm();
        
        res += f.toString();
        return res;
    }
}
