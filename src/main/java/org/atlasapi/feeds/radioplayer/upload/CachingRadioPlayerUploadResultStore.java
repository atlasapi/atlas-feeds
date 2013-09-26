package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.upload.FileUploadResult.TYPE_ORDERING;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.joda.time.LocalDate;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.concurrent.ExecutionException;

public class CachingRadioPlayerUploadResultStore implements RadioPlayerUploadResultStore {

    private final RadioPlayerUploadResultStore delegate;
    private final Map<FileType, Map<String, CachingRadioPlayerUploadResultStore.RemoteServiceSpecificResultCache>> fileTypeToRemoteServiceCacheMap;

    public CachingRadioPlayerUploadResultStore(Iterable<String> remoteServiceIds, final RadioPlayerUploadResultStore delegate) {
        this.delegate = delegate;
        
        Builder<FileType, Map<String, CachingRadioPlayerUploadResultStore.RemoteServiceSpecificResultCache>> fileTypeToRemoteServiceCacheMap = ImmutableMap.builder();
        for (FileType type : FileType.values()) {
            Map<String, CachingRadioPlayerUploadResultStore.RemoteServiceSpecificResultCache> remoteServiceCacheMap = Maps.newHashMap();
            for (String remoteService : remoteServiceIds) {
                remoteServiceCacheMap.put(remoteService, new CachingRadioPlayerUploadResultStore.RemoteServiceSpecificResultCache(type, remoteService));
            }
            fileTypeToRemoteServiceCacheMap.put(type, remoteServiceCacheMap);
        }
        this.fileTypeToRemoteServiceCacheMap = fileTypeToRemoteServiceCacheMap.build();
    }

    @Override
    public void record(RadioPlayerUploadResult result) {
        delegate.record(result);

        Map<String, CachingRadioPlayerUploadResultStore.RemoteServiceSpecificResultCache> remoteServiceCacheMap = fileTypeToRemoteServiceCacheMap.get(result.getType());

        CachingRadioPlayerUploadResultStore.RemoteServiceSpecificResultCache remoteServiceCache = remoteServiceCacheMap.get(result.getUpload().remote());

        ConcurrentMap<LocalDate,Set<FileUploadResult>> serviceMap = remoteServiceCache.get(result.getService()).asMap();

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
        try {
            return ImmutableSet.copyOf(fileTypeToRemoteServiceCacheMap.get(type).get(remoteServiceId).get(service).get(day));
        } catch (ExecutionException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    @Override
    public List<FileUploadResult> allSuccessfulResults(final String remoteServiceId) {
        return ImmutableList.copyOf(Iterables.concat(Iterables.transform(
                Arrays.asList(FileType.values()), 
                new Function<FileType, Iterable<FileUploadResult>>() {
                    @Override
                    public Iterable<FileUploadResult> apply(FileType input) {
                        return getSuccessfulResultsForFileTypeAndService(input, remoteServiceId);
                    }
                }
        )));
    }
    
    // TODO break this up a little
    private Iterable<FileUploadResult> getSuccessfulResultsForFileTypeAndService(FileType type, String remoteServiceId) {
        final RemoteServiceSpecificResultCache resultCache = fileTypeToRemoteServiceCacheMap.get(type).get(remoteServiceId);
        return Iterables.concat(Iterables.transform(
                RadioPlayerServices.all.entrySet(), 
                new Function<Entry<String, RadioPlayerService>, Iterable<FileUploadResult>>() {
                    @Override
                    public Iterable<FileUploadResult> apply(Entry<String, RadioPlayerService> input) {
                        LoadingCache<LocalDate,Set<FileUploadResult>> loadingCache = resultCache.get(input.getValue());
                        return Iterables.filter(
                                Iterables.concat(loadingCache.asMap().values()),
                                FileUploadResult.SUCCESSFUL
                        );
                    }
                }
        ));
    }
    
    private class RemoteServiceSpecificResultCache extends ForwardingMap<RadioPlayerService, LoadingCache<LocalDate, Set<FileUploadResult>>>{

        private final Map<RadioPlayerService, LoadingCache<LocalDate, Set<FileUploadResult>>> cache;
        private final String rsi;
        private final FileType type;
        
        public RemoteServiceSpecificResultCache(FileType type, String remoteServiceIdentifier) {
            this.type = type;
            this.rsi = remoteServiceIdentifier;
            this.cache = Maps.newHashMap();
            loadCache();
        }
        
        private void loadCache() {
            for (final RadioPlayerService service : RadioPlayerServices.services) {
                cache.put(service, CacheBuilder.newBuilder().softValues().expireAfterWrite(5, TimeUnit.MINUTES).<LocalDate, Set<FileUploadResult>>build(new CacheLoader<LocalDate, Set<FileUploadResult>>() {
                    @Override
                    public Set<FileUploadResult> load(LocalDate day) {
                        TreeSet<FileUploadResult> set = Sets.newTreeSet(TYPE_ORDERING);
                        Iterables.addAll(set, delegate.resultsFor(type, rsi, service, day));
                        return set;
                    }
                }));
            }
        }

        @Override
        protected Map<RadioPlayerService, LoadingCache<LocalDate, Set<FileUploadResult>>> delegate() {
            return cache;
        }
    }
}
