package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.atlasapi.feeds.radioplayer.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerOdFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerPiFeedSpec;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerOdUriResolver;
import org.atlasapi.feeds.upload.FileUpload;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.metabroadcast.common.media.MimeType;


public class UploadCreator {
    
    private final RadioPlayerOdUriResolver odUriResolver;
    
    public UploadCreator(RadioPlayerOdUriResolver odUriResolver) {
        this.odUriResolver = checkNotNull(odUriResolver);
    }

    public FileUpload createUpload(RadioPlayerService service, FileType type, LocalDate date) throws IOException {
        RadioPlayerFeedSpec spec;
        switch (type) {
        case PI:
            spec = new RadioPlayerPiFeedSpec(service, date);
            break;
        case OD:
            DateTime since = date.toDateTimeAtStartOfDay().minusHours(2);
            spec = new RadioPlayerOdFeedSpec(
                    service, 
                    date, 
                    Optional.of(since), 
                    odUriResolver.getServiceToUrisMapSince(since).get(service)
            );
            break;
        default:
            throw new IllegalArgumentException("Unknown file type");
        }

        return new FileUpload.Builder(spec.filename(), getFileContent(type, spec))
                .withContentType(MimeType.TEXT_XML)
                .build();
    }
    
    private byte[] getFileContent(FileType type, RadioPlayerFeedSpec spec) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RadioPlayerFeedCompiler.valueOf(type).compileFeedFor(spec, out);
        return out.toByteArray();
    }
    
}
