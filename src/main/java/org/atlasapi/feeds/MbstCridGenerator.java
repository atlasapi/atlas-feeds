package org.atlasapi.feeds;

import java.util.Map;

import org.atlasapi.media.entity.Identified;

import com.metabroadcast.applications.client.model.internal.Environment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import static com.metabroadcast.representative.util.Utils.encode;

public class MbstCridGenerator {

    private static final Map<Environment, String> MBST_IDENTIFIER = ImmutableMap.of(
            Environment.PROD, "metabroadcast.com",
            Environment.STAGE, "stage-metabroadcast.com"
    );

    private static final Joiner SLASH_JOINER = Joiner.on("/");

    private static final String CRID_START = "crid:/"; //<-missing a slash so we can use the Joiner.
    private static final String IMI_START = "imi:/"; //<-missing a slash so we can use the Joiner.
    private static final String CONTENT = "content";
    private static final String VERSION = "version";
    private static final String ONDEMAND = "ondemand";

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

    private Environment env;
    private String provider;

    public MbstCridGenerator(Environment env, String provider) {
        this.env = env;
        this.provider = provider;
    }

    // If the format changes, you have update the version pattern at
    // UnboxIdGenerator.getVersionCridPattern() as it's not automatically constructed.
    public String getVersionCrid(Identified baseItem ){
        return SLASH_JOINER.join(getContentCrid(baseItem), VERSION);
    }

    public String getOndemandImi(Identified baseItem, Quality quality) {
        return SLASH_JOINER.join(IMI_START, getContentBase(baseItem), ONDEMAND, quality);
    }

    public String getContentCrid(Identified i) {
        return getContentCrid(i.getId());
    }

    public String getContentCrid(Long id) {
        return getContentCrid(encode(id));
    }

    public String getContentCrid(String id) {
        return SLASH_JOINER.join(CRID_START, getContentBase(id));
    }

    private String getContentBase(Identified i) {
        return getContentBase(i.getId());
    }

    private String getContentBase(Long id) {
        return getContentBase(encode(id));
    }

    private String getContentBase(String id) {
        //e.g. crid://amazon.com/stage-metabroadcast.com/content/hhnf
        return SLASH_JOINER.join(provider, MBST_IDENTIFIER.get(env), CONTENT, id);
    }
}
