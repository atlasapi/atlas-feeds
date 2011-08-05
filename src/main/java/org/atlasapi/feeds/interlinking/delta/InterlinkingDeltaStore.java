package org.atlasapi.feeds.interlinking.delta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

public class InterlinkingDeltaStore {
    
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    private final Log log = LogFactory.getLog(getClass());
    
    private final AWSCredentials creds;
    private final String bucketName;
    private final String folderName;


    public InterlinkingDeltaStore(AWSCredentials creds, String bucketName, String folderName) {
        this.creds = creds;
        this.bucketName = bucketName;
        this.folderName = folderName;
    }

    public void store(DateTime time, Document file) {
        try {
            S3Service s3Service = new RestS3Service(creds);
            S3Object s3Object = new S3Object(getFilename(time), getXmlString(file));
            S3Bucket bucket = s3Service.getBucket(bucketName);
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
    
    public Maybe<Document> getExistingFeedElement(DateTime now) throws ValidityException, ParsingException, IOException, ServiceException {
        S3Object object;
        try {
            S3Service s3Service = new RestS3Service(creds);
            S3Bucket bucket = s3Service.getBucket(bucketName);
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
        return folderName + "/" + getDateString(date);
    }

    private String getDateString(DateTime date) {
        return dateFormat.format(date.toDate());
    }
}
