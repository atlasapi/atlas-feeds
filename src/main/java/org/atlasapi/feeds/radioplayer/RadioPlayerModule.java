package org.atlasapi.feeds.radioplayer;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFileUploader;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.scheduling.SimpleScheduler;

@Configuration
public class RadioPlayerModule {

	private @Autowired @Qualifier("mongoDbQueryExcutorThatFiltersUriQueries") KnownTypeQueryExecutor queryExecutor;
	
    private @Value("${rp.ftp.username}") String ftpUsername;
    private @Value("${rp.ftp.password}") String ftpPassword;
    private @Value("${rp.ftp.host}") String ftpHost;
    private @Value("${rp.ftp.host}") Integer ftpPort;
    private @Value("${rp.ftp.path}") String ftpPath;
	
	private @Autowired SimpleScheduler scheduler;
	private @Autowired AdapterLog log;

	public @Bean RadioPlayerController radioPlayerController() {
		return new RadioPlayerController(queryExecutor);
	}
	
	@PostConstruct 
	public void scheduleTasks() {
//		scheduler.schedule(new RadioPlayerFileUploader(ftpHost, ftpPort, ftpUsername, ftpPassword, ftpPath, queryExecutor, log), null);
	}
	
}
