package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

public interface RadioPlayerUploadResult {
    
    String filename();

    Boolean wasSuccessful();
    
    String message();
    
    DateTime uploadTime();
    
    ExceptionSummary exception();
    
}
