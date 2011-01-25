package org.atlasapi.feeds.radioplayer.upload;

import java.util.List;

import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;

import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class ExceptionSummaryTranslator {

    private static final String CAUSE_TRACE = "trace";
    private static final String CAUSE_MESSAGE = "message";
    private static final String CAUSE_CLASS_NAME = "className";
    private static final String CAUSE_PARENT = "cause";
    
    public DBObject toDBObject(ExceptionSummary summary) {
        DBObject dbo = new BasicDBObject();
        dbo.put(CAUSE_CLASS_NAME, summary.className());
        dbo.put(CAUSE_MESSAGE, summary.message());
        TranslatorUtils.fromList(dbo, summary.trace(), CAUSE_TRACE);
        
        ExceptionSummary current = summary.cause();
        DBObject parentDbo = dbo;
        
        while (current != null) {
            DBObject parent = toDBObject(current);
            parentDbo.put(CAUSE_PARENT, parent);
            current = current.cause();
            parentDbo = parent;
        }
        return dbo;
    }
    
    public ExceptionSummary fromDBObject(DBObject dbo) {
        List<String> trace = TranslatorUtils.toList(dbo, CAUSE_TRACE);
        String message = (String) dbo.get(CAUSE_MESSAGE);
        String className = (String) dbo.get(CAUSE_CLASS_NAME);
        return new ExceptionSummary(className, message, trace, causeFrom(dbo));
    }
    
    private ExceptionSummary causeFrom(DBObject dbo) {
        if (!dbo.containsField(CAUSE_PARENT)) {
            return null;
        }
        return fromDBObject((DBObject) dbo.get(CAUSE_PARENT));
    }
    
}
