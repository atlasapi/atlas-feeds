package org.atlasapi.feeds.radioplayer;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerGenreElementCreator;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerIdGenreMap;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerTSVReadingGenreMap;
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
import org.atlasapi.reporting.telescope.FeedsReporterNames;

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
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.metabroadcast.common.time.SystemClock;
import com.metabroadcast.common.webapp.health.HealthController;

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
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.metabroadcast.common.scheduling.RepetitionRules.NEVER;
import static org.atlasapi.persistence.logging.AdapterLogEntry.infoEntry;

@Configuration
public class RadioPlayerModule {

    private static final Logger log = LoggerFactory.getLogger(RadioPlayerModule.class);

    private static final String NITRO_ID_GENRE_PREFIX = "http://nitro.bbc.co.uk/genres/";

    private static final String RP_UPLOAD_SERVICE_PREFIX = "rp.upload.";
    private static final Every UPLOAD_EVERY_FIVE_MINUTES = RepetitionRules.every(
            Duration.standardMinutes(5)
    );
    private static final Every UPLOAD_EVERY_TEN_MINUTES = RepetitionRules.every(
            Duration.standardMinutes(10)
    );
    private static final Every UPLOAD_EVERY_THIRTY_MINUTES = RepetitionRules.every(
            Duration.standardMinutes(30)
    );
    private static final Every UPLOAD_EVERY_TWO_HOURS = RepetitionRules.every(
            Duration.standardHours(2)
    );

    private static final Publisher BBC = Publisher.BBC;
    private static final Publisher NITRO = Publisher.BBC_NITRO;

    private @Value("${rp.ftp.enabled}") String ftpUpload;
    private @Value("${rp.ftp.services}") String ftpUploadServices;
    private @Value("${rp.ftp.manualUpload.enabled}") String ftpManualUpload;

    private @Value("${rp.s3.serviceId}") String s3ServiceId;
    private @Value("${rp.s3.bucket}") String s3Bucket;
    private @Value("${s3.access}") String s3AccessKey;
    private @Value("${s3.secret}") String s3Secret;
    private @Value("${rp.s3.ftp.enabled}") String s3FtpUpload;
    private @Value("${rp.s3.https.enabled}") String s3HttpsUpload;

    private @Value("${rp.https.serviceId}") String httpsServiceId;
    private @Value("${rp.https.enabled}") String httpsUpload;
    private @Value("${rp.https.services}") String httpsUploadServices;
    private @Value("${rp.https.baseUrl}") String httpsUrl;
    private @Value("${rp.https.username}") String httpsUsername;
    private @Value("${rp.https.password}") String httpsPassword;
    private @Value("${rp.https.manualUpload.enabled}") String httpsManualUpload;

    private @Autowired KnownTypeContentResolver knownTypeContentResolver;
    private @Autowired SimpleScheduler scheduler;
    private @Autowired AdapterLog adapterLog;
    private @Autowired DatabasedMongo mongo;
    private @Autowired HealthController health;
    private @Autowired ScheduleResolver scheduleResolver;
    private @Autowired ChannelResolver channelResolver;
    private @Autowired ContentResolver contentResolver;
    private @Autowired LastUpdatedContentFinder lastUpdatedContentFinder;
    private @Autowired ContentLister contentLister;

    private static DayRangeGenerator dayRangeGenerator = new DayRangeGenerator().withLookAhead(7)
            .withLookBack(7);

    // this publisher will need to change if the output controller is to display files generated from a different publisher's content.
    public @Bean RadioPlayerController radioPlayerController() {
        return new RadioPlayerController(
                lastUpdatedContentFinder,
                contentLister,
                ImmutableSet.of(BBC, NITRO)
        );
    }

