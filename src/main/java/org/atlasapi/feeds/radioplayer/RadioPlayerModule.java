package org.atlasapi.feeds.radioplayer;

import static com.metabroadcast.common.scheduling.RepetitionRules.NEVER;

import java.util.Set;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.radioplayer.health.RadioPlayerHealthController;
import org.atlasapi.feeds.radioplayer.health.RadioPlayerServiceSummaryHealthProbe;
import org.atlasapi.feeds.radioplayer.health.RadioPlayerUploadHealthProbe;
import org.atlasapi.feeds.radioplayer.health.ResultTypeCalculator;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerGenreElementCreator;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerIdGenreMap;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerOdUriResolver;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadController;
import org.atlasapi.feeds.radioplayer.upload.ScheduledODUploadTask;
import org.atlasapi.feeds.radioplayer.upload.ScheduledPiUploadTask;
import org.atlasapi.feeds.radioplayer.upload.https.HttpsFileUploaderProvider;
import org.atlasapi.feeds.radioplayer.upload.https.HttpsRemoteCheckServiceProvider;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.persistence.MongoFileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.persistence.MongoTaskQueue;
import org.atlasapi.feeds.radioplayer.upload.persistence.RemoteCheckTaskTranslator;
import org.atlasapi.feeds.radioplayer.upload.persistence.TaskQueue;
import org.atlasapi.feeds.radioplayer.upload.persistence.UploadTaskTranslator;
import org.atlasapi.feeds.radioplayer.upload.queue.FileCreator;
import org.atlasapi.feeds.radioplayer.upload.queue.InteractionManager;
import org.atlasapi.feeds.radioplayer.upload.queue.QueueBasedInteractionManager;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckQueueWorker;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckTask;
import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckerSupplier;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadQueueWorker;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadServicesSupplier;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.atlasapi.feeds.radioplayer.upload.s3.S3FileUploaderProvider;
import org.atlasapi.feeds.radioplayer.upload.s3.S3RemoteCheckServiceProvider;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.ids.MongoSequentialIdGenerator;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.ids.IdGenerator;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.RepetitionRules.Every;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.metabroadcast.common.time.SystemClock;
import com.metabroadcast.common.webapp.health.HealthController;

@Configuration
public class RadioPlayerModule {

    private static final String NITRO_ID_GENRE_PREFIX = "http://nitro.bbc.co.uk/genres/";
    
    private static final Every UPLOAD_EVERY_THIRTY_MINUTES = RepetitionRules.every(Duration.standardMinutes(30));
	private static final Every UPLOAD_EVERY_TWO_HOURS = RepetitionRules.every(Duration.standardHours(2));
	
	private static final Publisher NITRO = Publisher.BBC_NITRO;

	private @Value("${rp.s3.bucket}") String s3Bucket;
	private @Value("${s3.access}") String s3AccessKey;
	private @Value("${s3.secret}") String s3Secret;
	private @Value("${rp.s3.https.enabled}") String s3HttpsUpload;
	
	private @Value("${rp.https.enabled}") String httpsUpload;
	private @Value("${rp.https.services}") String httpsUploadServices;
	private @Value("${rp.https.baseUrl}") String httpsUrl;
	private @Value("${rp.https.username}") String httpsUsername;
	private @Value("${rp.https.password}") String httpsPassword;
	
    private @Autowired KnownTypeContentResolver knownTypeContentResolver;
	private @Autowired SimpleScheduler scheduler;
	private @Autowired AdapterLog log;
	private @Autowired DatabasedMongo mongo;
	private @Autowired HealthController health;
	private @Autowired ScheduleResolver scheduleResolver;
	private @Autowired ChannelResolver channelResolver;
	@Autowired
	private ContentResolver contentResolver;
	@Autowired
	private LastUpdatedContentFinder lastUpdatedContentFinder;
	@Autowired
	private ContentLister contentLister;
	
	private final Clock clock = new SystemClock(DateTimeZone.UTC);
	
	private static DayRangeGenerator dayRangeGenerator = new DayRangeGenerator().withLookAhead(7).withLookBack(7);

	public @Bean RadioPlayerController radioPlayerController() {
		return new RadioPlayerController(odUriResolver());
	}
	
	public @Bean IdGenerator idGenerator() {
	    return new MongoSequentialIdGenerator(mongo, "rpUploadAttempts");
	}
	
