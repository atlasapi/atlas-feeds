package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TVAnytimeElementCreator;
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
import com.google.common.collect.ImmutableSet;


public class DefaultTvAnytimeElementCreator implements TVAnytimeElementCreator {
    
    private final ProgramInformationGenerator progInfoGenerator;
    private final GroupInformationGenerator groupInfoGenerator;
    private final OnDemandLocationGenerator onDemandGenerator;
    private final BroadcastEventGenerator broadcastGenerator;
    private final ContentHierarchyExtractor hierarchy;
    private final ContentPermit permit;
    
    // TODO this constructor is verging towards silliness
    public DefaultTvAnytimeElementCreator(ProgramInformationGenerator progInfoGenerator, 
            GroupInformationGenerator groupInfoGenerator, OnDemandLocationGenerator onDemandGenerator,
            BroadcastEventGenerator broadcastGenerator, ContentHierarchyExtractor hierarchy, 
            ContentPermit permit) {
        this.progInfoGenerator = checkNotNull(progInfoGenerator);
        this.groupInfoGenerator = checkNotNull(groupInfoGenerator);
        this.onDemandGenerator = checkNotNull(onDemandGenerator);
        this.broadcastGenerator = checkNotNull(broadcastGenerator);
        this.hierarchy = checkNotNull(hierarchy);
        this.permit = checkNotNull(permit);
    }
    
    @Override
    public ContentPermit permit() {
        return permit;
    }

    @Override
    public Iterable<GroupInformationType> createGroupInformationElementsFor(Content content) {
        if (!permit.isPermitted(content)) {
            return ImmutableSet.of();
        }
        
        if (content instanceof Film) {
            return ImmutableSet.of(groupInfoGenerator.generate((Film) content));
        }

        if (content instanceof Brand) {
            return ImmutableSet.of(groupInfoGenerator.generate((Brand) content, hierarchy.firstItemFrom((Brand) content)));
        }

        if (content instanceof Series) {
            ImmutableSet.Builder<GroupInformationType> groupInfoElems = ImmutableSet.builder();
            Series series = (Series) content;
            Optional<Brand> brand = hierarchy.brandFor(series);
            groupInfoElems.add(groupInfoGenerator.generate(series, brand, hierarchy.firstItemFrom(series)));
            if (brand.isPresent() && permit.isPermitted(brand.get())) {
                groupInfoElems.add(groupInfoGenerator.generate(brand.get(), hierarchy.firstItemFrom(brand.get())));
            }
            return groupInfoElems.build();
        }

        if (content instanceof Item) {
            ImmutableSet.Builder<GroupInformationType> groupInfoElems = ImmutableSet.builder(); 
            Item item = (Item) content;
            Optional<Series> series = hierarchy.seriesFor(item);
            Optional<Brand> brand = hierarchy.brandFor(item);

            groupInfoElems.add(groupInfoGenerator.generate(item, series, brand));
            if (series.isPresent() && permit.isPermitted(series.get())) {
                groupInfoElems.add(groupInfoGenerator.generate(series.get(), brand, hierarchy.firstItemFrom(series.get())));
            }
            if (brand.isPresent() && permit.isPermitted(brand.get())) {
                groupInfoElems.add(groupInfoGenerator.generate(brand.get(), hierarchy.firstItemFrom(brand.get())));
            }
            return groupInfoElems.build();
        }
        throw new UnexpectedContentTypeException(content);
    }

    @Override
    public Optional<ProgramInformationType> createProgramInformationElementFor(Content content) {
        if (!(content instanceof Item)) {
            return Optional.absent();
        }
        return Optional.of(progInfoGenerator.generate((Item) content));
    }

    // TODO this will need changing to allow generation of more than one ondemand for a given Item
    @Override
    public Iterable<OnDemandProgramType> createOnDemandElementsFor(Content content) {
        if (!(content instanceof Item)) {
            return ImmutableSet.of();
        }
        return onDemandGenerator.generate((Item) content).asSet();
    }

    @Override
    public Iterable<BroadcastEventType> createBroadcastEventElementsFor(Content content) {
        if (!(content instanceof Item)) {
            return ImmutableSet.of();
        }
        return broadcastGenerator.generate((Item) content);
    }
}
