package org.atlasapi.feeds.radioplayer;

import static com.metabroadcast.common.scheduling.RepetitionRules.NEVER;
import static org.atlasapi.persistence.logging.AdapterLogEntry.infoEntry;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.radioplayer.upload.CachingRadioPlayerUploadResultStore;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFtpRemoteProcessingChecker;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFtpUploadServicesSupplier;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerHttpsRemoteProcessingChecker;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerHttpsUploadServicesSupplier;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerRecordingExecutor;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerServerHealthProbe;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadController;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadHealthProbe;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadResultStore;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadServicesSupplier;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadTaskBuilder;
import org.atlasapi.feeds.radioplayer.upload.UploadResultStoreBackedRadioPlayerResultStore;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.atlasapi.feeds.upload.persistence.MongoFileUploadResultStore;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.common.net.HostSpecifier;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.common.properties.Parameter;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.RepetitionRules.Every;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.metabroadcast.common.webapp.health.HealthController;

@Configuration
public class RadioPlayerModule {

	private static final String RP_UPLOAD_SERVICE_PREFIX = "rp.upload.";
	private static final Every UPLOAD_EVERY_FIVE_MINUTES = RepetitionRules.every(Duration.standardMinutes(5));
    private static final Every UPLOAD_EVERY_TEN_MINUTES = RepetitionRules.every(Duration.standardMinutes(10));
	private static final Every UPLOAD_EVERY_TWO_HOURS = RepetitionRules.every(Duration.standardHours(2));
	
	private static final Publisher BBC = Publisher.BBC;
	private static final Publisher NITRO = Publisher.BBC_NITRO;

	private @Value("${rp.ftp.enabled}") String ftpUpload;
	private @Value("${rp.ftp.services}") String ftpUploadServices;
	
	private @Value("${rp.s3.serviceId}") String s3ServiceId;
	private @Value("${rp.s3.bucket}") String s3Bucket;
	private @Value("${s3.access}") String s3AccessKey;
	private @Value("${s3.secret}") String s3Secret;
	// This flag enables the jobs for both the ftp and https uploaders, but only adds
	// upload capability to s3. this allows diff-ing of the output from the two uploaders.
	private @Value("${rp.s3.s3UploadOnly.enabled}") String s3UploadOnly;
	
	private @Value("${rp.https.serviceId}") String httpsServiceId;
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
	
	private static DayRangeGenerator dayRangeGenerator = new DayRangeGenerator().withLookAhead(7).withLookBack(7);

	// this publisher will need to change if the output controller is to display files generated from a different publisher's content.
	public @Bean RadioPlayerController radioPlayerController() {
		return new RadioPlayerController(lastUpdatedContentFinder, contentLister, ImmutableSet.of(BBC, NITRO));
	}
	
	public @Bean Map<String,RemoteServiceDetails> radioPlayerUploadServiceDetails() {
	    ImmutableMap.Builder<String,RemoteServiceDetails> remotes = ImmutableMap.builder();
	    for (Entry<String, Parameter> property : Configurer.getParamsWithKeyMatching(Predicates.containsPattern(RP_UPLOAD_SERVICE_PREFIX))) {
            String serviceId = property.getKey().substring(RP_UPLOAD_SERVICE_PREFIX.length());
            String serviceConfig = property.getValue().get();
            if (!Strings.isNullOrEmpty(serviceConfig)) {
                RemoteServiceDetails details = detailsFor(serviceId, ImmutableList.copyOf(Splitter.on("|").split(serviceConfig)));
                log.record(infoEntry().withSource(getClass()).withDescription("Found details for service %s: %s", serviceId, details));
                remotes.put(serviceId, details);
            } else {
                log.record(infoEntry().withSource(getClass()).withDescription("Ignoring service %s: no details", serviceId));
            }
        }
	    return remotes.build();
	}
	