    public @Bean UsernameAndPassword radioPlayerS3Credentials() {
        return new UsernameAndPassword(s3AccessKey, s3Secret);
    }
    
    public @Bean SimpleHttpClient radioPlayerHttpClient() {
        return new SimpleHttpClientBuilder()
                .withPreemptiveBasicAuth(new UsernameAndPassword(httpsUsername, httpsPassword))
                .withHeader("Content-Type", MimeType.TEXT_XML.toString())
                .withTrustUnverifiedCerts()
                .build();
    }
	   
    public @Bean RadioPlayerHealthController radioPlayerHealthController() {
        return new RadioPlayerHealthController(health, uploadServices(), Configurer.get("rp.health.password", "").get(), fileHistoryStore());
    }
    
    public @Bean RadioPlayerUploadController radioPlayerUploadController() {
        return new RadioPlayerUploadController(dayRangeGenerator, Configurer.get("rp.health.password", "").get(), stateUpdater());
    }
    
    @Bean public UploadQueueWorker uploadQueueWorker() {
        return new UploadQueueWorker(uploadQueue(), uploaderSupplier(), clock, fileCreator(), stateUpdater());
    }
    
    private UploadServicesSupplier uploaderSupplier() {
        return new UploadServicesSupplier(
                ImmutableList.of(
                    new S3FileUploaderProvider(s3Service(), s3Bucket, clock),
                    new HttpsFileUploaderProvider(radioPlayerHttpClient(), httpsUrl, clock)
                ), 
                radioPlayerValidator()
        );
    }
    
    private S3Service s3Service() {
        try {
            AWSCredentials creds = new AWSCredentials(radioPlayerS3Credentials().username(), radioPlayerS3Credentials().password());
            return new RestS3Service(creds);
        } catch (S3ServiceException e) {
            throw Throwables.propagate(e);
        }
    }

    @Bean
    public TaskQueue<UploadTask> uploadQueue() {
        return new MongoTaskQueue<UploadTask>(mongo, "radioPlayerUploads", new UploadTaskTranslator(), clock);
    }
    
    @Bean
    public TaskQueue<RemoteCheckTask> remoteCheckQueue() {
        return new MongoTaskQueue<RemoteCheckTask>(mongo, "radioPlayerRemoteChecks", new RemoteCheckTaskTranslator(), clock);
    }

    @Bean public RemoteCheckQueueWorker remoteCheckQueueWorker() {
        return new RemoteCheckQueueWorker(remoteCheckQueue(), remoteCheckers(), stateUpdater());
    }
    
    private RemoteCheckerSupplier remoteCheckers() {
        return new RemoteCheckerSupplier(ImmutableList.of(
                new S3RemoteCheckServiceProvider(s3Service(), s3Bucket),
                new HttpsRemoteCheckServiceProvider(radioPlayerHttpClient())
        ));
    }

    @Bean public InteractionManager stateUpdater() {
       return new QueueBasedInteractionManager(uploadQueue(), remoteCheckQueue(), fileHistoryStore());
    }
    
    @Bean public FileHistoryStore fileHistoryStore() {
        return new MongoFileHistoryStore(mongo, idGenerator());
    }

    @Bean public FileCreator fileCreator() {
        return new FileCreator(odUriResolver());
    }
    
    @Bean public RadioPlayerOdUriResolver odUriResolver() {
        return new RadioPlayerOdUriResolver(contentLister, lastUpdatedContentFinder, NITRO);
    }
    
    @Bean public Clock clock() {
        return new SystemClock();
    }

