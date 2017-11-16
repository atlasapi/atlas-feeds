package org.atlasapi.feeds;

import java.util.Map;

import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class MbstCridGenerator {
    private static final Map<String, String> CRID_PREFIX = ImmutableMap.of(
            "prod", "crid://metabroadcast.com/",
            "stage", "crid://stage-metabroadcast.com/"
    );

    public static String getCrid(String env, Identified i){
        com.metabroadcast.common.http.
        return CRID_PREFIX.get(env) + "/" + "type" + i.getId();
    }
}
