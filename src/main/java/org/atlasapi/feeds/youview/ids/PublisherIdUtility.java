package org.atlasapi.feeds.youview.ids;

import org.atlasapi.media.entity.Item;

import com.google.common.base.Optional;


public interface PublisherIdUtility {

    String getGroupInformationServiceId();
    String getOnDemandServiceId();
    String getDeepLinkingAuthorityId();
    String getCridPrefix();
    String getImiPrefix();
    String getYouViewBaseUrl();
    Optional<String> getOtherIdentifier(Item item);
}
