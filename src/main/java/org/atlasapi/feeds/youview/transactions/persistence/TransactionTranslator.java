package org.atlasapi.feeds.youview.transactions.persistence;

import org.atlasapi.feeds.youview.transactions.Transaction;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.metabroadcast.common.persistence.mongo.MongoConstants;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


public class TransactionTranslator {
    
    private static final Joiner JOIN_ON_COLON = Joiner.on(':').skipNulls();    
    private static final String ID_KEY = "transaction_id";
    static final String PUBLISHER_KEY = "publisher";
    static final String CONTENT_KEY = "contentUrls";
    static final String UPLOAD_TIME_KEY = "uploadTime";
    static final String STATUS_KEY = "status";
    
    public static DBObject toDBObject(Transaction transaction) {
        DBObject dbo = new BasicDBObject();
        
        TranslatorUtils.from(dbo, MongoConstants.ID, idFrom(transaction.id(), transaction.publisher()));
        TranslatorUtils.from(dbo, ID_KEY, transaction.id());
        TranslatorUtils.from(dbo, PUBLISHER_KEY, transaction.publisher().key());
        TranslatorUtils.fromSet(dbo, transaction.content(), CONTENT_KEY);
        TranslatorUtils.fromDateTime(dbo, UPLOAD_TIME_KEY, transaction.uploadTime());
        TranslatorUtils.from(dbo, STATUS_KEY, TransactionStatusTranslator.toDBObject(transaction.status()));
        
        return dbo;
    }

    public static String idFrom(String id, Publisher publisher) {
        return JOIN_ON_COLON.join(id, publisher.name());
    }

    public static Transaction fromDBObject(DBObject dbo) {
        if (dbo == null) {
            return null;
        }
        return new Transaction(
                TranslatorUtils.toString(dbo, ID_KEY),
                Publisher.fromKey(TranslatorUtils.toString(dbo, PUBLISHER_KEY)).requireValue(),
                TranslatorUtils.toDateTime(dbo, UPLOAD_TIME_KEY),
                TranslatorUtils.toSet(dbo, CONTENT_KEY),
                TransactionStatusTranslator.fromDBObject(TranslatorUtils.toDBObject(dbo, STATUS_KEY))
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
