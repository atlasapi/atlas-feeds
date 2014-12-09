package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.granular.GranularBroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.granular.GranularOnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.granular.GranularProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.granular.GranularTvAnytimeElementCreator;
import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.ProgramInformationType;

import com.google.common.base.Optional;


public class NitroTvAnytimeElementCreator implements GranularTvAnytimeElementCreator {
    
    private final GranularProgramInformationGenerator progInfoGenerator;
    private final GroupInformationGenerator groupInfoGenerator;
    private final GranularOnDemandLocationGenerator onDemandGenerator;
    private final GranularBroadcastEventGenerator broadcastGenerator;
    private final ContentHierarchyExtractor hierarchy;
    
    public NitroTvAnytimeElementCreator(GranularProgramInformationGenerator progInfoGenerator, 
            GroupInformationGenerator groupInfoGenerator, GranularOnDemandLocationGenerator onDemandGenerator,
            GranularBroadcastEventGenerator broadcastGenerator, ContentHierarchyExtractor hierarchy) {
        this.progInfoGenerator = checkNotNull(progInfoGenerator);
        this.groupInfoGenerator = checkNotNull(groupInfoGenerator);
        this.onDemandGenerator = checkNotNull(onDemandGenerator);
        this.broadcastGenerator = checkNotNull(broadcastGenerator);
        this.hierarchy = checkNotNull(hierarchy);
    }

    @Override
    public GroupInformationType createGroupInformationElementFor(Content content) {
        if (content instanceof Film) {
            return groupInfoGenerator.generate((Film) content);
        }

        if (content instanceof Brand) {
            return groupInfoGenerator.generate((Brand) content, hierarchy.lastItemFrom((Brand) content));
        }

        if (content instanceof Series) {
            Series series = (Series) content;
            Optional<Brand> brand = hierarchy.brandFor(series);
            return groupInfoGenerator.generate(series, brand, hierarchy.lastItemFrom(series));
        }

        if (content instanceof Item) {
            Item item = (Item) content;
            Optional<Series> series = hierarchy.seriesFor(item);
            Optional<Brand> brand = hierarchy.brandFor(item);

            return groupInfoGenerator.generate(item, series, brand);
        }
        throw new UnexpectedContentTypeException(content);
    }

    @Override
    public ProgramInformationType createProgramInformationElementFor(ItemAndVersion version, String versionCrid) {
        return progInfoGenerator.generate(version, versionCrid);
    }

    @Override
    public OnDemandProgramType createOnDemandElementFor(ItemOnDemandHierarchy onDemand, String onDemandImi) {
        return onDemandGenerator.generate(onDemand, onDemandImi);
    }

    @Override
    public BroadcastEventType createBroadcastEventElementFor(ItemBroadcastHierarchy broadcast, String broadcastImi) {
        return broadcastGenerator.generate(broadcast, broadcastImi);
    }
}