    public @Bean Map<String, RemoteServiceDetails> radioPlayerUploadServiceDetails() {
        Builder<String, RemoteServiceDetails> remotes = ImmutableMap.builder();
        for (Entry<String, Parameter> property : Configurer.getParamsWithKeyMatching(Predicates.containsPattern(
                RP_UPLOAD_SERVICE_PREFIX))) {
            String serviceId = property.getKey().substring(RP_UPLOAD_SERVICE_PREFIX.length());
            String serviceConfig = property.getValue().get();
            if (!Strings.isNullOrEmpty(serviceConfig)) {
                RemoteServiceDetails details = detailsFor(
                        serviceId,
                        ImmutableList.copyOf(Splitter.on("|").split(serviceConfig))
                );
                adapterLog.record(infoEntry().withSource(getClass())
                        .withDescription("Found details for service %s: %s", serviceId, details));
                remotes.put(serviceId, details);
            } else {
                adapterLog.record(infoEntry().withSource(getClass())
                        .withDescription("Ignoring service %s: no details", serviceId));
            }
        }
        return remotes.build();
    }

    private RemoteServiceDetails detailsFor(String serviceId, ImmutableList<String> detailParts) {
        Preconditions.checkArgument(
                detailParts.size() == 3,
                "Bad details for service " + serviceId
        );
        try {
            return RemoteServiceDetails.forServer(HostSpecifier.from(detailParts.get(0)))
                    .withPort(Integer.parseInt(detailParts.get(1)))
                    .withCredentials(new UsernameAndPassword(
                            detailParts.get(2),
                            Configurer.get("rp.password." + serviceId).get()
                    )).build();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public @Bean RadioPlayerUploadServicesSupplier radioPlayerFtpUploadServices() {
        return new RadioPlayerFtpUploadServicesSupplier(
                Boolean.parseBoolean(s3FtpUpload),
                Boolean.parseBoolean(ftpUpload),
                s3ServiceId,
                s3Bucket,
                radioPlayerS3Credentials(),
                adapterLog,
                radioPlayerValidator(),
                radioPlayerUploadServiceDetails()
        );
    }

    public @Bean RadioPlayerUploadServicesSupplier radioPlayerHttpsUploadServices() {
        return new RadioPlayerHttpsUploadServicesSupplier(
                Boolean.parseBoolean(s3HttpsUpload),
                Boolean.parseBoolean(httpsUpload),
                s3ServiceId,
                s3Bucket,
                radioPlayerS3Credentials(),
                adapterLog,
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
        return new RadioPlayerHealthController(
                health,
                Sets.union(ftpRemoteServices().keySet(), httpsRemoteServices().keySet()),
                Configurer.get("rp.health.password", "").get()
        );
    }

    public @Bean RadioPlayerUploadController radioPlayerUploadController() {
        return new RadioPlayerUploadController(
                taskBuilderMap(),
                dayRangeGenerator,
                Configurer.get("rp.health.password", "").get()
        );
    }

    private Map<String, RadioPlayerUploadTaskBuilder> taskBuilderMap() {
        Builder<String, RadioPlayerUploadTaskBuilder> map = ImmutableMap.builder();
        if (Boolean.parseBoolean(ftpManualUpload)) {
            for (Entry<String, RemoteServiceDetails> ftpService : radioPlayerUploadServiceDetails().entrySet()) {
                map.put(ftpService.getKey(), radioPlayerFtpUploadTaskBuilder());
            }
        }
        if (Boolean.parseBoolean(httpsManualUpload)) {
            map.put(httpsServiceId, radioPlayerHttpsUploadTaskBuilder());
        }
        return map.build();
    }

    @Bean RadioPlayerUploadResultStore uploadResultRecorder() {
        return new CachingRadioPlayerUploadResultStore(
                Sets.union(
                        Sets.union(ftpRemoteServices().keySet(), s3RemoteServices()),
                        httpsRemoteServices().keySet()
                ),
                new UploadResultStoreBackedRadioPlayerResultStore(fileUploadResultStore())
        );
    }

    private MongoFileUploadResultStore fileUploadResultStore() {
        return new MongoFileUploadResultStore(mongo);
    }

    @Bean XMLValidator radioPlayerValidator() {
        //TODO: THe xmlvalidator has been disabled because RP website seems to be unresponsive.
        return XMLValidator.dummy();
//        try {
//            return XMLValidator.forSchemas(ImmutableSet.of(
//                    Resources.getResource("xml.xsd").openStream(),
//                    Resources.getResource("epgSI_11.xsd").openStream(),
//                    Resources.getResource("epgSchedule_11.xsd").openStream(),
//                    Resources.getResource("epgDataTypes_11.xsd").openStream(),
//                    Resources.getResource("rpDataTypes_11.xsd").openStream()
//            ));
//        } catch (Exception e) {
//            log.error("radioPlayerValidator creation failed, returning dummy", e);
//            return XMLValidator.dummy();
//        }
    }

    @Bean RadioPlayerUploadTaskBuilder radioPlayerFtpUploadTaskBuilder() {
        return new RadioPlayerUploadTaskBuilder(
                radioPlayerFtpUploadServices(),
                radioPlayerUploadTaskRunner(),
                lastUpdatedContentFinder,
                contentLister,
                BBC,
                uploadResultRecorder(),
                channelResolver
        ).withLog(adapterLog);
    }

    @Bean RadioPlayerUploadTaskBuilder radioPlayerHttpsUploadTaskBuilder() {
        return new RadioPlayerUploadTaskBuilder(
                radioPlayerHttpsUploadServices(),
                radioPlayerUploadTaskRunner(),
                lastUpdatedContentFinder,
                contentLister,
                NITRO,
                uploadResultRecorder(),
                channelResolver
        ).withLog(adapterLog);
    }

    @Bean RadioPlayerRecordingExecutor radioPlayerUploadTaskRunner() {
        return new RadioPlayerRecordingExecutor(uploadResultRecorder());
    }

    @Bean Map<String, Publisher> ftpRemoteServices() {
        Builder<String, Publisher> serviceMapping = ImmutableMap.builder();
        for (String remote : radioPlayerUploadServiceDetails().keySet()) {
            serviceMapping.put(remote, BBC);
        }
        return serviceMapping.build();
    }

    @Bean Map<String, Publisher> httpsRemoteServices() {
        return ImmutableMap.of(httpsServiceId, NITRO);
    }

    @Bean Set<String> s3RemoteServices() {
        if (Boolean.parseBoolean(s3FtpUpload) || Boolean.parseBoolean(s3HttpsUpload)) {
            return ImmutableSet.of(s3ServiceId);
        }
        return ImmutableSet.of();
    }

    @PostConstruct
    public void scheduleTasks() {
        Map<Publisher, RadioPlayerGenreElementCreator> genreCreators = ImmutableMap.of(
                Publisher.BBC, new RadioPlayerGenreElementCreator(
                        new RadioPlayerTSVReadingGenreMap(RadioPlayerTSVReadingGenreMap.GENRES_FILE)),
                Publisher.BBC_NITRO, new RadioPlayerGenreElementCreator(
                        new RadioPlayerIdGenreMap(
                                RadioPlayerIdGenreMap.GENRES_FILE,
                                NITRO_ID_GENRE_PREFIX
                        ))
        );
        RadioPlayerFeedCompiler.init(
                scheduleResolver,
                knownTypeContentResolver,
                contentResolver,
                channelResolver,
                ImmutableList.of(BBC, NITRO),
                genreCreators
        );
        if (!ftpRemoteServices().isEmpty() || !httpsRemoteServices().isEmpty()) {
            createHealthProbes(ftpRemoteServices(), ftpUploadServices());
            createHealthProbes(httpsRemoteServices(), httpsUploadServices());

            if (Boolean.parseBoolean(s3FtpUpload) || Boolean.parseBoolean(ftpUpload)) {

                scheduler.schedule(
                        radioPlayerFtpUploadTaskBuilder().newScheduledPiTask(
                                ftpUploadServices(),
                                dayRangeGenerator,
                                FeedsReporterNames.RADIO_PLAYER_AUTO_PI_UPLOADER
                        ).withName("Radioplayer PI Full Upload"),
                        UPLOAD_EVERY_TWO_HOURS
                );
                scheduler.schedule(
                        radioPlayerFtpUploadTaskBuilder().newScheduledPiTask(
                                ftpUploadServices(),
                                new DayRangeGenerator(),
                                FeedsReporterNames.RADIO_PLAYER_AUTO_PI_UPLOADER
                        ).withName("Radioplayer PI Today Upload"),
                        UPLOAD_EVERY_TEN_MINUTES
                );
                scheduler.schedule(
                        new RadioPlayerFtpRemoteProcessingChecker(
                                radioPlayerUploadServiceDetails(),
                                uploadResultRecorder(),
                                adapterLog
                        ).withName("Radioplayer Remote Processing Checker"),
                        UPLOAD_EVERY_TEN_MINUTES.withOffset(Duration.standardMinutes(5))
                );

                scheduler.schedule(
                        radioPlayerFtpUploadTaskBuilder().newScheduledOdTask(
                                ftpUploadServices(),
                                true,
                                FeedsReporterNames.RADIO_PLAYER_AUTO_OD_UPLOADER
                        ).withName("Radioplayer OD Full Upload"),
                        NEVER
                );
                scheduler.schedule(
                        radioPlayerFtpUploadTaskBuilder().newScheduledOdTask(
                                ftpUploadServices(),
                                false,
                                FeedsReporterNames.RADIO_PLAYER_AUTO_OD_UPLOADER
                        ).withName("Radioplayer OD Today Upload"),
                        UPLOAD_EVERY_TEN_MINUTES
                );
            }
            if (Boolean.parseBoolean(s3HttpsUpload) || Boolean.parseBoolean(httpsUpload)) {

                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledPiTask(
                                httpsUploadServices(),
                                dayRangeGenerator,
                                FeedsReporterNames.RADIO_PLAYER_AUTO_PI_UPLOADER
                        ).withName("Radioplayer HTTPS PI Full Upload"),
                        UPLOAD_EVERY_TWO_HOURS
                );
                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledPiTask(
                                httpsUploadServices(),
                                new DayRangeGenerator(),
                                FeedsReporterNames.RADIO_PLAYER_AUTO_PI_UPLOADER
                        ).withName("Radioplayer HTTPS PI Today Upload"),
                        UPLOAD_EVERY_THIRTY_MINUTES
                );
                scheduler.schedule(
                        new RadioPlayerHttpsRemoteProcessingChecker(
                                radioPlayerHttpClient(),
                                httpsServiceId,
                                uploadResultRecorder(),
                                adapterLog
                        ).withName("Radioplayer HTTPS Remote Processing Checker"),
                        UPLOAD_EVERY_FIVE_MINUTES.withOffset(Duration.standardMinutes(5))
                );

                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledOdTask(
                                httpsUploadServices(),
                                true,
                                FeedsReporterNames.RADIO_PLAYER_AUTO_OD_UPLOADER
                        ).withName("Radioplayer HTTPS OD Full Upload"),
                        NEVER
                );
                scheduler.schedule(
                        radioPlayerHttpsUploadTaskBuilder().newScheduledOdTask(
                                httpsUploadServices(),
                                false,
                                FeedsReporterNames.RADIO_PLAYER_AUTO_OD_UPLOADER
                        ).withName("Radioplayer HTTPS OD Today Upload"),
                        UPLOAD_EVERY_THIRTY_MINUTES
                );
            }
            if (!Boolean.parseBoolean(ftpUpload)
                    && !Boolean.parseBoolean(httpsUpload)
                    && !Boolean.parseBoolean(s3FtpUpload)
                    && !Boolean.parseBoolean(s3FtpUpload)) {
                adapterLog.record(
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
            return StreamSupport.stream(Splitter.on(',')
                    .split(uploadServices)
                    .spliterator(), false)
                    .map(RadioPlayerServices.all::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    private void createHealthProbes(Map<String, Publisher> remoteIds,
            Iterable<RadioPlayerService> radioPlayerServices) {
        final Clock clock = new SystemClock();
        for (final Entry<String, Publisher> remoteId : remoteIds.entrySet()) {
            Function<RadioPlayerService, HealthProbe> createProbe = service -> new RadioPlayerUploadHealthProbe(
                    clock,
                    remoteId.getKey(),
                    remoteId.getValue(),
                    uploadResultRecorder(),
                    service,
                    dayRangeGenerator
            );

            health.addProbes(Iterables.concat(
                    StreamSupport.stream(radioPlayerServices.spliterator(), false)
                            .map(createProbe::apply)
                            .collect(Collectors.toList()),
                    ImmutableList.of(new RadioPlayerServerHealthProbe(
                            remoteId.getKey(),
                            fileUploadResultStore()
                    ))
            ));
        }
    }

}
