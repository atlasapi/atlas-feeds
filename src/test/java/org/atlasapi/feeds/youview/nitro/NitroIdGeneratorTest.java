package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Restriction;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class NitroIdGeneratorTest {

    private static final String NITRO_PROGRAMMES_URI_PREFIX = "http://nitro.bbc.co.uk/programmes/";
    private static final String SERVICE_ID = "serviceId";
    private Clock clock = new TimeMachine();
    private HashFunction hashingFunction = Hashing.md5();
    private final NitroIdGenerator idGenerator = new NitroIdGenerator(hashingFunction);
    
    @Test
    public void testVersionCridGeneration() {
        String pid = "b012345";
        Duration duration = Duration.standardMinutes(30);
        boolean isRestricted = true;
        
        String versionCrid = idGenerator.generateVersionCrid(createItemWithPid(pid), createVersion("b98765", duration, isRestricted));
        
        String expectedId = pid + ":" + duration.getStandardSeconds() + ":" + isRestricted;
        
        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/" + hashingFunction.hashString(expectedId, Charsets.UTF_8), versionCrid);
    }

    @Test
    public void testContentCridGeneration() {
        String pid = "b012345";
        String contentCrid = idGenerator.generateContentCrid(createItemWithPid(pid));
        
        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/" + pid, contentCrid);
    }

    @Test
    public void testOnDemandImiGeneration() {
        String pid = "b012345";
        DateTime availabilityStart = clock.now();
        DateTime availabilityEnd = availabilityStart.plusDays(10);
        DateTime actualAvailabilityStart = availabilityStart.plusMinutes(5);
        String versionPid = "b98765";
        
        int horizontalSize = 1024;
        int verticalSize = 780;
        String aspectRatio = "16:9";
        
        String onDemandImi = idGenerator.generateOnDemandImi(
                createItemWithPid(pid), 
                createVersion(versionPid, Duration.standardMinutes(30), true), 
                createEncoding(horizontalSize, verticalSize, aspectRatio), 
                createLocation(availabilityStart, availabilityEnd, actualAvailabilityStart)
        );
        
        String expectedId = availabilityStart + ":"
                    + availabilityEnd + ":"
                    + "" + ":"
                    + actualAvailabilityStart + ":"
                    + versionPid + ":"
                    + horizontalSize + ":"
                    + verticalSize + ":"
                    + aspectRatio + ":"
                    + "true";
        
        assertEquals("imi:www.nitro.bbc.co.uk/" + hashingFunction.hashString(expectedId, Charsets.UTF_8), onDemandImi);
    }

    @Test
    public void testBroadcastImiGeneration() {
        String pid = "b012345";
        String contentCrid = idGenerator.generateBroadcastImi(SERVICE_ID, createBroadcastWithPid(pid));

        String expectedId = pid + ":" + SERVICE_ID;
        assertEquals("imi:www.nitro.bbc.co.uk/" + hashingFunction.hashString(expectedId, Charsets.UTF_8), contentCrid);
    }

    private Item createItemWithPid(String pid) {
        return new Film(NITRO_PROGRAMMES_URI_PREFIX + pid, "curie", Publisher.BBC_NITRO);
    }
    
    private Version createVersion(String pid, Duration duration, boolean isRestricted) {
        Version version = new Version();
        version.setCanonicalUri(NITRO_PROGRAMMES_URI_PREFIX + pid);
        
        Restriction restriction = new Restriction();
        restriction.setRestricted(isRestricted);
        
        version.setRestriction(restriction);
        version.setDuration(duration);
        
        return version;
    }
    
    private Encoding createEncoding(int horizontalSize, int verticalSize, String aspectRatio) {
        Encoding encoding = new Encoding();
        
        encoding.setVideoHorizontalSize(horizontalSize);
        encoding.setVideoVerticalSize(verticalSize);
        encoding.setVideoAspectRatio(aspectRatio);
        
        return encoding;
    }
    
    private Location createLocation(DateTime availabilityStart, DateTime availabilityEnd, DateTime actualAvailabilityStart) {
        Location location = new Location();
        
        Policy policy = new Policy();
        
        policy.setAvailabilityStart(availabilityStart);
        policy.setAvailabilityEnd(availabilityEnd);
        policy.setActualAvailabilityStart(actualAvailabilityStart);
        
        location.setPolicy(policy);
        
        return location;
    }
    
    private Broadcast createBroadcastWithPid(String pid) {
        return new Broadcast("channelUri", clock.now(), Duration.standardMinutes(30)).withId("bbc:" + pid);
    }
}
