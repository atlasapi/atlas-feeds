package org.atlasapi.feeds.youview.tasks.persistence;

import static org.atlasapi.feeds.youview.tasks.persistence.FragmentReportTranslator.STRING_TO_TEXTUALTYPE;
import static org.atlasapi.feeds.youview.tasks.persistence.FragmentReportTranslator.TEXTUALTYPE_TO_STRING;

import com.google.common.base.Function;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.SeverityType;


public class MessageTranslator {

    private static final String COMMENT_KEY = "comment";
    private static final String SEVERITY_KEY = "severity";
    private static final String LOCATION_KEY = "location";
    private static final String REASON_CODE_KEY = "reasonCode";

    private MessageTranslator() {
        // private constructor for factory class
    }

    public static Function<ControlledMessageType, DBObject> toDBObject() {
        return new Function<ControlledMessageType, DBObject>() {
            @Override
            public DBObject apply(ControlledMessageType input) {
                return toDBObject(input);
            }
        };
    }

    public static DBObject toDBObject(ControlledMessageType message) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, COMMENT_KEY, TEXTUALTYPE_TO_STRING.apply(message.getComment()));
        TranslatorUtils.from(dbo, SEVERITY_KEY, message.getSeverity().name());
        TranslatorUtils.from(dbo, LOCATION_KEY, message.getLocation());
        TranslatorUtils.from(dbo, REASON_CODE_KEY, message.getReasonCode());
        
        return dbo;
    }

    public static Function<DBObject, ControlledMessageType> fromDBObject() {
        return new Function<DBObject, ControlledMessageType>() {
            @Override
            public ControlledMessageType apply(DBObject input) {
                return fromDBObject(input);
            }
        };
    }

    public static ControlledMessageType fromDBObject(DBObject dbo) {
        ControlledMessageType message = new ControlledMessageType();
        
        message.setComment(STRING_TO_TEXTUALTYPE.apply(TranslatorUtils.toString(dbo, COMMENT_KEY)));
        message.setSeverity(SeverityType.valueOf(TranslatorUtils.toString(dbo, SEVERITY_KEY)));
        message.setLocation(TranslatorUtils.toString(dbo, LOCATION_KEY));
        message.setReasonCode(TranslatorUtils.toString(dbo, REASON_CODE_KEY));
        
        return message;
    }
}
