package org.atlasapi.feeds.tvanytime;

import java.util.NoSuchElementException;

import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;

import com.google.common.base.Optional;
import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata._2010.ServiceInformationType;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultTvAnytimeElementCreator implements TvAnytimeElementCreator {
    
    private final ProgramInformationGenerator progInfoGenerator;
    private final GroupInformationGenerator groupInfoGenerator;
    private final OnDemandLocationGenerator onDemandGenerator;
    private final BroadcastEventGenerator broadcastGenerator;
    private final ChannelElementGenerator channelElementGenerator;
    private final MasterbrandElementGenerator masterbrandElementGenerator;
    private final ContentHierarchyExtractor hierarchy;
    
    public DefaultTvAnytimeElementCreator(ProgramInformationGenerator progInfoGenerator, 
            GroupInformationGenerator groupInfoGenerator, OnDemandLocationGenerator onDemandGenerator,
            BroadcastEventGenerator broadcastGenerator, ChannelElementGenerator channelElementGenerator,
            MasterbrandElementGenerator masterbrandElementGenerator,
            ContentHierarchyExtractor hierarchy) {
        this.progInfoGenerator = checkNotNull(progInfoGenerator);
        this.groupInfoGenerator = checkNotNull(groupInfoGenerator);
        this.onDemandGenerator = checkNotNull(onDemandGenerator);
        this.broadcastGenerator = checkNotNull(broadcastGenerator);
        this.hierarchy = checkNotNull(hierarchy);
        this.channelElementGenerator = checkNotNull(channelElementGenerator);
        this.masterbrandElementGenerator = checkNotNull(masterbrandElementGenerator);
    }

    @Override
    public GroupInformationType createGroupInformationElementFor(Content content) {
        if (content instanceof Film) {
            return groupInfoGenerator.generate((Film) content);
        }

        if (content instanceof Brand) {
            Item child = null;
            try {
                child = hierarchy.lastItemFrom((Brand) content);
            } catch (NoSuchElementException e) {
                //oh well. If there is no such element, we'll give it no such element.
            }
            return groupInfoGenerator.generate((Brand) content, child);
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

    @Override
    public ServiceInformationType createChannelElementFor(Channel channel, Channel parentChannel) {
        return channelElementGenerator.generate(channel);
    }

    @Override
    public ServiceInformationType createMasterbrandElementFor(Channel channel) {
        return masterbrandElementGenerator.generate(channel);
    }
}
