package org.atlasapi.feeds.youview.transactions.persistence;

import tva.mpeg7._2008.TextualType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;


public class FragmentReportTranslator {
    
    private static final String REMARKS_KEY = "remarks";
    private static final String MESSAGE_KEY = "messages";
    private static final String SUCCESS_KEY = "success";
    private static final String FRAGMENT_ID_KEY = "fragmentId";
    private static final String RECORD_ID_KEY = "recordId";
    
    // TODO these belong elsewhere
    public static final Function<TextualType, String> TEXTUALTYPE_TO_STRING 
            = new Function<TextualType, String>() {
                @Override
                public String apply(TextualType input) {
                    return input.getValue();
                }
            };
    public static final Function<String, TextualType> STRING_TO_TEXTUALTYPE
            = new Function<String, TextualType>() {
                @Override
                public TextualType apply(String input) {
                    TextualType remark = new TextualType();
                    remark.setValue(input);
                    return remark;
                }
            };

    private FragmentReportTranslator() {
        // private constructor for factory class
    }
    
    public static DBObject toDBObject(FragmentReportType fragmentReport) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.fromIterableToStringList(dbo, REMARKS_KEY, fragmentReport.getRemark(), TEXTUALTYPE_TO_STRING);
        TranslatorUtils.fromIterable(dbo, MESSAGE_KEY, fragmentReport.getMessage(), MessageTranslator.toDBObject());
        TranslatorUtils.from(dbo, SUCCESS_KEY, fragmentReport.isSuccess());
        TranslatorUtils.from(dbo, FRAGMENT_ID_KEY, fragmentReport.getFragmentId());
        TranslatorUtils.from(dbo, RECORD_ID_KEY, fragmentReport.getRecordId());
        
        return dbo;
    }
    
    public static Function<FragmentReportType, DBObject> toDBObject() {
        return new Function<FragmentReportType, DBObject>() {
            @Override
            public DBObject apply(FragmentReportType input) {
                return toDBObject(input);
            }
        };
    }
    
    public static FragmentReportType fromDBObject(DBObject dbo) {
        FragmentReportType fragmentReport = new FragmentReportType();
        
        Optional<Iterable<TextualType>> remarks = TranslatorUtils.toIterableFromStringList(dbo, REMARKS_KEY, STRING_TO_TEXTUALTYPE);
        if (remarks.isPresent()) {
            fragmentReport.getRemark().addAll(ImmutableList.copyOf(remarks.get()));
        }
        Optional<Iterable<ControlledMessageType>> messages = TranslatorUtils.toIterable(dbo, MESSAGE_KEY, MessageTranslator.fromDBObject());
        if (messages.isPresent()) {
            fragmentReport.getMessage().addAll(ImmutableList.copyOf(messages.get()));
        }
        fragmentReport.setSuccess(TranslatorUtils.toBoolean(dbo, SUCCESS_KEY));
        fragmentReport.setFragmentId(TranslatorUtils.toString(dbo, FRAGMENT_ID_KEY));
        fragmentReport.setRecordId(TranslatorUtils.toString(dbo, RECORD_ID_KEY));
        
        return fragmentReport;
    }
    
    public static Function<DBObject, FragmentReportType> fromDBObject() {
        return new Function<DBObject, FragmentReportType>() {
            @Override
            public FragmentReportType apply(DBObject input) {
                return fromDBObject(input);
            }
        };
    }
}
