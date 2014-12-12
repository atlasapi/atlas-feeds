package org.atlasapi.feeds.youview.upload;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import tva.metadata._2010.TVAMainType;


public class NoOpYouViewRemoteClient implements YouViewRemoteClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpYouViewRemoteClient.class);
    private final JAXBContext context;
    
    public NoOpYouViewRemoteClient() throws JAXBException {
        this.context = JAXBContext.newInstance("tva.metadata._2010");
        
    }
    @Override
    public YouViewResult upload(JAXBElement<TVAMainType> tvaElem) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(tvaElem, baos);
            log.debug("Uploading " + baos.toString(Charsets.UTF_8.name()));
        } catch (JAXBException | UnsupportedEncodingException e) {
            log.error("Failed to generate XML", e);
        }
        return YouViewResult.success("");
    }

    @Override
    public YouViewResult sendDeleteFor(String remoteId) {
        log.debug("Deleting " + remoteId);
        return YouViewResult.success("");
    }

    @Override
    public YouViewResult checkRemoteStatusOf(String transactionId) {
        return YouViewResult.success("");
    }

}
