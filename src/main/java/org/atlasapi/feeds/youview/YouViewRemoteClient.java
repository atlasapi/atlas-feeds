package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.ids.IdParser;
import org.atlasapi.feeds.youview.ids.PublisherIdUtility;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import tva.metadata._2010.TVAMainType;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.url.QueryStringParameters;


public class YouViewRemoteClient {
    
    private static final Ordering<Content> HIERARCHICAL_ORDER = new Ordering<Content>() {
        @Override
        public int compare(Content left, Content right) {
            if (left instanceof Item) {
                if (right instanceof Item) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (left instanceof Series) {
                if (right instanceof Item) {
                    return -1;
                } else if (right instanceof Series) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if (right instanceof Brand) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    };
    
    private final class JaxbErrorHandler implements ErrorHandler {
        
        private boolean hasErrors = false;
        
        @Override
        public void warning(SAXParseException e) throws SAXException {
            log.error("XML Validation warning: " + e.getMessage(), e);
            hasErrors = true;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            log.error("XML Validation fatal error: " + e.getMessage(), e);
            hasErrors = true;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            log.error("XML Validation error: " + e.getMessage(), e);
            hasErrors = true;
        }
        
        public boolean hasErrors() {
            return hasErrors;
        }
    }

    private static final String UPLOAD_URL_SUFFIX = "/transaction";
    private static final String DELETION_URL_SUFFIX = "/fragment";
    private static final String DELETION_TYPE = "id";

    private final Logger log = LoggerFactory.getLogger(YouViewRemoteClient.class);
    
    private final TvAnytimeGenerator generator;
    private final YouViewPerPublisherFactory publisherConfig;
    private final TransactionStore transactionStore;
    private final Clock clock;
    private final boolean performValidation;
    
    public YouViewRemoteClient(TvAnytimeGenerator generator, YouViewPerPublisherFactory configurationFactory, 
            TransactionStore transactionStore, Clock clock, boolean performValidation) {
        this.publisherConfig = checkNotNull(configurationFactory);
        this.generator = checkNotNull(generator);
        this.transactionStore = checkNotNull(transactionStore);
        this.clock = checkNotNull(clock);
        this.performValidation = performValidation;
    }
    
    /**
     * <p>Given an Iterable<Content>, generates YouView TVAnytime XML and uploads it to YouView.
     * The configuration used for uploading is based upon the publisher of each piece of content.</p>
     * <p>N.B. The credentials used for upload are chosen from the publisher of the first piece of content.</p> 
     * @param chunk
     * @throws UnsupportedEncodingException
     * @throws HttpException
     */
    // TODO consider passing in publisher as param, validating chunk
    // TODO refactor this, there's too much going on here.
    // TODO JAXBContext/Marshaller is instantiated here as well as in generator - can they be passed around as context/
    // refactored out?
    public void upload(Iterable<Content> chunk) throws UnsupportedEncodingException, HttpException {
        Content first = Iterables.getFirst(chunk, null);
        if (first == null) {
            log.error("Chunk contained no content");
            return;
        }
        Publisher publisher = first.getPublisher();
        SimpleHttpClient httpClient = publisherConfig.getHttpClient(publisher);
        PublisherIdUtility config = publisherConfig.getIdUtil(publisher);
        
        String queryUrl = config.getYouViewBaseUrl() + UPLOAD_URL_SUFFIX;
        log.trace(String.format("Posting YouView output xml to %s", queryUrl));

        try {
            JAXBContext context = JAXBContext.newInstance("tva.metadata._2010");
            Marshaller marshaller = context.createMarshaller();

            JAXBElement<TVAMainType> tvaElem = generator.generateTVAnytimeFrom(chunk);

            if (performValidation) {
                validateXml(context, marshaller, tvaElem);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(tvaElem, baos);

            HttpResponse response = httpClient.post(queryUrl, new StringPayload(baos.toString(Charsets.UTF_8.name())));

            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                String transactionUrl = response.header("Location");
                log.info("Upload successful. Transaction url: " + transactionUrl);
                // TODO store transactions
                //            DateTime uploadTimestamp = clock.now();
                //            Map<Content, Duration> contentLatencies = calculateLatencies(chunk, uploadTimestamp);
                //            transactionStore.save(transactionUrl, publisher, contentLatencies);
            } else {
                throw new RuntimeException(String.format("An Http status code of %s was returned when POSTing to YouView. Error message:\n%s", response.statusCode(), response.body()));
            }
        } catch (JAXBException | SAXException | IOException e) {
            throw Throwables.propagate(e);
        }
    }
    
    // TODO move validation step to somewhere logical.
    private void validateXml(JAXBContext context, Marshaller marshaller,
            JAXBElement<TVAMainType> rootElem) throws JAXBException, SAXException, IOException {
        JAXBSource source = new JAXBSource(context, rootElem);

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); 
        Schema schema = sf.newSchema(new File("../atlas-feeds/src/main/resources/tvanytime/youview/youview_metadata_2011-07-06.xsd")); 

        Validator validator = schema.newValidator();
        JaxbErrorHandler errorHandler = new JaxbErrorHandler();
        validator.setErrorHandler(errorHandler);

        validator.validate(source);
        
        if (errorHandler.hasErrors()) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            marshaller.marshal(rootElem, os);
            log.trace("Invalid xml was: {}", os.toString());
            throw new RuntimeException("XML Validation against schema failed");
        }
    }
    
    private Map<Content, Duration> calculateLatencies(Iterable<Content> content,
            final DateTime uploadTimestamp) {
        return Maps.toMap(content, new Function<Content, Duration>() {
            @Override
            public Duration apply(Content input) {
                return new Duration(input.getLastUpdated(), uploadTimestamp);
            }
        });
    }

    public static List<Content> orderContentForDeletion(Iterable<Content> toBeDeleted) {
        return HIERARCHICAL_ORDER.immutableSortedCopy(toBeDeleted);
    }
    
    public boolean sendDeleteFor(Content content) {
        if (content instanceof Item) {
            return sendDelete((Item) content);
        } else if (content instanceof Series) {
            return sendDelete((Series) content);
        } else if (content instanceof Brand) {
            return sendDelete((Brand) content);
        } else {
            throw new IllegalArgumentException(String.format("content type %s not recognised. Uri: %s", content.getClass(), content.getCanonicalUri()));
        }
    }
    
    // TODO does this need two deletes for the item crid? surely it only needs deletes for crid, version-crid and imi?
    private boolean sendDelete(Item item) {
        Publisher publisher = item.getPublisher();
        IdParser idParser = publisherConfig.getIdParser(publisher);
        PublisherIdUtility config = publisherConfig.getIdUtil(publisher);
        SimpleHttpClient httpClient = publisherConfig.getHttpClient(publisher);
        
        return sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), item))
