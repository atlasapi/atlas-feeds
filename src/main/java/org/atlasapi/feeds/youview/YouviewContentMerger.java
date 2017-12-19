package org.atlasapi.feeds.youview;

import java.util.List;
import java.util.Map;

import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.ContentQueryBuilder;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.feeds.RepIdClientFactory;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

import com.metabroadcast.applications.client.model.internal.Application;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.representative.api.RepresentativeIdResponse;
import com.metabroadcast.representative.client.RepIdClientWithApp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.metabroadcast.representative.util.Utils.decode;


public class YouviewContentMerger {

    private static final Logger log = LoggerFactory.getLogger(YouviewContentMerger.class);

    private KnownTypeQueryExecutor mergingResolver;
    private final RepIdClientWithApp repIdClient;
    private final Publisher publisher;
    private final Application application;

    public YouviewContentMerger(
            KnownTypeQueryExecutor mergingResolver,
            Publisher publisher,
            Application application) {

        this.repIdClient = RepIdClientFactory.getRepIdClient(publisher);
        this.mergingResolver = mergingResolver;
        this.publisher = publisher;
        this.application = application;
    }

    public Content equivAndMerge(Content content) throws IllegalArgumentException {

        //Merge this content with equived contents.
        ContentQuery contentQuery = ContentQueryBuilder.query()
                .isAnEnumIn(
                        Attributes.DESCRIPTION_PUBLISHER,
                        ImmutableList.of(publisher)
                )
                .withSelection(Selection.all())
                .withApplication(application)
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
                     + "instead. OriginalContent={}", content);
        } else {
            mergedContent = (Content) mergedContents.get(0);
            log.warn("The output merger returned more than 1 results. This implies some of "
                     + "the equivalent content could not be merged. The first element of the "
                     + "result set was used instead. OriginalContent={}, "
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
}
