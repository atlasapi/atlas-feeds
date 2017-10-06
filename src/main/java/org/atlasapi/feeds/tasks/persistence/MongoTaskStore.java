package org.atlasapi.feeds.tasks.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.atlasapi.feeds.tasks.Action;
import org.atlasapi.feeds.tasks.Destination.DestinationType;
import org.atlasapi.feeds.tasks.Payload;
import org.atlasapi.feeds.tasks.Response;
import org.atlasapi.feeds.tasks.Status;
import org.atlasapi.feeds.tasks.TVAElementType;
import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.feeds.tasks.TaskQuery;
import org.atlasapi.feeds.tasks.YouViewDestination;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.persistence.mongo.MongoQueryBuilder;
import com.metabroadcast.common.persistence.mongo.MongoSortBuilder;
import com.metabroadcast.common.persistence.mongo.MongoUpdateBuilder;
import com.metabroadcast.common.query.Selection;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import jena.query;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.ACTION_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.CONTENT_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.CREATED_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.DESTINATION_TYPE_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.ELEMENT_ID_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.ELEMENT_TYPE_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.LAST_ERROR_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.PUBLISHER_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.REMOTE_ID_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.STATUS_KEY;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.fromDBObject;
import static org.atlasapi.feeds.tasks.persistence.TaskTranslator.toDBObject;


public class MongoTaskStore implements TaskStore {
    
    private static final String COLLECTION_NAME = "youviewTasks";
    private final Logger log = LoggerFactory.getLogger(MongoTaskStore.class);
    
    private final DBCollection collection;
    
    public MongoTaskStore(DatabasedMongo mongo) {
        this.collection = checkNotNull(mongo).collection(COLLECTION_NAME);
    }

    private DBCursor getOrderedCursor(DBObject query, TaskQuery.Sort sort) {
        MongoSortBuilder orderBy = new MongoSortBuilder();

        if (sort.getDirection() == TaskQuery.Sort.Direction.ASC) {
            orderBy.ascending(sort.getField().getDbField());
        } else {
            orderBy.descending(sort.getField().getDbField());
        }

        return collection
                .find(query)
                .sort(orderBy.build());
    }

    @Override
    public Task save(Task task) {
        collection.save(toDBObject(task));
        return task;
    }

    @Override
    public void updateWithStatus(Long taskId, Status status) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .setField(STATUS_KEY, status.name())
                .build();
        
