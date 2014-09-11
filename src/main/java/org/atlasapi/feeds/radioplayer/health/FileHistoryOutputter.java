package org.atlasapi.feeds.radioplayer.health;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.FAILURE;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.metabroadcast.common.media.MimeType;


public class FileHistoryOutputter {
    
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormat.forPattern("yyyyMMdd");
    private static final DateTimeFormatter LONG_DATE_PATTERN = ISODateTimeFormat.date(); 
    private static final DateTimeFormatter DATE_TIME_PATTERN = ISODateTimeFormat.basicDateTimeNoMillis();
    private static final Gson gson = new GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
            .create();
    
    private FileHistoryOutputter() {
        // private constructor for static helper class
    }

    public static void printJsonResponse(HttpServletResponse response, FileHistory fileHistory) throws IOException {
        response.setContentType(MimeType.APPLICATION_JSON.toString());
        
        PrintWriter writer = response.getWriter();
        gson.toJson(fileHistory, writer);
        writer.flush();
    }
    
    public static void printHtmlResponse(HttpServletResponse response, FileHistory fileHistory, boolean success) throws IOException {
        response.setContentType(MimeType.TEXT_HTML.toString());
        response.setCharacterEncoding(Charsets.UTF_8.toString());
        
        PrintWriter out = response.getWriter();
        printHeader(out, success);
        
        printFileDetails(out, fileHistory.file());
        
        printAttemptTable(out,fileHistory, success);
        
        printFooter(out);
        
        out.flush();
    }
    
    private static void printAttemptTable(PrintWriter out, FileHistory fileHistory, boolean success) {
        printTableHeader(out, fileHistory, success);
        for (UploadAttempt attempt : fileHistory.uploadAttempts()) {
            printUploadAttempt(out, attempt);
        }
        out.println("</table>");
    }

    private static void printTableHeader(PrintWriter out, FileHistory fileHistory, boolean success) {
        out.println("<table><tr class=\"tableheader\"><th colspan=\"3\">" + createFileName(fileHistory.file()) + "</th></tr>");
        out.println("<tr class=\"" + (success ? SUCCESS : FAILURE) + "\"><td>Upload Time</td><td>Upload</td><td>Remote Check</td></tr>");
    }
    
    private static void printUploadAttempt(PrintWriter out, UploadAttempt attempt) {
        StringBuilder attemptStr = new StringBuilder();
        
        attemptStr.append("<tr class=\"");
        attemptStr.append(attempt.uploadResult().toString().toLowerCase());
        attemptStr.append("\"><td>");
        attemptStr.append(attempt.uploadTime() != null ? DATE_TIME_PATTERN.print(attempt.uploadTime()) : "");
        attemptStr.append("</td><td>");
        attemptStr.append(buildUploadInfo(attempt));
        attemptStr.append("</td><td>");
        attemptStr.append(buildRemoteCheckInfo(attempt));
        attemptStr.append("</td></tr>");
        
        out.println(attemptStr.toString());
    }
    
    public static void printFileDetails(PrintWriter out, RadioPlayerFile file) {
        out.println(printFileDetails(file));
    }

    public static String printFileDetails(RadioPlayerFile file) {
        StringBuilder details = new StringBuilder();
        
        details.append("<table>");
        details.append(toTableRow("Filename:", linkedFilename(file)));
        details.append(toTableRow("Upload Service:", linkedUploadService(file.uploadService())));
        details.append(toTableRow("RP Service:", linkedService(file.uploadService(), file.service())));
        details.append(toTableRow("File Type:", file.type().name()));
        details.append(toTableRow("Date:", LONG_DATE_PATTERN.print(file.date())));
        details.append(toTableRow(uploadButton(file), fileHistoryButton(file)));
        details.append("</table>");
        
        return details.toString();
    }

    private static String linkedUploadService(UploadService uploadService) {
        return String.format("<a href=\"/feeds/ukradioplayer/health/%1$s\">%1$s</a>", uploadService.name().toLowerCase());
    }
    
    private static String linkedService(UploadService uploadService, RadioPlayerService service) {
        return String.format("<a href=\"/feeds/ukradioplayer/health/%s/services/%d\">[%d] %s</a>", uploadService.name().toLowerCase(),  service.getRadioplayerId(), service.getRadioplayerId(), service.getName());
    }

    public static String buildUploadInfo(UploadAttempt attempt) {
        StringBuilder uploadInfo = new StringBuilder();
        
        uploadInfo.append("<table width=\"100%\">");
        uploadInfo.append(toTableRow("Status:", attempt.uploadResult().name()));
        for (Entry<String, String> entry : attempt.uploadDetails().entrySet()) {
            uploadInfo.append(toTableRow(entry.getKey(), entry.getValue()));
        }
        uploadInfo.append("</table>");
        
        return uploadInfo.toString();
    }