    @Bean XMLValidator radioPlayerValidator() {
        try {
            return XMLValidator.forSchemas(ImmutableSet.of(
                Resources.getResource("xml.xsd").openStream(), 
                Resources.getResource("epgSI_11.xsd").openStream(), 
                Resources.getResource("epgSchedule_11.xsd").openStream(),
                Resources.getResource("epgDataTypes_11.xsd").openStream(),
                Resources.getResource("rpDataTypes_11.xsd").openStream()
            ));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    private ImmutableSet<UploadService> uploadServices() {
        ImmutableSet.Builder<UploadService> uploadServices = ImmutableSet.builder();
        if (Boolean.parseBoolean(s3HttpsUpload)) {
            uploadServices.add(UploadService.S3);
        }
        if (Boolean.parseBoolean(httpsUpload)) {
            uploadServices.add(UploadService.HTTPS);
        }
        return uploadServices.build();
    }
    
	@PostConstruct 
	public void scheduleTasks() {
	    RadioPlayerGenreElementCreator genreCreator = new RadioPlayerGenreElementCreator(
                new RadioPlayerIdGenreMap(RadioPlayerIdGenreMap.GENRES_FILE, NITRO_ID_GENRE_PREFIX));
        RadioPlayerFeedCompiler.init(scheduleResolver, knownTypeContentResolver, contentResolver, channelResolver, NITRO, genreCreator);
        
		if (!uploadServices().isEmpty()) {
		    createHealthProbes(uploadServices(), httpsUploadServices());
	
		    if (Boolean.parseBoolean(s3HttpsUpload) || Boolean.parseBoolean(httpsUpload)) {
                scheduler.schedule(
                        new ScheduledPiUploadTask(uploadServices(), dayRangeGenerator, httpsUploadServices(), stateUpdater()).withName("Radioplayer HTTPS/S3 PI Full Upload"), 
                        UPLOAD_EVERY_TWO_HOURS);
                scheduler.schedule(
                        new ScheduledPiUploadTask(uploadServices(), new DayRangeGenerator(), httpsUploadServices(), stateUpdater()).withName("Radioplayer HTTPS/S3 PI Today Upload"), 
                        UPLOAD_EVERY_THIRTY_MINUTES);
                
                scheduler.schedule(
                        new ScheduledODUploadTask(uploadServices(), dayRangeGenerator, httpsUploadServices(), stateUpdater()).withName("Radioplayer HTTPS/S3 OD Full Upload"), 
                        NEVER);
                scheduler.schedule(
                        new ScheduledODUploadTask(uploadServices(), new DayRangeGenerator(), httpsUploadServices(), stateUpdater()).withName("Radioplayer HTTPS/S3 OD Today Upload"),
                        UPLOAD_EVERY_THIRTY_MINUTES);
            } 
		    if (!Boolean.parseBoolean(httpsUpload) 
		            && !Boolean.parseBoolean(s3HttpsUpload)) {
				log.record(
				        new AdapterLogEntry(Severity.INFO)
				                .withSource(getClass())
				                .withDescription("Not installing Radioplayer uploader")
                );
			}
		}
	}

    @Bean Iterable<RadioPlayerService> httpsUploadServices() {
        return generateUploadServices(httpsUploadServices);
    }

    private Iterable<RadioPlayerService> generateUploadServices(String uploadServices) {
        if (Strings.isNullOrEmpty(uploadServices) || uploadServices.toLowerCase().equals("all")) {
            return RadioPlayerServices.services;
        } else {
            return Iterables.filter(Iterables.transform(Splitter.on(',').split(uploadServices), new Function<String, RadioPlayerService>() {
                @Override
                public RadioPlayerService apply(String input) {
                    return RadioPlayerServices.all.get(input);
                }
            }), Predicates.notNull());
        }
    }

    private void createHealthProbes(Set<UploadService> uploadServices, Iterable<RadioPlayerService> radioPlayerServices) {
        final ResultTypeCalculator resultCalculator = new ResultTypeCalculator(new SystemClock());
        for (final UploadService uploadService : uploadServices) {
            Function<RadioPlayerService, HealthProbe> createProbe = new Function<RadioPlayerService, HealthProbe>() {
                @Override
                public HealthProbe apply(RadioPlayerService service) {
                    return new RadioPlayerUploadHealthProbe(new SystemClock(), uploadService, fileHistoryStore(), service, dayRangeGenerator, resultCalculator);
                }
            };
            
            Function<RadioPlayerService, HealthProbe> createSummaryProbe = new Function<RadioPlayerService, HealthProbe>() {
                @Override
                public HealthProbe apply(RadioPlayerService service) {
                    return new RadioPlayerServiceSummaryHealthProbe(uploadService, service, fileHistoryStore(), new SystemClock(), dayRangeGenerator, resultCalculator);
                }
            };

            health.addProbes(Iterables.concat(
                    Iterables.transform(radioPlayerServices, createProbe),
                    Iterables.transform(radioPlayerServices, createSummaryProbe)
            ));
        }
    }
}
