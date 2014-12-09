package org.atlasapi.feeds.youview.upload;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.upload.YouViewResult.failure;
import static org.atlasapi.feeds.youview.upload.YouViewResult.success;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.metadata._2010.TVAMainType;

import com.google.common.base.Charsets;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpRequest;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.url.QueryStringParameters;

/**
 * Performs various actions using the YouView CCO HTTPS interface. If the action succeeds,
 * YouView return a 202 ACCEPTED response, along with a Transaction URL in the 'Location'
 * header, with which the resulting transaction's status can be tracked.
 * <p>
 * Upon failure, YouView return an error in the body of the response, which is passed
 * back to the caller.
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public final class HttpYouViewRemoteClient implements YouViewRemoteClient {

    private static final String TRANSACTION_URL_STEM = "/transaction";
    private static final String DELETION_URL_SUFFIX = "/fragment";
    
    private final Logger log = LoggerFactory.getLogger(HttpYouViewRemoteClient.class);
    
    private final SimpleHttpClient httpClient;
    private final String urlBase;
    private final JAXBContext context;

    public HttpYouViewRemoteClient(SimpleHttpClient httpClient, String urlBase) throws JAXBException {
        this.httpClient = checkNotNull(httpClient);
        this.urlBase = checkNotNull(urlBase);
        this.context = JAXBContext.newInstance("tva.metadata._2010");
    }

    @Override
    public YouViewResult upload(JAXBElement<TVAMainType> tvaElem) {
        try {
            String queryUrl = urlBase + TRANSACTION_URL_STEM;
            log.trace(String.format("POSTing YouView output xml to %s", queryUrl));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(tvaElem, baos);

            HttpResponse response = httpClient.post(queryUrl, new StringPayload(baos.toString(Charsets.UTF_8.name())));

            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                String transactionUrl = parseIdFrom(response.header("Location"));
                log.trace("Upload successful. Transaction url: " + transactionUrl);
                return success(transactionUrl);
            }
            
            return failure(response.body());
            
        } catch (JAXBException | IOException | HttpException e) {
            throw new YouViewRemoteClientException("Error uploading content " + tvaElem, e);
        }
    }

    @Override
    public YouViewResult sendDeleteFor(String remoteId) {
        try {
            String queryUrl = buildDeleteQuery(remoteId); 
            log.trace(String.format("Sending Delete request: %s", queryUrl));
            
            HttpResponse response = httpClient.delete(queryUrl);
            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                String transactionUrl = parseIdFrom(response.header("Location"));
                log.trace("Delete successful. Transaction url: " + transactionUrl);
                return success(transactionUrl);
            } 
            
            return failure(response.body());
        } catch (HttpException e) {
            throw new YouViewRemoteClientException("Error deleting id " + remoteId, e);
        }
    }

    private String parseIdFrom(String transactionUrl) {
        return transactionUrl.replace(urlBase + TRANSACTION_URL_STEM + "/", "");
    }

    private String buildDeleteQuery(String remoteId) {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add("id", remoteId);
        return urlBase + DELETION_URL_SUFFIX + "?" + qsp.toQueryString();
    }

    @Override
    public YouViewResult checkRemoteStatusOf(String transactionId) {
        try {
            return httpClient.get(new SimpleHttpRequest<>(buildTransactionUrl(transactionId), new ResultTransformer()));
        } catch (Exception e) {
            throw new RemoteCheckException("error polling status for " + transactionId, e);
        }
    }

    private String buildTransactionUrl(String id) {
        return urlBase + TRANSACTION_URL_STEM + "/" + id;
    }
    
    public static final class ResultTransformer implements HttpResponseTransformer<YouViewResult> {
        
        @Override
        public YouViewResult transform(HttpResponsePrologue prologue, InputStream body)
                throws HttpException, Exception {
            String bodyStr = IOUtils.toString(body, "UTF-8");
            if (prologue.statusCode() == HttpServletResponse.SC_OK) {
                return success(bodyStr);
            }
            return failure(bodyStr);
        }
    }
}
