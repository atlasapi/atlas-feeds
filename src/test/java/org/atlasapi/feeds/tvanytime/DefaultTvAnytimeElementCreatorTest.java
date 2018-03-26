package org.atlasapi.feeds.tvanytime;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.atlasapi.feeds.youview.ContentHierarchyExtractor;
import org.atlasapi.feeds.youview.hierarchy.ItemAndVersion;
import org.atlasapi.feeds.youview.hierarchy.ItemBroadcastHierarchy;
import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.Mockito;

import tva.metadata._2010.BroadcastEventType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.OnDemandProgramType;
import tva.metadata._2010.ProgramInformationType;

import com.google.common.base.Optional;
import tva.metadata._2010.ServiceInformationType;

public class DefaultTvAnytimeElementCreatorTest {
    
    private ProgramInformationGenerator progInfoGenerator = Mockito.mock(ProgramInformationGenerator.class);
    private GroupInformationGenerator groupInfoGenerator = Mockito.mock(GroupInformationGenerator.class);
    private OnDemandLocationGenerator onDemandGenerator = Mockito.mock(OnDemandLocationGenerator.class);
    private BroadcastEventGenerator broadcastGenerator = Mockito.mock(BroadcastEventGenerator.class);
    private ContentHierarchyExtractor contentHierarchy = Mockito.mock(ContentHierarchyExtractor.class);
    private ChannelElementGenerator channelElementGenerator = Mockito.mock(ChannelElementGenerator.class);
    private MasterbrandElementGenerator masterbrandElementGenerator = Mockito.mock(MasterbrandElementGenerator.class);

    private final TvAnytimeElementCreator elementCreator = new DefaultTvAnytimeElementCreator(
            progInfoGenerator, 
            groupInfoGenerator, 
            onDemandGenerator,
            broadcastGenerator,
            channelElementGenerator,
            masterbrandElementGenerator,
            contentHierarchy
            );
    
    @Test
    public void testSingleGroupInformationElementCreatedForFilm() {
        Film film = createFilm("filmUri");
        
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        Mockito.when(groupInfoGenerator.generate(film)).thenReturn(groupInfo);

        GroupInformationType createdElem = elementCreator.createGroupInformationElementFor(film);
        
        Mockito.verify(groupInfoGenerator).generate(film);
        assertEquals(groupInfo, createdElem);
    }
    
    @Test
    public void testSingleGroupInformationElementCreatedForBrand() {
        Brand brand = createBrand("brandUri");
        Item childItem = createItem("childItem");
        
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        Mockito.when(contentHierarchy.lastItemFrom(brand)).thenReturn(childItem);
        
        Mockito.when(groupInfoGenerator.generate(brand, childItem)).thenReturn(groupInfo);

        GroupInformationType createdElem = elementCreator.createGroupInformationElementFor(brand);
        
        Mockito.verify(groupInfoGenerator).generate(brand, childItem);
        assertEquals(groupInfo, createdElem);
    }
    
    @Test
    public void testSingleGroupInformationElementCreatedForSeries() {
        Series series = createSeries("seriesUri");
        Item childItem = createItem("childItem");
        Optional<Brand> brand = Optional.<Brand>absent();
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        
        Mockito.when(contentHierarchy.lastItemFrom(series)).thenReturn(childItem);
        Mockito.when(contentHierarchy.brandFor(series)).thenReturn(brand);
        Mockito.when(groupInfoGenerator.generate(series, brand, childItem)).thenReturn(groupInfo);

        GroupInformationType createdElem = elementCreator.createGroupInformationElementFor(series);
        
        Mockito.verify(groupInfoGenerator).generate(series, brand, childItem);
        assertEquals(groupInfo, createdElem);
    }
    
