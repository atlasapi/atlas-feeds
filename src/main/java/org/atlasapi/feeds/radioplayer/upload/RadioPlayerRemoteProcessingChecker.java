package org.atlasapi.feeds.radioplayer.upload;

import java.util.Map;
import java.util.Map.Entry;

import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.atlasapi.feeds.upload.ftp.CommonsDirectoryLister;
import org.atlasapi.persistence.logging.AdapterLog;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.metabroadcast.common.scheduling.ScheduledTask;

public class RadioPlayerRemoteProcessingChecker extends ScheduledTask {

    private final Map<String, CommonsDirectoryLister> serviceDetails;
    private final RadioPlayerUploadResultStore resultStore;
    private final AdapterLog log;

    public RadioPlayerRemoteProcessingChecker(Map<String,RemoteServiceDetails> serviceDetails, RadioPlayerUploadResultStore resultStore, AdapterLog log) {
        this.serviceDetails = Maps.transformValues(serviceDetails, new Function<RemoteServiceDetails, CommonsDirectoryLister>() {
            @Override
            public CommonsDirectoryLister apply(RemoteServiceDetails input) {
                return new CommonsDirectoryLister(input);
            }
        });
        this.resultStore = resultStore;
        this.log = log;
    }
    
    @Override
    protected void runTask() {
        for (Entry<String, CommonsDirectoryLister> service : serviceDetails.entrySet()) {
            new RadioPlayerSuccessChecker(service.getKey(), service.getValue(), resultStore, log).run();
        }
    }

}
