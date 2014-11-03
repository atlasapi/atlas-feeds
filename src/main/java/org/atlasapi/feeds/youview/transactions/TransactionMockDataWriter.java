package org.atlasapi.feeds.youview.transactions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import org.atlasapi.feeds.youview.transactions.persistence.MongoTransactionStore;
import org.atlasapi.feeds.youview.transactions.persistence.TransactionStore;
import org.atlasapi.media.entity.Publisher;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tva.mpeg7._2008.TextualType;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.SeverityType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class TransactionMockDataWriter {
    
    private static final Set<String> content = ImmutableSet.of(
            "http://nitro.bbc.co.uk/programmes/b01k4h9p",
            "http://nitro.bbc.co.uk/programmes/b01qyvnk"
            );

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Clock clock;
    private final TransactionStore transactionStore;
    
    public TransactionMockDataWriter(TransactionStore transactionStore, Clock clock) {
        this.transactionStore = checkNotNull(transactionStore);
        this.clock = checkNotNull(clock);
    }
    
    public void writeRecords(int numRecordsToWrite) {
        Transaction transaction;
        log.info("Writing mocked transactions to DB");
        for (int i = 0; i < numRecordsToWrite; i++) {
            String id = "transaction_id_" + i;
            transaction = new Transaction(id, Publisher.BBC_NITRO, clock.now(), content, createStatus(i));
            transactionStore.save(transaction);
            log.info("writing transaction with id {}", id);
        }
        log.info("Finished writing mocked transactions");
    }

    private TransactionStatus createStatus(int i) {
        if (i % 2 == 0) {
            return new TransactionStatus(TransactionStateType.ACCEPTED, "Fragment update transaction accepted and queued for later processing."); 
        } else {
            Iterable<FragmentReportType> fragmentReports = ImmutableSet.of(createFailedFragmentReport());
            return new TransactionStatus(TransactionStateType.FAILED, "Fragment delete transaction failed.", fragmentReports);
        }
    }

    private FragmentReportType createFailedFragmentReport() {
        FragmentReportType report = new FragmentReportType();
        
        report.setSuccess(false);
        report.setFragmentId("crid://bbc.co.uk/987654321");
        TextualType remark = new TextualType();
        remark.setValue("Unknown fragment");
        report.getRemark().add(remark);
        ControlledMessageType message = new ControlledMessageType();
        message.setReasonCode("http://refdata.youview.com/mpeg7cs/YouViewMetadataIngestStatusCS/2010-￼09-23#transactional");
        message.setSeverity(SeverityType.ERROR);
        message.setLocation("/TVAMain/ProgramDescription/ProgramInformationTable");
        TextualType comment = new TextualType();
        comment.setValue("Unknown fragment.");
        message.setComment(comment);
        report.getMessage().add(message);

        return report;
    }

    public static void main(String[] args) {
        String mongoHostStr = Configurer.get("mongo.host").get();
        String dbName = Configurer.get("mongo.dbName").get();
        DatabasedMongo mongo = new DatabasedMongo(configureMongo(mongoHostStr), dbName);
        
        TransactionStore mongoStore = new MongoTransactionStore(mongo);
        TransactionMockDataWriter writer = new TransactionMockDataWriter(mongoStore, new SystemClock());
        writer.writeRecords(5);
    }

    private static Mongo configureMongo(String mongoHostStr) {
        return new Mongo(mongoHosts(mongoHostStr));
    }

    private static List<ServerAddress> mongoHosts(String mongoHostStr) {
        Splitter splitter = Splitter.on(",").omitEmptyStrings().trimResults();
        return ImmutableList.copyOf(Iterables.filter(Iterables.transform(splitter.split(mongoHostStr), new Function<String, ServerAddress>() {

            @Override
            public ServerAddress apply(String input) {
                try {
                    return new ServerAddress(input, 27017);
                } catch (UnknownHostException e) {
                    return null;
                }
            }
        }), Predicates.notNull()));
    }
}
