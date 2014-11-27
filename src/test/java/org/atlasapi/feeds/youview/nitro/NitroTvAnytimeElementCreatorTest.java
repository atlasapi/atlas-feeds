package org.atlasapi.feeds.youview.nitro;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.tvanytime.BroadcastEventGenerator;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.tvanytime.OnDemandLocationGenerator;
import org.atlasapi.feeds.tvanytime.ProgramInformationGenerator;
import org.atlasapi.feeds.tvanytime.TvAnytimeElementCreator;
import org.atlasapi.feeds.youview.AlwaysPermittedContentPermit;
import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.ContentPermit;
import org.atlasapi.feeds.youview.nitro.NitroTvAnytimeElementCreator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.ProgramInformationType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;


public class NitroTvAnytimeElementCreatorTest {
    
    private ProgramInformationGenerator progInfoGenerator = Mockito.mock(ProgramInformationGenerator.class);
    private GroupInformationGenerator groupInfoGenerator = Mockito.mock(GroupInformationGenerator.class);
    private OnDemandLocationGenerator onDemandGenerator = Mockito.mock(OnDemandLocationGenerator.class);
    private BroadcastEventGenerator broadcastGenerator = Mockito.mock(BroadcastEventGenerator.class);
    private ContentHierarchyExtractor contentHierarchy = Mockito.mock(ContentHierarchyExtractor.class);
    private ContentPermit permit = new AlwaysPermittedContentPermit();
    
    private final TvAnytimeElementCreator elementCreator = new NitroTvAnytimeElementCreator(
            progInfoGenerator, 
            groupInfoGenerator, 
            onDemandGenerator,
            broadcastGenerator, 
            contentHierarchy, 
            permit);

    @Test
    public void testNoOnDemandElementCreatedForNonItemContent() {
        Brand brand = createBrand("brandUri");
        
        Iterable<OnDemandProgramType> onDemandElements = elementCreator.createOnDemandElementsFor(brand);
        
        Mockito.verifyZeroInteractions(onDemandGenerator);
        assertThat(ImmutableSet.copyOf(onDemandElements), is(empty()));
    }

    @Test
    public void testOnDemandElementCreatedForItem() {
        Item item = createItem("itemUri");
        
        OnDemandProgramType onDemand = Mockito.mock(OnDemandProgramType.class);
        Mockito.when(onDemandGenerator.generate(item)).thenReturn(ImmutableSet.of(onDemand));
        
        Iterable<OnDemandProgramType> onDemandElements = elementCreator.createOnDemandElementsFor(item);
        
        Mockito.verify(onDemandGenerator).generate(item);
        assertThat(ImmutableSet.copyOf(onDemandElements), is(equalTo(ImmutableSet.of(onDemand))));
    }
    
    @Test
    public void testNoBroadcastEventElementCreatedForNonItemContent() {
        Brand brand = createBrand("brandUri");
        
        Iterable<BroadcastEventType> broadcastEvents = elementCreator.createBroadcastEventElementsFor(brand);
        
        Mockito.verifyZeroInteractions(broadcastGenerator);
        assertThat(ImmutableSet.copyOf(broadcastEvents), is(empty()));
    }

    @Test
    public void testBroadcastEventElementCreatedForItem() {
        Item item = createItem("itemUri");
        
        BroadcastEventType broadcastEvent = Mockito.mock(BroadcastEventType.class);
        Mockito.when(broadcastGenerator.generate(item)).thenReturn(ImmutableSet.of(broadcastEvent));
        
        Iterable<BroadcastEventType> onDemandElements = elementCreator.createBroadcastEventElementsFor(item);
        
        Mockito.verify(broadcastGenerator).generate(item);
        assertThat(ImmutableSet.copyOf(onDemandElements), is(equalTo(ImmutableSet.of(broadcastEvent))));
    }

    @Test
    public void testNoProgramInformationElementCreatedForNonItemContent() {
        Brand brand = createBrand("brandUri");
        
        Iterable<ProgramInformationType> progInfoElems = elementCreator.createProgramInformationElementFor(brand);
      
        Mockito.verifyZeroInteractions(progInfoGenerator);
        assertTrue("No ProgramInformationType element should be generated for a non-Item piece of content", Iterables.isEmpty(progInfoElems));
    }

