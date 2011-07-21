package org.atlasapi.feeds.interlinking.delta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.feeds.interlinking.InterlinkFeed;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeed;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.feeds.interlinking.www.InterlinkController;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.joda.time.DateTime;

import com.google.common.base.Charsets;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.http.HttpStatusCode;

public class InterlinkingDeltaUpdater {

    public static final String BUCKET_NAME = "bbc-interlinking";
    public static final String FOLDER_NAME = "feeds/bbc-interlinking";
    private final Log log = LogFactory.getLog(getClass());

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    private final LastUpdatedContentFinder contentFinder;
    private final InterlinkFeedOutputter outputter;
    private final PlaylistToInterlinkFeed adapter;
    private final AWSCredentials awsCredentials;

    public InterlinkingDeltaUpdater(AWSCredentials awsCredentials, LastUpdatedContentFinder contentFinder, InterlinkFeedOutputter outputter, PlaylistToInterlinkFeed adapter) {
        this.awsCredentials = awsCredentials;
        this.contentFinder = contentFinder;
        this.outputter = outputter;
        this.adapter = adapter;
    }

    public void updateFeed(Maybe<Document> existingFeedElement, DateTime from, DateTime to) {
        Iterator<Content> newContent = contentFinder.updatedSince(Publisher.C4, from);
        InterlinkFeed interlinkFeed = adapter.fromContent(InterlinkController.FEED_ID + getDateString(from), Publisher.C4, from, to, newContent);

        Document document;
        if (existingFeedElement.hasValue()) {
            document = existingFeedElement.requireValue();
        } else {
            document = new Document(outputter.createFeed(interlinkFeed, from));
        }
        
        outputter.updateLastUpdated(interlinkFeed.entries(), getLastUpdated(document), document);
        outputter.outputFeedToElements(interlinkFeed.entries(), false, document.getRootElement());

        try {
            S3Service s3Service = new RestS3Service(awsCredentials);
            S3Object s3Object = new S3Object(getFilename(from), getXmlString(document));
            S3Bucket bucket = s3Service.getBucket(BUCKET_NAME);
            s3Service.putObject(bucket, s3Object);
        } catch (Exception e) {
            log.error("Error writing object to S3", e);
        }
    }

    private String getXmlString(Document document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String charset = Charsets.UTF_8.name();
        Serializer serializer = new Serializer(out, charset);
        serializer.setIndent(2);
        serializer.write(document);
        return out.toString(charset);
    }

    public DateTime getLastUpdated(Document document) {
        return new DateTime(InterlinkFeedOutputter.DATE_TIME_FORMAT.parseDateTime(document.getRootElement().getFirstChildElement("updated", InterlinkFeedOutputter.NS_ATOM.getUri()).getValue()));
    }

    public Maybe<Document> getExistingFeedElement(DateTime now) throws ValidityException, ParsingException, IOException, ServiceException {
        S3Object object;
        try {
            S3Service s3Service = new RestS3Service(awsCredentials);
            S3Bucket bucket = s3Service.getBucket(BUCKET_NAME);
            object = s3Service.getObject(bucket, getFilename(now));
            return Maybe.just(new Builder().build(object.getDataInputStream()));
        } catch (S3ServiceException e) {
            if (HttpStatusCode.NOT_FOUND.is(e.getResponseCode())) {
                return Maybe.nothing();
            } else {
                throw e;
            }
        }
    }

    public String getFilename(DateTime date) {
        return FOLDER_NAME + "/" + getDateString(date);
    }

    private String getDateString(DateTime date) {
        return dateFormat.format(date.toDate());
    }
}
