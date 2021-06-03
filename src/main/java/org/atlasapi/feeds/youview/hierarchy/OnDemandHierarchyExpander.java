package org.atlasapi.feeds.youview.hierarchy;

import java.util.Map;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;


public class OnDemandHierarchyExpander {

    private final IdGenerator idGenerator;
    
    public OnDemandHierarchyExpander(IdGenerator idGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
    }

    public Map<String, ItemOnDemandHierarchy> expandHierarchy(Item item) {
        Iterable<ItemOnDemandHierarchy> itemOndemandHierarchies = expandOnDemandHierarchyFor(item);
        
        // can't use ImmutableMap.Builder as if generated ids are non-unique, map.put will throw
        Map<String,ItemOnDemandHierarchy> onDemandHierarchies = Maps.newHashMap();
        for (ItemOnDemandHierarchy itemHierarchy : itemOndemandHierarchies) {
            onDemandHierarchies.put(
                    idGenerator.generateOnDemandImi(
                        itemHierarchy.item(), 
                        itemHierarchy.version(),
                        itemHierarchy.encoding(),
                        itemHierarchy.locations()
                    ), 
                    itemHierarchy
            );
        }
        return ImmutableMap.copyOf(onDemandHierarchies);
    }

    private Iterable<ItemOnDemandHierarchy> expandOnDemandHierarchyFor(Item item) {
        ImmutableList.Builder<ItemOnDemandHierarchy> list = ImmutableList.builder();

        for (Version version : item.getVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                //Amazon has one odemand per encoding
                if (item.getPublisher().equals(Publisher.AMAZON_UNBOX)) {
                    ItemOnDemandHierarchy ondemand =
                            createOndemandForEachEncoding(item, version, encoding);
                    if (ondemand != null) {
                        list.add(ondemand);
                    }
                } else {
                    //everything else has an ondemand per location
                    for (Location location : encoding.getAvailableAt()) {
                        ItemOnDemandHierarchy ondemand =
                                createOndemandForEachLocation(item, version, encoding, location);
                        if (ondemand != null) {
                            list.add(ondemand);
                        }
                    }
                }
            }
        }
        return list.build();
    }

    private ItemOnDemandHierarchy createOndemandForEachEncoding(
            Item item, Version version, Encoding encoding) {

        ImmutableList.Builder<Location> locationsBuilder = ImmutableList.builder();
        for (Location location : encoding.getAvailableAt()) {
            if (isYouViewIPlayerLocation(location, item)) {
                locationsBuilder.add(location);
            }
        }
        ImmutableList<Location> locations = locationsBuilder.build();

        if (!locations.isEmpty()) {
            return new ItemOnDemandHierarchy(
                    item,
                    version,
                    encoding,
                    locations
            );
        }

        return null;
    }

    private ItemOnDemandHierarchy createOndemandForEachLocation(
            Item item, Version version, Encoding encoding, Location location) {

        if (isYouViewIPlayerLocation(location, item)) {
            return new ItemOnDemandHierarchy(
                    item,
                    version,
                    encoding,
                    ImmutableList.of(location)
            );
        }
        return null;
    }

    private boolean isYouViewIPlayerLocation(Location location, Item item){
        Policy policy = location.getPolicy();
        if(item.getPublisher().equals(Publisher.AMAZON_V3)) {
            return true;
        }
        return (policy !=null && policy.getPlatform() != null &&
                (Platform.YOUVIEW_IPLAYER.equals(policy.getPlatform())
                        || Platform.YOUVIEW_AMAZON.equals(policy.getPlatform())));
    }

}
