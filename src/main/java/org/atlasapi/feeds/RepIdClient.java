package org.atlasapi.feeds;

import java.util.Map;

import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;

public class RepIdClient {

    public static RepIdClient getRepIdClient(Publisher publisher){
        Map<Publisher, String > publisherToAppIdMap = ImmutableMap.of(
                Publisher.BBC_NITRO, "nitroAppId",
                Publisher.AMAZON_UNBOX, "UnboxAppId"
        );
        return RepIdClient.getRepIdClient(publisherToAppIdMap.get(publisher));
    }

    public static RepIdClient getRepIdClient(String appId){
        return new RepIdClient();
    }

    public Long getDecoded(Long id) {
        return id;
    }
}
