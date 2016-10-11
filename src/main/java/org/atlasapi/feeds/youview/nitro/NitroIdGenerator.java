package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.hash.HashFunction;

import static com.google.common.base.Preconditions.checkNotNull;


public final class NitroIdGenerator implements IdGenerator {

    private static final String BROADCAST_SOURCE_ID_PREFIX = "bbc:";
    private static final String NITRO_PROGRAMMES_URI_PREFIX = "http://nitro.bbc.co.uk/programmes/";
    private static final Joiner JOIN_ON_COLON = Joiner.on(":").useForNull("");
    private static final String CRID_PREFIX = "crid://nitro.bbc.co.uk/iplayer/youview/";
    private static final String IMI_PREFIX = "imi:www.nitro.bbc.co.uk/";

    private final HashFunction hasher;

    public NitroIdGenerator(HashFunction hasher) {
        this.hasher = checkNotNull(hasher);
    }

    @Override
    public final String generateVersionCrid(Item item, Version version) {
        return CRID_PREFIX + hasher.hashString(generateVersionIdFor(item, version), Charsets.UTF_8);
    }

    @Override
    public String generateContentCrid(Content content) {
        return CRID_PREFIX + pidFrom(content);
    }

    @Override
    public String generateOnDemandImi(Item item, Version version, Encoding encoding, Location location) {
        return IMI_PREFIX + hasher.hashString(generateOnDemandIdFor(item, version, encoding, location), Charsets.UTF_8);
    }

    @Override
    public String generateBroadcastImi(String youViewServiceId, Broadcast broadcast) {
        return IMI_PREFIX + hasher.hashString(generateBroadcastIdFor(youViewServiceId, broadcast), Charsets.UTF_8);
    }

    @Override
    public String generateChannelCrid(Channel channel) {
        return pidFrom(channel).replace("http", "crid").replace("https", "crid");
    }

    public static String generateChannelServiceId(Channel channel) {
        return channel.getCanonicalUri();
    }

    private String generateOnDemandIdFor(Item item, Version version, Encoding encoding, Location location) {
//      scheduled_start
//      scheduled_end
//      youview_media_quality
//      ??? - hd/sd/undef - based on mediaset
//      version_id
//      horizontal_size
//      vertical_size
//      has_dubbed_audio
//      aspect_ratio
//      has_signing
        return JOIN_ON_COLON.join(
                location.getPolicy().getAvailabilityStart(),
                location.getPolicy().getAvailabilityEnd(),
                null,
                pidFrom(version),
                encoding.getVideoHorizontalSize(),
                encoding.getVideoVerticalSize(),
                // has_dubbed_audio - encoding.audio_described
                encoding.getVideoAspectRatio(),
                true
        );
    }
    
 // until id generator for b'casts takes into account yv service id not bbc service id
    private String generateBroadcastIdFor(String youViewServiceId, Broadcast broadcast) {
        return JOIN_ON_COLON.join(pidFrom(broadcast), youViewServiceId);
    }
    
    private String generateVersionIdFor(Content content, Version version) {
        return JOIN_ON_COLON.join(pidFrom(content), durationInSecsFrom(version), isRestricted(version));
    }
    
    private String pidFrom(Identified identified) {
        if (identified instanceof Broadcast) {
            Broadcast broadcast = (Broadcast) identified;
            return broadcast.getSourceId().replace(BROADCAST_SOURCE_ID_PREFIX, "");
        }
        return identified.getCanonicalUri().replace(NITRO_PROGRAMMES_URI_PREFIX, "");
    }

    private Integer durationInSecsFrom(Version version) {
        return version.getDuration();
    }

    private Boolean isRestricted(Version version) {
        return version.getRestriction() != null && Boolean.TRUE.equals(version.getRestriction().isRestricted());
    }

}
