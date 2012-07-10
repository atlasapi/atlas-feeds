package org.atlasapi.feeds.upload.persistence;

import org.atlasapi.feeds.upload.FileUploadResult;

import com.metabroadcast.common.base.Maybe;

/**
 * Stores and retrieves upload results for a given remote service (identified by
 * name). Results are stored against a specified result identification key.
 * 
 * @author Fred van den Driessche (fred@metabroadcast.com)
 * 
 */
public interface FileUploadResultStore {

    /**
     * Persist a file upload result.
     * 
     * @param identifier
     *            unique identifier for the result.
     * @param result
     *            the result itself.
     */
    void store(String identifier, FileUploadResult result);

    /**
     * Upload results for a service. Results are returned in reverse
     * chronological order.
     * 
     * @param service
     *            upload service identifier.
     * @return relevant upload results, most recent first.
     */
    Iterable<FileUploadResult> results(String service);


    /**
     * Upload results for a service filtered by identifier prefix. Results are
     * returned in reverse chronological order.
     * 
     * If a full identification key is given as a prefix it is highly likely
     * only one result will be returned.
     * 
     * @param service
     *            upload service identifier.
     * @param identifierPrefix
     *            result identifier filter.
     * @return relevant upload results, most recent first.
     */
    Iterable<FileUploadResult> result(String service, String identifierPrefix);
    
    /**
     * Upload results for a given filename. Results are returned in reverse
     * chronological order.
     * 
     * @param service
     * @param fileName
     * @return
     */
    Maybe<FileUploadResult> latestResultFor(String service, String fileName);
}