//                && sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), item))
                && sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createVersionCrid(config.getCridPrefix(), item))
                && sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createImi(config.getImiPrefix(), item));
    }
    
    private boolean sendDelete(Series series) {
        Publisher publisher = series.getPublisher();
        IdParser idParser = publisherConfig.getIdParser(publisher);
        PublisherIdUtility config = publisherConfig.getIdUtil(publisher);
        SimpleHttpClient httpClient = publisherConfig.getHttpClient(publisher);
        
        return sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), series));
    }
    
    private boolean sendDelete(Brand brand) {
        Publisher publisher = brand.getPublisher();
        IdParser idParser = publisherConfig.getIdParser(publisher);
        PublisherIdUtility config = publisherConfig.getIdUtil(publisher);
        SimpleHttpClient httpClient = publisherConfig.getHttpClient(publisher);
        
        return sendDelete(httpClient, config.getYouViewBaseUrl(), idParser.createCrid(config.getCridPrefix(), brand));
    }
    
    private boolean sendDelete(SimpleHttpClient httpClient, String baseUrl, String id) {
        QueryStringParameters qsp = new QueryStringParameters();
        qsp.add(DELETION_TYPE, id);
        String queryUrl = baseUrl + DELETION_URL_SUFFIX;
        log.info(String.format("Deleting YouView content with %s %s at %s", DELETION_TYPE, id, queryUrl));
        try {
            HttpResponse response = httpClient.delete(queryUrl + "?" + qsp.toString());
            if (response.statusCode() == HttpServletResponse.SC_ACCEPTED) {
                log.info("Response: " + response.header("Location"));
                return true;
            } else {
                log.error(String.format("An Http status code of %s was returned when POSTing to YouView. Error message:\n%s", response.statusCode(), response.body()));
                return false;
            }
        } catch (HttpException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
