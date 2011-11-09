package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;

import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;

import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;

public class RadioPlayerServerHealthProbe implements HealthProbe {

    private final String serviceIdentifier;
    private final FileUploadResultStore resultStore;

    public RadioPlayerServerHealthProbe(String serviceIdentifier, FileUploadResultStore resultStore) {
        this.serviceIdentifier = serviceIdentifier;
        this.resultStore = resultStore;
    }

    @Override
    public ProbeResult probe() {
        return new ProbeResult(title()).addEntry(buildEntry());
    }

    private ProbeResultEntry buildEntry() {
        FileUploadResult[] results = latestResults();
        return entryFor(results[0], results[1]);
    }

    private FileUploadResult[] latestResults() {
        FileUploadResult success = null, failure = null;
        for (FileUploadResult result : resultStore.results(serviceIdentifier)) {
            if(result.successfulConnection()) {
                success = success == null ? result : success;
            } else {
                failure = failure == null ? result : failure;
            }
            if(success != null && failure != null) {
                break;
            }
        }
        return new FileUploadResult[]{success, failure};
    }

    private ProbeResultEntry entryFor(FileUploadResult success, FileUploadResult failure) {
        if (success == null && failure == null) {
            return new ProbeResultEntry(INFO, serviceIdentifier, "No Data");
        }

        String value = buildValue(success, failure);

        FileUploadResult mostRecent = failure == null ? success : (success == null ? failure : success.uploadTime().isAfter(failure.uploadTime()) ? success : failure);
        return new ProbeResultEntry(resultType(mostRecent), serviceIdentifier, value);
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

    private void buildResult(StringBuilder builder, FileUploadResult result) {
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
        return "Connection";
    }

    @Override
    public String slug() {
        return "ukrp-connect-" + serviceIdentifier;
    }

}
