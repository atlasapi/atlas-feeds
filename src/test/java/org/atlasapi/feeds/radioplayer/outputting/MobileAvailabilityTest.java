package org.atlasapi.feeds.radioplayer.outputting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import nu.xom.Element;
import nu.xom.Elements;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Network;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;

public class MobileAvailabilityTest {
    Clock clock = new TimeMachine();
    
    @Test
    public void testMobileAvailabilityNoPlatform() {
        RadioPlayerBroadcastItem broadcastItem = buildRadioPlayerBroadcastItem();
        broadcastItem.getVersion().setCanonicalUri("canonicalUri");
        List<Location> locations = Lists.newArrayList(buildNoPlatformLocation());
        Encoding encoding = new Encoding();
        for (Location location : locations) {
            encoding.addAvailableAt(location);
        }
        broadcastItem.getVersion().addManifestedAs(encoding);
        
        RadioPlayerProgrammeInformationOutputter outputter = new RadioPlayerProgrammeInformationOutputter();
        Element ondemandElement = outputter.ondemandElement(broadcastItem, locations, new RadioPlayerService(342, "radio2"));
        // test whether audiostreamgroup is available (it should be)
        assertTrue(hasAudioStreamGroup(ondemandElement));
    }
    
    @Test
    public void testMobileAvailabilityXBox() {
        RadioPlayerBroadcastItem broadcastItem = buildRadioPlayerBroadcastItem();
        broadcastItem.getVersion().setCanonicalUri("canonicalUri");
        List<Location> locations = Lists.newArrayList(buildXBoxLocation());
        Encoding encoding = new Encoding();
        for (Location location : locations) {
            encoding.addAvailableAt(location);
        }
        broadcastItem.getVersion().addManifestedAs(encoding);
        
        RadioPlayerProgrammeInformationOutputter outputter = new RadioPlayerProgrammeInformationOutputter();
        Element ondemandElement = outputter.ondemandElement(broadcastItem, locations, new RadioPlayerService(342, "radio2"));
        // test whether audiostreamgroup is available (it should be)
        assertTrue(hasAudioStreamGroup(ondemandElement));
    }
    
    @Test
    public void testMobileAvailabilityPCWithNoIOSLocations() {
        RadioPlayerBroadcastItem broadcastItem = buildRadioPlayerBroadcastItem();
        broadcastItem.getVersion().setCanonicalUri("canonicalUri");
        List<Location> locations = Lists.newArrayList(buildPCLocation());
        Encoding encoding = new Encoding();
        for (Location location : locations) {
            encoding.addAvailableAt(location);
        }
        broadcastItem.getVersion().addManifestedAs(encoding);
        
        RadioPlayerProgrammeInformationOutputter outputter = new RadioPlayerProgrammeInformationOutputter();
        Element ondemandElement = outputter.ondemandElement(broadcastItem, locations, new RadioPlayerService(342, "radio2"));
        // test whether audiostreamgroup is available (it shouldn't be)
        assertFalse(hasAudioStreamGroup(ondemandElement));
    }
    
    @Test
    public void testMobileAvailabilityPCWithIOSLocations() {
        RadioPlayerBroadcastItem broadcastItem = buildRadioPlayerBroadcastItem();
        broadcastItem.getVersion().setCanonicalUri("canonicalUri");
        List<Location> locations = Lists.newArrayList(buildPCLocation(), buildIOS3GLocation(false), buildIOSWifiLocation(false));
        Encoding encoding = new Encoding();
        for (Location location : locations) {
            encoding.addAvailableAt(location);
        }
        broadcastItem.getVersion().addManifestedAs(encoding);
        
        RadioPlayerProgrammeInformationOutputter outputter = new RadioPlayerProgrammeInformationOutputter();
        Element ondemandElement = outputter.ondemandElement(broadcastItem, locations, new RadioPlayerService(342, "radio2"));
        // test whether audiostreamgroup is available (it shouldn't be)
        assertFalse(hasAudioStreamGroup(ondemandElement));
    }
    
    @Test
    public void testMobileAvailabilityPCWithIOSLocationsActualAvailabilitySet() {
        RadioPlayerBroadcastItem broadcastItem = buildRadioPlayerBroadcastItem();
        broadcastItem.getVersion().setCanonicalUri("canonicalUri");
        List<Location> locations = Lists.newArrayList(buildPCLocation(), buildIOS3GLocation(true), buildIOSWifiLocation(true));
        Encoding encoding = new Encoding();
        for (Location location : locations) {
            encoding.addAvailableAt(location);
        }
        broadcastItem.getVersion().addManifestedAs(encoding);
        
        RadioPlayerProgrammeInformationOutputter outputter = new RadioPlayerProgrammeInformationOutputter();
        Element ondemandElement = outputter.ondemandElement(broadcastItem, locations, new RadioPlayerService(342, "radio2"));
        // test whether audiostreamgroup is available (it should be)
        assertTrue(hasAudioStreamGroup(ondemandElement));
    }
    
    public boolean hasAudioStreamGroup(Element ondemandElement) {
        Elements elements = ondemandElement.getChildElements();
        for (int i = 0; i < ondemandElement.getChildCount(); i++) {
            Element child = elements.get(i);
            if (child.getLocalName().equals("audioStreamGroup")) {
                return true;
            }
        }
        return false;
    }
    
    private Location buildXBoxLocation() {
    Policy policy = new Policy();
        policy.setPlatform(Platform.XBOX);
        applyToPolicy(policy);
        Location location = new Location();
        location.setPolicy(policy);
        return location;
    }
    
    private Location buildNoPlatformLocation() {
        Policy policy = new Policy();
        applyToPolicy(policy);
        Location location = new Location();
        location.setPolicy(policy);
        return location;
    }
    
    private Location buildIOSWifiLocation(boolean isActuallyAvailable) {
    Policy policy = new Policy();
        policy.setNetwork(Network.WIFI);
        policy.setPlatform(Platform.IOS);
        if (isActuallyAvailable) {
            policy.setActualAvailabilityStart(clock.now().minusDays(1));
        }
        applyToPolicy(policy);
        Location location = new Location();
        location.setPolicy(policy);
        return location;
    }
    
    private Location buildIOS3GLocation(boolean isActuallyAvailable) {
    Policy policy = new Policy();
        policy.setNetwork(Network.THREE_G);
        policy.setPlatform(Platform.IOS);
        if (isActuallyAvailable) {
            policy.setActualAvailabilityStart(clock.now().minusDays(1));
        }
        applyToPolicy(policy);
        Location location = new Location();
        location.setPolicy(policy);
        return location;
    }
    
    private Location buildPCLocation() {
    Policy policy = new Policy();
        policy.setPlatform(Platform.PC);
        applyToPolicy(policy);
        Location location = new Location();
        location.setPolicy(policy);
        return location;
    }
    
    private void applyToPolicy(Policy policy) {
        policy.setAvailabilityStart(clock.now().minusDays(1));
        policy.setAvailabilityEnd(clock.now().plusDays(7));
    }

    private RadioPlayerBroadcastItem buildRadioPlayerBroadcastItem() { 
        Episode item = RadioPlayerProgrammeInformationOutputterTest.buildItem();
        Version version = new Version();
        
        Broadcast broadcast = new Broadcast("on", clock.now(), Duration.standardHours(1));
        return new RadioPlayerBroadcastItem(item, version, broadcast);
    }
}
