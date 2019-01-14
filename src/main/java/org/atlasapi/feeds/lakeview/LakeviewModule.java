package org.atlasapi.feeds.lakeview;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.lakeview.upload.LakeviewFileUpdater;
import org.atlasapi.feeds.lakeview.validation.AzureLatestFileDownloader;
import org.atlasapi.feeds.lakeview.validation.LakeviewFileValidator;
import org.atlasapi.feeds.lakeview.validation.LakeviewServerHealthProbe;
import org.atlasapi.feeds.lakeview.validation.rules.CompletenessValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.HeirarchyValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.LakeviewFeedValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.UpToDateValidationRule;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.ResultStoringFileUploader;
import org.atlasapi.feeds.upload.azure.AzureFileUploader;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.atlasapi.feeds.upload.persistence.MongoFileUploadResultStore;
import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;

import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.persistence.mongo.DatabasedMongo;
import com.metabroadcast.common.scheduling.RepetitionRule;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LakeviewModule {

	private final static RepetitionRule LAKEVIEW_UPLOAD = RepetitionRules
			.every(Duration.standardHours(12))
			.withOffset(Duration.standardHours(4));
	private final static String SERVICE_NAME = "lakeview";
	private final static String REMOTE_ID = "azure";
	
	@Autowired
	ContentLister contentLister;
	@Autowired
	ContentResolver contentResolver;
	@Autowired
	AdapterLog log;
	@Autowired 
	SimpleScheduler scheduler;
	@Autowired 
	DatabasedMongo mongo;
	@Autowired
	ChannelResolver channelResolver;

	private @Value("${lakeview.upload.enabled}")
	String enabled;
	private @Value("${lakeview.upload.container}")
	String container;
	private @Value("${lakeview.upload.account}")
	String account;
	private @Value("${lakeview.upload.key}")
	String key;

	private @Value("${lakeview.feature.genericTitlesEnabled}")
	boolean genericTitlesEnabled;
	
    private @Value("${lakeview.feature.addXBoxOneAvailability}")
    boolean addXboxOneAvailability;
	
	private static final String SCHEMA_VERSION = "0_6";
	private static final String FILENAME_PROVIDER_ID = "CA1.Xbox4oD";

	public @Bean
	LakeviewController lakeviewController() {
		return new LakeviewController(lakeviewContentFetcher(),
				lakeviewFeedCompiler(), lakeviewFeedOutputter());
	}

	public @Bean
	LakeviewContentFetcher lakeviewContentFetcher() {
		return new LakeviewContentFetcher(contentLister, contentResolver);
	}

	public @Bean
	LakeviewFeedCompiler lakeviewFeedCompiler() {
		return new LakeviewFeedCompiler(channelResolver, genericTitlesEnabled, addXboxOneAvailability);
	}

	public @Bean
	XmlFeedOutputter lakeviewFeedOutputter() {
		return /* new ValidatingXmlFeedOutputter(lakeViewValidator(), */new SerializingFeedOutputter()/* ) */;
	}

	public @Bean
	XMLValidator lakeViewValidator() {
		try {
			return XMLValidator.forSchemas(ImmutableSet.of(Resources
					.getResource("xml.xsd").openStream(), Resources
					.getResource("Lakeview_Content_Catalog_Feed.xsd")
					.openStream()));
		} catch (Exception e) {
			log.record(new AdapterLogEntry(Severity.WARN).withDescription(
					"Couldn't load schemas for Lakeview XML validation")
					.withCause(e));
			return null;
		}
	}

	public @Bean
	HealthProbe lakeviewHealthProbe() {
		return new LakeviewServerHealthProbe(new SystemClock(), lakeviewFileValidator(), lakeviewResultStore(), REMOTE_ID);
	}

	public @Bean
	FileUploader lakeviewAzureUploader() {
		return ResultStoringFileUploader.resultStoringFileUploader(lakeviewResultStore(), SERVICE_NAME, REMOTE_ID, 
				new AzureFileUploader(account, key, container));

	}
	
	public @Bean
	AzureLatestFileDownloader azureLatestFileDownloader() {
		return new AzureLatestFileDownloader(account, key, container);
	}
	
	public @Bean 
	LakeviewFileValidator lakeviewFileValidator() {
		Clock clock = new SystemClock();
		Builder<LakeviewFeedValidationRule> validationRules = ImmutableList.builder();
		validationRules.add(new CompletenessValidationRule(contentLister, 200));
		validationRules.add(new HeirarchyValidationRule());
		validationRules.add(new UpToDateValidationRule(1, clock));
		// We need to find better candidates for this, as we don't see updates to these brands
		//validationRules.add(new RecentUpdateToBrandValidationRule("http://channel4.com/en-GB/TVSeries/deal-or-no-deal", 5, clock));
		//validationRules.add(new RecentUpdateToBrandValidationRule("http://channel4.com/en-GB/TVSeries/countdown", 5, clock));
		return new LakeviewFileValidator(lakeviewContentFetcher(), lakeviewFeedCompiler(),
				lakeviewFeedOutputter(), validationRules.build(), log);
	}
	
	public @Bean
	FileUploadResultStore lakeviewResultStore() {
		return new MongoFileUploadResultStore(mongo);
	}
	
//	@PostConstruct
//	public void scheduleTasks() {
//		if (Boolean.parseBoolean(enabled)) {
//			LakeviewFileUpdater updater = new LakeviewFileUpdater(
//					lakeviewContentFetcher(), lakeviewFeedCompiler(),
//					lakeviewFeedOutputter(), FILENAME_PROVIDER_ID,
//					SCHEMA_VERSION, lakeviewAzureUploader(), new SystemClock(),
//					log);
//			scheduler.schedule(updater.withName("Lakeview Azure updater"),
//					LAKEVIEW_UPLOAD);
//		}
//	}
}
