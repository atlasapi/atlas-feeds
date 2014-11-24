package org.atlasapi.feeds.youview.upload;

import javax.xml.bind.JAXBElement;

import tva.metadata._2010.TVAMainType;


public interface YouViewRemoteClient {

    /**
     * Upon success, returns the transaction ID associated with the uploaded
     * {@link TVAMainType}, otherwise returns the error provided by YouView
     * @param tvaElem
     */
    YouViewResult upload(JAXBElement<TVAMainType> tvaElem);

    YouViewResult sendDeleteFor(String remoteId);
    
    YouViewResult checkRemoteStatusOf(String transactionId);
}
