package org.atlasapi.feeds.radioplayer.upload.queue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.xml.XMLValidator;
import org.joda.time.DateTime;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;


public class UploadServicesSupplier {
    
    private static final Function<FileUploaderProvider, UploadService> TO_KEY = new Function<FileUploaderProvider, UploadService>() {
        @Override
        public UploadService apply(FileUploaderProvider input) {
            return input.serviceKey();
        }
    }; 
    
    private final Map<UploadService, FileUploaderProvider> uploaders;
    private final XMLValidator validator;

    
    public UploadServicesSupplier(Iterable<FileUploaderProvider> uploaders, XMLValidator validator) {
        this.uploaders = Maps.uniqueIndex(uploaders, TO_KEY);
        this.validator = checkNotNull(validator);
    }
    
    public Optional<FileUploader> get(UploadService remoteService, DateTime uploadTime, FileType type) {
        FileUploaderProvider uploaderProvider = uploaders.get(remoteService);
        if (uploaderProvider == null) {
            return Optional.absent();
        }
        return Optional.<FileUploader>of(new ValidatingFileUploader(validator, uploaderProvider.get(uploadTime, type)));
    }
}
