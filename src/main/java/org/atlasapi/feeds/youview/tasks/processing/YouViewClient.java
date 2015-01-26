package org.atlasapi.feeds.youview.tasks.processing;

import org.atlasapi.feeds.youview.tasks.Payload;
import org.atlasapi.feeds.youview.upload.YouViewResult;


public interface YouViewClient {

    /**
     * Upon success, returns the transaction ID associated with the deletion
     * performed, otherwise returns the error provided by YouView
     * @param elementId the element to be deleted from the YouView environment
     */
    YouViewResult delete(String elementId);

    /**
     * Upon success, returns the transaction ID associated with the upload
     * performed, otherwise returns the error provided by YouView
     * @param payload the stringified TVAnytime XML to be uploaded to the 
     * YouView environment
     */
    YouViewResult upload(Payload payload);

    /**
     * Upon success, returns the status of the transaction ID provided, otherwise 
     * returns the error provided by YouView
     * @param transactionId the unique ID identifying the transaction to be checked
     */
    YouViewResult checkRemoteStatus(String transactionId);

}
