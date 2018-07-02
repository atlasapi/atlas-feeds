package org.atlasapi.feeds;

import java.util.Map;

import org.atlasapi.media.entity.Identified;

import com.metabroadcast.applications.client.model.internal.Environment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import static com.metabroadcast.representative.util.Utils.encode;

public class MbstCridGenerator {

    public static final Map<Environment, String> MBST_IDENTIFIER = ImmutableMap.of(
            Environment.PROD, "metabroadcast.com",
            Environment.STAGE, "stage-metabroadcast.com"
    );

    public static final Joiner COLON = Joiner.on(":");
    public static final Joiner SLASH = Joiner.on("/");

    public static final String CRID_START = "crid:/"; //<-missing a slash so we can use the Joiner.
    public static final String IMI_START = "imi"; //<-missing the colon so we can use the Joiner.
    public static final String CONTENT = "content";
    public static final String VERSION = "version";
    public static final String ONDEMAND = "ondemand";

    public enum Quality {
        SD,
        HD,
        UHD
    }

    public static final Map<org.atlasapi.media.entity.Quality, Quality> ATLAS_TO_YV_QUALITY_MAPPING
            = ImmutableMap.of(
            org.atlasapi.media.entity.Quality.SD, Quality.SD,
            org.atlasapi.media.entity.Quality.HD, Quality.HD,
            org.atlasapi.media.entity.Quality.FOUR_K, Quality.UHD
    );

    private String mbstIdentifier;
    private String provider;

    public MbstCridGenerator(Environment env, String provider) {
        this.mbstIdentifier = MBST_IDENTIFIER.get(env);
        this.provider = provider;
    }

    // If the format changes, you have update the version pattern at
    // AmazonIdGenerator.getVersionCridPattern() as it's not automatically constructed.
    public String getVersionCrid(Identified baseItem ){
        return COLON.join(getContentCrid(baseItem), VERSION);
    }

    public String getOndemandImi(Identified baseItem, Quality quality) {
        // imi:amazon.com/metabroadcast.com:content:dmn6std:ondemand:HD
        String namePart = COLON.join(IMI_START, mbstIdentifier);
        String datapart = COLON.join(getContentBase(baseItem), ONDEMAND, quality);
        return SLASH.join(namePart, datapart);
    }

    public String getContentCrid(Identified i) {
        return getContentCrid(i.getId());
    }

    public String getContentCrid(Long id) {
        return getContentCrid(encode(id));
    }

    public String getContentCrid(String id) {
        return SLASH.join(CRID_START, mbstIdentifier, getContentBase(id));
    }

    private String getContentBase(Identified i) {
        return getContentBase(i.getId());
    }

    private String getContentBase(Long id) {
        return getContentBase(encode(id));
    }

    private String getContentBase(String id) {
        //stage-metabroadcast.com:content:hhnf
        return COLON.join(provider, CONTENT, id);
    }
}