    private RemoteServiceDetails detailsFor(String serviceId, ImmutableList<String> detailParts) {
        Preconditions.checkArgument(detailParts.size() == 3, "Bad details for service " + serviceId);
        try {
            return RemoteServiceDetails.forServer(HostSpecifier.from(detailParts.get(0)))
                    .withPort(Integer.parseInt(detailParts.get(1)))
                    .withCredentials(new UsernameAndPassword(detailParts.get(2), Configurer.get("rp.password."+serviceId).get())).build();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public @Bean RadioPlayerUploadServicesSupplier radioPlayerFtpUploadServices() {
        return new RadioPlayerFtpUploadServicesSupplier(
                Boolean.parseBoolean(s3UploadOnly),
                Boolean.parseBoolean(ftpUpload),
                s3ServiceId, 
                s3Bucket, 
                radioPlayerS3Credentials(), 
                log, 
                radioPlayerValidator(), 
                radioPlayerUploadServiceDetails()
        );
    }

    public @Bean RadioPlayerUploadServicesSupplier radioPlayerHttpsUploadServices() {
        return new RadioPlayerHttpsUploadServicesSupplier(
                Boolean.parseBoolean(s3UploadOnly),
                Boolean.parseBoolean(httpsUpload),
                s3ServiceId, 
                s3Bucket, 
                radioPlayerS3Credentials(), 
                log, 
                radioPlayerValidator(), 
                httpsServiceId, 
                radioPlayerHttpClient(), 
                httpsUrl
        );
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
        return new RadioPlayerHealthController(health, Sets.union(ImmutableSet.copyOf(ftpRemoteServices().values()), ImmutableSet.copyOf(httpsRemoteServices().values())), Configurer.get("rp.health.password", "").get());
    }
    
    public @Bean RadioPlayerUploadController radioPlayerUploadController() {
        return new RadioPlayerUploadController(radioPlayerFtpUploadTaskBuilder(), dayRangeGenerator, Configurer.get("rp.health.password", "").get());
    }
    
    @Bean RadioPlayerUploadResultStore uploadResultRecorder() {
        return new CachingRadioPlayerUploadResultStore(
                Sets.union(ImmutableSet.copyOf(ftpRemoteServices().values()), ImmutableSet.copyOf(httpsRemoteServices().values())), 
                new UploadResultStoreBackedRadioPlayerResultStore(fileUploadResultStore())
        );
    }
    
    private MongoFileUploadResultStore fileUploadResultStore() {
        return new MongoFileUploadResultStore(mongo);
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
    
    @Bean RadioPlayerUploadTaskBuilder radioPlayerFtpUploadTaskBuilder() {
        return new RadioPlayerUploadTaskBuilder(radioPlayerFtpUploadServices(), radioPlayerUploadTaskRunner(), lastUpdatedContentFinder, contentLister, BBC).withLog(log);
    }
    
    @Bean RadioPlayerUploadTaskBuilder radioPlayerHttpsUploadTaskBuilder() {
        return new RadioPlayerUploadTaskBuilder(radioPlayerHttpsUploadServices(), radioPlayerUploadTaskRunner(), lastUpdatedContentFinder, contentLister, NITRO).withLog(log);
    }
    
    @Bean RadioPlayerRecordingExecutor radioPlayerUploadTaskRunner() {
        return new RadioPlayerRecordingExecutor(uploadResultRecorder());
    }
    
    @Bean Map<Publisher, String> ftpRemoteServices() {
        Builder<Publisher, String> serviceMapping = ImmutableMap.builder();
        for (String remote : radioPlayerUploadServiceDetails().keySet()) {
            serviceMapping.put(BBC, remote);
        }
        if (Boolean.parseBoolean(s3UploadOnly)) {
            serviceMapping.put(BBC, s3ServiceId);
        }
        return serviceMapping.build();
    }
    
    @Bean Map<Publisher, String> httpsRemoteServices() {
        Builder<Publisher, String> serviceMapping = ImmutableMap.builder();
        serviceMapping.put(NITRO, httpsServiceId);
        if (Boolean.parseBoolean(s3UploadOnly)) {
            serviceMapping.put(BBC, s3ServiceId);
        }
        return serviceMapping.build();
    }
    
	@PostConstruct 
	public void scheduleTasks() {
	    RadioPlayerFeedCompiler.init(scheduleResolver, knownTypeContentResolver, contentResolver, channelResolver, ImmutableList.of(BBC, NITRO));
		if (!ftpRemoteServices().isEmpty() || !httpsRemoteServices().isEmpty()) {
		    createHealthProbes(ftpRemoteServices(), ftpUploadServices());
		    createHealthProbes(httpsRemoteServices(), httpsUploadServices());
	
		    if (Boolean.parseBoolean(s3UploadOnly) || Boolean.parseBoolean(ftpUpload)) {
				
	            scheduler.schedule(
	                    radioPlayerFtpUploadTaskBuilder().newScheduledPiTask(ftpUploadServices(), dayRangeGenerator).withName("Radioplayer PI Full Upload"), 
	                    UPLOAD_EVERY_TWO_HOURS);
	            scheduler.schedule(
	                    radioPlayerFtpUploadTaskBuilder().newScheduledPiTask(ftpUploadServices(), new DayRangeGenerator()).withName("Radioplayer PI Today Upload"), 
	                    UPLOAD_EVERY_TEN_MINUTES);
	            scheduler.schedule(
	                    new RadioPlayerFtpRemoteProcessingChecker(radioPlayerUploadServiceDetails(), uploadResultRecorder(), log).withName("Radioplayer Remote Processing Checker"),
	                    UPLOAD_EVERY_TEN_MINUTES.withOffset(Duration.standardMinutes(5)));
	            
	            scheduler.schedule(
	                    radioPlayerFtpUploadTaskBuilder().newScheduledOdTask(ftpUploadServices(), true).withName("Radioplayer OD Full Upload"), 
	                    NEVER);
	            scheduler.schedule(
	                    radioPlayerFtpUploadTaskBuilder().newScheduledOdTask(ftpUploadServices(), false).withName("Radioplayer OD Today Upload"),
	                    UPLOAD_EVERY_TEN_MINUTES);
			} 
		    if (Boolean.parseBoolean(s3UploadOnly) || Boolean.parseBoolean(httpsUpload)) {
                
                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledPiTask(httpsUploadServices(), dayRangeGenerator).withName("Radioplayer HTTPS PI Full Upload"), 
                        UPLOAD_EVERY_TWO_HOURS);
                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledPiTask(httpsUploadServices(), new DayRangeGenerator()).withName("Radioplayer HTTPS PI Today Upload"), 
                        UPLOAD_EVERY_TEN_MINUTES);
                scheduler.schedule(
                        new RadioPlayerHttpsRemoteProcessingChecker(radioPlayerHttpClient(), httpsServiceId, uploadResultRecorder(), log).withName("Radioplayer HTTPS Remote Processing Checker"),
                        UPLOAD_EVERY_FIVE_MINUTES.withOffset(Duration.standardMinutes(5)));
                
                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledOdTask(httpsUploadServices(), true).withName("Radioplayer HTTPS OD Full Upload"), 
                        NEVER);
                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledOdTask(httpsUploadServices(), false).withName("Radioplayer HTTPS OD Today Upload"),
                        UPLOAD_EVERY_TEN_MINUTES);
            } 
		    if (!Boolean.parseBoolean(ftpUpload) && !Boolean.parseBoolean(httpsUpload) && !Boolean.parseBoolean(s3UploadOnly)) {
				log.record(
				        new AdapterLogEntry(Severity.INFO)
				                .withSource(getClass())
				                .withDescription("Not installing Radioplayer uploader")
                );
			}
		}
	}

    @Bean Iterable<RadioPlayerService> ftpUploadServices() {
        return generateUploadServices(ftpUploadServices);
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

    private void createHealthProbes(Map<Publisher, String> remoteIds, Iterable<RadioPlayerService> radioPlayerServices) {
        for (final Entry<Publisher, String> remoteId : remoteIds.entrySet()) {
            Function<RadioPlayerService, HealthProbe> createProbe = new Function<RadioPlayerService, HealthProbe>() {
                @Override
                public HealthProbe apply(RadioPlayerService service) {
                    return new RadioPlayerUploadHealthProbe(remoteId.getValue(), remoteId.getKey(), uploadResultRecorder(), service, dayRangeGenerator);
                }
            };
            
            health.addProbes(Iterables.concat(
                    Iterables.transform(radioPlayerServices, createProbe),
                    ImmutableList.of(new RadioPlayerServerHealthProbe(remoteId.getValue(), fileUploadResultStore()))
            ));
        }
    }
    
}
