package org.atlasapi.feeds.interlinking.delta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import com.google.common.base.Charsets;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.http.HttpStatusCode;

public class S3DocumentStore implements InterlinkingDocumentStore {
    
    private final Log log = LogFactory.getLog(getClass());
    
    private final AWSCredentials creds;
    private final String bucketName;
    private final String folderName;

    public S3DocumentStore(AWSCredentials creds, String bucketName, String folderName) {
        this.creds = creds;
        this.bucketName = bucketName;
        this.folderName = folderName;
    }

    @Override
    public void storeDocument(String filename, Document file) {
        try {
            S3Service s3Service = new RestS3Service(creds);
            S3Object s3Object = new S3Object(getFullPath(filename), getXmlString(file));
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
    
    @Override
    public Maybe<Document> getDocument(String filename) {
        S3Object object;
        try {
            S3Service s3Service = new RestS3Service(creds);
            object = s3Service.getObject(bucketName, getFullPath(filename));
            return Maybe.just(new Builder().build(object.getDataInputStream()));
        } catch (Exception e) {
            if (e instanceof S3ServiceException && HttpStatusCode.NOT_FOUND.is(((S3ServiceException)e).getResponseCode())) {
                return Maybe.nothing();
            } else {
                log.error("Error retrieving document for " + filename, e);
                return Maybe.nothing();
            }
        }
    }
    
    private String getFullPath(String filename) {
        return String.format("%s/%s", folderName, filename);
    }
}
