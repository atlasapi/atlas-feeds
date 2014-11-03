package org.atlasapi.feeds.youview.transactions.persistence;

import org.atlasapi.feeds.youview.transactions.FragmentReportTranslator;
import org.atlasapi.feeds.youview.transactions.TransactionStatus;

import com.google.common.base.Optional;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class TransactionStatusTranslator {

    static final String STATUS_KEY = "type";
    private static final String MESSAGE_KEY = "message";
    private static final String FRAGMENT_REPORTS_KEY = "fragmentReports";
    
    private TransactionStatusTranslator() {
        // private constructor for factory class
    }

    public static DBObject toDBObject(TransactionStatus status) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, STATUS_KEY, status.status().name());
        TranslatorUtils.from(dbo, MESSAGE_KEY, status.message());
        if (status.fragmentReports().isPresent()) {
            TranslatorUtils.fromIterable(dbo, FRAGMENT_REPORTS_KEY, status.fragmentReports().get(), FragmentReportTranslator.toDBObject());
        }
        
        return dbo;
    }
    
    public static TransactionStatus fromDBObject(DBObject dbo) {
        TransactionStateType status = TransactionStateType.valueOf(TranslatorUtils.toString(dbo, STATUS_KEY));
        String message = TranslatorUtils.toString(dbo, MESSAGE_KEY);
        Optional<Iterable<FragmentReportType>> fragmentReports = TranslatorUtils.toIterable(dbo, FRAGMENT_REPORTS_KEY, FragmentReportTranslator.fromDBObject());
        if (fragmentReports.isPresent()) {
            return new TransactionStatus(status, message, fragmentReports.get());
        } else {
            return new TransactionStatus(status, message);
        }
    }
}