    public static String buildRemoteCheckInfo(UploadAttempt attempt) {
        StringBuilder remoteCheckInfo = new StringBuilder();
        
        String status = attempt.remoteCheckResult() == null ? "Not yet checked" : attempt.remoteCheckResult().name();
        String message = attempt.remoteCheckMessage() == null ? "" : attempt.remoteCheckMessage();
        
        remoteCheckInfo.append("<table>");
        remoteCheckInfo.append(toTableRow("Status:", status));
        remoteCheckInfo.append(toTableRow("Remote System Response:", message));
        remoteCheckInfo.append("</table>");
        
        return remoteCheckInfo.toString();
    }
    
    /**
     * transforms a key/value pair into an html table row
     */
    public static String toTableRow(String key, String value) {
        StringBuilder row = new StringBuilder();
        
        row.append("<tr><td>");
        row.append(key);
        row.append("</td>");
        row.append("<td>");
        row.append(value);
        row.append("</td></tr>");
        
        return row.toString();
    }

    public static String uploadButton(RadioPlayerFile file) {
        String postTarget = String.format(
                "/feeds/ukradioplayer/upload/%s/%s/%s", 
                file.uploadService().name().toLowerCase(), 
                file.type().name(), 
                file.service().getRadioplayerId()
        );
        if(file.date() != null) {
            postTarget += file.date().toString("/yyyyMMdd");
        }
        return "<form style=\"text-align:center, padding-bottom: 10px\" action=\""+postTarget+"\" method=\"post\"><input type=\"submit\" value=\"Update File Now\"/></form>";
    }
    
    private static String fileHistoryButton(RadioPlayerFile file) {
        String getTarget = String.format(
                "/feeds/ukradioplayer/health/%s/services/%d/files/%s/%s", 
                file.uploadService().name().toLowerCase(), 
                file.service().getRadioplayerId(), 
                file.type().name(), 
                DATE_PATTERN.print(file.date())
        );
        return "<form style=\"text-align:center, padding-bottom: 10px\" action=\""+getTarget+"\" method=\"get\"><input type=\"submit\" value=\"View File Upload History\"/></form>";
    }

    private static String linkedFilename(RadioPlayerFile file) {
        return String.format(
                "<a href=\"/feeds/ukradioplayer/%1$s_%2$s_%3$s.xml\">%1$s_%2$s_%3$s.xml</a>", 
                DATE_PATTERN.print(file.date()), 
                file.service().getRadioplayerId(), 
                file.type().name()
        );
    }

    private static String createFileName(RadioPlayerFile file) {
        return String.format(
                "%s_%d_%s.xml", 
                DATE_PATTERN.print(file.date()), 
                file.service().getRadioplayerId(), 
                file.type().name()
        );
    }
    
    private static void printHeader(PrintWriter out, boolean success) throws IOException {
        StringBuilder header = new StringBuilder();
        
        header.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        header.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        header.append("<head><title>File Upload History</title>");
        header.append("<style>");
        header.append("* {font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-weight: 300;color: #333;}");
        header.append("h1 {font-size:250%; font-weight:100; margin:10px 0; text-align:center; width:500px;}");
        header.append(".overall {border: 1px solid #999;display: block;float: left;height: 50px;width: 50px;}");
        header.append("table { padding: 10px 0; width:550px;}");
        header.append("th {background-color: #EEE; border:solid #DFDFDF; border-width:1px 0px;}"); 
        header.append(".success { background-color: #9F9; }");
        header.append(".failure { background-color: #F44; }");
        header.append("td {padding: 1px 5px;}");
        header.append("</style></head>");
        header.append("<body><h1>File Upload History</h1>");
        header.append("<table id=\"js_toggle\"><tr></tr></table>");
        
        header.append("<script src=\"//code.jquery.com/jquery-1.11.0.min.js\"></script>");
        header.append("<script type=\"text/javascript\">");
        header.append("$(function() {");
        header.append("$(['success', 'info', 'failure']).each(function(foo,label) {");
        header.append("$('#js_toggle').find('tr').after($('<td><button>toggle '+label+'</button></td>').click(function() {");
        header.append("$('tr.'+label).toggle();");
        header.append("}));");
        header.append("});");
        header.append("});");
        header.append("</script>");
        
        out.println(header.toString());
    }
    
    private static void printFooter(PrintWriter out) throws IOException {
        out.println("</body></html>");
    }
    
    public static String createJsToggleCode() {
        StringBuilder script = new StringBuilder();

        script.append("<script src=\"//code.jquery.com/jquery-1.11.0.min.js\"></script>");
        script.append("<script type=\"text/javascript\">");
        script.append("$(function() {");
        script.append("$(['success', 'info', 'failure']).each(function(foo,label) {");
        script.append("$('h1').after($('<button>toggle '+label+'</button>').click(function() {");
        script.append("$('tr.'+label).toggle();");
        script.append("}));");
        script.append("});");
        script.append("});");
        script.append("</script>");

        return script.toString();
    }
}
