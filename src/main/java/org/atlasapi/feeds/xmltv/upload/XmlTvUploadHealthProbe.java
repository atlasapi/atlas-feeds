package org.atlasapi.feeds.xmltv.upload;

import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.FAILURE;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.INFO;
import static com.metabroadcast.common.health.ProbeResult.ProbeResultType.SUCCESS;
import static org.atlasapi.feeds.upload.FileUploadResult.DATE_ORDERING;
import static org.atlasapi.feeds.upload.FileUploadResult.TYPE_ORDERING;

import java.util.Collection;
import java.util.Map.Entry;

import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.atlasapi.feeds.xmltv.XmlTvChannelLookup;
import org.atlasapi.media.entity.Channel;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;

public class XmlTvUploadHealthProbe implements HealthProbe {

    protected static final String DATE_TIME = "dd/MM/yy HH:mm:ss";

    private final XmlTvChannelLookup channels;
    private final FileUploadResultStore resultStore;

    public XmlTvUploadHealthProbe(XmlTvChannelLookup channels, FileUploadResultStore resultStore) {
        this.channels = channels;
        this.resultStore = resultStore;
    }
    
    @Override
    public ProbeResult probe() throws Exception {
        final ProbeResult probeResult = new ProbeResult(title());
        
        Iterable<FileUploadResult> results = FileUploadResult.NAME_ORDERING.immutableSortedCopy(resultStore.results("xmltv"));
        
        Multimap<String,FileUploadResult> groupedResults = Multimaps.index(results, new Function<FileUploadResult, String>() {
            @Override
            public String apply(FileUploadResult input) {
                return input.filename();
            }
        });
        
        addChannelsDatEntry(probeResult, groupedResults);
        
        for (Entry<Integer, Channel> channel : channels.entrySet()) {
            String entryKey = String.format("%s (%s)", channel.getValue(), channel.getValue().title());
            Collection<FileUploadResult> resultsGroup = groupedResults.get(String.format("%s.dat",channel.getKey()));
            
            ProbeResultEntry entry;
            if(resultsGroup.isEmpty()) {
                entry = new ProbeResultEntry(INFO, entryKey, "No results");
            } else {
                entry = probeEntryFor(entryKey, resultsGroup);
            }
            probeResult.addEntry(entry);
        }
       
        return probeResult;
    }

    private void addChannelsDatEntry(final ProbeResult probeResult, Multimap<String, FileUploadResult> groupedResults) {
        String key = "channels.dat";
        Collection<FileUploadResult> channelsResults = groupedResults.get(key);
        ProbeResultEntry entry;
        if(channelsResults.isEmpty()) {
            entry = new ProbeResultEntry(INFO, key, "No results");
        } else {
            entry = probeEntryFor(key, channelsResults);
        }
        probeResult.addEntry(entry);
    }

    private ProbeResultEntry probeEntryFor(String entryKey, Collection<FileUploadResult> resultsGroup) {
        return new ProbeResultEntry(entryType(resultsGroup), entryKey, probeValue(resultsGroup));
    }

    private String probeValue(Collection<FileUploadResult> results) {
        StringBuilder builder = new StringBuilder("<table>");
        for (FileUploadResult result : Iterables.limit(TYPE_ORDERING.sortedCopy(results),2)) {
            appendResult(builder, result);
        }
        return builder.append("</table>").toString();
    }
    
    private void appendResult(StringBuilder builder, FileUploadResult result) {
        builder.append("<tr><td>Last ");
        builder.append(result.type().toNiceString());
        builder.append(": ");
        builder.append(result.uploadTime().toString(DATE_TIME));
        builder.append("</td><td>");
        if (result.message() != null) {
            builder.append(result.message());
        } else {
            if (FileUploadResultType.SUCCESS == result.type()) {
                builder.append("File uploaded successfully");
            } else if (result.exceptionSummary() != null && result.exceptionSummary().message() != null) {
                builder.append(result.exceptionSummary().message());
            }
        }
        builder.append("</td></tr>");
    }

    private ProbeResultType entryType(Collection<FileUploadResult> results) {
        FileUploadResult mostRecentResult = DATE_ORDERING.max(results);
        switch (mostRecentResult.type()) {
        case SUCCESS:
            return SUCCESS;
        case FAILURE:
                return FAILURE;
        default:
            return INFO;
        }
    }

    @Override
    public String title() {
        return "XMLTV";
    }

    @Override
    public String slug() {
        return "xmltv";
    }

}
