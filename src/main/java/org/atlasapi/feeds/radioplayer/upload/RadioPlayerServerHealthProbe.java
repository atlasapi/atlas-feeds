package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;

import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.atlasapi.feeds.upload.persistence.FileUploadResultTranslator;

import com.google.common.collect.Iterables;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class RadioPlayerServerHealthProbe implements HealthProbe {

    private final DBCollection results;
    private final RemoteServiceDetails credentials;
    private final FileUploadResultTranslator translator;

    public RadioPlayerServerHealthProbe(DatabasedMongo mongo, RemoteServiceDetails credentials) {
        this.results = mongo.collection("radioplayer");
        this.credentials = credentials;
        this.translator = new FileUploadResultTranslator();
    }

    @Override
    public ProbeResult probe() {
        return new ProbeResult(title()).addEntry(buildEntry());
    }

    private ProbeResultEntry buildEntry() {
        return entryFor(connection(true), connection(false));
    }

    private FileUploadResult connection(boolean successfulConnection) {
        DBObject first = Iterables.getFirst(results.find(new BasicDBObject("connected", successfulConnection)).sort(new BasicDBObject("time", -1)).limit(1), null);
        if (first != null) {
            return translator.fromDBObject(first);
        }
        return null;
    }

    private ProbeResultEntry entryFor(FileUploadResult success, FileUploadResult failure) {
        if (success == null && failure == null) {
            return new ProbeResultEntry(INFO, credentials.toString(), "No Data");
        }

        String value = buildValue(success, failure);

        FileUploadResult mostRecent = failure == null ? success : (success == null ? failure : success.uploadTime().isAfter(failure.uploadTime()) ? success : failure);
        return new ProbeResultEntry(resultType(mostRecent), credentials.toString(), value);
    }

    private ProbeResultType resultType(FileUploadResult mostRecent) {
        if (mostRecent.successfulConnection()) {
            return SUCCESS;
        } else {
            return FAILURE;
        }
    }

    private String buildValue(FileUploadResult success, FileUploadResult failure) {
        StringBuilder builder = new StringBuilder("<table>");
        if (success != null) {
            buildResult(builder, success);
        }
        if (failure != null) {
            buildResult(builder, failure);
        }
        return builder.append("</table>").toString();
    }

    public void buildResult(StringBuilder builder, FileUploadResult result) {
        builder.append("<tr><td>Last ");
        builder.append(result.successfulConnection() ? "Success" : "Failure");
        builder.append(": ");
        builder.append(result.uploadTime().toString("dd/MM/yy HH:mm:ss"));
        builder.append("</td><td>");
        if (result.successfulConnection()) {
            builder.append("Connected successfully");
        } else if (result.message() != null && !result.successfulConnection()) {
            builder.append(result.message());
        }
        builder.append("</td></tr>");
    }

    @Override
    public String title() {
        return "FTP";
    }

    @Override
    public String slug() {
        return "ukrpFTP";
    }

}