    @Test
    public void testProgramInformationElementCreatedForItem() {
        Item item = createItem("itemUri");
        
        Iterable<ProgramInformationType> progInfos = ImmutableList.of(Mockito.mock(ProgramInformationType.class));
        Mockito.when(progInfoGenerator.generate(item)).thenReturn(progInfos);
        
        Iterable<ProgramInformationType> progInfoElems = elementCreator.createProgramInformationElementFor(item);
        
        Mockito.verify(progInfoGenerator).generate(item);
        assertFalse("A ProgramInformationType element should be generated any Item", Iterables.isEmpty(progInfoElems));
    }
    
    @Test
    public void testSingleGroupInformationElementCreatedForFilm() {
        Film film = createFilm("filmUri");
        
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        Mockito.when(groupInfoGenerator.generate(film)).thenReturn(groupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(film);
        
        Mockito.verify(groupInfoGenerator).generate(film);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(groupInfo))));
    }
    
    @Test
    public void testSingleGroupInformationElementCreatedForBrand() {
        Brand brand = createBrand("brandUri");
        Item childItem = createItem("childItem");
        
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        Mockito.when(contentHierarchy.lastItemFrom(brand)).thenReturn(childItem);
        
        Mockito.when(groupInfoGenerator.generate(brand, childItem)).thenReturn(groupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(brand);
        
        Mockito.verify(groupInfoGenerator).generate(brand, childItem);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(groupInfo))));
    }
    
    @Test
    public void testSingleGroupInformationElementCreatedForSeriesWithoutBrand() {
        Series series = createSeries("seriesUri");
        Item childItem = createItem("childItem");
        
        Optional<Brand> brand = Optional.<Brand>absent();
        Mockito.when(contentHierarchy.lastItemFrom(series)).thenReturn(childItem);
        Mockito.when(contentHierarchy.brandFor(series)).thenReturn(brand);
        
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        Mockito.when(groupInfoGenerator.generate(series, brand, childItem)).thenReturn(groupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(series);
        
        Mockito.verify(groupInfoGenerator).generate(series, brand, childItem);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(groupInfo))));
    }

    @Test
    public void testGroupInformationElementCreatedForBothSeriesAndBrandWhenSeriesHasBrand() {
        Brand brand = createBrand("brandUri");
        Series series = createSeries("seriesUri");
        Item childItem = createItem("childItem");
        
        Mockito.when(contentHierarchy.lastItemFrom(series)).thenReturn(childItem);
        Mockito.when(contentHierarchy.brandFor(series)).thenReturn(Optional.of(brand));
        Mockito.when(contentHierarchy.lastItemFrom(brand)).thenReturn(childItem);
        
        GroupInformationType seriesGroupInfo = Mockito.mock(GroupInformationType.class);
        GroupInformationType brandGroupInfo = Mockito.mock(GroupInformationType.class);
        
        Mockito.when(groupInfoGenerator.generate(series, Optional.of(brand), childItem)).thenReturn(seriesGroupInfo);
        Mockito.when(groupInfoGenerator.generate(brand, childItem)).thenReturn(brandGroupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(series);
        
        Mockito.verify(groupInfoGenerator).generate(series, Optional.of(brand), childItem);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(seriesGroupInfo, brandGroupInfo))));
    }
    
    @Test
    public void testSingleGroupInformationElementCreatedForItemWithoutSeriesAndBrand() {
        Item item = createItem("childItem");
        Optional<Series> series = Optional.absent();
        Optional<Brand> brand = Optional.absent();
        
        Mockito.when(contentHierarchy.brandFor(item)).thenReturn(brand);
        Mockito.when(contentHierarchy.seriesFor(item)).thenReturn(series);
        
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        
        Mockito.when(groupInfoGenerator.generate(item, series, brand)).thenReturn(groupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(item);
        
        Mockito.verify(groupInfoGenerator).generate(item, series, brand);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(groupInfo))));
    }
    
    @Test
    public void testGroupInformationElementsCreatedForBothItemAndSeriesWhenItemHasSeriesButNotBrand() {
        Item childItem = createItem("childItem");
        Series series = createSeries("seriesUri");
        Optional<Brand> brand = Optional.absent();
        
        Mockito.when(contentHierarchy.brandFor(childItem)).thenReturn(brand);
        Mockito.when(contentHierarchy.seriesFor(childItem)).thenReturn(Optional.of(series));
        Mockito.when(contentHierarchy.lastItemFrom(series)).thenReturn(childItem);
        
        GroupInformationType itemGroupInfo = Mockito.mock(GroupInformationType.class);
        GroupInformationType seriesGroupInfo = Mockito.mock(GroupInformationType.class);
        
        Mockito.when(groupInfoGenerator.generate(childItem, Optional.of(series), brand)).thenReturn(itemGroupInfo);
        Mockito.when(groupInfoGenerator.generate(series, brand, childItem)).thenReturn(seriesGroupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(childItem);
        
        Mockito.verify(groupInfoGenerator).generate(childItem, Optional.of(series), brand);
        Mockito.verify(groupInfoGenerator).generate(series, brand, childItem);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(itemGroupInfo, seriesGroupInfo))));
    }
    
    @Test
    public void testGroupInformationElementsCreatedForBothItemAndSeriesWhenItemHasBrandButNotSeries() {
        Item childItem = createItem("childItem");
        Optional<Series> series = Optional.absent();
        Brand brand = createBrand("brandUri");
        
        Mockito.when(contentHierarchy.brandFor(childItem)).thenReturn(Optional.of(brand));
        Mockito.when(contentHierarchy.seriesFor(childItem)).thenReturn(series);
        Mockito.when(contentHierarchy.lastItemFrom(brand)).thenReturn(childItem);
        
        GroupInformationType itemGroupInfo = Mockito.mock(GroupInformationType.class);
        GroupInformationType brandGroupInfo = Mockito.mock(GroupInformationType.class);
        
        Mockito.when(groupInfoGenerator.generate(childItem, series, Optional.of(brand))).thenReturn(itemGroupInfo);
        Mockito.when(groupInfoGenerator.generate(brand, childItem)).thenReturn(brandGroupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(childItem);
        
        Mockito.verify(groupInfoGenerator).generate(childItem, series, Optional.of(brand));
        Mockito.verify(groupInfoGenerator).generate(brand, childItem);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(itemGroupInfo, brandGroupInfo))));
    }
    
    @Test
    public void testGroupInformationElementsCreatedForEntireHierarchyWhenItemHasBrandAndSeries() {
        Item childItem = createItem("childItem");
        Series series = createSeries("seriesUri");
        Brand brand = createBrand("brandUri");
        
        Mockito.when(contentHierarchy.brandFor(childItem)).thenReturn(Optional.of(brand));
        Mockito.when(contentHierarchy.seriesFor(childItem)).thenReturn(Optional.of(series));
        Mockito.when(contentHierarchy.lastItemFrom(brand)).thenReturn(childItem);
        Mockito.when(contentHierarchy.lastItemFrom(series)).thenReturn(childItem);
        
        GroupInformationType itemGroupInfo = Mockito.mock(GroupInformationType.class);
        GroupInformationType seriesGroupInfo = Mockito.mock(GroupInformationType.class);
        GroupInformationType brandGroupInfo = Mockito.mock(GroupInformationType.class);
        
        Mockito.when(groupInfoGenerator.generate(childItem, Optional.of(series), Optional.of(brand))).thenReturn(itemGroupInfo);
        Mockito.when(groupInfoGenerator.generate(series, Optional.of(brand), childItem)).thenReturn(seriesGroupInfo);
        Mockito.when(groupInfoGenerator.generate(brand, childItem)).thenReturn(brandGroupInfo);

        Iterable<GroupInformationType> groupInfos = elementCreator.createGroupInformationElementsFor(childItem);
        
        Mockito.verify(groupInfoGenerator).generate(childItem, Optional.of(series), Optional.of(brand));
        Mockito.verify(groupInfoGenerator).generate(series, Optional.of(brand), childItem);
        Mockito.verify(groupInfoGenerator).generate(brand, childItem);
        assertThat(ImmutableSet.copyOf(groupInfos), is(equalTo(ImmutableSet.of(itemGroupInfo, seriesGroupInfo, brandGroupInfo))));
    }
    
    private Series createSeries(String seriesUri) {
        return new Series(seriesUri, "curie", Publisher.METABROADCAST);
    }
    
    private Film createFilm(String filmUri) {
        return new Film(filmUri, "curie", Publisher.METABROADCAST);
    }

    private Item createItem(String itemUri) {
        return new Item(itemUri, "curie", Publisher.METABROADCAST);
    }

    private Brand createBrand(String brandUri) {
        return new Brand(brandUri, "curie", Publisher.METABROADCAST);
    }

}