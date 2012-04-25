package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.TYPE_ORDERING;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CachingRadioPlayerUploadResultStore implements RadioPlayerUploadResultStore {

    private final RadioPlayerUploadResultStore delegate;
    private final Map<FileType, Map<String, RemoteServiceSpecificResultCache>> fileTypeToRemoteServiceCacheMap;

    public CachingRadioPlayerUploadResultStore(Iterable<String> remoteServiceIds, final RadioPlayerUploadResultStore delegate) {
        this.delegate = delegate;
        
        Builder<FileType, Map<String, RemoteServiceSpecificResultCache>> fileTypeToRemoteServiceCacheMap = ImmutableMap.builder();
        for (FileType type : FileType.values()) {
            Map<String, RemoteServiceSpecificResultCache> remoteServiceCacheMap = Maps.newHashMap();
            for (String remoteService : remoteServiceIds) {
                remoteServiceCacheMap.put(remoteService, new RemoteServiceSpecificResultCache(remoteService));
            }
            fileTypeToRemoteServiceCacheMap.put(type, remoteServiceCacheMap);
        }
        this.fileTypeToRemoteServiceCacheMap = fileTypeToRemoteServiceCacheMap.build();
    }

    @Override
    public void record(RadioPlayerUploadResult result) {
        delegate.record(result);

        Map<String, RemoteServiceSpecificResultCache> remoteServiceCacheMap = fileTypeToRemoteServiceCacheMap.get(result.getType());
        
        RemoteServiceSpecificResultCache remoteServiceCache = remoteServiceCacheMap.get(result.getUpload().remote());
        
        ConcurrentMap<LocalDate,Set<FileUploadResult>> serviceMap = remoteServiceCache.get(result.getService());

        Set<FileUploadResult> current = serviceMap.putIfAbsent(result.getDay(), treeSetWith(result.getUpload()));
        if (current != null) {
            current.remove(result.getUpload());
            current.add(result.getUpload());
        }
    }

    private Set<FileUploadResult> treeSetWith(FileUploadResult result) {
        TreeSet<FileUploadResult> set = Sets.newTreeSet(TYPE_ORDERING);
        set.add(result);
        return set;
    }

    @Override
    public Iterable<FileUploadResult> resultsFor(FileType type, String remoteServiceId, RadioPlayerService service, LocalDate day) {
        return ImmutableSet.copyOf(fileTypeToRemoteServiceCacheMap.get(type).get(remoteServiceId).get(service).get(day));
    }
    
    private class RemoteServiceSpecificResultCache extends ForwardingMap<RadioPlayerService, ConcurrentMap<LocalDate, Set<FileUploadResult>>>{

        private final Map<RadioPlayerService, ConcurrentMap<LocalDate, Set<FileUploadResult>>> cache;
        private final String rsi;
        
        public RemoteServiceSpecificResultCache(String remoteServiceIdentifier) {
            this.rsi = remoteServiceIdentifier;
            this.cache = Maps.newHashMap();
            loadCache();
        }
        
        private void loadCache() {
            for (final RadioPlayerService service : RadioPlayerServices.services) {
                cache.put(service, new MapMaker().softValues().expireAfterWrite(5, TimeUnit.MINUTES).<LocalDate, Set<FileUploadResult>>makeComputingMap(new Function<LocalDate, Set<FileUploadResult>>() {
                    @Override
                    public Set<FileUploadResult> apply(LocalDate day) {
                        TreeSet<FileUploadResult> set = Sets.newTreeSet(TYPE_ORDERING);
                        Iterables.addAll(set, delegate.resultsFor(null, rsi, service, day));
                        return set;
                    }
                }));
            }
        }

        @Override
        protected Map<RadioPlayerService, ConcurrentMap<LocalDate, Set<FileUploadResult>>> delegate() {
            return cache;
        }
    }
}
