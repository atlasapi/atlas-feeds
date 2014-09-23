package org.atlasapi.feeds.radioplayer.upload.queue;

import java.util.Map;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;


public class RemoteCheckerSupplier {
    
    private static final Function<RemoteCheckServiceProvider, UploadService> TO_KEY = new Function<RemoteCheckServiceProvider, UploadService>() {
        @Override
        public UploadService apply(RemoteCheckServiceProvider input) {
            return input.remoteService();
        }
    };
    
    private final Map<UploadService, RemoteCheckServiceProvider> checkers;

    public RemoteCheckerSupplier(Iterable<RemoteCheckServiceProvider> checkers) {
        this.checkers = Maps.uniqueIndex(checkers, TO_KEY);
        
    }

    public Optional<RemoteCheckService> get(UploadService remoteService, DateTime uploadTime, FileType type) {
        RemoteCheckServiceProvider checkerProvider = checkers.get(remoteService);
        if (checkerProvider == null) {
            return Optional.absent();
        }
        return Optional.of(checkerProvider.get(uploadTime, type));
    }
}
