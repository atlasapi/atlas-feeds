package org.atlasapi.feeds.youview;


public interface PublisherConfiguration {

    int getDefaultImageHeight();
    int getDefaultImageWidth();
    String getGroupInformationServiceId();
    String getOnDemandServiceId();
    String getDeepLinkingAuthorityId();
    String getCridPrefix();
    String getImiPrefix();
    String getYouViewBaseUrl();
}
