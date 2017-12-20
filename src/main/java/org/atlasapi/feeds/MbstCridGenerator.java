package org.atlasapi.feeds;

import java.util.Map;

import org.atlasapi.media.entity.Identified;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import static com.metabroadcast.representative.util.Utils.encode;

public class MbstCridGenerator {

    private static final Map<String, String> CRID_PREFIX = ImmutableMap.of(
            "prod", "crid://metabroadcast.com",
            "stage", "crid://stage-metabroadcast.com"
    );

    public static String getContentCrid(String env, Identified i) {
        return getContentCrid(env, i.getId());
    }

    public static String getContentCrid(String env, Long id) {
        return getContentCrid(env, encode(id));
    }

    public static String getContentCrid(String env, String id) {
        return getCrid(env, "content", id);
    }

    public static String getCrid(String env, String type, String id) {
        return Joiner.on("/").join(CRID_PREFIX.get(env), type, id);
    }
}
