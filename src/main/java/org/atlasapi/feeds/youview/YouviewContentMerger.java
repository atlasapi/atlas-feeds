package org.atlasapi.feeds.youview;

import java.util.List;
import java.util.Map;

import org.atlasapi.application.query.InvalidApiKeyException;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.ContentQueryBuilder;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.feeds.RepIdClientFactory;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

import com.metabroadcast.applications.client.ApplicationsClient;
import com.metabroadcast.applications.client.ApplicationsClientImpl;
import com.metabroadcast.applications.client.model.internal.Application;
import com.metabroadcast.applications.client.model.internal.Environment;
import com.metabroadcast.applications.client.query.Query;
import com.metabroadcast.applications.client.query.Result;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.representative.api.RepresentativeIdResponse;
import com.metabroadcast.representative.client.RepIdClientWithApp;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static com.metabroadcast.representative.util.Utils.decode;

public class YouviewContentMerger {

    private static final Logger log = LoggerFactory.getLogger(YouviewContentMerger.class);

    private @Autowired @Qualifier("YouviewQueryExecutor") KnownTypeQueryExecutor mergingResolver;
    private final RepIdClientWithApp repIdClient;
    private final Publisher publisher;

    public YouviewContentMerger(Publisher publisher) {

        this.repIdClient = RepIdClientFactory.getRepIdClient(publisher);
        ;
        this.publisher = publisher;
    }

    public Content equivAndMerge(Content content) throws IllegalArgumentException {

        RepIdClientWithApp repIdClient = RepIdClientFactory.getRepIdClient(publisher);

        //Merge this content with equived contents.
        ContentQuery contentQuery = ContentQueryBuilder.query()
                .isAnEnumIn(
                        Attributes.DESCRIPTION_PUBLISHER,
                        ImmutableList.of(publisher)
                )
                .withSelection(Selection.all())
                .withApplication(getApplication())
                .build();

        Map<String, List<Identified>> mergedResults =
                mergingResolver.executeUriQuery(
                        ImmutableSet.of(content.getCanonicalUri()),
                        contentQuery
                );

        Content mergedContent;

        List<Identified> mergedContents = mergedResults.entrySet().iterator().next().getValue();
        if (mergedContents.size() == 1) {
            mergedContent = (Content) mergedContents.get(0);
        } else if (mergedContents.isEmpty()) {
            mergedContent = content;
            log.warn("The output merger returned no items. The original content was used "
                     + "instead.OriginalContent={}", content);
        } else {
            mergedContent = (Content) mergedContents.get(0);
            log.warn("The output merger returned more than 1 results. This implies some of "
                     + "the equivalent content could not be merged. OriginalContent={}, "
                     + "ResultOfMerge={}"
                    , content.getCanonicalUri()
                    , mergedContents);
        }

        //Update the existing content ID with a representative ID. Will throw IAE if not found.
        RepresentativeIdResponse repIdResponse = repIdClient.getRepId(mergedContent.getId());

        log.info(
                "{} swapped {} for repId {}",
                (mergedContent.getId()) == (decode(repIdResponse.getRepresentative().getId())),
                mergedContent.getId(),
                decode(repIdResponse.getRepresentative().getId())
        );
        mergedContent.setId(decode(repIdResponse.getRepresentative().getId()));
        return mergedContent;
    }


    protected Application getApplication(){
        //TODO:make this configurable.
        // Configurer.get("applications.client.host").get()
        // Configurer.get("applications.client.env").get()
        ApplicationsClient applicationsClient = ApplicationsClientImpl.create("http://applications-service.production.svc.cluster.local", new MetricRegistry());
        java.util.Optional<Application> application;
        try {
            Result result =
                    applicationsClient.resolve(Query.create(getEquivApiKey(), Environment.PROD));
            if (result.getErrorCode().isPresent()) {
                throw InvalidApiKeyException.create(getEquivApiKey(), result.getErrorCode().get());
            } else {
                application = result.getSingleResult();
            }

            if (!application.isPresent()) {
                throw new IllegalArgumentException("No application found for API key=" + getEquivApiKey());
            }

        } catch (InvalidApiKeyException | IllegalArgumentException e) {
            log.error("There was a problem with the API key.", e);
            application = java.util.Optional.empty();
        }

        return application.get();
    }

    private String getEquivApiKey(){
        return PerPublisherConfig.TO_API_KEY_MAP.get(this.publisher);
    }
}
