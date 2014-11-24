package org.atlasapi.feeds.youview.unbox;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementCreator;
import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.ContentPermit;
import org.atlasapi.feeds.youview.UnexpectedContentTypeException;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


public class UnboxTvAnytimeElementCreator implements TvAnytimeElementCreator {
    
    private final ProgramInformationGenerator progInfoGenerator;
    private final GroupInformationGenerator groupInfoGenerator;
    private final OnDemandLocationGenerator onDemandGenerator;
    private final ContentHierarchyExtractor hierarchy;
    private final ContentPermit permit;
    
    // TODO this constructor is verging towards silliness
    public UnboxTvAnytimeElementCreator(ProgramInformationGenerator progInfoGenerator, 
            GroupInformationGenerator groupInfoGenerator, OnDemandLocationGenerator onDemandGenerator,
            ContentHierarchyExtractor hierarchy, 
            ContentPermit permit) {
        this.progInfoGenerator = checkNotNull(progInfoGenerator);
        this.groupInfoGenerator = checkNotNull(groupInfoGenerator);
        this.onDemandGenerator = checkNotNull(onDemandGenerator);
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
            return ImmutableSet.of(groupInfoGenerator.generate((Brand) content, hierarchy.lastItemFrom((Brand) content)));
        }

        if (content instanceof Series) {
            ImmutableSet.Builder<GroupInformationType> groupInfoElems = ImmutableSet.builder();
            Series series = (Series) content;
            Optional<Brand> brand = hierarchy.brandFor(series);
            groupInfoElems.add(groupInfoGenerator.generate(series, brand, hierarchy.lastItemFrom(series)));
            if (brand.isPresent() && permit.isPermitted(brand.get())) {
                groupInfoElems.add(groupInfoGenerator.generate(brand.get(), hierarchy.lastItemFrom(brand.get())));
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
                groupInfoElems.add(groupInfoGenerator.generate(series.get(), brand, hierarchy.lastItemFrom(series.get())));
            }
            if (brand.isPresent() && permit.isPermitted(brand.get())) {
                groupInfoElems.add(groupInfoGenerator.generate(brand.get(), hierarchy.lastItemFrom(brand.get())));
            }
            return groupInfoElems.build();
        }
        throw new UnexpectedContentTypeException(content);
    }

    @Override
    public Iterable<ProgramInformationType> createProgramInformationElementFor(Content content) {
        if (!(content instanceof Item)) {
            return ImmutableList.of();
        }
        return progInfoGenerator.generate((Item) content);
    }

    @Override
    public Iterable<OnDemandProgramType> createOnDemandElementsFor(Content content) {
        if (!(content instanceof Item)) {
            return ImmutableSet.of();
        }
        return onDemandGenerator.generate((Item) content);
    }

    /**
     * The LOVEFiLM catalogue contains no broadcast data, so this is a no-op
     */
    @Override
    public Iterable<BroadcastEventType> createBroadcastEventElementsFor(Content content) {
        return ImmutableSet.of();
    }
}
