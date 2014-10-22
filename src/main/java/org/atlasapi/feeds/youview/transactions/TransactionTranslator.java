package org.atlasapi.feeds.youview.transactions;

import com.google.common.base.Function;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class TransactionTranslator {
    
    private static final String CONTENT_URLS_KEY = "contentUrls";
    static final String STATUS_KEY = "status";

    public static DBObject toDBObject(Transaction transaction) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, transaction.url());
        TranslatorUtils.fromIterable(dbo, transaction.contentUrls(), CONTENT_URLS_KEY);
        TranslatorUtils.from(dbo, STATUS_KEY, transaction.status().name());
        
        return dbo;
    }
    
    public static Transaction fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        return new Transaction(
                TranslatorUtils.toString(dbo, MongoConstants.ID),
                TranslatorUtils.toSet(dbo, CONTENT_URLS_KEY),
                TransactionStatus.valueOf(TranslatorUtils.toString(dbo, STATUS_KEY))
        );
    }
    
    public static Function<DBObject, Transaction> fromDBObjects() {
        return new Function<DBObject, Transaction>() {
            @Override
            public Transaction apply(DBObject input) {
                return fromDBObject(input);
            }
        };
    }
}