        collection.update(idQuery, updateStatus, false, false);
    }
    
    @Override
    public void updateWithLastError(Long taskId, String lastError) {
        DBObject idQuery = new MongoQueryBuilder()
                                .idEquals(taskId)
                                .build();
        
        DBObject updateLastError = new MongoUpdateBuilder()
                                      .setField(LAST_ERROR_KEY, lastError)
                                      .build();
        collection.update(idQuery, updateLastError, false, false);
    }

    @Override
    public void updateWithRemoteId(Long taskId, Status status, String remoteId, DateTime uploadTime) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .setField(TaskTranslator.STATUS_KEY, status.name())
                .setField(TaskTranslator.REMOTE_ID_KEY, remoteId)
                .setField(TaskTranslator.UPLOAD_TIME_KEY, uploadTime)
                .build();
        
        collection.update(idQuery, updateStatus, false, false);
    }

    @Override
    public void updateWithResponse(Long taskId, Response response) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .push(TaskTranslator.REMOTE_STATUSES_KEY, ResponseTranslator.toDBObject(response))
                .setField(STATUS_KEY, response.status().name())
                .build();
        
        collection.update(idQuery, updateStatus, false, false);
    }

    @Override
    public void updateWithPayload(Long taskId, Payload payload) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        DBObject updateStatus = new MongoUpdateBuilder()
                .setField(TaskTranslator.PAYLOAD_KEY, PayloadTranslator.toDBObject(payload))
                .build();
        
        collection.update(idQuery, updateStatus, false, false);
    }

    @Override
    public Optional<Task> taskFor(Long taskId) {
        DBObject idQuery = new MongoQueryBuilder()
                .idEquals(taskId)
                .build();
        return Optional.fromNullable(fromDBObject(collection.findOne(idQuery)));
    }

    @Override
    public Iterable<Task> allTasks(DestinationType type, Status status) {
        log.info("gettting tasks for "+type+" and status "+status);
        MongoQueryBuilder mongoQuery = new MongoQueryBuilder()
                .fieldEquals(DESTINATION_TYPE_KEY, type.name())
                .fieldEquals(STATUS_KEY, status.name())
                .fieldEquals("publisher", Publisher.AMAZON_UNBOX.key())
                .fieldEquals("_id","49767548")
                ;
        log.info("the query is ready "+mongoQuery);
        DBCursor cursor = getOrderedCursor(mongoQuery.build(), TaskQuery.Sort.DEFAULT)
                                .limit(5)
                                .addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        log.info("the query got run, and we got a cursor back "+cursor);

        log.info("cursor size: "+ cursor.size());
        log.info("cursor count: "+cursor.count());


        List<Task> a = new ArrayList<>();

        if(status == Status.NEW){
            Task task = Task.builder()
                    .withId(49767548L)
                    .withAtlasDbId(33191554L)
                    .withCreated(new DateTime("2017-10-05T13:11:03.322Z"))
                    .withPublisher(Publisher.AMAZON_UNBOX)
                    .withAction(Action.UPDATE)
                    .withDestination(new YouViewDestination(
                                    "http://unbox.amazon.co.uk/B00IG82A06",
                                    TVAElementType.ITEM,
                                    "crid://nitro.bbc.co.uk/iplayer/youview/http://unbox.amazon.co.uk/B00IG82A06"
                            )
                    )
                    .withStatus(Status.NEW)
                    .withUploadTime(new DateTime())
                    .withRemoteId("")
                    .withPayload(new Payload(
                            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><TVAMain xmlns=\"urn:tva:metadata:2010\" xmlns:ns2=\"urn:tva:metadata:extended:2010\" xmlns:ns3=\"urn:tva:mpeg7:2008\" xmlns:ns4=\"http://refdata.youview.com/schemas/Metadata/2012-11-19\" xml:lang=\"en-GB\"><ProgramDescription><ProgramInformationTable/><GroupInformationTable><GroupInformation groupId=\"crid://unbox.amazon.co.uk/product/http://unbox.amazon.co.uk/B00IG82A06\"><GroupType xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ProgramGroupTypeType\" value=\"programConcept\"/><BasicDescription><Title type=\"main\">Fashion of the Christ</Title><Synopsis length=\"short\">Nancy gets a rude awakening when her brother-in-lax Andy comes for a surprise visit...</Synopsis><Synopsis length=\"medium\">Nancy gets a rude awakening when her brother-in-lax Andy comes for a surprise visit. Doug has an idea for a location for the faux bakery, which Nancy may need more than ever Andy in the way. While Andy...</Synopsis><Synopsis length=\"long\">Nancy gets a rude awakening when her brother-in-lax Andy comes for a surprise visit. Doug has an idea for a location for the faux bakery, which Nancy may need more than ever Andy in the way. While Andy wreaks havoc in the Botwin's lives, Nancy is introduced to &quot;The Candy Man&quot; in order to help meet her customer's needs, and Celia drops a bombshell on her husband.</Synopsis><Genre type=\"main\" href=\"urn:tva:metadata:cs:ContentCS:2010:3.1\"/><Genre type=\"main\" href=\"urn:tva:metadata:cs:OriginationCS:2005:5.8\"/><Genre type=\"other\" href=\"urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3\"/><Language type=\"original\">en</Language><CreditsList><CreditsItem role=\"urn:mpeg:mpeg7:cs:RoleCS:2001:UNKNOWN\"><PersonName><ns3:GivenName>Lee Rose,Burr Steers</ns3:GivenName></PersonName></CreditsItem><CreditsItem role=\"urn:mpeg:mpeg7:cs:RoleCS:2001:UNKNOWN\"><PersonName><ns3:GivenName>Mary-Louise Parker</ns3:GivenName></PersonName></CreditsItem><CreditsItem role=\"urn:mpeg:mpeg7:cs:RoleCS:2001:UNKNOWN\"><PersonName><ns3:GivenName>Romany Malco</ns3:GivenName></PersonName></CreditsItem></CreditsList><RelatedMaterial xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns2:ExtendedRelatedMaterialType\"><HowRelated href=\"urn:tva:metadata:cs:HowRelatedCS:2010:19\"/><Format href=\"urn:mpeg:mpeg7:cs:FileFormatCS:2001:1\"/><MediaLocator><ns3:MediaUri>http://ecx.images-amazon.com/images/I/51ijt5PFjkL._SX320_SY240_.jpg</ns3:MediaUri></MediaLocator><ns2:ContentProperties><ns2:ContentAttributes xsi:type=\"ns2:StillImageContentAttributesType\"><ns2:Width>320</ns2:Width><ns2:Height>240</ns2:Height><ns2:IntendedUse href=\"http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary\"/></ns2:ContentAttributes></ns2:ContentProperties></RelatedMaterial></BasicDescription><MemberOf xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"MemberOfType\" index=\"4\" crid=\"crid://unbox.amazon.co.uk/product/http://unbox.amazon.co.uk/B00HUT9GOA\"/></GroupInformation></GroupInformationTable><ProgramLocationTable/><ServiceInformationTable/></ProgramDescription></TVAMain>",
                            new DateTime("2017-10-05T13:11:03.311Z")
                    ))
                    .withRemoteResponses(new ArrayList<>())
                    .withLastError("")
                    .withManuallyCreated(false)
                    .build();
            a.add(task);
        }

        while(cursor.hasNext()) {
            DBObject obj = cursor.next();
            System.out.println("got next "+obj);
            Task task = TaskTranslator.fromDBObject(obj);
            System.out.println("made a task out of it "+task);
            a.add(task);
            System.out.println("added to tasks list size:"+a.size());
        }

        return a;


//        FluentIterable<DBObject> from = FluentIterable.from(cursor);
//        log.info("we got a iterable from the cursor "+from);
//        FluentIterable<Task> transform = from
//                .transform();
//        log.info("We transform the stuff we got to tasks "+transform);
//        FluentIterable<Task> filter = transform
//                .filter(Predicates.notNull());
//        log.info("tasks for filtered for non null ones "+filter);
//        return filter;
    }

    @Override
    public Iterable<Task> allTasks(TaskQuery query) {
        MongoQueryBuilder mongoQuery = new MongoQueryBuilder();
        
        mongoQuery.fieldEquals(PUBLISHER_KEY, query.publisher().key())
                .fieldEquals(DESTINATION_TYPE_KEY, query.destinationType().name());
        
        if (query.contentUri().isPresent()) {
            mongoQuery.regexMatch(CONTENT_KEY, transformToPrefixRegexPattern(query.contentUri().get()));
        }
        if (query.remoteId().isPresent()) {
            mongoQuery.regexMatch(REMOTE_ID_KEY, transformToPrefixRegexPattern(query.remoteId().get()));
        }
        if (query.status().isPresent()) {
            mongoQuery.fieldEquals(STATUS_KEY, query.status().get().name());
        }
        if (query.action().isPresent()) {
            mongoQuery.fieldEquals(ACTION_KEY, query.action().get().name());
        }
        if (query.elementType().isPresent()) {
            mongoQuery.fieldEquals(ELEMENT_TYPE_KEY, query.elementType().get().name());
        }
        if (query.elementId().isPresent()) {
            mongoQuery.fieldEquals(ELEMENT_ID_KEY, transformToPrefixRegexPattern(query.elementId().get()));
        }

        Selection selection = query.selection();
        DBCursor cursor = getOrderedCursor(mongoQuery.build(), query.sort())
                .addOption(Bytes.QUERYOPTION_NOTIMEOUT);

        if (selection.getOffset() != 0) {
            cursor.skip(selection.getOffset());
        }

        if (selection.getLimit() != null) {
            cursor.limit(selection.getLimit());
        }

        return FluentIterable.from(cursor)
                .transform(TaskTranslator.fromDBObjects())
                .filter(Predicates.notNull());
    }

    private String transformToPrefixRegexPattern(String input) {
        return "^" + Pattern.quote(input);
    }

    @Override
    public void removeBefore(DateTime removalDate) {
        DBObject mongoQuery = new MongoQueryBuilder()
            .fieldBefore(CREATED_KEY, removalDate)
            .build();
        
        collection.remove(mongoQuery);
    }
}
