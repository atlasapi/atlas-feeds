package org.atlasapi.feeds.lakeview;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.lakeview.upload.LakeviewFileUpdater;
import org.atlasapi.feeds.upload.azure.AzureFileUploader;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.time.SystemClock;

@Configuration
public class LakeviewModule {

    @Autowired ContentLister contentLister;
    @Autowired ContentResolver contentResolver;
    @Autowired AdapterLog log;
    @Autowired private SimpleScheduler scheduler;
    
    private @Value("${lakeview.upload.enabled}") String enabled;
    private @Value("${lakeview.upload.hostname}") String hostname;
    private @Value("${lakeview.upload.container}") String container;
    private @Value("${lakeview.upload.account}") String account;
    private @Value("${lakeview.upload.key}") String key;
   
    private static final String SCHEMA_VERSION = "0_4";
    private static final String FILENAME_PROVIDER_ID = "CA1.Xbox4oD";
    
    public @Bean LakeviewController lakeviewController() {
        return new LakeviewController(lakeviewContentFetcher(), lakeviewFeedCompiler(), lakeviewFeedOutputter());
    }

    public @Bean LakeviewContentFetcher lakeviewContentFetcher() {
        return new LakeviewContentFetcher(contentLister, contentResolver);
    }
    
    public @Bean LakeviewFeedCompiler lakeviewFeedCompiler() {
        return new LakeviewFeedCompiler();
    }

    public @Bean XmlFeedOutputter lakeviewFeedOutputter() {
        return /*new ValidatingXmlFeedOutputter(lakeViewValidator(),*/ new SerializingFeedOutputter()/*)*/;
    }

    public @Bean XMLValidator lakeViewValidator() {
        try {
            return XMLValidator.forSchemas(ImmutableSet.of(
                    Resources.getResource("xml.xsd").openStream(), 
                    Resources.getResource("Lakeview_Content_Catalog_Feed.xsd").openStream()
                ));
        } catch (Exception e) {
            log.record(new AdapterLogEntry(Severity.WARN).withDescription("Couldn't load schemas for Lakeview XML validation").withCause(e));
            return null;
        }
    }
    
    public @Bean AzureFileUploader lakeviewAzureUploader() {
    	return new AzureFileUploader(hostname, account, key, container);
    	
    }
    
    @PostConstruct
    public void scheduleTasks() {
        if(Boolean.parseBoolean(enabled)) {
        	LakeviewFileUpdater updater = new LakeviewFileUpdater(lakeviewContentFetcher(), lakeviewFeedCompiler(), lakeviewFeedOutputter(), FILENAME_PROVIDER_ID, SCHEMA_VERSION, lakeviewAzureUploader(), new SystemClock(), log);
            scheduler.schedule(updater.withName("Lakeview Azure updater"), RepetitionRules.every(Duration.standardDays(1)));
        }        
    }
}
