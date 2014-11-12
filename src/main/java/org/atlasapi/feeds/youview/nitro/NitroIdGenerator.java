package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.feeds.tvanytime.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import com.google.common.base.Joiner;


public final class NitroIdGenerator implements IdGenerator {

    private static final String NITRO_PROGRAMMES_URI_PREFIX = "http://nitro.bbc.co.uk/programmes/";
    private static final Joiner JOIN_ON_COLON = Joiner.on(":").useForNull("");
    private static final String CRID_PREFIX = "crid://nitro.bbc.co.uk/iplayer/youview/";
    private static final String IMI_PREFIX = "imi:www.nitro.bbc.co.uk/";

    @Override
    public final String generateVersionCrid(Item item, Version version) {
        return CRID_PREFIX + generateVersionIdFor(item, version);
    }
    
    @Override
    public String generateContentCrid(Content content) {
        return CRID_PREFIX + pidFrom(content);
    }
    
    @Override
    public String generateOnDemandImi(Item item, Version version, Encoding encoding) {
        // TODO Auto-generated method stub
        // generated from:
//        scheduled_start
//        scheduled_end
//        youview_media_quality
//        actual_start
//        version_id
//        horizontal_size
//        vertical_size
//        has_dubbed_audio
//        aspect_ratio
//        has_signing
        return null;
    }
    
    @Override
    public String generateBroadcastImi(Broadcast broadcast) {
        // TODO Auto-generated method stub
        // generated from:
        // broadcast pid
        // service id
        return null;
    }
    
    // TODO hash this output string
    private String generateVersionIdFor(Content content, Version version) {
        return JOIN_ON_COLON.join(pidFrom(content), durationInSecsFrom(version), isRestricted(version));
    }
    
    private String pidFrom(Content content) {
        return content.getCanonicalUri().replace(NITRO_PROGRAMMES_URI_PREFIX, "");
    }

    private Integer durationInSecsFrom(Version version) {
        return version.getDuration();
    }

    private Boolean isRestricted(Version version) {
        return version.getRestriction().isRestricted();
    }

}
