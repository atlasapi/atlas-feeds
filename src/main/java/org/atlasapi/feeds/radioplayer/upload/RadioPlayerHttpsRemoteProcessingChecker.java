package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import org.atlasapi.feeds.radioplayer.RadioPlayerFilenameMatcher;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.persistence.logging.AdapterLog;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.scheduling.ScheduledTask;

public class RadioPlayerHttpsRemoteProcessingChecker extends ScheduledTask {

    private final SimpleHttpClient httpClient;
    private final String service;
    private final RadioPlayerUploadResultStore resultStore;
    private final AdapterLog log;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(FileUploadResultType.class, new RadioPlayerFileUploadResultTypeDeserializer())
            .create();

    public RadioPlayerHttpsRemoteProcessingChecker(SimpleHttpClient httpClient, String service, RadioPlayerUploadResultStore resultStore, AdapterLog log) {
        this.httpClient = httpClient;
        this.service = service;
        this.resultStore = resultStore;
        this.log = log;
    }
    
    @Override
    public void runTask() {
        try {
            pollStatuses();
        } catch (Exception e) {
            log.record(errorEntry().withDescription("Exception checking remote processing (over HTTPS) for " + service).withCause(e));
        }
    }

    private void pollStatuses() throws HttpException, Exception {
        List<FileUploadResult> allResults = resultStore.allUnknownResults(service);
        
        List<FileUploadResult> unknowns = unknownsFrom(allResults);
        
        for (FileUploadResult unknown : unknowns) {
            FileUploadResultType newResult = performRemoteCheck(unknown);
            
            RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(unknown.filename().trim().replace(".xml", ""));
            for (FileUploadResult result : getCurrentResults(matcher)) {
                resultStore.record(radioPlayerResult(matcher, result.withRemoteProcessingResult(newResult)));
            }
        }
    }

    private FileUploadResultType performRemoteCheck(FileUploadResult fileResult) throws HttpException, Exception {
        if (!fileResult.remoteProcessingResult().equals(FileUploadResultType.UNKNOWN)) {
            return fileResult.type();
        }
        
        String message = fileResult.message();
        if (message == null) {
            return FileUploadResultType.FAILURE;
        }
        
        return httpClient.get(SimpleHttpRequest.httpRequestFrom(message, new HttpResponseTransformer<FileUploadResultType>() {

            @Override
            public FileUploadResultType transform(HttpResponsePrologue prologue, InputStream body)
                    throws HttpException, Exception {
                if (prologue.statusCode() == HttpStatusCode.NOT_FOUND.code()) {
                    return FileUploadResultType.FAILURE;
                }
                return gson.fromJson(new InputStreamReader(body, Charsets.UTF_8), FileUploadResultType.class);
            }
        }));
        
    }
    
    private List<FileUploadResult> unknownsFrom(List<FileUploadResult> allResults) {
        return ImmutableList.copyOf(Iterables.filter(allResults, new Predicate<FileUploadResult>() {
            @Override
            public boolean apply(FileUploadResult input) {
                return FileUploadResultType.UNKNOWN.equals(input.type());
            }
        }));
    }

    private RadioPlayerUploadResult radioPlayerResult(RadioPlayerFilenameMatcher matcher, FileUploadResult result) {
        return new RadioPlayerUploadResult(matcher.type().requireValue(), matcher.service().requireValue(), matcher.date().requireValue(), result);
    }

    private Set<FileUploadResult> getCurrentResults(RadioPlayerFilenameMatcher matcher) {
        if (RadioPlayerFilenameMatcher.hasMatch(matcher)) {
            return ImmutableSet.copyOf(resultStore.resultsFor(matcher.type().requireValue(), service, matcher.service().requireValue(), matcher.date().requireValue()));
        }
        return ImmutableSet.of();
    }
}