    @Test
    public void testSingleGroupInformationElementCreatedForItemWithoutSeriesAndBrand() {
        Item item = createItem("childItem");
        Optional<Series> series = Optional.of(createSeries("seriesUri"));
        Optional<Brand> brand = Optional.of(createBrand("brandUri"));
        GroupInformationType groupInfo = Mockito.mock(GroupInformationType.class);
        
        Mockito.when(contentHierarchy.brandFor(item)).thenReturn(brand);
        Mockito.when(contentHierarchy.seriesFor(item)).thenReturn(series);
        Mockito.when(groupInfoGenerator.generate(item, series, brand)).thenReturn(groupInfo);

        GroupInformationType createdElem = elementCreator.createGroupInformationElementFor(item);
        
        Mockito.verify(groupInfoGenerator).generate(item, series, brand);
        assertEquals(groupInfo, createdElem);
    }
    
    @Test
    public void testProgramInformationElementCreatedForVersion() {
        Item item = createItem("childItem");
        Version version = mock(Version.class);
        
        ItemAndVersion versionHierarchy = new ItemAndVersion(item, version);
        String versionCrid = "versionCrid";
        ProgramInformationType programInfo = Mockito.mock(ProgramInformationType.class);
        
        Mockito.when(progInfoGenerator.generate(versionHierarchy, versionCrid)).thenReturn(programInfo);

        ProgramInformationType createdElem = elementCreator.createProgramInformationElementFor(versionHierarchy, versionCrid);
        
        Mockito.verify(progInfoGenerator).generate(versionHierarchy, versionCrid);
        assertEquals(programInfo, createdElem);
    }
    
    @Test
    public void testOnDemandLocationElementCreatedForOnDemand() {
        Item item = createItem("childItem");
        Version version = mock(Version.class);
        Encoding encoding = mock(Encoding.class);
        Location location = mock(Location.class);
        
        ItemOnDemandHierarchy onDemandHierarchy = new ItemOnDemandHierarchy(item, version, encoding,  ImmutableList.of(location));
        String onDemandImi = "onDemandImi";
        OnDemandProgramType onDemand = Mockito.mock(OnDemandProgramType.class);
        
        Mockito.when(onDemandGenerator.generate(onDemandHierarchy, onDemandImi)).thenReturn(onDemand);

        OnDemandProgramType createdElem = elementCreator.createOnDemandElementFor(onDemandHierarchy, onDemandImi);
        
        Mockito.verify(onDemandGenerator).generate(onDemandHierarchy, onDemandImi);
        assertEquals(onDemand, createdElem);
    }
    
    @Test
    public void testBroadcastEventElementCreatedForBroadcast() {
        Item item = createItem("childItem");
        Version version = mock(Version.class);
        Broadcast broadcast = mock(Broadcast.class);
        
        ItemBroadcastHierarchy broadcastHierarchy = new ItemBroadcastHierarchy(item, version, broadcast, "serviceId");
        String broadcastImi = "broadcastImi";
        BroadcastEventType broadcastEvent = Mockito.mock(BroadcastEventType.class);
        
        Mockito.when(broadcastGenerator.generate(broadcastHierarchy, broadcastImi)).thenReturn(broadcastEvent);

        BroadcastEventType createdElem = elementCreator.createBroadcastEventElementFor(broadcastHierarchy, broadcastImi);
        
        Mockito.verify(broadcastGenerator).generate(broadcastHierarchy, broadcastImi);
        assertEquals(broadcastEvent, createdElem);
    }

    @Test
    public void testChannelElementCreatedForChannel() {
        Channel channel = createChannel("channel");

        ServiceInformationType serviceInformationType = Mockito.mock(ServiceInformationType.class);

        Mockito.when(channelElementGenerator.generate(channel)).thenReturn(serviceInformationType);

        ServiceInformationType createdElem = elementCreator.createChannelElementFor(channel, channel);

        Mockito.verify(channelElementGenerator).generate(channel);
        assertEquals(serviceInformationType, createdElem);
    }

    private Channel createChannel(String channelUri) {
        return Channel.builder().withUri(channelUri).withBroadcaster(Publisher.METABROADCAST).build();
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
