package org.atlasapi.feeds.radioplayer;

import static org.atlasapi.persistence.logging.AdapterLogEntry.infoEntry;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.radioplayer.upload.CachingRadioPlayerUploadResultStore;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerRecordingExecutor;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerRemoteProcessingChecker;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerServerHealthProbe;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadController;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadHealthProbe;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadResultStore;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadTaskBuilder;
import org.atlasapi.feeds.radioplayer.upload.UploadResultStoreBackedRadioPlayerResultStore;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.feeds.upload.LoggingFileUploader;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.atlasapi.feeds.upload.ValidatingFileUploader;
import org.atlasapi.feeds.upload.ftp.CommonsFTPFileUploader;
import org.atlasapi.feeds.upload.persistence.MongoFileUploadResultStore;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.google.common.net.HostSpecifier;
import com.metabroadcast.common.health.HealthProbe;
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
    private static final Every UPLOAD_EVERY_TEN_MINUTES = RepetitionRules.every(Duration.standardMinutes(10));
	private static final Every UPLOAD_EVERY_TWO_HOURS = RepetitionRules.every(Duration.standardHours(2));

	private @Value("${rp.ftp.enabled}") String upload;
	private @Value("${rp.ftp.services}") String uploadServices;
	
    private @Autowired KnownTypeContentResolver contentResolver;
	private @Autowired SimpleScheduler scheduler;
	private @Autowired AdapterLog log;
	private @Autowired DatabasedMongo mongo;
	private @Autowired HealthController health;
	private @Autowired ScheduleResolver scheduleResolver;
	
	private static DayRangeGenerator dayRangeGenerator = new DayRangeGenerator().withLookAhead(7).withLookBack(7);

	public @Bean RadioPlayerController radioPlayerController() {
		return new RadioPlayerController();
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

    public @Bean Iterable<FileUploadService> radioPlayerUploadServices() {
        return Iterables.transform(radioPlayerUploadServiceDetails().entrySet(), new Function<Entry<String, RemoteServiceDetails>, FileUploadService>() {
            @Override
            public FileUploadService apply(Entry<String, RemoteServiceDetails> input) {
                return new FileUploadService(input.getKey(), new LoggingFileUploader(log, new ValidatingFileUploader(radioPlayerValidator(), new CommonsFTPFileUploader(input.getValue()))));
            }
        });
    }
	   
    public @Bean RadioPlayerHealthController radioPlayerHealthController() {
        return new RadioPlayerHealthController(health, radioPlayerUploadServiceDetails().keySet(), Configurer.get("rp.health.password", "").get());
    }
    
    public @Bean RadioPlayerUploadController radioPlayerUploadController() {
        return new RadioPlayerUploadController(radioPlayerUploadTaskBuilder(), dayRangeGenerator, Configurer.get("rp.health.password", "").get());
    }
    
    @Bean RadioPlayerUploadResultStore uploadResultRecorder() {
        return new CachingRadioPlayerUploadResultStore(radioPlayerUploadServiceDetails().keySet(), new UploadResultStoreBackedRadioPlayerResultStore(fileUploadResultStore()));
    }

    private MongoFileUploadResultStore fileUploadResultStore() {
        return new MongoFileUploadResultStore(mongo);
    }
    
    @Bean XMLValidator radioPlayerValidator() {
        try {
            return XMLValidator.forSchemas(ImmutableSet.of(
                Resources.getResource("xml.xsd").openStream(), 
                Resources.getResource("epgSI_10.xsd").openStream(), 
                Resources.getResource("epgSchedule_10.xsd").openStream()
            ));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
    
    @Bean RadioPlayerUploadTaskBuilder radioPlayerUploadTaskBuilder() {
        return new RadioPlayerUploadTaskBuilder(radioPlayerUploadServices(), radioPlayerUploadTaskRunner()).withLog(log);
    }
    
    @Bean RadioPlayerRecordingExecutor radioPlayerUploadTaskRunner() {
        return new RadioPlayerRecordingExecutor(uploadResultRecorder());
    }
    
	@PostConstruct 
	public void scheduleTasks() {
	    RadioPlayerFeedCompiler.init(scheduleResolver, contentResolver);
		if (!radioPlayerUploadServiceDetails().isEmpty()) {
		    createHealthProbes(radioPlayerUploadServiceDetails().keySet());
	
		    if (Boolean.parseBoolean(upload)) {
				
	            scheduler.schedule(
	                    radioPlayerUploadTaskBuilder().newTask(uploadServices(), dayRangeGenerator).withName("Radioplayer Full Upload"), 
	                    UPLOAD_EVERY_TWO_HOURS);
	            scheduler.schedule(
	                    radioPlayerUploadTaskBuilder().newTask(uploadServices(), new DayRangeGenerator()).withName("Radioplayer Today Upload"), 
	                    UPLOAD_EVERY_TEN_MINUTES);
	            scheduler.schedule(
	                    new RadioPlayerRemoteProcessingChecker(radioPlayerUploadServiceDetails(), uploadResultRecorder(), log).withName("Radioplayer Remote Processing Checker"),
	                    UPLOAD_EVERY_TEN_MINUTES.withOffset(Duration.standardMinutes(5)));
	
			} else {
				log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass())
				.withDescription("Not installing Radioplayer uploader"));
			}
		}
	}

    @Bean Iterable<RadioPlayerService> uploadServices() {
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

    private void createHealthProbes(Set<String> remoteIds) {
        for (final String remoteId : remoteIds) {
            Function<RadioPlayerService, HealthProbe> createProbe = new Function<RadioPlayerService, HealthProbe>() {
                @Override
                public HealthProbe apply(RadioPlayerService service) {
                    return new RadioPlayerUploadHealthProbe(remoteId, uploadResultRecorder(), service, dayRangeGenerator);
                }
            };
            
            health.addProbes(
                    Iterables.concat(Iterables.transform(uploadServices(), createProbe),
                            ImmutableList.of(new RadioPlayerServerHealthProbe(remoteId, fileUploadResultStore())))
                    );
        }
    }
    
}
