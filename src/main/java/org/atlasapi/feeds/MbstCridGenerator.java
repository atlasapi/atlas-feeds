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

    private static final String CRID_START = "crid:/";

    public static String getContentCrid(String provider, Environment env, Identified i) {
        return getContentCrid(provider, env, i.getId());
    }

    public static String getContentCrid(String provider, Environment env, Long id) {
        return getContentCrid(provider, env, encode(id));
    }

    public static String getContentCrid(String provider, Environment env, String id) {
        return getCrid(provider, env, "content", id );
    }

    public static String getCrid(String provider, Environment env, String type, String id) {
        return Joiner.on("/").join(CRID_START, provider, MBST_IDENTIFIER.get(env), type, id);
    }
}
