package org.atlasapi.feeds.radioplayer;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.radioplayer.upload.FTPUploadResultRecorder;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadService;
import org.atlasapi.feeds.radioplayer.upload.MongoFTPUploadResultRecorder;
import org.atlasapi.feeds.radioplayer.upload.FTPCredentials;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadHealthProbe;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerUploadTask;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerXMLValidator;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.RepetitionRules.RepetitionInterval;
import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class RadioPlayerModule {

	private static final RepetitionInterval UPLOAD = RepetitionRules.atInterval(new Duration(5 * 60 * 60 * 1000));

	private @Autowired @Qualifier("mongoDbQueryExcutorThatFiltersUriQueries") KnownTypeQueryExecutor queryExecutor;
	
	private @Value("${rp.ftp.enabled}") String upload;
    private @Value("${rp.ftp.username}") String ftpUsername;
    private @Value("${rp.ftp.password}") String ftpPassword;
    private @Value("${rp.ftp.host}") String ftpHost;
    private @Value("${rp.ftp.port}") Integer ftpPort;
    private @Value("${rp.ftp.path}") String ftpPath;
	
	private @Autowired SimpleScheduler scheduler;
	private @Autowired AdapterLog log;
	private @Autowired DatabasedMongo mongo;

	public @Bean RadioPlayerController radioPlayerController() {
		return new RadioPlayerController(queryExecutor);
	}
	
	@PostConstruct 
	public void scheduleTasks() {
		if (Boolean.parseBoolean(upload)) {
			FTPCredentials credentials = FTPCredentials.forServer(ftpHost).withPort(ftpPort).withUsername(ftpUsername).withPassword(ftpPassword).build();
			RadioPlayerXMLValidator validator = createValidator();
			FTPUploadResultRecorder recorder = new MongoFTPUploadResultRecorder(mongo);
			FTPUploadService ftpService = new FTPUploadService(credentials, ftpPath);
			scheduler.schedule(new RadioPlayerUploadTask(queryExecutor, ftpService, RadioPlayerServices.services).withResultRecorder(recorder).withValidator(validator).withLog(log), UPLOAD);
			log.record(new AdapterLogEntry(Severity.INFO).withDescription("Radioplayer uploader scheduled task installed for:" + credentials).withSource(getClass()));
		} else {
			log.record(new AdapterLogEntry(Severity.INFO)
			.withDescription("Not installing Radioplayer uploader"));
		}
	}

	private RadioPlayerXMLValidator createValidator() {
		try {
			return RadioPlayerXMLValidator.forSchemas(ImmutableSet.of(
				Resources.getResource("epgSI_10.xsd").openStream(), 
				Resources.getResource("epgSchedule_10.xsd").openStream()
			));
		} catch (Exception e) {
			log.record(new AdapterLogEntry(Severity.WARN).withDescription("Couldn't load schemas for RadioPlayer XML validation").withCause(e));
			return null;
		}
	}
	
	@Bean HealthProbe radioPlayerProbe() {
	    return new RadioPlayerUploadHealthProbe(mongo, RadioPlayerServices.services);
	}
	
}
